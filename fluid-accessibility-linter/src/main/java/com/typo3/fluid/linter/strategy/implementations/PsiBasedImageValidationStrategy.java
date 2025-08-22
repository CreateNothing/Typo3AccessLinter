package com.typo3.fluid.linter.strategy.implementations;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlTag;
import com.typo3.fluid.linter.fixes.FixContext;
import com.typo3.fluid.linter.fixes.AddAttributeFixStrategy;
import com.typo3.fluid.linter.fixes.ChangeAttributeFixStrategy;
import com.typo3.fluid.linter.fixes.ModifyTextFixStrategy;
import com.typo3.fluid.linter.fixes.AddChildElementFixStrategy;
import com.typo3.fluid.linter.parser.PsiElementParser;
import com.typo3.fluid.linter.strategy.ValidationResult;
import com.typo3.fluid.linter.strategy.ValidationStrategy;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Comprehensive PSI-based validation strategy for image accessibility.
 * Implements all accessibility best practices for HTML images, Fluid images, SVGs, and input images.
 */
public class PsiBasedImageValidationStrategy implements ValidationStrategy {
    
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
    
    @Override
    public List<ValidationResult> validate(PsiFile file, String content) {
        List<ValidationResult> results = new ArrayList<>();
        
        // Find all img elements using PSI
        List<PsiElement> imgElements = PsiElementParser.findElementsByTagName(file, "img");
        for (PsiElement element : imgElements) {
            validateImageElement(element, results, "img");
        }
        
        // Find all f:image ViewHelpers
        List<PsiElement> fluidImages = PsiElementParser.findElements(file, 
            el -> "f:image".equals(PsiElementParser.getTagName(el)));
        for (PsiElement element : fluidImages) {
            validateImageElement(element, results, "f:image");
        }
        
        // Find all SVG elements
        List<PsiElement> svgElements = PsiElementParser.findElementsByTagName(file, "svg");
        for (PsiElement element : svgElements) {
            validateSvgElement(element, results);
        }
        
        // Find all input type="image" elements
        List<PsiElement> inputImageElements = PsiElementParser.findElements(file, 
            el -> "input".equals(PsiElementParser.getTagName(el)) && 
                  "image".equalsIgnoreCase(PsiElementParser.getAttributeValue(el, "type")));
        for (PsiElement element : inputImageElements) {
            validateInputImageElement(element, results);
        }
        
        return results;
    }
    
    private void validateImageElement(PsiElement element, List<ValidationResult> results, String elementType) {
        ImageContext context = analyzeImageContext(element);
        
        // Skip if image has ARIA labeling
        if (context.hasAriaLabel || context.hasAriaLabelledby) {
            return; // Has alternative text method
        }
        
        // Check decorative images
        if (context.isLikelyDecorative) {
            if (context.altText == null) {
                results.add(createValidationResult(element, 
                    "Decorative image should have empty alt=\"\" attribute",
                    ProblemHighlightType.WARNING,
                    createAddEmptyAltFix()));
            } else if (!context.altText.isEmpty()) {
                results.add(createValidationResult(element,
                    "Decorative image should have empty alt text (alt=\"\")",
                    ProblemHighlightType.WARNING,
                    createChangeToEmptyAltFix()));
            }
            return;
        }
        
        // Check for missing alt text
        if (context.altText == null) {
            String message = context.isInLink ? 
                "Image in link must have alt text describing the link destination" :
                "Image needs alt text to describe its content";
            results.add(createValidationResult(element, message, 
                ProblemHighlightType.ERROR,
                createAddAltAttributeFix("")));
            return;
        }
        
        // Validate alt text quality
        validateAltTextQuality(context, element, results);
    }
    
