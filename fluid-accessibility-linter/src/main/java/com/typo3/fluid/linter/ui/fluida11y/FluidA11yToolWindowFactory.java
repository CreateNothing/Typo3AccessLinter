package com.typo3.fluid.linter.ui.fluida11y;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.typo3.fluid.linter.ui.fluida11y.tabs.DiagnosticsTab;
import com.typo3.fluid.linter.ui.fluida11y.tabs.OutlineTab;
import com.typo3.fluid.linter.ui.fluida11y.tabs.OverridesTab;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * Unified Fluid A11y tool window hosting Outline, Overrides, and Diagnostics tabs.
 */
public class FluidA11yToolWindowFactory implements ToolWindowFactory, DumbAware {
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        // Ensure Aggregator service is initialized so subscribers receive first snapshot
        project.getService(Aggregator.class).requestRefresh();

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Outline", new OutlineTab(project));
        tabs.addTab("Parents & Overrides", new OverridesTab(project));
        tabs.addTab("Diagnostics", new DiagnosticsTab(project));

        ContentFactory factory = ContentFactory.getInstance();
        Content content = factory.createContent(tabs, "Fluid A11y", false);
        toolWindow.getContentManager().addContent(content);
    }
}

