package com.typo3.fluid.linter.resolution;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ui.update.MergingUpdateQueue;
import com.intellij.util.ui.update.Update;
import com.typo3.fluid.linter.catalog.ImplementationCatalogService;
import com.typo3.fluid.linter.catalog.ImplementationKind;
import com.typo3.fluid.linter.context.ContextChangeTopic;
import com.typo3.fluid.linter.context.ContextId;
import org.jetbrains.annotations.NotNull;

import java.util.*;

@Service(Service.Level.PROJECT)
public final class EffectiveResolutionService implements DumbAware {
    private static final Logger LOG = Logger.getInstance(EffectiveResolutionService.class);

    private final Project project;
    private final ImplementationCatalogService catalog;
    private final Map<ResolutionKey, VirtualFile> effective = new HashMap<>();
    private final MergingUpdateQueue queue;

    public EffectiveResolutionService(@NotNull Project project) {
        this.project = project;
        this.catalog = project.getService(ImplementationCatalogService.class);
        this.queue = new MergingUpdateQueue("FluidEffectiveResolution", 100, true, null, project);
        project.getMessageBus().connect().subscribe(ContextChangeTopic.TOPIC, diff -> refreshAll(ContextId.DEFAULT));
    }

    public void refreshAll(@NotNull ContextId ctx) {
        queue.queue(new Update("recompute-" + ctx) {
            @Override
            public void run() {
                doRefreshAll(ctx);
            }
        });
    }

    public void refreshDefault() { refreshAll(ContextId.DEFAULT); }

    private synchronized void doRefreshAll(@NotNull ContextId ctx) {
        Set<ResolutionKey> keys = new HashSet<>();
        for (ImplementationKind k : ImplementationKind.values()) {
            for (String name : catalog.logicalNames(ctx, k)) {
                keys.add(new ResolutionKey(ctx, k, name));
            }
        }
        // Also include previously known keys (to detect removals)
        for (ResolutionKey k : new ArrayList<>(effective.keySet())) if (k.ctx.equals(ctx)) keys.add(k);

        List<ResolutionChange> changes = new ArrayList<>();
        for (ResolutionKey key : keys) {
            VirtualFile prev = effective.get(key);
            VirtualFile next = catalog.effective(key.ctx, key.kind, key.logicalName);
            if (!Objects.equals(urlOf(prev), urlOf(next))) {
                effective.put(key, next);
                changes.add(new ResolutionChange(key, prev, next));
            }
        }

        if (!changes.isEmpty()) {
            project.getMessageBus().syncPublisher(ResolutionChangeTopic.TOPIC)
                   .resolutionsChanged(new ResolutionDiff(Collections.unmodifiableList(changes)));
            LOG.info("Effective resolutions updated: " + changes.size() + " changes");
        }
    }

    private static String urlOf(VirtualFile f) { return f != null ? f.getUrl() : null; }
}

