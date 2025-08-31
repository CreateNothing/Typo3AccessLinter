package com.typo3.fluid.linter.fixes;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlTag;

/**
 * Centralized PSI mutation helpers used by Fix strategies.
 */
public final class PsiMutationUtils {
    private PsiMutationUtils() {}

    /**
     * Wrap a text range in the file with a given tag and optional attributes.
     */
    public static void wrapRangeWithTag(Project project, PsiFile file, int startOffset, int endOffset, String tagName, String attributes) {
        Document doc = PsiDocumentManager.getInstance(project).getDocument(file);
        if (doc == null) return;
        String attrs = (attributes != null && !attributes.isBlank()) ? (" " + attributes.trim()) : "";
        String openTag = "<" + tagName + attrs + ">";
        String closeTag = "</" + tagName + ">";
        WriteCommandAction.runWriteCommandAction(project, "Wrap with tag", null, () -> {
            doc.insertString(endOffset, closeTag);
            doc.insertString(startOffset, openTag);
            PsiDocumentManager.getInstance(project).commitDocument(doc);
        });
    }

    /**
     * Set or update an attribute on an XmlTag.
     */
    public static void setAttribute(Project project, XmlTag tag, String name, String value) {
        WriteCommandAction.runWriteCommandAction(project, "Set attribute", null, () -> tag.setAttribute(name, value));
    }

    /**
     * Find the nearest XmlTag ancestor of a PSI element.
     */
    public static XmlTag findNearestTag(PsiElement element) {
        PsiElement cur = element;
        while (cur != null && !(cur instanceof XmlTag)) {
            cur = cur.getParent();
        }
        return (XmlTag) cur;
    }
}
