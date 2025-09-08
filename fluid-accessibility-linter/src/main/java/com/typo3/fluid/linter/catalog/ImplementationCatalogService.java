package com.typo3.fluid.linter.catalog;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileVisitor;
import com.intellij.openapi.vfs.newvfs.events.*;
import com.typo3.fluid.linter.context.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

@Service(Service.Level.PROJECT)
public final class ImplementationCatalogService implements DumbAware {
    private static final Logger LOG = Logger.getInstance(ImplementationCatalogService.class);

    private final Project project;

    private final Map<ContextId, Catalog> catalogs = new HashMap<>();

    public ImplementationCatalogService(@NotNull Project project) {
        this.project = project;
        // React to context changes
        project.getMessageBus().connect().subscribe(ContextChangeTopic.TOPIC, diff -> {
            for (Map.Entry<ContextId, RootPathSet> e : diff.changed.entrySet()) {
                rebuildForContext(e.getKey(), e.getValue());
            }
        });

        // Build initial from current context
        RootPathSet set = project.getService(FluidContextManager.class).get(ContextId.DEFAULT);
        if (set != null) rebuildForContext(ContextId.DEFAULT, set);
    }

    public synchronized @Nullable VirtualFile effective(@NotNull ContextId ctx,
                                                        @NotNull ImplementationKind kind,
                                                        @NotNull String logicalName) {
        List<VirtualFile> c = candidates(ctx, kind, logicalName);
        return c.isEmpty() ? null : c.get(0);
    }

    public synchronized @NotNull List<VirtualFile> candidates(@NotNull ContextId ctx,
                                                              @NotNull ImplementationKind kind,
                                                              @NotNull String logicalName) {
        Catalog cat = catalogs.get(ctx);
        if (cat == null) return Collections.emptyList();
        Map<String, List<VirtualFile>> m = cat.byKind(kind);
        List<VirtualFile> list = m.get(logicalName);
        return list != null ? Collections.unmodifiableList(list) : Collections.emptyList();
    }

    public synchronized @NotNull Set<String> logicalNames(@NotNull ContextId ctx,
                                                          @NotNull ImplementationKind kind) {
        Catalog cat = catalogs.get(ctx);
        if (cat == null) return Collections.emptySet();
        return new HashSet<>(cat.byKind(kind).keySet());
    }

    public synchronized void onFluidVfsEvents(@NotNull List<? extends VFileEvent> events) {
        if (catalogs.isEmpty()) return;
        Catalog defaultCat = catalogs.get(ContextId.DEFAULT);
        if (defaultCat == null) return;

        for (VFileEvent e : events) {
            if (e instanceof VFileCreateEvent) {
                VirtualFile f = ((VFileCreateEvent) e).getFile();
                if (f != null) handleAdd(defaultCat, f);
            } else if (e instanceof VFileDeleteEvent) {
                VirtualFile f = ((VFileDeleteEvent) e).getFile();
                if (f != null) handleDelete(defaultCat, f);
            } else if (e instanceof VFileMoveEvent) {
                VirtualFile f = ((VFileMoveEvent) e).getFile();
                if (f != null) { handleDelete(defaultCat, f); handleAdd(defaultCat, f); }
            } else if (e instanceof VFilePropertyChangeEvent) {
                VFilePropertyChangeEvent pe = (VFilePropertyChangeEvent) e;
                if (VirtualFile.PROP_NAME.equals(pe.getPropertyName())) {
                    VirtualFile f = pe.getFile();
                    if (f != null) { handleDelete(defaultCat, f); handleAdd(defaultCat, f); }
                }
            }
        }
    }

    private void rebuildForContext(@NotNull ContextId ctx, @NotNull RootPathSet set) {
        Catalog cat = new Catalog(set);
        scanKind(cat, ImplementationKind.TEMPLATE, set.templates());
        scanKind(cat, ImplementationKind.LAYOUT, set.layouts());
        scanKind(cat, ImplementationKind.PARTIAL, set.partials());
        synchronized (this) {
            catalogs.put(ctx, cat);
        }
        LOG.info("ImplementationCatalog rebuilt for context=" + ctx + " with T=" + cat.templates.size() + ", L=" + cat.layouts.size() + ", P=" + cat.partials.size());
    }

    private void scanKind(@NotNull Catalog cat, @NotNull ImplementationKind kind, @NotNull List<String> roots) {
        for (int idx = 0; idx < roots.size(); idx++) {
            String rp = roots.get(idx);
            String resolved = resolvePath(rp);
            if (resolved == null) continue;
            VirtualFile root = LocalFileSystem.getInstance().findFileByPath(resolved);
            if (root == null || !root.isValid() || !root.isDirectory()) continue;

            final int priority = idx; // lower index -> lower priority; reversed for candidates
            VfsUtilCore.visitChildrenRecursively(root, new VirtualFileVisitor<>() {
                @Override
                public boolean visitFile(@NotNull VirtualFile file) {
                    if (!file.isDirectory() && file.getName().toLowerCase().endsWith(".html")) {
                        String logical = toLogicalName(root, file);
                        if (logical != null) cat.add(kind, logical, file, priority);
                    }
                    return true;
                }
            });
        }
    }

