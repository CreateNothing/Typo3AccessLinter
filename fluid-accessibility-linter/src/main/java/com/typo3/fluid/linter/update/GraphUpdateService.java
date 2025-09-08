package com.typo3.fluid.linter.update;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.*;
import com.intellij.util.ui.update.MergingUpdateQueue;
import com.intellij.util.ui.update.Update;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;

import java.util.ArrayList;
import java.util.List;

import static com.typo3.fluid.linter.update.FileChangeFilter.Reason;

@Service(Service.Level.PROJECT)
final class GraphUpdateService implements Disposable, DumbAware {
    private static final Logger LOG = Logger.getInstance(GraphUpdateService.class);

    private final Project project;
    private final MergingUpdateQueue queue;
    private volatile boolean testPassThrough = false;
    private volatile int runCountForTests = 0;
    private volatile boolean lastRunHadChanges = false;
    private final java.util.List<VFileEvent> backlogFluidEvents = new java.util.ArrayList<>();
    private final java.util.Set<VirtualFile> backlogConfigFiles = new java.util.HashSet<>();
    private boolean flushScheduled = false;

    GraphUpdateService(@NotNull Project project) {
        this.project = project;
        this.queue = new MergingUpdateQueue(
                "FluidGraph",
                300,
                true,
                null,
                project,
                null,
                false
        );
        this.queue.setRestartTimerOnAdd(true);
        if (testPassThrough) this.queue.setPassThrough(true);

        // Subscribe to VFS changes
        var connection = project.getMessageBus().connect(this);
        connection.subscribe(VirtualFileManager.VFS_CHANGES, new VfsListener());
        connection.subscribe(DumbService.DUMB_MODE, new DumbService.DumbModeListener() {
            @Override public void enteredDumbMode() { LOG.info("FluidGraph: entered Dumb Mode; deferring heavy recomputations"); }
            @Override public void exitDumbMode() { LOG.info("FluidGraph: exited Dumb Mode; flushing backlog"); flushBacklogWhenSmart(); }
        });

        LOG.info("GraphUpdateService initialized: BulkFileListener wired");
    }

