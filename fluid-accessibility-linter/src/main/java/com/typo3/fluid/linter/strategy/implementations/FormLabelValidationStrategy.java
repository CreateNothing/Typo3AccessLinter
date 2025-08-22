package com.typo3.fluid.linter.strategy.implementations;

import com.intellij.psi.PsiFile;
import com.typo3.fluid.linter.strategy.ValidationResult;
import com.typo3.fluid.linter.strategy.ValidationStrategy;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Strategy for validating form input labels
 */
public class FormLabelValidationStrategy implements ValidationStrategy {
    
    private static final Pattern INPUT_PATTERN = Pattern.compile(
        "<input\\s+([^>]*type\\s*=\\s*[\"'](?:text|email|password|tel|number|date|search|url)[\"'][^>]*)>",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    
    private static final Pattern ID_PATTERN = Pattern.compile(
        "\\sid\\s*=\\s*[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);
    
    private static final Pattern ARIA_LABEL_PATTERN = Pattern.compile(
        "\\saria-label\\s*=\\s*[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);
    
    private static final Pattern ARIA_LABELLEDBY_PATTERN = Pattern.compile(
        "\\saria-labelledby\\s*=\\s*[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);
    
    @Override
    public List<ValidationResult> validate(PsiFile file, String content) {
        List<ValidationResult> results = new ArrayList<>();
        
        Matcher matcher = INPUT_PATTERN.matcher(content);
        while (matcher.find()) {
            String inputTag = matcher.group(0);
            String attributes = matcher.group(1);
            
            boolean hasAriaLabel = ARIA_LABEL_PATTERN.matcher(attributes).find();
            boolean hasAriaLabelledBy = ARIA_LABELLEDBY_PATTERN.matcher(attributes).find();
            
            if (!hasAriaLabel && !hasAriaLabelledBy) {
                // Check if there's a label element pointing to this input
                Matcher idMatcher = ID_PATTERN.matcher(attributes);
                if (idMatcher.find()) {
                    String inputId = idMatcher.group(1);
                    String labelPattern = "<label\\s+[^>]*for\\s*=\\s*[\"']" + Pattern.quote(inputId) + "[\"']";
                    if (!Pattern.compile(labelPattern, Pattern.CASE_INSENSITIVE).matcher(content).find()) {
                        results.add(new ValidationResult(
                            matcher.start(),
                            matcher.end(),
                            "Form input missing associated label"
                        ));
                    }
                } else {
                    // No ID means it can't have an associated label
                    results.add(new ValidationResult(
                        matcher.start(),
                        matcher.end(),
                        "Form input missing id attribute and label"
                    ));
                }
            }
        }
        
        return results;
    }
    
    @Override
    public int getPriority() {
        return 90;
    }
}