package com.typo3.fluid.linter.inspections;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Arrays;
import java.util.List;

/**
 * Base class for all Fluid accessibility inspections.
 * Provides common functionality for pattern matching and Fluid ViewHelper detection.
 */
public abstract class FluidAccessibilityInspection extends LocalInspectionTool {
    
    // Common Fluid control flow ViewHelpers that are allowed as direct children of certain elements
    protected static final List<String> CONTROL_FLOW_VIEWHELPERS = Arrays.asList(
        "f:for", "f:if", "f:else", "f:then", "f:switch", "f:case", "f:defaultCase",
        "f:groupedFor", "f:cycle", "f:variable", "f:alias", "f:comment", "f:spaceless",
        "f:cObject", "f:debug", "f:render", "f:section", "f:layout"
    );
    
    /**
     * Check if a tag is a Fluid ViewHelper
     */
    protected boolean isFluidViewHelper(String tag) {
        return tag != null && (tag.startsWith("f:") || tag.contains(":"));
    }
    
    /**
     * Check if a tag is a control flow ViewHelper that doesn't generate HTML
     */
    protected boolean isControlFlowViewHelper(String tag) {
        if (tag == null) return false;
        
        // Remove closing slash and attributes
        String cleanTag = tag.replaceAll("[/>]", "").trim().split("\\s+")[0];
        
        return CONTROL_FLOW_VIEWHELPERS.contains(cleanTag);
    }
    
    /**
     * Extract tag name from HTML/Fluid tag string
     */
    protected String extractTagName(String tagString) {
        if (tagString == null) return null;
        
        // Remove < and > brackets
        String cleaned = tagString.replaceAll("[<>]", "");
        
        // Remove closing slash for self-closing tags
        cleaned = cleaned.replaceAll("^/", "");
        
        // Get first word (tag name)
        String[] parts = cleaned.trim().split("\\s+");
        return parts.length > 0 ? parts[0].replaceAll("/", "") : null;
    }
    
    /**
     * Check if a string contains an attribute
     */
    protected boolean hasAttribute(String tagString, String attributeName) {
        if (tagString == null || attributeName == null) return false;
        
        // Check for attribute with or without value
        String pattern = "\\b" + Pattern.quote(attributeName) + "\\s*(?:=|\\s|>|/)";
        return Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(tagString).find();
    }
    
    /**
     * Get attribute value from tag string
     */
    protected String getAttributeValue(String tagString, String attributeName) {
        if (tagString == null || attributeName == null) return null;
        
        // Match attribute="value" or attribute='value'
        String pattern = "\\b" + Pattern.quote(attributeName) + "\\s*=\\s*[\"']([^\"']*)[\"']";
        Pattern p = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(tagString);
        
        return m.find() ? m.group(1) : null;
    }
    
    /**
     * Check if a file is a Fluid template
     */
    protected boolean isFluidTemplate(PsiFile file) {
        if (file == null) return false;
        
        String fileName = file.getName().toLowerCase();
        if (!fileName.endsWith(".html")) return false;
        
        // Check for Fluid namespace declaration or ViewHelper usage
        String content = file.getText();
        return content.contains("xmlns:f=") || 
               content.contains("<f:") || 
               content.matches(".*<\\w+:\\w+.*");
    }
    