    private void validateSvgElement(PsiElement element, List<ValidationResult> results) {
        String role = PsiElementParser.getAttributeValue(element, "role");
        String ariaLabel = PsiElementParser.getAttributeValue(element, "aria-label");
        String ariaLabelledby = PsiElementParser.getAttributeValue(element, "aria-labelledby");
        
        // Check for title and desc elements within SVG
        boolean hasTitle = hasChildElement(element, "title");
        boolean hasDesc = hasChildElement(element, "desc");
        
        if ("img".equals(role) || "presentation".equals(role) || "none".equals(role)) {
            // Role is explicitly set
            if ("img".equals(role) && ariaLabel == null && ariaLabelledby == null && !hasTitle) {
                results.add(createValidationResult(element,
                    "SVG with role='img' needs accessible text (aria-label, aria-labelledby, or <title>)",
                    ProblemHighlightType.ERROR,
                    createAddSvgTitleFix()));
            }
        } else if (!hasTitle && !hasDesc && ariaLabel == null && ariaLabelledby == null) {
            // No accessibility features at all
            results.add(createValidationResult(element,
                "SVG images should have accessible text via <title>, <desc>, or ARIA attributes",
                ProblemHighlightType.WARNING,
                createAddSvgTitleFix()));
        }
    }
    
    private void validateInputImageElement(PsiElement element, List<ValidationResult> results) {
        String altText = PsiElementParser.getAttributeValue(element, "alt");
        String ariaLabel = PsiElementParser.getAttributeValue(element, "aria-label");
        String ariaLabelledby = PsiElementParser.getAttributeValue(element, "aria-labelledby");
        String title = PsiElementParser.getAttributeValue(element, "title");
        
        if (altText == null && ariaLabel == null && ariaLabelledby == null && title == null) {
            results.add(createValidationResult(element,
                "Image input button needs alt text or ARIA label",
                ProblemHighlightType.ERROR,
                createAddAltAttributeFix("Submit")));
        }
    }
    
    private void validateAltTextQuality(ImageContext context, PsiElement element, List<ValidationResult> results) {
        String altText = context.altText;
        String trimmedAlt = altText.trim().toLowerCase();
        
        // Check for filename as alt text
        if (FILENAME_PATTERN.matcher(altText).find() || 
            (context.src != null && altText.equals(context.src.substring(context.src.lastIndexOf('/') + 1)))) {
            results.add(createValidationResult(element,
                "Alt text appears to be a filename - provide descriptive text instead",
                ProblemHighlightType.WARNING, null));
            return;
        }
        
        // Check for placeholder text
        if (PLACEHOLDER_PATTERN.matcher(trimmedAlt).matches()) {
            results.add(createValidationResult(element,
                "Alt text contains placeholder text - provide meaningful description",
                ProblemHighlightType.WARNING, null));
            return;
        }
        
        // Check for redundant phrases
        for (String redundant : REDUNDANT_PHRASES) {
            if (trimmedAlt.startsWith(redundant + " ") || trimmedAlt.equals(redundant)) {
                results.add(createValidationResult(element,
                    "Alt text contains redundant phrase '" + redundant + "' - screen readers already announce images",
                    ProblemHighlightType.WARNING,
                    createRemoveRedundantPhraseFix(redundant)));
                return;
            }
        }
        
        // Check for single word/very short alt text (might be insufficient)
        if (trimmedAlt.length() < 3 && !context.isInLink) {
            results.add(createValidationResult(element,
                "Alt text might be too brief - consider providing more descriptive text",
                ProblemHighlightType.WEAK_WARNING, null));
        }
        
        // Check for excessively long alt text
        if (altText.length() > 125) {
            results.add(createValidationResult(element,
                "Alt text is very long (>125 chars) - consider using aria-describedby for detailed descriptions",
                ProblemHighlightType.WEAK_WARNING, null));
        }
        
        // Check for space in empty alt (alt=" " vs alt="")
        if (altText.equals(" ") || (altText.trim().isEmpty() && !altText.isEmpty())) {
            results.add(createValidationResult(element,
                "Empty alt text should not contain spaces - use alt=\"\" instead",
                ProblemHighlightType.WARNING,
                createChangeToEmptyAltFix()));
        }
    }
    
