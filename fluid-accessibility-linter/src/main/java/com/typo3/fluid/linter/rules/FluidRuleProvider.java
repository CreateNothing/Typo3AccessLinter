package com.typo3.fluid.linter.rules;

import com.typo3.fluid.linter.strategy.implementations.*;
import java.util.Arrays;
import java.util.HashMap;

/**
 * Provider for Fluid-specific accessibility rules
 */
public class FluidRuleProvider implements RuleProvider {
    
    @Override
    public void loadRules(RuleEngine engine) {
        // Fluid image ViewHelper rules
        AccessibilityRule fluidImageRule = createRule(
            "fluid-image-alt",
            "Fluid image ViewHelpers must have alt text",
            "All <f:image> ViewHelpers must have an alt attribute",
            AccessibilityRule.RuleSeverity.ERROR,
            AccessibilityRule.RuleCategory.IMAGES
        );
        engine.registerRule(fluidImageRule, new FluidImageValidationStrategy());
        
        // Fluid form ViewHelper rules
        AccessibilityRule fluidFormRule = createRule(
            "fluid-form-label",
            "Fluid form ViewHelpers must have labels",
            "All Fluid form ViewHelpers must have associated labels",
            AccessibilityRule.RuleSeverity.ERROR,
            AccessibilityRule.RuleCategory.FORMS
        );
        engine.registerRule(fluidFormRule, new FluidFormValidationStrategy());
        
        // Fluid link ViewHelper rules
        AccessibilityRule fluidLinkRule = createRule(
            "fluid-link-text",
            "Fluid link ViewHelpers must have descriptive text",
            "All <f:link.*> ViewHelpers must have descriptive link text",
            AccessibilityRule.RuleSeverity.WARNING,
            AccessibilityRule.RuleCategory.NAVIGATION
        );
        engine.registerRule(fluidLinkRule, new FluidLinkValidationStrategy());
        
        // Fluid structure rules
        AccessibilityRule fluidStructureRule = createRule(
            "fluid-structure",
            "Fluid templates must maintain proper HTML structure",
            "Fluid control flow ViewHelpers should not break HTML structure",
            AccessibilityRule.RuleSeverity.WARNING,
            AccessibilityRule.RuleCategory.STRUCTURE
        );
        engine.registerRule(fluidStructureRule, new FluidStructureValidationStrategy());
    }
    
    @Override
    public String getName() {
        return "TYPO3 Fluid Accessibility Rules";
    }
    
    private AccessibilityRule createRule(String id, String name, String description,
                                        AccessibilityRule.RuleSeverity severity,
                                        AccessibilityRule.RuleCategory category) {
        AccessibilityRule rule = new AccessibilityRule();
        rule.setId(id);
        rule.setName(name);
        rule.setDescription(description);
        rule.setSeverity(severity);
        rule.setCategory(category);
        rule.setEnabled(true);
        rule.setConfiguration(new HashMap<>());
        rule.setTags(Arrays.asList("fluid", "typo3", "wcag", "a11y"));
        return rule;
    }
}