    private final class VfsListener implements BulkFileListener {
        @Override
        public void after(@NotNull List<? extends VFileEvent> events) {
            if (events.isEmpty()) return;

            List<VFileEvent> relevant = new ArrayList<>(events.size());
            for (VFileEvent e : events) {
                VirtualFile vf = extractFile(e);
                if (vf != null && vf.isValid() && FileChangeFilter.isRelevant(vf)) {
                    relevant.add(e);
                } else if (e instanceof VFilePropertyChangeEvent) {
                    // Renames: check both old/new names for relevance
                    VFilePropertyChangeEvent pe = (VFilePropertyChangeEvent) e;
                    if (VirtualFile.PROP_NAME.equals(pe.getPropertyName())) {
                        VirtualFile file = pe.getFile();
                        if (file != null && file.isValid()) {
                            String oldName = String.valueOf(pe.getOldValue());
                            String newName = String.valueOf(pe.getNewValue());
                            boolean oldRelevant = isNameRelevant(file, oldName);
                            boolean newRelevant = isNameRelevant(file, newName);
                            if (oldRelevant || newRelevant) {
                                relevant.add(e);
                            }
                        }
                    }
                }
            }

            if (relevant.isEmpty()) return;

            queue.queue(new Update("vfs-batch") {
                @Override
                public void run() {
                    logRelevantEvents(relevant);
                    if (DumbService.isDumb(project)) {
                        stashForLater(relevant);
                        flushBacklogWhenSmart();
                        return;
                    }
                    // Forward config-related changes to ContextManager for incremental diffs
                    var changedConfigFiles = new ArrayList<VirtualFile>();
                    var fluidEvents = new ArrayList<VFileEvent>();
                    for (VFileEvent e : relevant) {
                        VirtualFile f = extractFile(e);
                        if (f == null) continue;
                        Reason reason = FileChangeFilter.classify(f);
                        if (reason == Reason.TYPOSCRIPT || reason == Reason.SITE_SETTINGS) {
                            changedConfigFiles.add(f);
                        } else if (reason == Reason.FLUID) {
                            fluidEvents.add(e);
                        } else if (e instanceof VFilePropertyChangeEvent) {
                            VFilePropertyChangeEvent pe = (VFilePropertyChangeEvent) e;
                            if (VirtualFile.PROP_NAME.equals(pe.getPropertyName())) {
                                VirtualFile file = pe.getFile();
                                if (file != null) {
                                    String oldName = String.valueOf(pe.getOldValue());
                                    String newName = String.valueOf(pe.getNewValue());
                                    boolean oldRelevant = isNameRelevant(file, oldName);
                                    boolean newRelevant = isNameRelevant(file, newName);
                                    if (oldRelevant || newRelevant) {
                                        // Determine if rename concerns Fluid or config by extension heuristics
                                        if (oldName.endsWith(".html") || newName.endsWith(".html")) {
                                            fluidEvents.add(e);
                                        } else {
                                            changedConfigFiles.add(file);
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (!changedConfigFiles.isEmpty()) {
                        project.getService(com.typo3.fluid.linter.context.FluidContextManager.class)
                               .onConfigFilesChanged(changedConfigFiles);
                    }
                    if (!fluidEvents.isEmpty()) {
                        project.getService(com.typo3.fluid.linter.catalog.ImplementationCatalogService.class)
                               .onFluidVfsEvents(fluidEvents);
                        // Invalidate flatten cache for directly changed files
                        var changedFiles = new java.util.ArrayList<VirtualFile>();
                        for (VFileEvent e : fluidEvents) {
                            VirtualFile f = extractFile(e);
                            if (f != null && f.isValid()) changedFiles.add(f);
                        }
                        if (!changedFiles.isEmpty()) {
                            project.getService(com.typo3.fluid.linter.flatten.FlattenCacheService.class)
                                   .invalidateByFiles(changedFiles);
                        }
                    }
                    // Recompute effective resolutions after catalog/context updates
                    project.getService(com.typo3.fluid.linter.resolution.EffectiveResolutionService.class)
                           .refreshDefault();
                    // Subsequent steps: include index refresh, effective resolution, flatten invalidation.
                    runCountForTests++;
                    lastRunHadChanges = !changedConfigFiles.isEmpty() || !fluidEvents.isEmpty();
                }
            });
        }
    }

    private static boolean isNameRelevant(@NotNull VirtualFile file, @NotNull String nameCandidate) {
        VirtualFile parent = file.getParent();
        String parentPath = parent != null ? parent.getPath() : "";
        return FileChangeFilter.isRelevantNameInParent(parentPath, nameCandidate);
    }

    private synchronized void stashForLater(@NotNull List<? extends VFileEvent> events) {
        for (VFileEvent e : events) {
            VirtualFile f = extractFile(e);
            if (f == null) continue;
            Reason reason = FileChangeFilter.classify(f);
            if (reason == Reason.TYPOSCRIPT || reason == Reason.SITE_SETTINGS) {
                backlogConfigFiles.add(f);
            } else if (reason == Reason.FLUID || e instanceof VFilePropertyChangeEvent) {
                backlogFluidEvents.add(e);
            }
        }
    }

    private synchronized void flushBacklogWhenSmart() {
        if (flushScheduled) return;
        flushScheduled = true;
        DumbService.getInstance(project).runWhenSmart(() -> {
            List<VFileEvent> fluid;
            List<VirtualFile> cfg;
            synchronized (GraphUpdateService.this) {
                flushScheduled = false;
                fluid = new ArrayList<>(backlogFluidEvents);
                cfg = new ArrayList<>(backlogConfigFiles);
                backlogFluidEvents.clear();
                backlogConfigFiles.clear();
            }
            if (!cfg.isEmpty() || !fluid.isEmpty()) {
                queue.queue(new Update("flush-backlog") {
                    @Override public void run() {
                        if (!cfg.isEmpty()) {
                            project.getService(com.typo3.fluid.linter.context.FluidContextManager.class)
                                   .onConfigFilesChanged(cfg);
                        }
                        if (!fluid.isEmpty()) {
                            project.getService(com.typo3.fluid.linter.catalog.ImplementationCatalogService.class)
                                   .onFluidVfsEvents(fluid);
                            var changedFiles = new java.util.ArrayList<VirtualFile>();
                            for (VFileEvent e : fluid) { VirtualFile f = extractFile(e); if (f != null && f.isValid()) changedFiles.add(f); }
                            if (!changedFiles.isEmpty()) {
                                project.getService(com.typo3.fluid.linter.flatten.FlattenCacheService.class)
                                       .invalidateByFiles(changedFiles);
                            }
                        }
                        project.getService(com.typo3.fluid.linter.resolution.EffectiveResolutionService.class)
                               .refreshDefault();
                        runCountForTests++;
                        lastRunHadChanges = !cfg.isEmpty() || !fluid.isEmpty();
                    }
                });
            }
        });
    }

    private static VirtualFile extractFile(@NotNull VFileEvent e) {
        if (e instanceof VFileContentChangeEvent) return ((VFileContentChangeEvent) e).getFile();
        if (e instanceof VFileCreateEvent) return ((VFileCreateEvent) e).getFile();
        if (e instanceof VFileDeleteEvent) return ((VFileDeleteEvent) e).getFile();
        if (e instanceof VFileMoveEvent) return ((VFileMoveEvent) e).getFile();
        if (e instanceof VFileCopyEvent) return ((VFileCopyEvent) e).getNewParent();
        if (e instanceof VFilePropertyChangeEvent) return ((VFilePropertyChangeEvent) e).getFile();
        return null;
    }

    private static void logRelevantEvents(List<? extends VFileEvent> events) {
        int fluid = 0, ts = 0, yaml = 0;
        for (VFileEvent e : events) {
            VirtualFile f = extractFile(e);
            if (f == null) continue;
            Reason r = FileChangeFilter.classify(f);
            switch (r) {
                case FLUID: fluid++; break;
                case TYPOSCRIPT: ts++; break;
                case SITE_SETTINGS: yaml++; break;
                default: break;
            }
        }
        LOG.info("FluidGraph: relevant VFS changes -> html=" + fluid + ", typoscript=" + ts + ", siteYaml=" + yaml);
    }

    @Override
    public void dispose() {
        Disposer.dispose(queue);
    }

    // Test hooks
    @TestOnly void enableImmediateTestMode() { this.testPassThrough = true; this.queue.setPassThrough(true); }
    @TestOnly void flushForTests() { this.queue.flush(); }
    @TestOnly int getRunCountForTests() { return runCountForTests; }
    @TestOnly void resetRunCountForTests() { runCountForTests = 0; }
    @TestOnly boolean lastRunHadChanges() { return lastRunHadChanges; }
}
