package com.typo3.fluid.linter.settings;

import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import com.typo3.fluid.linter.rules.AccessibilityRule;
import com.typo3.fluid.linter.rules.RuleEngine;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.util.*;

public class RuleSettingsConfigurable implements SearchableConfigurable {
    private final Project project;
    private JPanel panel;
    private JCheckBox universalEnabled;
    private JCheckBox suppressLegacy;
    private volatile boolean built;
    private static class Row {
        JCheckBox enabled;
        JComboBox<String> severity;
    }
    private final Map<String, Row> ruleRows = new LinkedHashMap<>();

    public RuleSettingsConfigurable(Project project) {
        this.project = project;
    }

    // Fallback: some IDE builds instantiate via default constructor; pick an open project or default.
    public RuleSettingsConfigurable() {
        this(com.intellij.openapi.project.ProjectManager.getInstance().getOpenProjects().length > 0
                ? com.intellij.openapi.project.ProjectManager.getInstance().getOpenProjects()[0]
                : com.intellij.openapi.project.ProjectManager.getInstance().getDefaultProject());
    }

    @Override
    public @Nls(capitalization = Nls.Capitalization.Title) String getDisplayName() {
        return "Fluid Accessibility";
    }

    @Override
    public @Nullable JComponent createComponent() {
        try {
            panel = new JPanel(new BorderLayout());
            JPanel listPanel = new JPanel();
            listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
            JScrollPane scroll = new JScrollPane(listPanel);
            scroll.setBorder(BorderFactory.createEmptyBorder());

            // Toolbar with toggles + Presets/Import/Export
            JPanel toolbar = new JPanel();
            universalEnabled = new JCheckBox("Enable Universal inspection (rule engine)");
            suppressLegacy = new JCheckBox("Suppress legacy duplicates (Link Text, Skip Links, Tables)");
            RuleSettingsState st0 = RuleSettingsState.getInstance(project);
            if (st0 != null) {
                universalEnabled.setSelected(st0.isUniversalEnabled());
                suppressLegacy.setSelected(st0.isSuppressLegacyDuplicates());
            }
            toolbar.add(universalEnabled);
            toolbar.add(suppressLegacy);
            JComboBox<String> presets = new JComboBox<>(Presets.names());
            javax.swing.JButton applyPreset = new javax.swing.JButton("Apply Preset");
            javax.swing.JButton importBtn = new javax.swing.JButton("Import Profile...");
            javax.swing.JButton exportBtn = new javax.swing.JButton("Export Profile...");
            toolbar.add(new JLabel("Preset:"));
            toolbar.add(presets);
            toolbar.add(applyPreset);
            toolbar.add(importBtn);
            toolbar.add(exportBtn);
            panel.add(toolbar, BorderLayout.NORTH);
            applyPreset.addActionListener(e -> {
                String name = (String) presets.getSelectedItem();
                if (name != null) {
                    RuleSettingsState.State s = Presets.buildPreset(name);
                    RuleSettingsState svc = RuleSettingsState.getInstance(project);
                    if (svc != null) {
                        svc.loadState(s);
                    }
                    reset();
                }
            });
            importBtn.addActionListener(e -> doImport());
            exportBtn.addActionListener(e -> doExport());

            // Placeholder while we gather rules
            listPanel.add(new JLabel("Loading rules…"));
            panel.add(scroll, BorderLayout.CENTER);

            // Populate rules off-EDT to avoid blocking Settings
            com.intellij.openapi.application.ApplicationManager.getApplication().executeOnPooledThread(() -> {
                Map<AccessibilityRule.RuleCategory, java.util.List<AccessibilityRule>> byCat = new TreeMap<>(Comparator.comparing(AccessibilityRule.RuleCategory::name));
                for (AccessibilityRule rule : RuleEngine.getInstance().getAllRules()) {
                    byCat.computeIfAbsent(rule.getCategory(), k -> new ArrayList<>()).add(rule);
                }
                RuleSettingsState state = RuleSettingsState.getInstance(project);
                com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater(() -> {
                    listPanel.removeAll();
                    ruleRows.clear();
                    for (Map.Entry<AccessibilityRule.RuleCategory, java.util.List<AccessibilityRule>> entry : byCat.entrySet()) {
                        JLabel cat = new JLabel(entry.getKey().getDisplayName());
                        cat.setBorder(BorderFactory.createEmptyBorder(8, 8, 4, 8));
                        listPanel.add(cat);
                        for (AccessibilityRule rule : entry.getValue()) {
                            Row row = new Row();
                            row.enabled = new JCheckBox(rule.getName());
                            row.enabled.setToolTipText(rule.getDescription());
                            Boolean ov = state != null ? state.getEnabledOverride(rule.getId()) : null;
                            row.enabled.setSelected(ov != null ? ov : rule.isEnabled());

                            row.severity = new JComboBox<>(new String[]{
                                    AccessibilityRule.RuleSeverity.ERROR.name(),
                                    AccessibilityRule.RuleSeverity.WARNING.name(),
                                    AccessibilityRule.RuleSeverity.WEAK_WARNING.name(),
                                    AccessibilityRule.RuleSeverity.INFO.name()
                            });
                            String sevOv = state != null ? state.getSeverityOverride(rule.getId()) : null;
                            row.severity.setSelectedItem(sevOv != null ? sevOv : rule.getSeverity().name());

                            JPanel line = new JPanel(new BorderLayout());
                            line.add(row.enabled, BorderLayout.CENTER);
                            JPanel right = new JPanel();
                            right.add(new JLabel("Severity:"));
                            right.add(row.severity);
                            line.add(right, BorderLayout.EAST);
                            listPanel.add(pad(line));

                            ruleRows.put(rule.getId(), row);
                        }
                    }
                    listPanel.revalidate();
                    listPanel.repaint();
                    built = true;
                });
            });

            // Auto-load project profile if present (async to avoid EDT stalls)
            tryAutoloadProjectProfileAsync();
            return panel;
        } catch (Throwable t) {
            // Fail-safe UI to avoid infinite loading spinner
            JPanel fallback = new JPanel(new BorderLayout());
            fallback.add(new JLabel("Failed to load Fluid Accessibility settings. See idea.log."), BorderLayout.NORTH);
            this.panel = fallback;
            return fallback;
        }
    }

