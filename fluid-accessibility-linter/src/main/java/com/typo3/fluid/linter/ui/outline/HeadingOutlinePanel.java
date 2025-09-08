package com.typo3.fluid.linter.ui.outline;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.messages.MessageBusConnection;
import com.typo3.fluid.linter.context.ContextId;
import com.typo3.fluid.linter.flatten.FlattenCacheService;
import com.typo3.fluid.linter.outline.UnsavedOutlineService;
import com.typo3.fluid.linter.outline.UnsavedOutlineTopic;
import com.typo3.fluid.linter.resolution.ResolutionChangeTopic;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.Comparator;
import java.util.List;

class HeadingOutlinePanel extends JPanel {
    private final Project project;
    private final JBList<Item> list;
    private final DefaultListModel<Item> model;
    private final JBLabel status;
    private final MessageBusConnection connection;
    private boolean indexing;

    private VirtualFile currentFile;
    private Integer lastSelectedOffset;

    HeadingOutlinePanel(@NotNull Project project) {
        super(new BorderLayout());
        this.project = project;
        this.model = new DefaultListModel<>();
        this.list = new JBList<>(model);
        this.status = new JBLabel("No file selected");
        add(new JBScrollPane(list), BorderLayout.CENTER);
        add(status, BorderLayout.SOUTH);

        // Listen for active editor changes
        project.getMessageBus().connect().subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, new FileEditorManagerListener() {
            @Override public void selectionChanged(@NotNull FileEditorManagerEvent event) { onEditorSelectionChanged(); }
        });

        // Subscribe to unsaved outline updates and resolution changes
        connection = project.getMessageBus().connect();
        connection.subscribe(UnsavedOutlineTopic.TOPIC, (file, result) -> {
            if (file.equals(currentFile)) applyUnsaved(result);
        });
        connection.subscribe(ResolutionChangeTopic.TOPIC, diff -> {
            // Coarse refresh from saved content
            if (currentFile != null) refreshFromSaved(currentFile);
        });

        // Initialize with current editor
        onEditorSelectionChanged();

        // Indexing status
        indexing = DumbService.isDumb(project);
        updateStatusSuffix("");
        connection.subscribe(DumbService.DUMB_MODE, new DumbService.DumbModeListener() {
            @Override public void enteredDumbMode() { indexing = true; updateStatusSuffix(""); }
            @Override public void exitDumbMode() { indexing = false; updateStatusSuffix(""); if (currentFile != null) refreshFromSaved(currentFile); }
        });
    }

    private void onEditorSelectionChanged() {
        VirtualFile[] files = FileEditorManager.getInstance(project).getSelectedFiles();
        VirtualFile next = files.length > 0 ? files[0] : null;
        if (next == null || next.equals(currentFile)) return;
        currentFile = next;
        lastSelectedOffset = null;
        refreshFromSaved(currentFile);
    }

    private void applyUnsaved(@NotNull UnsavedOutlineService.OutlineResult result) {
        ApplicationManager.getApplication().invokeLater(() -> {
            int preserve = selectedStartOffset();
            model.clear();
            for (UnsavedOutlineService.OutlineEntry e : result.entries) {
                model.addElement(new Item(e.level, e.text, e.startOffset));
            }
            restoreSelectionNear(preserve);
            updateStatusSuffix(result.unsavedPreview ? "Unsaved (preview)" : "Saved");
        });
    }

    private void refreshFromSaved(@NotNull VirtualFile file) {
        FlattenCacheService cache = project.getService(FlattenCacheService.class);
        FlattenCacheService.FlattenResult r = cache.getOrCompute(ContextId.DEFAULT, file);
        ApplicationManager.getApplication().invokeLater(() -> {
            int preserve = selectedStartOffset();
            model.clear();
            for (FlattenCacheService.FlattenedHeading h : r.headings) {
                model.addElement(new Item(h.level, h.text, h.startOffset));
            }
            restoreSelectionNear(preserve);
            updateStatusSuffix("Saved");
        });
    }

    private void updateStatusSuffix(String base) {
        String prefix = indexing ? "Indexing… " : "";
        status.setText(prefix + (base == null || base.isEmpty() ? status.getText() : base));
    }

    private int selectedStartOffset() {
        Item sel = list.getSelectedValue();
        return sel != null ? sel.startOffset : (lastSelectedOffset != null ? lastSelectedOffset : -1);
    }

    private void restoreSelectionNear(int offset) {
        if (offset < 0 || model.isEmpty()) { if (!model.isEmpty()) list.setSelectedIndex(0); return; }
        int bestIdx = 0;
        int bestDist = Integer.MAX_VALUE;
        for (int i = 0; i < model.size(); i++) {
            int d = Math.abs(model.get(i).startOffset - offset);
            if (d < bestDist) { bestDist = d; bestIdx = i; }
        }
        int idx = Math.max(0, Math.min(bestIdx, model.size() - 1));
        list.setSelectedIndex(idx);
        list.ensureIndexIsVisible(idx);
        lastSelectedOffset = model.get(idx).startOffset;
    }

    static final class Item {
        final int level; final String text; final int startOffset;
        Item(int level, String text, int startOffset) { this.level = level; this.text = text; this.startOffset = startOffset; }
        @Override public String toString() { return "H" + level + ": " + text; }
    }
}
