package com.typo3.fluid.linter.ui.fluida11y.editor;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.EditorNotificationProvider;
import com.typo3.fluid.linter.context.AutoRootDetector;
import com.typo3.fluid.linter.resolution.EffectiveResolutionService;
import com.typo3.fluid.linter.ui.fluida11y.A11yUiState;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.function.Function;

public final class InspectorBarProvider implements EditorNotificationProvider, DumbAware {
    @Override
    public @NotNull Function<? super FileEditor, ? extends JComponent> collectNotificationData(@NotNull Project project, @NotNull VirtualFile file) {
        if (!isHtml(file)) return EditorNotificationProvider.CONST_NULL;
        return editor -> createPanel(project);
    }

    private static boolean isHtml(@NotNull VirtualFile file) {
        String name = file.getName().toLowerCase();
        return name.endsWith(".html");
    }

    private static JComponent createPanel(@NotNull Project project) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        JLabel label = new JLabel("Fluid A11y");
        JButton rebuild = new JButton("Rebuild");
        rebuild.addActionListener(e -> project.getService(EffectiveResolutionService.class).refreshDefault());
        JButton detect = new JButton("Detect Roots");
        detect.addActionListener(e -> project.getService(AutoRootDetector.class).detectAndApplyIfEmpty());
        JButton toggle = new JButton("Toggle Unsaved Preview");
        toggle.addActionListener(e -> project.getService(A11yUiState.class).toggleUnsavedPreview());
        JButton live = new JButton("Toggle Live Updates");
        live.addActionListener(e -> project.getService(A11yUiState.class).toggleLiveUpdates());
        panel.add(label);
        panel.add(rebuild);
        panel.add(detect);
        panel.add(toggle);
        panel.add(live);
        return panel;
    }
}