    private ImageContext analyzeImageContext(PsiElement element) {
        ImageContext context = new ImageContext();
        
        // Extract alt text
        context.altText = PsiElementParser.getAttributeValue(element, "alt");
        
        // Extract role
        context.role = PsiElementParser.getAttributeValue(element, "role");
        
        // Check for ARIA labels
        context.hasAriaLabel = PsiElementParser.hasAttribute(element, "aria-label");
        context.hasAriaLabelledby = PsiElementParser.hasAttribute(element, "aria-labelledby");
        context.hasAriaDescribedby = PsiElementParser.hasAttribute(element, "aria-describedby");
        
        // Extract classes
        String classAttr = PsiElementParser.getAttributeValue(element, "class");
        if (classAttr != null) {
            context.classes = Arrays.asList(classAttr.split("\\s+"));
        }
        
        // Extract src
        context.src = PsiElementParser.getAttributeValue(element, "src");
        
        // Extract dimensions
        String widthStr = PsiElementParser.getAttributeValue(element, "width");
        if (widthStr != null) {
            try {
                context.width = Integer.parseInt(widthStr.replaceAll("[^0-9]", ""));
            } catch (NumberFormatException ignored) {}
        }
        
        String heightStr = PsiElementParser.getAttributeValue(element, "height");
        if (heightStr != null) {
            try {
                context.height = Integer.parseInt(heightStr.replaceAll("[^0-9]", ""));
            } catch (NumberFormatException ignored) {}
        }
        
        // Check if image is in a link
        context.isInLink = isImageInLink(element);
        
        // Determine if decorative
        context.isLikelyDecorative = isLikelyDecorative(context);
        
        return context;
    }
    
    private boolean isImageInLink(PsiElement element) {
        // Look for parent <a> element using PSI
        PsiElement parent = element.getParent();
        while (parent != null) {
            String tagName = PsiElementParser.getTagName(parent);
            if ("a".equals(tagName)) {
                return true;
            }
            parent = parent.getParent();
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
    
    private boolean hasChildElement(PsiElement parent, String childTagName) {
        if (!(parent instanceof XmlTag)) return false;
        
        XmlTag parentTag = (XmlTag) parent;
        XmlTag[] childTags = parentTag.getSubTags();
        
        for (XmlTag child : childTags) {
            if (childTagName.equals(child.getName())) {
                return true;
            }
        }
        return false;
    }
    
    private ValidationResult createValidationResult(PsiElement element, String message, 
                                                   ProblemHighlightType type, LocalQuickFix fix) {
        LocalQuickFix[] fixes = fix != null ? new LocalQuickFix[]{fix} : new LocalQuickFix[0];
        return new ValidationResult(
            element.getTextRange().getStartOffset(),
            element.getTextRange().getEndOffset(),
            message,
            type,
            fixes
        );
    }
    
    // Quick fix creation methods
    private LocalQuickFix createAddAltAttributeFix(String defaultText) {
        AddAttributeFixStrategy strategy = new AddAttributeFixStrategy();
        FixContext context = new FixContext("missing-alt");
        context.setAttribute("attributeName", "alt");
        context.setAttribute("attributeValue", defaultText);
        return strategy.createFix(null, 0, 0, context);
    }
    
    private LocalQuickFix createAddEmptyAltFix() {
        return createAddAltAttributeFix("");
    }
    
    private LocalQuickFix createChangeToEmptyAltFix() {
        ChangeAttributeFixStrategy strategy = new ChangeAttributeFixStrategy();
        FixContext context = new FixContext("change-alt-empty");
        context.setAttribute("attributeName", "alt");
        context.setAttribute("newValue", "");
        return strategy.createFix(null, 0, 0, context);
    }
    
    private LocalQuickFix createRemoveRedundantPhraseFix(String phrase) {
        ModifyTextFixStrategy strategy = new ModifyTextFixStrategy();
        FixContext context = new FixContext("remove-redundant-phrase");
        context.setAttribute("attributeName", "alt");
        context.setAttribute("textToRemove", phrase);
        context.setAttribute("replacement", "");
        return strategy.createFix(null, 0, 0, context);
    }
    
    private LocalQuickFix createAddSvgTitleFix() {
        AddChildElementFixStrategy strategy = new AddChildElementFixStrategy();
        FixContext context = new FixContext("add-svg-title");
        context.setAttribute("childTagName", "title");
        context.setAttribute("childContent", "Accessible description");
        context.setAttribute("parentTagName", "svg");
        return strategy.createFix(null, 0, 0, context);
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
    
    @Override
    public int getPriority() {
        return 100; // High priority
    }
    
    @Override
    public boolean shouldApply(PsiFile file) {
        return file.getName().toLowerCase().endsWith(".html");
    }
}