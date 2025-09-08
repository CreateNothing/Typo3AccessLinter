package com.typo3.fluid.linter.ui.parents;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

public class ParentsOverridesToolWindowFactory implements ToolWindowFactory, DumbAware {
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        ParentsOverridesPanel panel = new ParentsOverridesPanel(project);
        ContentFactory factory = ContentFactory.getInstance();
        Content content = factory.createContent(panel, "Parents & Overrides", false);
        toolWindow.getContentManager().addContent(content);
    }
}

