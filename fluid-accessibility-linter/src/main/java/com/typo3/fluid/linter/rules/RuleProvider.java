package com.typo3.fluid.linter.rules;

/**
 * Interface for providing rules to the rule engine
 */
public interface RuleProvider {
    
    /**
     * Load rules into the rule engine
     * @param engine The rule engine to load rules into
     */
    void loadRules(RuleEngine engine);
    
    /**
     * Get the name of this provider
     * @return Provider name
     */
    String getName();
}