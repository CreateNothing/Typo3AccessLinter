package com.typo3.fluid.linter.inspections;

import com.intellij.codeInspection.*;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Enhanced image accessibility inspection that provides context-aware validation
 * including decorative image detection, alt text quality validation, and SVG support.
 */
public class EnhancedImageAccessibilityInspection extends FluidAccessibilityInspection {
    
    // Image patterns
    private static final Pattern IMG_TAG_PATTERN = Pattern.compile(
        "<img\\s+([^>]*)>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern F_IMAGE_PATTERN = Pattern.compile(
        "<f:image\\s+([^/>]*)(/>|>.*?</f:image>)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern SVG_PATTERN = Pattern.compile(
        "<svg\\s+([^>]*)>(.*?)</svg>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern INPUT_IMAGE_PATTERN = Pattern.compile(
        "<input\\s+[^>]*type\\s*=\\s*[\"']image[\"'][^>]*>", Pattern.CASE_INSENSITIVE);
    
    // Attribute patterns
    private static final Pattern ALT_ATTR_PATTERN = Pattern.compile(
        "\\salt\\s*=\\s*[\"']([^\"']*)[\"']", Pattern.CASE_INSENSITIVE);
    private static final Pattern ROLE_ATTR_PATTERN = Pattern.compile(
        "\\srole\\s*=\\s*[\"']([^\"']*)[\"']", Pattern.CASE_INSENSITIVE);
    private static final Pattern ARIA_LABEL_PATTERN = Pattern.compile(
        "\\saria-label\\s*=\\s*[\"']([^\"']*)[\"']", Pattern.CASE_INSENSITIVE);
    private static final Pattern ARIA_LABELLEDBY_PATTERN = Pattern.compile(
        "\\saria-labelledby\\s*=\\s*[\"']([^\"']*)[\"']", Pattern.CASE_INSENSITIVE);
    private static final Pattern ARIA_DESCRIBEDBY_PATTERN = Pattern.compile(
        "\\saria-describedby\\s*=\\s*[\"']([^\"']*)[\"']", Pattern.CASE_INSENSITIVE);
    private static final Pattern CLASS_ATTR_PATTERN = Pattern.compile(
        "\\sclass\\s*=\\s*[\"']([^\"']*)[\"']", Pattern.CASE_INSENSITIVE);
    private static final Pattern SRC_ATTR_PATTERN = Pattern.compile(
        "\\ssrc\\s*=\\s*[\"']([^\"']*)[\"']", Pattern.CASE_INSENSITIVE);
    private static final Pattern WIDTH_ATTR_PATTERN = Pattern.compile(
        "\\swidth\\s*=\\s*[\"']([^\"']*)[\"']", Pattern.CASE_INSENSITIVE);
    private static final Pattern HEIGHT_ATTR_PATTERN = Pattern.compile(
        "\\sheight\\s*=\\s*[\"']([^\"']*)[\"']", Pattern.CASE_INSENSITIVE);
    private static final Pattern TITLE_PATTERN = Pattern.compile(
        "\\stitle\\s*=\\s*[\"']([^\"']*)[\"']", Pattern.CASE_INSENSITIVE);
    
    // SVG accessibility patterns
    private static final Pattern SVG_TITLE_PATTERN = Pattern.compile(
        "<title[^>]*>([^<]+)</title>", Pattern.CASE_INSENSITIVE);
    private static final Pattern SVG_DESC_PATTERN = Pattern.compile(
        "<desc[^>]*>([^<]+)</desc>", Pattern.CASE_INSENSITIVE);
    
    // Decorative indicators
    private static final Set<String> DECORATIVE_CLASSES = new HashSet<>(Arrays.asList(
        "decorative", "decoration", "ornament", "spacer", "divider", "separator",
        "icon", "bullet", "arrow", "background", "bg-image", "pattern"
    ));
    
    private static final Set<String> DECORATIVE_FILENAMES = new HashSet<>(Arrays.asList(
        "spacer", "divider", "separator", "bullet", "arrow", "decoration",
        "pattern", "background", "bg", "ornament", "dot", "line"
    ));
    
    // Bad alt text patterns
    private static final Set<String> REDUNDANT_PHRASES = new HashSet<>(Arrays.asList(
        "image", "picture", "photo", "graphic", "icon", "illustration",
        "image of", "picture of", "photo of", "graphic of", "icon of"
    ));
    
    private static final Pattern FILENAME_PATTERN = Pattern.compile(
        "\\.(jpg|jpeg|png|gif|svg|webp|bmp|ico)$", Pattern.CASE_INSENSITIVE);
    
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile(
        "^(img|image|pic|photo|untitled|placeholder|temp|test|dummy|sample)\\d*$", 
        Pattern.CASE_INSENSITIVE);
    
