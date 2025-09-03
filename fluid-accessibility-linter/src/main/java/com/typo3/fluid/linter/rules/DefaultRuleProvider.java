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

        // ARIA role rules (validity, redundancy, conflicts, required props)
        AccessibilityRule ariaRoleRule = createRule(
            "aria-role",
            "ARIA roles must be appropriate",
            "Validate ARIA roles for validity, redundancy on semantic elements, and required properties",
            AccessibilityRule.RuleSeverity.WARNING,
            AccessibilityRule.RuleCategory.ARIA
        );
        engine.registerRule(ariaRoleRule, new AriaRoleValidationStrategy());

        // Keyboard and focus rules
        AccessibilityRule tabindexRule = createRule(
            "tabindex-positive",
            "Avoid positive tabindex",
            "Positive tabindex values disrupt natural focus order",
            AccessibilityRule.RuleSeverity.WARNING,
            AccessibilityRule.RuleCategory.KEYBOARD
        );
        engine.registerRule(tabindexRule, new TabindexPositiveValidationStrategy());

        AccessibilityRule focusCssRule = createRule(
            "focus-indicator-css",
            "Ensure visible focus indicators",
            "Do not remove outlines for :focus/:focus-visible; provide strong indicators",
            AccessibilityRule.RuleSeverity.WARNING,
            AccessibilityRule.RuleCategory.KEYBOARD
        );
        engine.registerRule(focusCssRule, new FocusIndicatorCssValidationStrategy());

        // Semantics: link vs button
        AccessibilityRule linkButtonRule = createRule(
            "link-vs-button",
            "Use links for navigation, buttons for actions",
            "Anchors should navigate; buttons should not be used for navigation",
            AccessibilityRule.RuleSeverity.WARNING,
            AccessibilityRule.RuleCategory.SEMANTICS
        );
        engine.registerRule(linkButtonRule, new LinkButtonSemanticsValidationStrategy());

        // Document rules
        AccessibilityRule langRule = createRule(
            "page-language",
            "Page language must be set",
            "Declare valid language on <html> and language of parts",
            AccessibilityRule.RuleSeverity.WARNING,
            AccessibilityRule.RuleCategory.LANGUAGE
        );
        engine.registerRule(langRule, new LanguageValidationStrategy());

        AccessibilityRule titleRule = createRule(
            "page-title",
            "Pages must have descriptive titles",
            "Provide a unique, descriptive <title>",
            AccessibilityRule.RuleSeverity.WARNING,
            AccessibilityRule.RuleCategory.STRUCTURE
        );
        engine.registerRule(titleRule, new PageTitleValidationStrategy());

        // Motion
        AccessibilityRule motionRule = createRule(
            "reduced-motion",
            "Honor reduced motion preference",
            "Provide @media (prefers-reduced-motion: reduce) alternative",
            AccessibilityRule.RuleSeverity.WARNING,
            AccessibilityRule.RuleCategory.STRUCTURE
        );
        engine.registerRule(motionRule, new ReducedMotionValidationStrategy());

        // Target size
        AccessibilityRule targetSizeRule = createRule(
            "target-size",
            "Ensure minimum target size",
            "Clickable targets should be at least 24x24 CSS px",
            AccessibilityRule.RuleSeverity.WARNING,
            AccessibilityRule.RuleCategory.SEMANTICS
        );
        engine.registerRule(targetSizeRule, new TargetSizeValidationStrategy());

        // Status messages
        AccessibilityRule statusRule = createRule(
            "status-messages",
            "Expose status messages to AT",
            "Use appropriate roles (e.g., role='alert') for errors and status",
            AccessibilityRule.RuleSeverity.WARNING,
            AccessibilityRule.RuleCategory.FORMS
        );
        engine.registerRule(statusRule, new StatusMessageValidationStrategy());

        // ARIA label sanity rules (non-interactive misuse, empty labels, hidden elements)
        AccessibilityRule ariaLabelSanityRule = createRule(
            "aria-label-sanity",
            "Avoid unnecessary aria-label",
            "Use aria-label only when there is no visible text or to name landmarks/controls; avoid on non-interactive content and empty values",
            AccessibilityRule.RuleSeverity.WARNING,
            AccessibilityRule.RuleCategory.ARIA
        );
        engine.registerRule(ariaLabelSanityRule, new AriaLabelSanityStrategy());
        
        // Navigation rules
        AccessibilityRule linkTextRule = createRule(
            "link-text",
            "Links must be descriptive",
            "Anchor text should clearly describe the destination or purpose",
            AccessibilityRule.RuleSeverity.WARNING,
            AccessibilityRule.RuleCategory.NAVIGATION
        );
        engine.registerRule(linkTextRule, new LinkTextValidationStrategy());

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
