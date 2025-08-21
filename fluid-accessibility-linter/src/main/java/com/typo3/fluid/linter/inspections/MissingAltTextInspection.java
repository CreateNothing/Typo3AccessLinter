package com.typo3.fluid.linter.inspections;

import com.intellij.codeInspection.*;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.xml.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MissingAltTextInspection extends LocalInspectionTool {
    private static final String DESCRIPTION = "Image missing alt attribute for accessibility";
    private static final Pattern IMG_TAG_PATTERN = Pattern.compile("<img\\s+[^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern F_IMAGE_PATTERN = Pattern.compile("<f:image\\s+[^>]*/>", Pattern.CASE_INSENSITIVE);
    private static final Pattern ALT_ATTR_PATTERN = Pattern.compile("\\salt\\s*=\\s*[\"'][^\"']*[\"']", Pattern.CASE_INSENSITIVE);
    
    @NotNull
    @Override
    public String getDisplayName() {
        return "Missing alt text on images";
    }
    
    @NotNull
    @Override
    public String getShortName() {
        return "MissingAltText";
    }
    
    @NotNull
    @Override
    public String getGroupDisplayName() {
        return "Fluid Accessibility";
    }
    
    @Override
    public boolean isEnabledByDefault() {
        return true;
    }
    
    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new PsiElementVisitor() {
            @Override
            public void visitFile(@NotNull PsiFile file) {
                if (!file.getName().endsWith(".html")) {
                    return;
                }
                
                String content = file.getText();
                checkImagesForAltText(content, file, holder);
            }
        };
    }
    
    private void checkImagesForAltText(String content, PsiFile file, ProblemsHolder holder) {
        // Check regular HTML img tags
        Matcher imgMatcher = IMG_TAG_PATTERN.matcher(content);
        while (imgMatcher.find()) {
            String imgTag = imgMatcher.group();
            if (!ALT_ATTR_PATTERN.matcher(imgTag).find()) {
                int start = imgMatcher.start();
                int end = imgMatcher.end();
                
                // Find the element that covers the full tag
                PsiElement startElement = file.findElementAt(start);
                if (startElement != null) {
                    PsiElement targetElement = findElementForRange(startElement, start, end);
                    holder.registerProblem(
                        targetElement,
                        DESCRIPTION,
                        ProblemHighlightType.WARNING,
                        new AddAltAttributeQuickFix()
                    );
                }
            }
        }
        
        // Check Fluid f:image ViewHelpers
        Matcher fImageMatcher = F_IMAGE_PATTERN.matcher(content);
        while (fImageMatcher.find()) {
            String fImageTag = fImageMatcher.group();
            if (!ALT_ATTR_PATTERN.matcher(fImageTag).find()) {
                int start = fImageMatcher.start();
                int end = fImageMatcher.end();
                
                // Find the element that covers the full tag
                PsiElement startElement = file.findElementAt(start);
                if (startElement != null) {
                    PsiElement targetElement = findElementForRange(startElement, start, end);
                    holder.registerProblem(
                        targetElement,
                        "Fluid image ViewHelper missing alt attribute",
                        ProblemHighlightType.WARNING,
                        new AddAltAttributeQuickFix()
                    );
                }
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
    
    private static class AddAltAttributeQuickFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getName() {
            return "Add alt attribute";
        }
        
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add accessibility attribute";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            PsiElement element = descriptor.getPsiElement();
            if (element == null) return;
            
            PsiFile file = element.getContainingFile();
            if (file == null) return;
            
            // Find the tag containing this element
            String text = element.getText();
            int startOffset = element.getTextOffset();
            
            // Simple implementation: add alt="" before the closing >
            String fileText = file.getText();
            int tagEnd = fileText.indexOf('>', startOffset);
            if (tagEnd != -1) {
                String beforeTag = fileText.substring(0, tagEnd);
                String afterTag = fileText.substring(tagEnd);
                
                // Check if it's self-closing
                if (beforeTag.endsWith("/")) {
                    beforeTag = beforeTag.substring(0, beforeTag.length() - 1) + " alt=\"\" /";
                } else {
                    beforeTag = beforeTag + " alt=\"\"";
                }
                
                String newContent = beforeTag + afterTag;
                
                PsiFile newFile = PsiFileFactory.getInstance(project)
                    .createFileFromText(file.getName(), file.getFileType(), newContent);
                file.getNode().replaceAllChildrenToChildrenOf(newFile.getNode());
            }
        }
    }
}