    private static JComponent pad(JComponent c) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 8));
        p.add(c, BorderLayout.CENTER);
        return p;
    }

    private void tryAutoloadProjectProfileAsync() {
        String base = project.getBasePath();
        if (base == null) return;
        java.io.File f = new java.io.File(base, "a11y-profile.json");
        if (!f.isFile()) return;
        com.intellij.openapi.application.ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                ProfileIO.importProfile(project, f);
                com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater(this::reset);
            } catch (Throwable ignored) {
                // ignore malformed or missing Gson etc.
            }
        });
    }

    private void doImport() {
        javax.swing.JFileChooser chooser = new javax.swing.JFileChooser();
        chooser.setDialogTitle("Import Accessibility Profile (JSON)");
        int res = chooser.showOpenDialog(panel);
        if (res == javax.swing.JFileChooser.APPROVE_OPTION) {
            try {
                ProfileIO.importProfile(project, chooser.getSelectedFile());
                reset();
            } catch (Exception ex) {
                javax.swing.JOptionPane.showMessageDialog(panel, "Failed to import profile: " + ex.getMessage(),
                        "Import Error", javax.swing.JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void doExport() {
        // Apply current UI values before exporting
        apply();
        javax.swing.JFileChooser chooser = new javax.swing.JFileChooser();
        chooser.setDialogTitle("Export Accessibility Profile (JSON)");
        int res = chooser.showSaveDialog(panel);
        if (res == javax.swing.JFileChooser.APPROVE_OPTION) {
            try {
                ProfileIO.exportProfile(project, chooser.getSelectedFile());
            } catch (Exception ex) {
                javax.swing.JOptionPane.showMessageDialog(panel, "Failed to export profile: " + ex.getMessage(),
                        "Export Error", javax.swing.JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    @Override
    public boolean isModified() {
        if (!built || universalEnabled == null) return false;
        RuleSettingsState state = RuleSettingsState.getInstance(project);
        if (state == null) return false;
        if (state.isUniversalEnabled() != universalEnabled.isSelected()) return true;
        if (state.isSuppressLegacyDuplicates() != suppressLegacy.isSelected()) return true;
        for (Map.Entry<String, Row> e : ruleRows.entrySet()) {
            String id = e.getKey();
            Row row = e.getValue();
            Boolean ov = state.getEnabledOverride(id);
            boolean current = row.enabled.isSelected();
            AccessibilityRule rule = RuleEngine.getInstance().getRule(id);
            if ((ov == null ? (rule != null && rule.isEnabled()) : ov) != current) return true;

            String sevOv = state.getSeverityOverride(id);
            String currentSev = Objects.toString(row.severity.getSelectedItem(), null);
            String baseSev = rule != null ? rule.getSeverity().name() : null;
            if (!Objects.equals(sevOv != null ? sevOv : baseSev, currentSev)) return true;
        }
        return false;
    }

    @Override
    public void apply() {
        if (!built || universalEnabled == null) return;
        RuleSettingsState state = RuleSettingsState.getInstance(project);
        if (state == null) return;
        state.setUniversalEnabled(universalEnabled.isSelected());
        state.setSuppressLegacyDuplicates(suppressLegacy.isSelected());
        for (Map.Entry<String, Row> e : ruleRows.entrySet()) {
            state.setRuleEnabled(e.getKey(), e.getValue().enabled.isSelected());
            state.setSeverity(e.getKey(), Objects.toString(e.getValue().severity.getSelectedItem(), null));
        }
    }

    @Override
    public void reset() {
        if (!built || universalEnabled == null) return;
        RuleSettingsState state = RuleSettingsState.getInstance(project);
        boolean univ = state != null ? state.isUniversalEnabled() : true;
        boolean suppr = state != null ? state.isSuppressLegacyDuplicates() : false;
        universalEnabled.setSelected(univ);
        suppressLegacy.setSelected(suppr);
        for (Map.Entry<String, Row> e : ruleRows.entrySet()) {
            AccessibilityRule rule = RuleEngine.getInstance().getRule(e.getKey());
            Row row = e.getValue();
            Boolean ov = state != null ? state.getEnabledOverride(e.getKey()) : null;
            row.enabled.setSelected(ov != null ? ov : (rule != null && rule.isEnabled()));
            String sevOv = state != null ? state.getSeverityOverride(e.getKey()) : null;
            row.severity.setSelectedItem(sevOv != null ? sevOv : (rule != null ? rule.getSeverity().name() : AccessibilityRule.RuleSeverity.WARNING.name()));
        }
    }

    @Override
    public @Nullable Runnable enableSearch(String option) {
        return null;
    }

    @Override
    public @NotNull String getId() {
        return "fluid.accessibility.settings";
    }
}
