package com.typo3.fluid.linter.strategy.implementations;

import com.intellij.psi.PsiFile;
import com.typo3.fluid.linter.strategy.ValidationResult;
import com.typo3.fluid.linter.strategy.ValidationStrategy;

import java.util.ArrayList;
import java.util.List;

/**
 * Flags pages with animation but no prefers-reduced-motion handling.
 */
public class ReducedMotionValidationStrategy implements ValidationStrategy {
    @Override
    public List<ValidationResult> validate(PsiFile file, String content) {
        List<ValidationResult> results = new ArrayList<>();
        String lower = content.toLowerCase();
        boolean hasAnimation = lower.contains("animation:") || lower.contains("@keyframes");
        boolean hasPrm = lower.contains("@media (prefers-reduced-motion: reduce)");
        if (hasAnimation && !hasPrm) {
            results.add(new ValidationResult(0, Math.min(content.length(), 1),
                "Provide reduced-motion alternative via @media (prefers-reduced-motion: reduce)"));
        }
        return results;
    }

    @Override
    public int getPriority() { return 25; }
}

