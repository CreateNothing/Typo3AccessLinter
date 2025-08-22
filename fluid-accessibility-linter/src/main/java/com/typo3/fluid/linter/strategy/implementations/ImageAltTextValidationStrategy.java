package com.typo3.fluid.linter.strategy.implementations;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.psi.PsiFile;
import com.typo3.fluid.linter.fixes.FixContext;
import com.typo3.fluid.linter.fixes.FixRegistry;
import com.typo3.fluid.linter.strategy.ValidationResult;
import com.typo3.fluid.linter.strategy.ValidationStrategy;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Strategy for validating alt text on images
 */
public class ImageAltTextValidationStrategy implements ValidationStrategy {
    
    private static final Pattern IMG_TAG_PATTERN = Pattern.compile(
        "<img\\s+([^>]*)>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern ALT_ATTR_PATTERN = Pattern.compile(
        "\\salt\\s*=\\s*[\"']([^\"']*)[\"']", Pattern.CASE_INSENSITIVE);
    private static final Pattern ROLE_ATTR_PATTERN = Pattern.compile(
        "\\srole\\s*=\\s*[\"']presentation[\"']", Pattern.CASE_INSENSITIVE);
    
    @Override
    public List<ValidationResult> validate(PsiFile file, String content) {
        List<ValidationResult> results = new ArrayList<>();
        
        Matcher matcher = IMG_TAG_PATTERN.matcher(content);
        while (matcher.find()) {
            String imgTag = matcher.group(0);
            String attributes = matcher.group(1);
            
            boolean hasAlt = ALT_ATTR_PATTERN.matcher(attributes).find();
            boolean isDecorative = ROLE_ATTR_PATTERN.matcher(attributes).find();
            
            if (!hasAlt && !isDecorative) {
                // Create fix context
                FixContext fixContext = new FixContext("missing-alt");
                fixContext.setAttribute("attributeName", "alt");
                fixContext.setAttribute("elementType", "img");
                
                LocalQuickFix[] fixes = FixRegistry.getInstance().getFixes(
                    file, matcher.start(), matcher.end(), fixContext
                );
                
                results.add(new ValidationResult(
                    matcher.start(),
                    matcher.end(),
                    "Image missing alt attribute",
                    fixes
                ));
            } else if (hasAlt) {
                // Check for empty or poor quality alt text
                Matcher altMatcher = ALT_ATTR_PATTERN.matcher(attributes);
                if (altMatcher.find()) {
                    String altText = altMatcher.group(1).trim();
                    if (altText.isEmpty()) {
                        results.add(new ValidationResult(
                            matcher.start(),
                            matcher.end(),
                            "Alt attribute should not be empty (use role='presentation' for decorative images)"
                        ));
                    } else if (isLowQualityAltText(altText)) {
                        results.add(new ValidationResult(
                            matcher.start(),
                            matcher.end(),
                            "Alt text appears to be low quality: " + altText
                        ));
                    }
                }
            }
        }
        
        return results;
    }
    
    private boolean isLowQualityAltText(String altText) {
        String lower = altText.toLowerCase();
        return lower.equals("image") || 
               lower.equals("picture") || 
               lower.equals("photo") ||
               lower.matches("img\\d+") ||
               lower.matches("image\\d+");
    }
    
    @Override
    public int getPriority() {
        return 100; // High priority
    }
}