package com.typo3.fluid.linter.settings;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.annotations.MapAnnotation;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

@Service(Service.Level.PROJECT)
@State(name = "FluidAccessibilityRuleSettings",
        storages = {@Storage(StoragePathMacros.WORKSPACE_FILE)})
public class RuleSettingsState implements PersistentStateComponent<RuleSettingsState.State> {

    public static class State {
        public Map<String, Boolean> enabled = new HashMap<>();
        public Map<String, String> severity = new HashMap<>();
        public Map<String, Map<String, String>> config = new HashMap<>();
        public boolean universalEnabled = true;
        public boolean suppressLegacyDuplicates = false;
    }

    private State state = new State();
    private long version = 0L;

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
        version++;
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
        version++;
    }

    public void setSeverity(String ruleId, String severity) {
        state.severity.put(ruleId, severity);
        version++;
    }

    public void setRuleConfig(String ruleId, Map<String, String> cfg) {
        state.config.put(ruleId, cfg);
        version++;
    }

    public long getVersion() {
        return version;
    }

    // Global toggles
    public boolean isUniversalEnabled() {
        return state.universalEnabled;
    }

    public void setUniversalEnabled(boolean enabled) {
        state.universalEnabled = enabled;
        version++;
    }

    public boolean isSuppressLegacyDuplicates() {
        return state.suppressLegacyDuplicates;
    }

    public void setSuppressLegacyDuplicates(boolean suppress) {
        state.suppressLegacyDuplicates = suppress;
        version++;
    }
}
