package com.typo3.fluid.linter.settings;

import com.typo3.fluid.linter.rules.AccessibilityRule;
import com.typo3.fluid.linter.rules.RuleEngine;

import java.util.HashMap;
import java.util.Map;

/**
 * Built-in named rule profiles (presets) that can be applied quickly.
 */
public final class Presets {
    private Presets() {}

    public static final String WCAG_AA = "WCAG 2.1 AA";
    public static final String WCAG_AAA = "WCAG 2.1 AAA";
    public static final String STRICT_QA = "Strict QA";
    public static final String RELAXED_DEV = "Relaxed Dev";
    public static final String FLUID_HEAVY = "Fluid-Heavy";

    public static String[] names() {
        return new String[]{WCAG_AA, WCAG_AAA, STRICT_QA, RELAXED_DEV, FLUID_HEAVY};
    }

    public static RuleSettingsState.State buildPreset(String name) {
        RuleSettingsState.State s = new RuleSettingsState.State();

        // Start with defaults from current rules
        for (AccessibilityRule rule : RuleEngine.getInstance().getAllRules()) {
            s.enabled.put(rule.getId(), rule.isEnabled());
            s.severity.put(rule.getId(), rule.getSeverity().name());
        }

        switch (name) {
            case WCAG_AA:
                enable(s, true, "img-alt-text", "form-label", "aria-valid", "table-headers",
                        "list-structure", "heading-hierarchy", "skip-links", "link-text",
                        "fluid-image-alt", "fluid-form-label", "fluid-link-text", "fluid-structure");
                severity(s, AccessibilityRule.RuleSeverity.ERROR,
                        "img-alt-text", "form-label", "aria-valid", "table-headers");
                severity(s, AccessibilityRule.RuleSeverity.WARNING,
                        "list-structure", "heading-hierarchy", "skip-links", "link-text",
                        "fluid-structure");
                break;
            case WCAG_AAA:
                enableAll(s, true);
                severityAll(s, AccessibilityRule.RuleSeverity.WARNING);
                severity(s, AccessibilityRule.RuleSeverity.ERROR,
                        "img-alt-text", "form-label", "aria-valid", "table-headers", "link-text");
                break;
            case STRICT_QA:
                enableAll(s, true);
                severityAll(s, AccessibilityRule.RuleSeverity.ERROR);
                break;
            case RELAXED_DEV:
                enableAll(s, true);
                severityAll(s, AccessibilityRule.RuleSeverity.WEAK_WARNING);
                // Critical still warnings
                severity(s, AccessibilityRule.RuleSeverity.WARNING,
                        "img-alt-text", "form-label", "aria-valid");
                // Allow some noise off
                enable(s, false, "skip-links", "heading-hierarchy");
                break;
            case FLUID_HEAVY:
                enableAll(s, true);
                // Emphasize Fluid rules
                severity(s, AccessibilityRule.RuleSeverity.ERROR,
                        "fluid-image-alt", "fluid-form-label");
                severity(s, AccessibilityRule.RuleSeverity.WARNING,
                        "fluid-link-text", "fluid-structure");
                break;
            default:
                // no-op; return defaults
        }
        return s;
    }

    private static void enableAll(RuleSettingsState.State s, boolean enabled) {
        for (String id : s.enabled.keySet()) {
            s.enabled.put(id, enabled);
        }
    }

    private static void severityAll(RuleSettingsState.State s, AccessibilityRule.RuleSeverity sev) {
        for (String id : s.severity.keySet()) {
            s.severity.put(id, sev.name());
        }
    }

    private static void enable(RuleSettingsState.State s, boolean enabled, String... ids) {
        for (String id : ids) s.enabled.put(id, enabled);
    }

    private static void severity(RuleSettingsState.State s, AccessibilityRule.RuleSeverity sev, String... ids) {
        for (String id : ids) s.severity.put(id, sev.name());
    }
}

