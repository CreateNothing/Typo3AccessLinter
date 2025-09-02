package com.typo3.fluid.linter.fixes;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlTag;
import org.jetbrains.annotations.NotNull;

/**
 * Quick fix: mark image as decorative by setting alt="" and aria-hidden="true".
 */
public class MarkImageDecorativeFixStrategy implements FixStrategy {
    @Override
    public LocalQuickFix createFix(PsiFile file, int startOffset, int endOffset, FixContext context) {
        return new MarkDecorativeFix();
    }

    @Override
    public boolean canHandle(String problemType) {
        return "missing-alt".equals(problemType);
    }

    private static class MarkDecorativeFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getName() { return "Mark image as decorative (alt=\"\", aria-hidden)"; }

        @NotNull
        @Override
        public String getFamilyName() { return "Images"; }

        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            PsiElement element = descriptor.getPsiElement();
            XmlTag tag = PsiMutationUtils.findNearestTag(element);
            if (tag == null) return;
            PsiMutationUtils.setAttribute(project, tag, "alt", "");
            PsiMutationUtils.ensureAttribute(project, tag, "aria-hidden", "true");
        }
    }
}
