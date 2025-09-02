package com.typo3.fluid.linter.rules;

import com.intellij.codeInspection.ProblemHighlightType;
import java.util.List;
import java.util.Map;

/**
 * Domain model for accessibility rules
 */
public class AccessibilityRule {
    private String id;
    private String name;
    private String description;
    private RuleSeverity severity;
    private RuleCategory category;
    private List<String> tags;
    private Map<String, Object> configuration;
    private boolean enabled;
    
    public enum RuleSeverity {
        ERROR(ProblemHighlightType.ERROR),
        WARNING(ProblemHighlightType.GENERIC_ERROR_OR_WARNING),
        WEAK_WARNING(ProblemHighlightType.WEAK_WARNING),
        INFO(ProblemHighlightType.INFORMATION);
        
        private final ProblemHighlightType highlightType;
        
        RuleSeverity(ProblemHighlightType highlightType) {
            this.highlightType = highlightType;
        }
        
        public ProblemHighlightType getHighlightType() {
            return highlightType;
        }
    }
    
    public enum RuleCategory {
        IMAGES("Images and Media"),
        FORMS("Forms and Inputs"),
        NAVIGATION("Navigation and Links"),
        STRUCTURE("Document Structure"),
        ARIA("ARIA Attributes"),
        SEMANTICS("Semantic HTML"),
        KEYBOARD("Keyboard Navigation"),
        FOCUS("Focus Management"),
        COLOR("Color and Contrast"),
        LANGUAGE("Language and Text");
        
        private final String displayName;
        
        RuleCategory(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    // Getters and setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public RuleSeverity getSeverity() {
        return severity;
    }
    
    public void setSeverity(RuleSeverity severity) {
        this.severity = severity;
    }
    
    public RuleCategory getCategory() {
        return category;
    }
    
    public void setCategory(RuleCategory category) {
        this.category = category;
    }
    
    public List<String> getTags() {
        return tags;
    }
    
    public void setTags(List<String> tags) {
        this.tags = tags;
    }
    
    public Map<String, Object> getConfiguration() {
        return configuration;
    }
    
    public void setConfiguration(Map<String, Object> configuration) {
        this.configuration = configuration;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public Object getConfigValue(String key) {
        return configuration != null ? configuration.get(key) : null;
    }
    
    public String getConfigString(String key) {
        Object value = getConfigValue(key);
        return value != null ? value.toString() : null;
    }
    
    public boolean getConfigBoolean(String key, boolean defaultValue) {
        Object value = getConfigValue(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return defaultValue;
    }
}