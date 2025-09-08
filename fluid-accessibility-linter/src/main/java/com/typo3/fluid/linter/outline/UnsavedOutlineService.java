package com.typo3.fluid.linter.outline;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.ui.update.MergingUpdateQueue;
import com.intellij.util.ui.update.Update;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service(Service.Level.PROJECT)
public final class UnsavedOutlineService implements Disposable, DumbAware {
    private static final Logger LOG = Logger.getInstance(UnsavedOutlineService.class);

    private final Project project;
    private final MergingUpdateQueue queue;
    private final ConcurrentMap<VirtualFile, OutlineResult> cache = new ConcurrentHashMap<>();

    public UnsavedOutlineService(@NotNull Project project) {
        this.project = project;
        this.queue = new MergingUpdateQueue("FluidUnsavedOutline", 250, true, null, project);
        this.queue.setRestartTimerOnAdd(true);

        // Listen for document changes across editors
        var multicaster = com.intellij.openapi.editor.EditorFactory.getInstance().getEventMulticaster();
        multicaster.addDocumentListener(new DocListener(), this);
        LOG.info("UnsavedOutlineService initialized: DocumentListener attached");
    }

    public OutlineResult getOutline(@NotNull VirtualFile file) {
        return cache.get(file);
    }

    private final class DocListener implements DocumentListener {
        @Override
        public void documentChanged(@NotNull DocumentEvent event) {
            Document doc = event.getDocument();
            VirtualFile vf = FileDocumentManager.getInstance().getFile(doc);
            if (vf == null) return;
            String name = vf.getName().toLowerCase();
            if (!name.endsWith(".html")) return; // Fluid templates are HTML

            // Only if document is shown in an editor for this project (active tab heuristic)
            var editors = com.intellij.openapi.editor.EditorFactory.getInstance().getEditors(doc, project);
            if (editors.length == 0) return;

            queue.queue(new Update(vf) {
                @Override
                public void run() {
                    recompute(doc, vf);
                }
            });
        }
    }

    private void recompute(@NotNull Document doc, @NotNull VirtualFile file) {
        PsiDocumentManager psiMgr = PsiDocumentManager.getInstance(project);
        psiMgr.commitDocument(doc);
        PsiFile psi = psiMgr.getPsiFile(doc);
        if (psi == null) return;

        List<OutlineEntry> entries = new ArrayList<>();
        for (XmlTag tag : PsiTreeUtil.findChildrenOfType(psi, XmlTag.class)) {
            String local = tag.getLocalName().toLowerCase();
            int level = headingLevel(local);
            if (level > 0) {
                String text = tag.getValue().getText().trim();
                int start = tag.getTextOffset();
                entries.add(new OutlineEntry(level, text, start));
            }
        }
        Collections.sort(entries, (a, b) -> Integer.compare(a.startOffset, b.startOffset));
        OutlineResult result = new OutlineResult(Collections.unmodifiableList(entries), true);
        cache.put(file, result);
        project.getMessageBus().syncPublisher(UnsavedOutlineTopic.TOPIC).outlineUpdated(file, result);
    }

    private static int headingLevel(@NotNull String localName) {
        if (localName.length() == 2 && localName.charAt(0) == 'h') {
            char c = localName.charAt(1);
            if (c >= '1' && c <= '6') return c - '0';
        }
        return 0;
    }

    @Override
    public void dispose() {
        Disposer.dispose(queue);
        cache.clear();
    }

    // Data classes
    public static final class OutlineEntry {
        public final int level;
        public final String text;
        public final int startOffset;

        public OutlineEntry(int level, String text, int startOffset) {
            this.level = level;
            this.text = text;
            this.startOffset = startOffset;
        }
    }

    public static final class OutlineResult {
        public final List<OutlineEntry> entries;
        public final boolean unsavedPreview;

        public OutlineResult(List<OutlineEntry> entries, boolean unsavedPreview) {
            this.entries = entries;
            this.unsavedPreview = unsavedPreview;
        }
    }
}
