package com.typo3.fluid.linter.fixes;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlTag;
import org.jetbrains.annotations.NotNull;

/**
 * Quick fixes for labeling inputs: add <label for> with generated id or add aria-label.
 */
public class GenerateLabelForInputFixStrategy implements FixStrategy {
    @Override
    public LocalQuickFix createFix(PsiFile file, int startOffset, int endOffset, FixContext context) {
        String kind = context.getStringAttribute("labelFixKind");
        if ("aria".equals(kind)) return new AddAriaLabelFix();
        return new AddLabelElementFix();
    }

    @Override
    public boolean canHandle(String problemType) {
        return "form-input-missing-label".equals(problemType) || "form-input-missing-id".equals(problemType);
    }

    private static class AddLabelElementFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getName() { return "Add <label for> with generated id"; }

        @NotNull
        @Override
        public String getFamilyName() { return "Forms"; }

        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            PsiElement element = descriptor.getPsiElement();
            PsiFile file = element.getContainingFile();
            XmlTag input = PsiMutationUtils.findNearestTag(element);
            if (input == null) return;
            String id = input.getAttributeValue("id");
            if (id == null || id.trim().isEmpty()) {
                id = "input-" + input.getTextOffset();
                PsiMutationUtils.setAttribute(project, input, "id", id);
            }
            String labelXml = String.format("<label for=\"%s\">Label</label>\n", id);
            PsiMutationUtils.insertBeforeElement(project, file, input, labelXml);
        }
    }

    private static class AddAriaLabelFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getName() { return "Add aria-label to input"; }

        @NotNull
        @Override
        public String getFamilyName() { return "Forms"; }

        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            PsiElement element = descriptor.getPsiElement();
            XmlTag input = PsiMutationUtils.findNearestTag(element);
            if (input == null) return;
            PsiMutationUtils.setAttribute(project, input, "aria-label", "Label");
        }
    }
}
