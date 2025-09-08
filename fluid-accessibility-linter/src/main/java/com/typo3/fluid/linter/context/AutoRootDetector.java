package com.typo3.fluid.linter.context;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileVisitor;
import org.jetbrains.annotations.NotNull;

import java.util.*;

@Service(Service.Level.PROJECT)
public final class AutoRootDetector implements DumbAware {
    private static final Logger LOG = Logger.getInstance(AutoRootDetector.class);

    private final Project project;
    private volatile boolean enabled = true; // default ON
    private volatile RootPathSet lastDetected;

    public AutoRootDetector(@NotNull Project project) {
        this.project = project;
    }

    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public boolean isEnabled() { return enabled; }

    public synchronized void detectAndApplyIfEmpty() {
        if (!enabled) return;
        FluidContextManager cm = project.getService(FluidContextManager.class);
        RootPathSet current = cm.get(ContextId.DEFAULT);
        if (current != null && (!current.templates().isEmpty() || !current.layouts().isEmpty() || !current.partials().isEmpty())) {
            return; // already configured via TypoScript/site settings
        }
        RootPathSet detected = detect();
        if (isEmpty(detected)) {
            LOG.info("AutoRootDetector: no conventional roots found");
            return;
        }
        lastDetected = detected;
        cm.applyAutoDetectedRoots(detected);
        LOG.info("AutoRootDetector: applied detected roots " + detected);
    }

    public synchronized RootPathSet getLastDetected() { return lastDetected; }

    public RootPathSet detect() {
        VirtualFile base = project.getBaseDir();
        if (base == null) return new RootPathSet(List.of(), List.of(), List.of());

        final List<String> templates = new ArrayList<>();
        final List<String> layouts = new ArrayList<>();
        final List<String> partials = new ArrayList<>();

        VfsUtilCore.visitChildrenRecursively(base, new VirtualFileVisitor<>() {
            @Override
            public boolean visitFile(@NotNull VirtualFile file) {
                if (!file.isDirectory()) return true;
                String path = file.getPath().replace('\\', '/');
                if (path.endsWith("/Resources/Private/Templates")) templates.add(path);
                if (path.endsWith("/Resources/Private/Layouts")) layouts.add(path);
                if (path.endsWith("/Resources/Private/Partials")) partials.add(path);
                return true;
            }
        });

        List<String> sTemplates = sortByPriority(templates);
        List<String> sLayouts   = sortByPriority(layouts);
        List<String> sPartials  = sortByPriority(partials);
        return new RootPathSet(sTemplates, sLayouts, sPartials);
    }

    private static boolean isEmpty(RootPathSet s) {
        return s.templates().isEmpty() && s.layouts().isEmpty() && s.partials().isEmpty();
    }

    private static List<String> sortByPriority(List<String> paths) {
        // Vendor lowest, generic middle, site-like highest. We want highest priority last.
        paths.sort(Comparator.comparingInt(AutoRootDetector::score));
        return paths;
    }

    private static int score(String p) {
        String path = p.toLowerCase(Locale.ROOT);
        int s = 10;
        if (path.contains("/vendor/") || path.contains("/composer/")) s = 0; // lowest
        else if (path.contains("/site_") || path.contains("/site-") || path.contains("/site/")) s = 20; // highest
        else s = 10;
        // Tiebreaker: deeper paths win slightly to push specific modules later
        return s * 1000 + path.length();
    }
}
