package com.typo3.fluid.linter.strategy;

import com.intellij.psi.PsiFile;
import java.util.List;

/**
 * Strategy interface for accessibility validations.
 * Enables composition-based design instead of inheritance.
 */
public interface ValidationStrategy {
    
    /**
     * Validate the given content and return validation results
     * @param file The PSI file being validated
     * @param content The content to validate
     * @return List of validation results
     */
    List<ValidationResult> validate(PsiFile file, String content);
    
    /**
     * Get the priority of this strategy (higher = runs first)
     * @return Priority value
     */
    default int getPriority() {
        return 0;
    }
    
    /**
     * Check if this strategy should be applied to the given file
     * @param file The file to check
     * @return true if strategy should be applied
     */
    default boolean shouldApply(PsiFile file) {
        return true;
    }
}