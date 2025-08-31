package com.typo3.fluid.linter.settings;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.annotations.MapAnnotation;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

@State(name = "FluidAccessibilityRuleSettings",
        storages = {@Storage(StoragePathMacros.WORKSPACE_FILE)})
public class RuleSettingsState implements PersistentStateComponent<RuleSettingsState.State> {

    public static class State {
        public Map<String, Boolean> enabled = new HashMap<>();
        public Map<String, String> severity = new HashMap<>();
        public Map<String, Map<String, String>> config = new HashMap<>();
    }

    private State state = new State();

    public static RuleSettingsState getInstance(Project project) {
        return project.getService(RuleSettingsState.class);
    }

    @Nullable
    @Override
    public State getState() {
        return state;
    }

    @Override
    public void loadState(State state) {
        this.state = state != null ? state : new State();
    }

    public Boolean getEnabledOverride(String ruleId) {
        return state.enabled.get(ruleId);
    }

    public String getSeverityOverride(String ruleId) {
        return state.severity.get(ruleId);
    }

    public Map<String, String> getRuleConfig(String ruleId) {
        return state.config.get(ruleId);
    }

    public void setRuleEnabled(String ruleId, boolean enabled) {
        state.enabled.put(ruleId, enabled);
    }

    public void setSeverity(String ruleId, String severity) {
        state.severity.put(ruleId, severity);
    }

    public void setRuleConfig(String ruleId, Map<String, String> cfg) {
        state.config.put(ruleId, cfg);
    }
}