    /**
     * Calculate line number from string offset
     */
    protected int getLineNumber(String text, int offset) {
        if (text == null || offset < 0 || offset > text.length()) return 1;
        
        int lineNumber = 1;
        for (int i = 0; i < offset && i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                lineNumber++;
            }
        }
        return lineNumber;
    }
    
    /**
     * Calculate column number from string offset
     */
    protected int getColumnNumber(String text, int offset) {
        if (text == null || offset < 0 || offset > text.length()) return 1;
        
        int columnNumber = 1;
        for (int i = 0; i < offset && i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                columnNumber = 1;
            } else {
                columnNumber++;
            }
        }
        return columnNumber;
    }
    
    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new PsiElementVisitor() {
            @Override
            public void visitFile(@NotNull PsiFile file) {
                if (shouldInspectFile(file)) {
                    inspectFile(file, holder);
                }
            }
        };
    }
    
    /**
     * Override to determine if a file should be inspected
     */
    protected boolean shouldInspectFile(PsiFile file) {
        return file.getName().toLowerCase().endsWith(".html");
    }
    
    /**
     * Find the start position of an HTML element containing the given position
     */
    protected int findElementStart(String content, int position) {
        if (position <= 0) return 0;
        
        // Look backwards to find the start of the element
        int i = position;
        while (i > 0 && content.charAt(i) != '<') {
            i--;
        }
        return i;
    }
    
    /**
     * Find the end position of an HTML element starting at the given position
     */
    protected int findElementEnd(String content, int start) {
        if (start < 0 || start >= content.length()) return content.length();
        
        int depth = 0;
        int i = start;
        boolean inTag = false;
        boolean inQuote = false;
        char quoteChar = ' ';
        String tagName = null;
        
        while (i < content.length()) {
            char c = content.charAt(i);
            
            if (!inQuote && (c == '"' || c == '\'')) {
                inQuote = true;
                quoteChar = c;
            } else if (inQuote && c == quoteChar) {
                inQuote = false;
            } else if (!inQuote) {
                if (c == '<') {
                    inTag = true;
                    // Extract tag name
                    if (tagName == null && i == start) {
                        int tagEnd = content.indexOf('>', i);
                        int spaceEnd = content.indexOf(' ', i);
                        int actualEnd = (spaceEnd > 0 && spaceEnd < tagEnd) ? spaceEnd : tagEnd;
                        if (actualEnd > i) {
                            tagName = content.substring(i + 1, actualEnd).toLowerCase();
                        }
                    }
                    
                    // Check for closing tag
                    if (i + 1 < content.length() && content.charAt(i + 1) == '/') {
                        depth--;
                        if (depth < 0 && tagName != null) {
                            // Check if this is the matching closing tag
                            String closingTag = content.substring(i + 2, Math.min(i + 2 + tagName.length(), content.length()));
                            if (closingTag.equalsIgnoreCase(tagName)) {
                                int closingEnd = content.indexOf('>', i);
                                return closingEnd > 0 ? closingEnd + 1 : i;
                            }
                        }
                    } else {
                        depth++;
                    }
                } else if (c == '>' && inTag) {
                    inTag = false;
                    if (i > 0 && content.charAt(i - 1) == '/') {
                        depth--; // Self-closing tag
                    }
                    if (depth == 0 && i > start) {
                        return i + 1;
                    }
                }
            }
            i++;
        }
        
        return content.length();
    }
    
    /**
     * Register a problem with the given parameters
     */
    protected void registerProblem(ProblemsHolder holder, PsiFile file, int start, int end,
                                  String message, LocalQuickFix fix) {
        // Find the starting element
        PsiElement startElement = file.findElementAt(start);
        if (startElement != null) {
            // Try to find a parent element that encompasses the full range
            PsiElement targetElement = findElementForRange(startElement, start, end);
            
            if (fix != null) {
                holder.registerProblem(targetElement, message, fix);
            } else {
                holder.registerProblem(targetElement, message);
            }
        }
    }
    
    /**
     * Register a problem with the given parameters and highlight type
     */
    protected void registerProblem(ProblemsHolder holder, PsiFile file, int start, int end,
                                  String message, ProblemHighlightType highlightType, LocalQuickFix fix) {
        // Find the starting element
        PsiElement startElement = file.findElementAt(start);
        if (startElement != null) {
            // Try to find a parent element that encompasses the full range
            PsiElement targetElement = findElementForRange(startElement, start, end);
            
            if (fix != null) {
                holder.registerProblem(targetElement, message, highlightType, fix);
            } else {
                holder.registerProblem(targetElement, message, highlightType);
            }
        }
    }
    
    /**
     * Find the best element that covers the given range
     */
    private PsiElement findElementForRange(PsiElement startElement, int start, int end) {
        PsiElement current = startElement;
        PsiElement best = startElement;
        
        // Walk up the tree to find an element that better represents the range
        while (current != null) {
            int elementStart = current.getTextRange().getStartOffset();
            int elementEnd = current.getTextRange().getEndOffset();
            
            // If this element fully contains our range and is closer to it, use it
            if (elementStart <= start && elementEnd >= end) {
                // Check if it's a better match (closer to our target range)
                if (Math.abs(elementEnd - elementStart - (end - start)) < 
                    Math.abs(best.getTextRange().getEndOffset() - best.getTextRange().getStartOffset() - (end - start))) {
                    best = current;
                }
            }
            
            // Don't go beyond the file level
            if (current instanceof PsiFile) {
                break;
            }
            
            current = current.getParent();
        }
        
        return best;
    }
    
    
    /**
     * Override to implement the actual inspection logic
     */
    protected abstract void inspectFile(@NotNull PsiFile file, @NotNull ProblemsHolder holder);
}