    @NotNull
    @Override
    public String getDisplayName() {
        return "Enhanced image accessibility validation";
    }
    
    @NotNull
    @Override
    public String getShortName() {
        return "EnhancedImageAccessibility";
    }
    
    @Override
    protected void inspectFile(@NotNull PsiFile file, @NotNull ProblemsHolder holder) {
        String content = file.getText();
        
        // Check different types of images
        checkHtmlImages(content, file, holder);
        checkFluidImages(content, file, holder);
        checkSvgImages(content, file, holder);
        checkInputImages(content, file, holder);
    }
    
    private void checkHtmlImages(String content, PsiFile file, ProblemsHolder holder) {
        Matcher matcher = IMG_TAG_PATTERN.matcher(content);
        while (matcher.find()) {
            String attributes = matcher.group(1);
            int start = matcher.start();
            int end = matcher.end();
            
            ImageContext context = analyzeImageContext(attributes, content, start);
            validateImageAccessibility(context, file, holder, start, end, "img");
        }
    }
    
    private void checkFluidImages(String content, PsiFile file, ProblemsHolder holder) {
        Matcher matcher = F_IMAGE_PATTERN.matcher(content);
        while (matcher.find()) {
            String attributes = matcher.group(1);
            int start = matcher.start();
            int end = matcher.end();
            
            ImageContext context = analyzeImageContext(attributes, content, start);
            validateImageAccessibility(context, file, holder, start, end, "f:image");
        }
    }
    
    private void checkSvgImages(String content, PsiFile file, ProblemsHolder holder) {
        Matcher matcher = SVG_PATTERN.matcher(content);
        while (matcher.find()) {
            String attributes = matcher.group(1);
            String svgContent = matcher.group(2);
            int start = matcher.start();
            int end = matcher.end();
            
            // Check for SVG accessibility features
            boolean hasTitle = SVG_TITLE_PATTERN.matcher(svgContent).find();
            boolean hasDesc = SVG_DESC_PATTERN.matcher(svgContent).find();
            boolean hasAriaLabel = ARIA_LABEL_PATTERN.matcher(attributes).find();
            boolean hasAriaLabelledby = ARIA_LABELLEDBY_PATTERN.matcher(attributes).find();
            Matcher roleMatcher = ROLE_ATTR_PATTERN.matcher(attributes);
            String role = roleMatcher.find() ? roleMatcher.group(1) : null;
            
            if ("img".equals(role) || "presentation".equals(role) || "none".equals(role)) {
                // Role is explicitly set
                if ("img".equals(role) && !hasAriaLabel && !hasAriaLabelledby && !hasTitle) {
                    registerProblem(holder, file, start, end,
                        "SVG with role='img' needs accessible text (aria-label, aria-labelledby, or <title>)",
                        new AddSvgTitleFix());
                }
            } else if (!hasTitle && !hasDesc && !hasAriaLabel && !hasAriaLabelledby) {
                // No accessibility features at all
                registerProblem(holder, file, start, end,
                    "SVG images should have accessible text via <title>, <desc>, or ARIA attributes",
                    new AddSvgTitleFix());
            }
        }
    }
    
    private void checkInputImages(String content, PsiFile file, ProblemsHolder holder) {
        Matcher matcher = INPUT_IMAGE_PATTERN.matcher(content);
        while (matcher.find()) {
            String inputTag = matcher.group();
            int start = matcher.start();
            int end = matcher.end();
            
            boolean hasAlt = ALT_ATTR_PATTERN.matcher(inputTag).find();
            boolean hasAriaLabel = ARIA_LABEL_PATTERN.matcher(inputTag).find();
            boolean hasAriaLabelledby = ARIA_LABELLEDBY_PATTERN.matcher(inputTag).find();
            boolean hasTitle = TITLE_PATTERN.matcher(inputTag).find();
            
            if (!hasAlt && !hasAriaLabel && !hasAriaLabelledby && !hasTitle) {
                registerProblem(holder, file, start, end,
                    "Image input button needs alt text or ARIA label",
                    new AddAltAttributeFix("Submit"));
            }
        }
    }
    
