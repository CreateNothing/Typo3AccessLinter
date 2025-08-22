package com.typo3.fluid.linter.fixes;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.psi.PsiFile;

/**
 * Strategy interface for creating quick fixes
 */
public interface FixStrategy {
    
    /**
     * Create a quick fix for the given context
     * @param file The file being fixed
     * @param startOffset Start position of the problem
     * @param endOffset End position of the problem
     * @param context Additional context data
     * @return A quick fix or null if not applicable
     */
    LocalQuickFix createFix(PsiFile file, int startOffset, int endOffset, FixContext context);
    
    /**
     * Check if this fix strategy can handle the given problem type
     * @param problemType The type of problem
     * @return true if this strategy can create a fix
     */
    boolean canHandle(String problemType);
}