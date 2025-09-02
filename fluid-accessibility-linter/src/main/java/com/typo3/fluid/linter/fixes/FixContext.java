package com.typo3.fluid.linter.fixes;

import java.util.HashMap;
import java.util.Map;

/**
 * Context information for creating fixes
 */
public class FixContext {
    private final String problemType;
    private final Map<String, Object> attributes;
    
    public FixContext(String problemType) {
        this.problemType = problemType;
        this.attributes = new HashMap<>();
    }
    
    public String getProblemType() {
        return problemType;
    }
    
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }
    
    public Object getAttribute(String key) {
        return attributes.get(key);
    }
    
    public String getStringAttribute(String key) {
        Object value = attributes.get(key);
        return value != null ? value.toString() : null;
    }
    
    public boolean hasAttribute(String key) {
        return attributes.containsKey(key);
    }
}