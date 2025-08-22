package com.typo3.fluid.linter.rules;

import com.intellij.psi.PsiFile;
import com.typo3.fluid.linter.strategy.ValidationResult;
import com.typo3.fluid.linter.strategy.ValidationStrategy;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rule engine for managing and executing accessibility rules
 */
public class RuleEngine {
    private static final RuleEngine INSTANCE = new RuleEngine();
    private final Map<String, AccessibilityRule> rules = new ConcurrentHashMap<>();
    private final Map<String, ValidationStrategy> strategies = new ConcurrentHashMap<>();
    private final List<RuleProvider> providers = new ArrayList<>();
    
    private RuleEngine() {
        initialize();
    }
    
    public static RuleEngine getInstance() {
        return INSTANCE;
    }
    
    /**
     * Register a rule with its validation strategy
     */
    public void registerRule(AccessibilityRule rule, ValidationStrategy strategy) {
        rules.put(rule.getId(), rule);
        strategies.put(rule.getId(), strategy);
    }
    
    /**
     * Register a rule provider
     */
    public void registerProvider(RuleProvider provider) {
        providers.add(provider);
        provider.loadRules(this);
    }
    
    /**
     * Execute all enabled rules on a file
     */
    public List<RuleViolation> execute(PsiFile file) {
        List<RuleViolation> violations = new ArrayList<>();
        String content = file.getText();
        
        for (Map.Entry<String, AccessibilityRule> entry : rules.entrySet()) {
            AccessibilityRule rule = entry.getValue();
            if (!rule.isEnabled()) {
                continue;
            }
            
            ValidationStrategy strategy = strategies.get(rule.getId());
            if (strategy != null && strategy.shouldApply(file)) {
                List<ValidationResult> results = strategy.validate(file, content);
                for (ValidationResult result : results) {
                    violations.add(new RuleViolation(rule, result));
                }
            }
        }
        
        // Sort by priority/severity
        violations.sort(Comparator.comparing(v -> v.getRule().getSeverity()));
        
        return violations;
    }
    
    /**
     * Get a rule by ID
     */
    public AccessibilityRule getRule(String ruleId) {
        return rules.get(ruleId);
    }
    
    /**
     * Get all rules
     */
    public Collection<AccessibilityRule> getAllRules() {
        return rules.values();
    }
    
    /**
     * Get rules by category
     */
    public List<AccessibilityRule> getRulesByCategory(AccessibilityRule.RuleCategory category) {
        List<AccessibilityRule> result = new ArrayList<>();
        for (AccessibilityRule rule : rules.values()) {
            if (rule.getCategory() == category) {
                result.add(rule);
            }
        }
        return result;
    }
    
    /**
     * Enable/disable a rule
     */
    public void setRuleEnabled(String ruleId, boolean enabled) {
        AccessibilityRule rule = rules.get(ruleId);
        if (rule != null) {
            rule.setEnabled(enabled);
        }
    }
    
    /**
     * Update rule configuration
     */
    public void updateRuleConfiguration(String ruleId, Map<String, Object> configuration) {
        AccessibilityRule rule = rules.get(ruleId);
        if (rule != null) {
            rule.setConfiguration(configuration);
        }
    }
    
    /**
     * Initialize with default rules
     */
    private void initialize() {
        // Register default providers
        registerProvider(new DefaultRuleProvider());
        registerProvider(new FluidRuleProvider());
    }
    
    /**
     * Reload all rules from providers
     */
    public void reloadRules() {
        rules.clear();
        strategies.clear();
        for (RuleProvider provider : providers) {
            provider.loadRules(this);
        }
    }
}