    private ImageContext analyzeImageContext(String attributes, String content, int position) {
        ImageContext context = new ImageContext();
        
        // Extract alt text
        Matcher altMatcher = ALT_ATTR_PATTERN.matcher(attributes);
        if (altMatcher.find()) {
            context.altText = altMatcher.group(1);
        }
        
        // Extract role
        Matcher roleMatcher = ROLE_ATTR_PATTERN.matcher(attributes);
        if (roleMatcher.find()) {
            context.role = roleMatcher.group(1);
        }
        
        // Check for ARIA labels
        context.hasAriaLabel = ARIA_LABEL_PATTERN.matcher(attributes).find();
        context.hasAriaLabelledby = ARIA_LABELLEDBY_PATTERN.matcher(attributes).find();
        context.hasAriaDescribedby = ARIA_DESCRIBEDBY_PATTERN.matcher(attributes).find();
        
        // Extract classes
        Matcher classMatcher = CLASS_ATTR_PATTERN.matcher(attributes);
        if (classMatcher.find()) {
            context.classes = Arrays.asList(classMatcher.group(1).split("\\s+"));
        }
        
        // Extract src
        Matcher srcMatcher = SRC_ATTR_PATTERN.matcher(attributes);
        if (srcMatcher.find()) {
            context.src = srcMatcher.group(1);
        }
        
        // Extract dimensions
        Matcher widthMatcher = WIDTH_ATTR_PATTERN.matcher(attributes);
        if (widthMatcher.find()) {
            try {
                context.width = Integer.parseInt(widthMatcher.group(1).replaceAll("[^0-9]", ""));
            } catch (NumberFormatException ignored) {}
        }
        
        Matcher heightMatcher = HEIGHT_ATTR_PATTERN.matcher(attributes);
        if (heightMatcher.find()) {
            try {
                context.height = Integer.parseInt(heightMatcher.group(1).replaceAll("[^0-9]", ""));
            } catch (NumberFormatException ignored) {}
        }
        
        // Check if image is in a link
        context.isInLink = isImageInLink(content, position);
        
        // Determine if decorative
        context.isLikelyDecorative = isLikelyDecorative(context);
        
        return context;
    }
    
    private boolean isImageInLink(String content, int position) {
        // Simple check: look for enclosing <a> tag
        int searchStart = Math.max(0, position - 500);
        int searchEnd = Math.min(content.length(), position + 500);
        String nearbyContent = content.substring(searchStart, searchEnd);
        
        int relativePos = position - searchStart;
        
        // Look for opening <a> before and closing </a> after
        Pattern linkStartPattern = Pattern.compile("<a\\s+[^>]*>", Pattern.CASE_INSENSITIVE);
        Pattern linkEndPattern = Pattern.compile("</a>", Pattern.CASE_INSENSITIVE);
        
        Matcher startMatcher = linkStartPattern.matcher(nearbyContent);
        int lastLinkStart = -1;
        while (startMatcher.find() && startMatcher.start() < relativePos) {
            lastLinkStart = startMatcher.start();
        }
        
        if (lastLinkStart >= 0) {
            Matcher endMatcher = linkEndPattern.matcher(nearbyContent);
            while (endMatcher.find()) {
                if (endMatcher.start() > relativePos) {
                    return true; // Image is between <a> and </a>
                }
            }
        }
        
        return false;
    }
    
    private boolean isLikelyDecorative(ImageContext context) {
        // Check role attribute
        if ("presentation".equals(context.role) || "none".equals(context.role)) {
            return true;
        }
        
        // Check for decorative classes
        if (context.classes != null) {
            for (String cls : context.classes) {
                for (String decorativeClass : DECORATIVE_CLASSES) {
                    if (cls.toLowerCase().contains(decorativeClass)) {
                        return true;
                    }
                }
            }
        }
        
        // Check filename
        if (context.src != null) {
            String filename = context.src.substring(context.src.lastIndexOf('/') + 1);
            String nameWithoutExt = filename.replaceFirst("\\.[^.]+$", "");
            for (String decorative : DECORATIVE_FILENAMES) {
                if (nameWithoutExt.toLowerCase().contains(decorative)) {
                    return true;
                }
            }
        }
        
        // Check dimensions (very small images might be decorative)
        if (context.width != null && context.height != null) {
            if (context.width < 50 && context.height < 50) {
                return true;
            }
        }
        
        // Empty alt text might indicate decorative intent
        if (context.altText != null && context.altText.isEmpty()) {
            return true;
        }
        
        return false;
    }
    
