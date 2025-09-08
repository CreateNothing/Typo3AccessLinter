package com.typo3.fluid.linter.flatten;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlTag;
import com.typo3.fluid.linter.context.ContextId;
import com.typo3.fluid.linter.resolution.ResolutionChangeTopic;
import org.jetbrains.annotations.NotNull;

import java.util.*;

@Service(Service.Level.PROJECT)
public final class FlattenCacheService implements DumbAware {
    private static final Logger LOG = Logger.getInstance(FlattenCacheService.class);

    private final Project project;
    private final Map<EntryPoint, FlattenResult> cache = new HashMap<>();

    public FlattenCacheService(@NotNull Project project) {
        this.project = project;
        project.getMessageBus().connect().subscribe(ResolutionChangeTopic.TOPIC, diff -> invalidateAll());
    }

    public synchronized void invalidateByFiles(@NotNull Collection<VirtualFile> files) {
        if (files.isEmpty()) return;
        Set<String> urls = new HashSet<>();
        for (VirtualFile f : files) urls.add(f.getUrl());
        cache.entrySet().removeIf(e -> e.getValue().dependsOnUrls.stream().anyMatch(urls::contains));
        LOG.debug("FlattenCache: invalidated by files=" + files.size());
    }

    public synchronized void invalidateAll() {
        cache.clear();
        LOG.debug("FlattenCache: invalidated all");
    }

    public synchronized @NotNull FlattenResult getOrCompute(@NotNull ContextId ctx, @NotNull VirtualFile file) {
        EntryPoint ep = new EntryPoint(ctx, file);
        FlattenResult r = cache.get(ep);
        if (r != null) return r;
        r = computeFromPsi(file);
        cache.put(ep, r);
        return r;
    }

    private FlattenResult computeFromPsi(@NotNull VirtualFile file) {
        PsiFile psi = PsiManager.getInstance(project).findFile(file);
        List<FlattenedHeading> heads = new ArrayList<>();
        if (psi != null) {
            for (XmlTag tag : PsiTreeUtil.findChildrenOfType(psi, XmlTag.class)) {
                String local = tag.getLocalName().toLowerCase();
                int lvl = headingLevel(local);
                if (lvl > 0) {
                    String text = tag.getValue().getText().trim();
                    heads.add(new FlattenedHeading(lvl, text, tag.getTextOffset()));
                }
            }
            heads.sort(Comparator.comparingInt(h -> h.startOffset));
        }
        // Minimal dependsOn: just the file itself for now
        return new FlattenResult(Collections.unmodifiableList(heads), List.of(file.getUrl()));
    }

    private static int headingLevel(String local) {
        if (local.length() == 2 && local.charAt(0) == 'h') {
            char c = local.charAt(1);
            if (c >= '1' && c <= '6') return c - '0';
        }
        return 0;
    }

    public static final class EntryPoint {
        public final ContextId ctx; public final VirtualFile file;
        public EntryPoint(ContextId ctx, VirtualFile file) { this.ctx = ctx; this.file = file; }
        @Override public boolean equals(Object o) { if (this == o) return true; if (!(o instanceof EntryPoint)) return false; EntryPoint that = (EntryPoint) o; return Objects.equals(ctx, that.ctx) && Objects.equals(file.getUrl(), that.file.getUrl()); }
        @Override public int hashCode() { return Objects.hash(ctx, file.getUrl()); }
    }

    public static final class FlattenResult {
        public final List<FlattenedHeading> headings;
        public final List<String> dependsOnUrls;
        public FlattenResult(List<FlattenedHeading> headings, List<String> dependsOnUrls) { this.headings = headings; this.dependsOnUrls = dependsOnUrls; }
    }

    public static final class FlattenedHeading {
        public final int level; public final String text; public final int startOffset;
        public FlattenedHeading(int level, String text, int startOffset) { this.level = level; this.text = text; this.startOffset = startOffset; }
    }
}

