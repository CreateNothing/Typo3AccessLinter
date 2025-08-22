package com.typo3.fluid.linter.strategy;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemHighlightType;

/**
 * Represents a validation result from a strategy
 */
public class ValidationResult {
    private final int startOffset;
    private final int endOffset;
    private final String message;
    private final ProblemHighlightType highlightType;
    private final LocalQuickFix[] fixes;
    
    public ValidationResult(int startOffset, int endOffset, String message) {
        this(startOffset, endOffset, message, ProblemHighlightType.GENERIC_ERROR_OR_WARNING, new LocalQuickFix[0]);
    }
    
    public ValidationResult(int startOffset, int endOffset, String message, LocalQuickFix... fixes) {
        this(startOffset, endOffset, message, ProblemHighlightType.GENERIC_ERROR_OR_WARNING, fixes);
    }
    
    public ValidationResult(int startOffset, int endOffset, String message, 
                          ProblemHighlightType highlightType, LocalQuickFix... fixes) {
        this.startOffset = startOffset;
        this.endOffset = endOffset;
        this.message = message;
        this.highlightType = highlightType;
        this.fixes = fixes;
    }
    
    public int getStartOffset() {
        return startOffset;
    }
    
    public int getEndOffset() {
        return endOffset;
    }
    
    public String getMessage() {
        return message;
    }
    
    public ProblemHighlightType getHighlightType() {
        return highlightType;
    }
    
    public LocalQuickFix[] getFixes() {
        return fixes;
    }
}