    private void validateImageAccessibility(ImageContext context, PsiFile file, 
                                           ProblemsHolder holder, int start, int end, String tagType) {
        
        // Skip if image has ARIA labeling
        if (context.hasAriaLabel || context.hasAriaLabelledby) {
            return; // Has alternative text method
        }
        
        // Check decorative images
        if (context.isLikelyDecorative) {
            if (context.altText == null) {
                registerProblem(holder, file, start, end,
                    "Decorative image should have empty alt=\"\" attribute",
                    new AddEmptyAltFix());
            } else if (!context.altText.isEmpty()) {
                registerProblem(holder, file, start, end,
                    "Decorative image should have empty alt text (alt=\"\")",
                    new ChangeToEmptyAltFix());
            }
            return;
        }
        
        // Check for missing alt text
        if (context.altText == null) {
            String message = context.isInLink ? 
                "Image in link must have alt text describing the link destination" :
                "Image needs alt text to describe its content";
            registerProblem(holder, file, start, end, message, new AddAltAttributeFix(""));
            return;
        }
        
        // Validate alt text quality
        validateAltTextQuality(context.altText, context, file, holder, start, end);
    }
    
    private void validateAltTextQuality(String altText, ImageContext context, PsiFile file,
                                       ProblemsHolder holder, int start, int end) {
        
        String trimmedAlt = altText.trim().toLowerCase();
        
        // Check for filename as alt text
        if (FILENAME_PATTERN.matcher(altText).find() || 
            (context.src != null && altText.equals(context.src.substring(context.src.lastIndexOf('/') + 1)))) {
            registerProblem(holder, file, start, end,
                "Alt text appears to be a filename - provide descriptive text instead",
                ProblemHighlightType.WARNING, null);
            return;
        }
        
        // Check for placeholder text
        if (PLACEHOLDER_PATTERN.matcher(trimmedAlt).matches()) {
            registerProblem(holder, file, start, end,
                "Alt text contains placeholder text - provide meaningful description",
                ProblemHighlightType.WARNING, null);
            return;
        }
        
        // Check for redundant phrases
        for (String redundant : REDUNDANT_PHRASES) {
            if (trimmedAlt.startsWith(redundant + " ") || trimmedAlt.equals(redundant)) {
                registerProblem(holder, file, start, end,
                    "Alt text contains redundant phrase '" + redundant + "' - screen readers already announce images",
                    new RemoveRedundantPhraseFix(redundant));
                return;
            }
        }
        
        // Check for single word/very short alt text (might be insufficient)
        if (trimmedAlt.length() < 3 && !context.isInLink) {
            registerProblem(holder, file, start, end,
                "Alt text might be too brief - consider providing more descriptive text",
                ProblemHighlightType.WEAK_WARNING, null);
        }
        
        // Check for excessively long alt text
        if (altText.length() > 125) {
            registerProblem(holder, file, start, end,
                "Alt text is very long (>125 chars) - consider using aria-describedby for detailed descriptions",
                ProblemHighlightType.WEAK_WARNING, null);
        }
    }
    
    
    
    // Data class for image context
    private static class ImageContext {
        String altText;
        String role;
        boolean hasAriaLabel;
        boolean hasAriaLabelledby;
        boolean hasAriaDescribedby;
        List<String> classes;
        String src;
        Integer width;
        Integer height;
        boolean isInLink;
        boolean isLikelyDecorative;
    }
    
    // Quick fixes
    private static class AddAltAttributeFix implements LocalQuickFix {
        private final String defaultText;
        
        AddAltAttributeFix(String defaultText) {
            this.defaultText = defaultText;
        }
        
        @NotNull
        @Override
        public String getName() {
            return defaultText.isEmpty() ? "Add alt attribute" : "Add alt=\"" + defaultText + "\"";
        }
        
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add alt attribute";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add alt attribute to the tag
        }
    }
    
    private static class AddEmptyAltFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getName() {
            return "Add empty alt=\"\" for decorative image";
        }
        
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add empty alt attribute";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add alt="" to the tag
        }
    }
    
    private static class ChangeToEmptyAltFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getName() {
            return "Change to empty alt=\"\"";
        }
        
        @NotNull
        @Override
        public String getFamilyName() {
            return "Change alt to empty";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would change existing alt to alt=""
        }
    }
    
    private static class RemoveRedundantPhraseFix implements LocalQuickFix {
        private final String phrase;
        
        RemoveRedundantPhraseFix(String phrase) {
            this.phrase = phrase;
        }
        
        @NotNull
        @Override
        public String getName() {
            return "Remove redundant '" + phrase + "' from alt text";
        }
        
        @NotNull
        @Override
        public String getFamilyName() {
            return "Remove redundant phrase";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would remove the redundant phrase from alt text
        }
    }
    
    private static class AddSvgTitleFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getName() {
            return "Add <title> element to SVG";
        }
        
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add SVG title";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add <title> element inside SVG
        }
    }
}