    private static String toLogicalName(@NotNull VirtualFile root, @NotNull VirtualFile file) {
        String rootPath = root.getPath();
        String filePath = file.getPath();
        if (!filePath.startsWith(rootPath)) return null;
        String rel = filePath.substring(rootPath.length());
        if (rel.startsWith("/")) rel = rel.substring(1);
        int dot = rel.lastIndexOf('.');
        if (dot >= 0) rel = rel.substring(0, dot);
        return rel.replace('\\', '/');
    }

    private String resolvePath(@NotNull String raw) {
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) return null;
        // EXT:alias/... resolution -> project root + "/" + alias + "/..."
        if (trimmed.startsWith("EXT:")) {
            String rest = trimmed.substring(4);
            int slash = rest.indexOf('/');
            String ext = slash >= 0 ? rest.substring(0, slash) : rest;
            String suffix = slash >= 0 ? rest.substring(slash + 1) : "";
            VirtualFile base = project.getBaseDir();
            if (base != null) return normalize(base.getPath() + "/" + ext + "/" + suffix);
            return normalize(ext + "/" + suffix);
        }
        // If absolute path, return as-is
        if (trimmed.startsWith("/") || trimmed.matches("^[A-Za-z]:\\\\.*")) {
            return normalize(trimmed);
        }
        // Project-relative
        VirtualFile base = project.getBaseDir();
        if (base != null) {
            return normalize(base.getPath() + "/" + trimmed);
        }
        return normalize(trimmed);
    }

    private static String normalize(String p) { return p.replace('\\', '/').replaceAll("/+$", ""); }

    private static final class Catalog {
        final RootPathSet roots;
        final Map<String, List<VirtualFile>> templates = new HashMap<>();
        final Map<String, List<VirtualFile>> layouts = new HashMap<>();
        final Map<String, List<VirtualFile>> partials = new HashMap<>();

        Catalog(RootPathSet roots) { this.roots = roots; }

        Map<String, List<VirtualFile>> byKind(ImplementationKind k) {
            switch (k) {
                case TEMPLATE: return templates;
                case LAYOUT: return layouts;
                case PARTIAL: return partials;
            }
            throw new IllegalStateException();
        }

        void add(ImplementationKind k, String logicalName, VirtualFile file, int priorityIndex) {
            Map<String, List<VirtualFile>> m = byKind(k);
            List<VirtualFile> list = m.computeIfAbsent(logicalName, x -> new ArrayList<>());
            // Insert respecting reverse-order search: higher index wins => candidates list ordered best-first
            int insertAt = 0;
            while (insertAt < list.size() && priorityIndex <= priorityIndexOf(list.get(insertAt))) insertAt++;
            list.add(insertAt, file);
        }

        private int priorityIndexOf(VirtualFile file) {
            // Approximate by matching the root with the longest prefix in roots lists, finding its index
            String path = file.getPath();
            int bestIdx = -1;
            bestIdx = Math.max(bestIdx, bestIndex(roots.templates(), path));
            bestIdx = Math.max(bestIdx, bestIndex(roots.layouts(), path));
            bestIdx = Math.max(bestIdx, bestIndex(roots.partials(), path));
            return bestIdx;
        }

        private int bestIndex(List<String> roots, String path) {
            for (int i = roots.size() - 1; i >= 0; i--) {
                String r = roots.get(i).replace('\\', '/');
                if (path.startsWith(r)) return i;
            }
            return -1;
        }

        void removeFile(VirtualFile file) {
            removeFrom(templates, file);
            removeFrom(layouts, file);
            removeFrom(partials, file);
        }

        private void removeFrom(Map<String, List<VirtualFile>> m, VirtualFile f) {
            for (Iterator<Map.Entry<String, List<VirtualFile>>> it = m.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<String, List<VirtualFile>> e = it.next();
                e.getValue().removeIf(vf -> Objects.equals(vf.getUrl(), f.getUrl()));
                if (e.getValue().isEmpty()) it.remove();
            }
        }
    }

    private void handleAdd(@NotNull Catalog cat, @NotNull VirtualFile file) {
        if (file.isDirectory()) return;
        if (!file.getName().toLowerCase().endsWith(".html")) return;
        addIfUnder(cat, ImplementationKind.TEMPLATE, cat.roots.templates(), file);
        addIfUnder(cat, ImplementationKind.LAYOUT, cat.roots.layouts(), file);
        addIfUnder(cat, ImplementationKind.PARTIAL, cat.roots.partials(), file);
    }

    private void addIfUnder(@NotNull Catalog cat, @NotNull ImplementationKind kind, @NotNull List<String> rootPaths, @NotNull VirtualFile file) {
        String filePath = file.getPath().replace('\\', '/');
        for (int idx = 0; idx < rootPaths.size(); idx++) {
            String root = normalize(rootPaths.get(idx));
            if (filePath.startsWith(root + "/")) {
                String logical = filePath.substring(root.length() + 1);
                int dot = logical.lastIndexOf('.');
                if (dot >= 0) logical = logical.substring(0, dot);
                cat.add(kind, logical, file, idx);
                return;
            }
        }
    }

    private void handleDelete(@NotNull Catalog cat, @NotNull VirtualFile file) {
        cat.removeFile(file);
    }
}
