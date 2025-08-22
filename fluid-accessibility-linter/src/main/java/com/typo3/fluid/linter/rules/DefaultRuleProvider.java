package com.typo3.fluid.linter.rules;

import com.typo3.fluid.linter.strategy.implementations.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Provider for default HTML accessibility rules
 */
public class DefaultRuleProvider implements RuleProvider {
    
    @Override
    public void loadRules(RuleEngine engine) {
        // Image accessibility rules
        AccessibilityRule altTextRule = createRule(
            "img-alt-text",
            "Images must have alt text",
            "All <img> elements must have an alt attribute for screen readers",
            AccessibilityRule.RuleSeverity.ERROR,
            AccessibilityRule.RuleCategory.IMAGES
        );
        engine.registerRule(altTextRule, new ImageAltTextValidationStrategy());
        
        // Form accessibility rules
        AccessibilityRule formLabelRule = createRule(
            "form-label",
            "Form inputs must have labels",
            "All form input elements must have associated labels",
            AccessibilityRule.RuleSeverity.ERROR,
            AccessibilityRule.RuleCategory.FORMS
        );
        engine.registerRule(formLabelRule, new FormLabelValidationStrategy());
        
        // Structure rules
        AccessibilityRule headingHierarchyRule = createRule(
            "heading-hierarchy",
            "Heading hierarchy must be sequential",
            "Headings should follow a logical hierarchy without skipping levels",
            AccessibilityRule.RuleSeverity.WARNING,
            AccessibilityRule.RuleCategory.STRUCTURE
        );
        engine.registerRule(headingHierarchyRule, new HeadingHierarchyValidationStrategy());
        
        // List structure rules
        AccessibilityRule listStructureRule = createRule(
            "list-structure",
            "Lists must have proper structure",
            "UL and OL elements must only contain LI elements as direct children",
            AccessibilityRule.RuleSeverity.ERROR,
            AccessibilityRule.RuleCategory.STRUCTURE
        );
        engine.registerRule(listStructureRule, new ListStructureValidationStrategy());
        
        // ARIA rules
        AccessibilityRule ariaValidRule = createRule(
            "aria-valid",
            "ARIA attributes must be valid",
            "ARIA attributes must have valid names and values",
            AccessibilityRule.RuleSeverity.ERROR,
            AccessibilityRule.RuleCategory.ARIA
        );
        engine.registerRule(ariaValidRule, new AriaValidationStrategy());
        
        // Navigation rules
        AccessibilityRule skipLinksRule = createRule(
            "skip-links",
            "Pages should have skip links",
            "Pages should provide skip links for keyboard navigation",
            AccessibilityRule.RuleSeverity.WARNING,
            AccessibilityRule.RuleCategory.NAVIGATION
        );
        engine.registerRule(skipLinksRule, new SkipLinksValidationStrategy());
        
        // Table accessibility
        AccessibilityRule tableHeadersRule = createRule(
            "table-headers",
            "Tables must have headers",
            "Data tables must have TH elements to identify headers",
            AccessibilityRule.RuleSeverity.ERROR,
            AccessibilityRule.RuleCategory.STRUCTURE
        );
        engine.registerRule(tableHeadersRule, new TableAccessibilityValidationStrategy());
    }
    
    @Override
    public String getName() {
        return "Default HTML Accessibility Rules";
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
        rule.setTags(Arrays.asList("wcag", "a11y"));
        return rule;
    }
}