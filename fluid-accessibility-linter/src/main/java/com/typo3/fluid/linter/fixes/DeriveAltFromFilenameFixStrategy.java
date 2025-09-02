package com.typo3.fluid.linter.fixes;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlTag;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * Quick fix: derive alt text from the image filename (src/image/file attribute).
 */
public class DeriveAltFromFilenameFixStrategy implements FixStrategy {
    @Override
    public LocalQuickFix createFix(PsiFile file, int startOffset, int endOffset, FixContext context) {
        return new DeriveAltFix();
    }

    @Override
    public boolean canHandle(String problemType) {
        return "missing-alt".equals(problemType);
    }

    private static class DeriveAltFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getName() { return "Set alt from image filename"; }

        @NotNull
        @Override
        public String getFamilyName() { return "Images"; }

        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            PsiElement element = descriptor.getPsiElement();
            XmlTag tag = PsiMutationUtils.findNearestTag(element);
            if (tag == null) return;
            String[] attrs = {"alt", "src", "image", "file"};
            String src = null;
            for (String a : attrs) {
                if (a.equals("alt")) continue;
                String v = tag.getAttributeValue(a);
                if (v != null && !v.trim().isEmpty()) { src = v; break; }
            }
            if (src == null) return;
            String base = src;
            int q = base.indexOf('?'); if (q >= 0) base = base.substring(0, q);
            base = base.replace("\\", "/");
            int slash = base.lastIndexOf('/'); if (slash >= 0) base = base.substring(slash + 1);
            int dot = base.lastIndexOf('.'); if (dot > 0) base = base.substring(0, dot);
            String alt = humanize(base);
            if (alt.isEmpty()) return;
            PsiMutationUtils.setAttribute(project, tag, "alt", alt);
        }

        private String humanize(String s) {
            String t = s.replace('_', ' ').replace('-', ' ').trim();
            t = t.replaceAll("\\s+", " ");
            if (t.isEmpty()) return t;
            return t.substring(0,1).toUpperCase(Locale.ROOT) + t.substring(1);
        }
    }
}
