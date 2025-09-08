package com.typo3.fluid.linter.update;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import org.jetbrains.annotations.NotNull;

    public final class GraphUpdateStartupActivity implements StartupActivity, DumbAware {
        @Override
        public void runActivity(@NotNull Project project) {
            // Initialize the project service so the VFS listener is wired.
            project.getService(GraphUpdateService.class);
            // Initialize unsaved outline computation for active editor tabs
            project.getService(com.typo3.fluid.linter.outline.UnsavedOutlineService.class);
            // Initialize context manager to build initial RootPathSet
            project.getService(com.typo3.fluid.linter.context.FluidContextManager.class);
            // Attempt auto-detect of roots if no TypoScript/site settings are present
            project.getService(com.typo3.fluid.linter.context.AutoRootDetector.class).detectAndApplyIfEmpty();
            // Initialize implementation catalog from current context roots
            project.getService(com.typo3.fluid.linter.catalog.ImplementationCatalogService.class);
            // Build initial effective resolutions and emit a first diff so UI can populate immediately
            project.getService(com.typo3.fluid.linter.resolution.EffectiveResolutionService.class)
                   .refreshDefault();
        }
    }
