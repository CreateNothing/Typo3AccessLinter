package com.typo3.fluid.linter.strategy.implementations;

import com.intellij.psi.PsiFile;
import com.typo3.fluid.linter.strategy.ValidationResult;
import com.typo3.fluid.linter.strategy.ValidationStrategy;
import java.util.Collections;
import java.util.List;

/**
 * Base implementation for validation strategies.
 * Provides common functionality and stub implementations.
 */
public abstract class BaseValidationStrategy implements ValidationStrategy {
    
    @Override
    public List<ValidationResult> validate(PsiFile file, String content) {
        // Default implementation - to be overridden
        return Collections.emptyList();
    }
    
    @Override
    public int getPriority() {
        return 50; // Default medium priority
    }
    
    @Override
    public boolean shouldApply(PsiFile file) {
        return file.getName().toLowerCase().endsWith(".html");
    }
}

