package com.typo3.fluid.linter.ui.fluida11y;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.util.ui.update.MergingUpdateQueue;
import com.intellij.util.ui.update.Update;
import com.typo3.fluid.linter.ui.fluida11y.vm.*;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Aggregates service topics into immutable view-model snapshots for the unified tool window.
 * This is a scaffold with minimal data; richer reducers will come in subsequent steps.
 */
@Service(Service.Level.PROJECT)
public final class Aggregator implements DumbAware {
    private final Project project;
    private final MergingUpdateQueue queue;
    private final java.util.Map<RowKey, OverridesVM.Row> overrides = new java.util.LinkedHashMap<>();

    public Aggregator(@NotNull Project project) {
        this.project = project;
        this.queue = new MergingUpdateQueue("FluidA11yAggregator", 250, true, null, project);
        this.queue.setRestartTimerOnAdd(true);

        var connection = project.getMessageBus().connect();
        connection.subscribe(com.typo3.fluid.linter.resolution.ResolutionChangeTopic.TOPIC, diff -> {
            for (com.typo3.fluid.linter.resolution.ResolutionChange ch : diff.changes) {
                RowKey key = new RowKey(ch.key.ctx, ch.key.kind, ch.key.logicalName);
                String badge = toBadge(ch.oldFile, ch.newFile);
                String path = displayPath(ch.newFile != null ? ch.newFile : ch.oldFile);
                overrides.put(key, new OverridesVM.Row(key.ctx, key.kind, key.logicalName, badge, path));
            }
            requestRefresh();
        });
        connection.subscribe(com.typo3.fluid.linter.context.ContextChangeTopic.TOPIC, diff -> {
            overrides.clear();
            requestRefresh();
        });
        connection.subscribe(DumbService.DUMB_MODE, new DumbService.DumbModeListener() {
            @Override public void enteredDumbMode() { requestRefresh(); }
            @Override public void exitDumbMode() { requestRefresh(); }
        });

        // Initial snapshot
        queue.queue(new Update("initial") { @Override public void run() { publishSnapshot(); } });
    }

    public void requestRefresh() {
        if (!project.getService(com.typo3.fluid.linter.ui.fluida11y.A11yUiState.class).isLiveUpdatesEnabled()) return;
        queue.queue(new Update("refresh") { @Override public void run() { publishSnapshot(); } });
    }

    private void publishSnapshot() {
        boolean indexing = DumbService.isDumb(project);
        long now = System.currentTimeMillis();

        OutlineVM outline = new OutlineVM(indexing, false, now, new ArrayList<>());
        java.util.List<OverridesVM.Row> rows = new java.util.ArrayList<>(overrides.values());
        OverridesVM overridesVM = new OverridesVM(indexing, now, rows);

        int explicitRoots = 0;
        int autoRoots = 0;
        com.typo3.fluid.linter.context.FluidContextManager cm = project.getService(com.typo3.fluid.linter.context.FluidContextManager.class);
        com.typo3.fluid.linter.context.RootPathSet eff = cm.get(com.typo3.fluid.linter.context.ContextId.DEFAULT);
        if (eff != null) {
            explicitRoots = eff.templates().size() + eff.layouts().size() + eff.partials().size();
        }
        com.typo3.fluid.linter.context.AutoRootDetector det = project.getService(com.typo3.fluid.linter.context.AutoRootDetector.class);
        com.typo3.fluid.linter.context.RootPathSet last = det.getLastDetected();
        if (last != null) autoRoots = last.templates().size() + last.layouts().size() + last.partials().size();

        int impacted = rows.size();
        int queueDepth = 0; // placeholder; GraphUpdateService does not expose this publicly
        DiagnosticsVM diagnostics = new DiagnosticsVM(indexing, now, explicitRoots, autoRoots, impacted, queueDepth);

        AggregatedSnapshot snap = new AggregatedSnapshot(outline, overridesVM, diagnostics);
        project.getMessageBus().syncPublisher(FluidA11yAggregatorTopic.TOPIC).updated(snap);
    }

    private String toBadge(com.intellij.openapi.vfs.VirtualFile oldF, com.intellij.openapi.vfs.VirtualFile newF) {
        if (oldF == null && newF != null) return "override introduced";
        if (oldF != null && newF == null) return "override removed";
        if (oldF != null && newF != null && !java.util.Objects.equals(oldF.getUrl(), newF.getUrl())) return "override changed";
        return "override changed";
    }

    private String displayPath(com.intellij.openapi.vfs.VirtualFile file) {
        if (file == null) return "(none)";
        String base = project.getBasePath();
        String full = file.getPath();
        if (base != null && full.startsWith(base)) return full.substring(base.length() + 1);
        return full;
    }

    private static final class RowKey {
        final com.typo3.fluid.linter.context.ContextId ctx; final com.typo3.fluid.linter.catalog.ImplementationKind kind; final String name;
        RowKey(com.typo3.fluid.linter.context.ContextId ctx, com.typo3.fluid.linter.catalog.ImplementationKind kind, String name) { this.ctx = ctx; this.kind = kind; this.name = name; }
        @Override public boolean equals(Object o) { if (this == o) return true; if (!(o instanceof RowKey)) return false; RowKey that = (RowKey) o; return java.util.Objects.equals(ctx, that.ctx) && kind == that.kind && java.util.Objects.equals(name, that.name); }
        @Override public int hashCode() { return java.util.Objects.hash(ctx, kind, name); }
    }
}
