package com.typo3.fluid.linter.rules;

import com.typo3.fluid.linter.strategy.ValidationResult;

/**
 * Represents a rule violation found during validation
 */
public class RuleViolation {
    private final AccessibilityRule rule;
    private final ValidationResult result;
    private final AccessibilityRule.RuleSeverity effectiveSeverity;

    public RuleViolation(AccessibilityRule rule, ValidationResult result) {
        this(rule, result, rule.getSeverity());
    }

    public RuleViolation(AccessibilityRule rule, ValidationResult result, AccessibilityRule.RuleSeverity effectiveSeverity) {
        this.rule = rule;
        this.result = result;
        this.effectiveSeverity = effectiveSeverity != null ? effectiveSeverity : rule.getSeverity();
    }

    public AccessibilityRule getRule() {
        return rule;
    }

    public ValidationResult getResult() {
        return result;
    }

    public String getMessage() {
        return String.format("[%s] %s", rule.getId(), result.getMessage());
    }

    public AccessibilityRule.RuleSeverity getSeverity() {
        return effectiveSeverity;
    }
}
