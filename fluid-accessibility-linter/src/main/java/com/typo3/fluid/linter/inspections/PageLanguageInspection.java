package com.typo3.fluid.linter.inspections;

import com.intellij.codeInspection.*;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.typo3.fluid.linter.utils.AccessibilityUtils;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Inspection to check language attributes on HTML elements:
 * - Missing lang attribute on <html> element (WCAG 3.1.1 Level A)
 * - Invalid language codes
 * - Language changes within document (lang attributes on other elements)
 * - XML compatibility (xml:lang should match lang)
 */
public class PageLanguageInspection extends FluidAccessibilityInspection {
    
    // Pattern to find the html element
    protected static final Pattern HTML_TAG_PATTERN = Pattern.compile(
        "<html\\s*[^>]*>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    
    // Pattern to find any element with lang attribute
    protected static final Pattern LANG_ATTR_PATTERN = Pattern.compile(
        "<(\\w+)\\s+[^>]*\\blang\\s*=\\s*[\"']([^\"']*)[\"'][^>]*>",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    
    // Pattern to find xml:lang attributes
    protected static final Pattern XML_LANG_ATTR_PATTERN = Pattern.compile(
        "\\bxml:lang\\s*=\\s*[\"']([^\"']*)[\"']",
        Pattern.CASE_INSENSITIVE
    );
    
    @NotNull
    @Override
    public String getDisplayName() {
        return "Page language attributes";
    }
    
    @NotNull
    @Override
    public String getShortName() {
        return "PageLanguage";
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
    
    @Override
    protected void inspectFile(@NotNull PsiFile file, @NotNull ProblemsHolder holder) {
        String content = file.getText();
        
        // Skip lang attribute checks for Fluid Layout/Partial files
        if (isFluidLayoutOrPartial(file, content)) {
            return;
        }
        
        // Check HTML element for lang attribute
        checkHtmlElementLanguage(content, file, holder);
        
        // Check all lang attributes for valid codes
        checkLanguageCodeValidity(content, file, holder);
        
        // Check XML compatibility
        checkXmlLangCompatibility(content, file, holder);
    }
    
    /**
     * Determines if this is a Fluid Layout or Partial file that shouldn't require lang attributes
     */
    private boolean isFluidLayoutOrPartial(PsiFile file, String content) {
        String filePath = file.getVirtualFile() != null ? file.getVirtualFile().getPath() : "";
        
        // Check if file is in Layouts/ or Partials/ directory
        if (filePath.contains("/Layouts/") || filePath.contains("/Partials/")) {
            return true;
        }
        
        // Check for Fluid-specific patterns that indicate this is a fragment, not a full HTML document
        // Look for f:render or f:section tags which are typical in Layouts/Partials
        if (content.contains("<f:render") || content.contains("<f:section")) {
            // Check if html tag exists and has xmlns:f attribute (namespace declaration only)
            Pattern htmlWithNamespace = Pattern.compile("<html[^>]*xmlns:f=[^>]*>", Pattern.CASE_INSENSITIVE);
            if (htmlWithNamespace.matcher(content).find()) {
                // If html tag only has Fluid namespace and no DOCTYPE, it's likely a Layout/Partial
                if (!content.contains("<!DOCTYPE") && !content.contains("<head>") && !content.contains("<body>")) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    private void checkHtmlElementLanguage(String content, PsiFile file, ProblemsHolder holder) {
        Matcher htmlMatcher = HTML_TAG_PATTERN.matcher(content);
        
        if (htmlMatcher.find()) {
            String htmlTag = htmlMatcher.group();
            int offset = htmlMatcher.start();
            
            // Check for lang attribute
            if (!hasAttribute(htmlTag, "lang")) {
                registerProblem(file, holder, offset,
                    "Missing lang attribute on <html> element (WCAG 3.1.1 Level A)",
                    ProblemHighlightType.ERROR,
                    new AddLangAttributeQuickFix());
            } else {
                // Check if lang value is empty
                String langValue = getAttributeValue(htmlTag, "lang");
                if (AccessibilityUtils.isEmptyOrWhitespace(langValue)) {
                    registerProblem(file, holder, offset,
                        "Lang attribute is empty - must specify a valid language code",
                        ProblemHighlightType.ERROR,
                        new SetLangValueQuickFix("en"));
                } else if (!AccessibilityUtils.isValidLanguageCode(langValue)) {
                    registerProblem(file, holder, offset,
                        String.format("Invalid language code '%s' - use BCP 47 format (e.g., 'en', 'en-US', 'de')",
                            langValue),
                        ProblemHighlightType.ERROR,
                        new SetLangValueQuickFix("en"));
                }
            }
            
            // Check for xml:lang attribute
            if (hasAttribute(htmlTag, "xml:lang")) {
                String xmlLangValue = getAttributeValue(htmlTag, "xml:lang");
                String langValue = getAttributeValue(htmlTag, "lang");
                
                if (langValue != null && xmlLangValue != null && !langValue.equals(xmlLangValue)) {
                    registerProblem(file, holder, offset,
                        String.format("xml:lang='%s' doesn't match lang='%s' - they should be identical",
                            xmlLangValue, langValue),
                        ProblemHighlightType.WARNING,
                        new SyncXmlLangQuickFix());
                }
            }
        } else {
            // No HTML element found - this might be a partial template
            // Check if this looks like a complete HTML document
            if (content.contains("<!DOCTYPE") || content.contains("<head>") || content.contains("<body>")) {
                registerProblem(file, holder, 0,
                    "Missing <html> element with lang attribute",
                    ProblemHighlightType.ERROR,
                    null);
            }
        }
    }
    
    private void checkLanguageCodeValidity(String content, PsiFile file, ProblemsHolder holder) {
        Matcher langMatcher = LANG_ATTR_PATTERN.matcher(content);
        
        while (langMatcher.find()) {
            String element = langMatcher.group(1);
            String langCode = langMatcher.group(2);
            int offset = langMatcher.start();
            
            if (AccessibilityUtils.isEmptyOrWhitespace(langCode)) {
                registerProblem(file, holder, offset,
                    String.format("Empty lang attribute on <%s> element", element),
                    ProblemHighlightType.ERROR,
                    null);
            } else if (!AccessibilityUtils.isValidLanguageCode(langCode)) {
                registerProblem(file, holder, offset,
                    String.format("Invalid language code '%s' on <%s> - use BCP 47 format",
                        langCode, element),
                    ProblemHighlightType.ERROR,
                    null);
            } else if (!element.equalsIgnoreCase("html")) {
                // This is a language change within the document (WCAG 3.1.2 Level AA)
                // Just informational - language changes are good for accessibility
                registerProblem(file, holder, offset,
                    String.format("Language change detected on <%s> element (lang='%s') - good for accessibility",
                        element, langCode),
                    ProblemHighlightType.INFORMATION,
                    null);
            }
        }
    }
    
    private void checkXmlLangCompatibility(String content, PsiFile file, ProblemsHolder holder) {
        // Find all elements with xml:lang
        Pattern combinedPattern = Pattern.compile(
            "<(\\w+)\\s+([^>]*\\bxml:lang\\s*=\\s*[\"']([^\"']*)[\"'][^>]*)>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );
        
        Matcher matcher = combinedPattern.matcher(content);
        
        while (matcher.find()) {
            String element = matcher.group(1);
            String attributes = matcher.group(2);
            String xmlLangValue = matcher.group(3);
            int offset = matcher.start();
            
            // Check if there's also a lang attribute
            Pattern langInSameElement = Pattern.compile(
                "\\blang\\s*=\\s*[\"']([^\"']*)[\"']"
            );
            Matcher langMatcher = langInSameElement.matcher(attributes);
            
            if (langMatcher.find()) {
                String langValue = langMatcher.group(1);
                if (!langValue.equals(xmlLangValue)) {
                    registerProblem(file, holder, offset,
                        String.format("xml:lang='%s' doesn't match lang='%s' on <%s> - they should be identical",
                            xmlLangValue, langValue, element),
                        ProblemHighlightType.WARNING,
                        null);
                }
            } else {
                // xml:lang without lang attribute
                registerProblem(file, holder, offset,
                    String.format("<%s> has xml:lang but missing lang attribute - both should be present for compatibility",
                        element),
                    ProblemHighlightType.WARNING,
                    new AddMatchingLangQuickFix(xmlLangValue));
            }
        }
    }
    
    private void registerProblem(PsiFile file, ProblemsHolder holder, int offset,
                                String description, ProblemHighlightType type, LocalQuickFix fix) {
        PsiElement element = file.findElementAt(offset);
        if (element != null) {
            if (fix != null) {
                holder.registerProblem(element, description, type, fix);
            } else {
                holder.registerProblem(element, description, type);
            }
        }
    }
    
    /**
     * Quick fix to add lang attribute to html element
     */
    private static class AddLangAttributeQuickFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getName() {
            return "Add lang=\"en\" attribute";
        }
        
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add language attribute";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            PsiElement element = descriptor.getPsiElement();
            if (element == null) return;
            
            PsiFile file = element.getContainingFile();
            if (file == null) return;
            
            String fileText = file.getText();
            int startOffset = element.getTextOffset();
            
            // Find the HTML tag
            int htmlStart = fileText.indexOf("<html", startOffset);
            if (htmlStart == -1) {
                htmlStart = fileText.indexOf("<HTML", startOffset);
            }
            if (htmlStart == -1) return;
            
            int tagEnd = fileText.indexOf('>', htmlStart);
            if (tagEnd == -1) return;
            
            // Add lang attribute
            String beforeEnd = fileText.substring(0, tagEnd);
            String afterEnd = fileText.substring(tagEnd);
            
            String newContent = beforeEnd + " lang=\"en\"" + afterEnd;
            
            PsiFile newFile = PsiFileFactory.getInstance(project)
                .createFileFromText(file.getName(), file.getFileType(), newContent);
            file.getNode().replaceAllChildrenToChildrenOf(newFile.getNode());
        }
    }
    
    /**
     * Quick fix to set lang attribute value
     */
    private static class SetLangValueQuickFix implements LocalQuickFix {
        private final String languageCode;
        
        public SetLangValueQuickFix(String languageCode) {
            this.languageCode = languageCode;
        }
        
        @NotNull
        @Override
        public String getName() {
            return String.format("Set lang=\"%s\"", languageCode);
        }
        
        @NotNull
        @Override
        public String getFamilyName() {
            return "Fix language code";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            PsiElement element = descriptor.getPsiElement();
            if (element == null) return;
            
            PsiFile file = element.getContainingFile();
            if (file == null) return;
            
            String fileText = file.getText();
            int startOffset = element.getTextOffset();
            
            // Find the lang attribute
            int langStart = fileText.indexOf("lang=", startOffset);
            if (langStart == -1) return;
            
            int quoteStart = fileText.indexOf('"', langStart);
            if (quoteStart == -1) quoteStart = fileText.indexOf('\'', langStart);
            if (quoteStart == -1) return;
            
            char quoteChar = fileText.charAt(quoteStart);
            int quoteEnd = fileText.indexOf(quoteChar, quoteStart + 1);
            if (quoteEnd == -1) return;
            
            // Replace the language code
            String newContent = fileText.substring(0, quoteStart + 1) +
                              languageCode +
                              fileText.substring(quoteEnd);
            
            PsiFile newFile = PsiFileFactory.getInstance(project)
                .createFileFromText(file.getName(), file.getFileType(), newContent);
            file.getNode().replaceAllChildrenToChildrenOf(newFile.getNode());
        }
    }
    
    /**
     * Quick fix to sync xml:lang with lang attribute
     */
    private static class SyncXmlLangQuickFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getName() {
            return "Sync xml:lang with lang value";
        }
        
        @NotNull
        @Override
        public String getFamilyName() {
            return "Fix language attributes";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            PsiElement element = descriptor.getPsiElement();
            if (element == null) return;
            
            PsiFile file = element.getContainingFile();
            if (file == null) return;
            
            String fileText = file.getText();
            int startOffset = element.getTextOffset();
            
            // Find the lang value
            int langStart = fileText.indexOf("lang=", startOffset);
            if (langStart == -1) return;
            
            int langQuoteStart = fileText.indexOf('"', langStart);
            if (langQuoteStart == -1) langQuoteStart = fileText.indexOf('\'', langStart);
            if (langQuoteStart == -1) return;
            
            char langQuoteChar = fileText.charAt(langQuoteStart);
            int langQuoteEnd = fileText.indexOf(langQuoteChar, langQuoteStart + 1);
            if (langQuoteEnd == -1) return;
            
            String langValue = fileText.substring(langQuoteStart + 1, langQuoteEnd);
            
            // Find xml:lang attribute
            int xmlLangStart = fileText.indexOf("xml:lang=", startOffset);
            if (xmlLangStart == -1) return;
            
            int xmlQuoteStart = fileText.indexOf('"', xmlLangStart);
            if (xmlQuoteStart == -1) xmlQuoteStart = fileText.indexOf('\'', xmlLangStart);
            if (xmlQuoteStart == -1) return;
            
            char xmlQuoteChar = fileText.charAt(xmlQuoteStart);
            int xmlQuoteEnd = fileText.indexOf(xmlQuoteChar, xmlQuoteStart + 1);
            if (xmlQuoteEnd == -1) return;
            
            // Replace xml:lang value with lang value
            String newContent = fileText.substring(0, xmlQuoteStart + 1) +
                              langValue +
                              fileText.substring(xmlQuoteEnd);
            
            PsiFile newFile = PsiFileFactory.getInstance(project)
                .createFileFromText(file.getName(), file.getFileType(), newContent);
            file.getNode().replaceAllChildrenToChildrenOf(newFile.getNode());
        }
    }
    
    /**
     * Quick fix to add matching lang attribute when xml:lang exists
     */
    private static class AddMatchingLangQuickFix implements LocalQuickFix {
        private final String langValue;
        
        public AddMatchingLangQuickFix(String langValue) {
            this.langValue = langValue;
        }
        
        @NotNull
        @Override
        public String getName() {
            return String.format("Add lang=\"%s\" to match xml:lang", langValue);
        }
        
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add language attribute";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            PsiElement element = descriptor.getPsiElement();
            if (element == null) return;
            
            PsiFile file = element.getContainingFile();
            if (file == null) return;
            
            String fileText = file.getText();
            int startOffset = element.getTextOffset();
            
            // Find xml:lang position
            int xmlLangStart = fileText.indexOf("xml:lang=", startOffset);
            if (xmlLangStart == -1) return;
            
            // Add lang attribute before xml:lang
            String newContent = fileText.substring(0, xmlLangStart) +
                              String.format("lang=\"%s\" ", langValue) +
                              fileText.substring(xmlLangStart);
            
            PsiFile newFile = PsiFileFactory.getInstance(project)
                .createFileFromText(file.getName(), file.getFileType(), newContent);
            file.getNode().replaceAllChildrenToChildrenOf(newFile.getNode());
        }
    }
}