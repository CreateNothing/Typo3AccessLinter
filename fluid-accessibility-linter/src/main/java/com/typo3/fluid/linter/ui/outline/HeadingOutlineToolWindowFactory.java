package com.typo3.fluid.linter.ui.outline;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

public class HeadingOutlineToolWindowFactory implements ToolWindowFactory, DumbAware {
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        HeadingOutlinePanel panel = new HeadingOutlinePanel(project);
        ContentFactory factory = ContentFactory.getInstance();
        Content content = factory.createContent(panel, "Heading Outline", false);
        toolWindow.getContentManager().addContent(content);
    }
}

