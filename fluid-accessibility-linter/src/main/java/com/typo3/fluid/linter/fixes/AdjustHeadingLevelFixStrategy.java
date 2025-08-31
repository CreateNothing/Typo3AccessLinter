package com.typo3.fluid.linter.fixes;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlTag;
import org.jetbrains.annotations.NotNull;

/**
 * Quick fix: adjust heading level to the expected level (e.g., h3 -> h2).
 */
public class AdjustHeadingLevelFixStrategy implements FixStrategy {
    @Override
    public LocalQuickFix createFix(PsiFile file, int startOffset, int endOffset, FixContext context) {
        String level = context.getStringAttribute("desiredLevel");
        int desired = 0;
        try { desired = level != null ? Integer.parseInt(level) : 0; } catch (NumberFormatException ignored) {}
        return new AdjustHeadingFix(desired);
    }

    @Override
    public boolean canHandle(String problemType) {
        return "heading-level-jump".equals(problemType);
    }

    private static class AdjustHeadingFix implements LocalQuickFix {
        private final int desired;
        AdjustHeadingFix(int desired) { this.desired = desired; }

        @NotNull
        @Override
        public String getName() { return desired > 0 ? ("Change to <h" + desired + ">") : "Adjust heading level"; }

        @NotNull
        @Override
        public String getFamilyName() { return "Headings"; }

        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            PsiElement element = descriptor.getPsiElement();
            XmlTag tag = PsiMutationUtils.findNearestTag(element);
            if (tag == null) return;
            String name = tag.getName().toLowerCase();
            if (!name.matches("h[1-6]")) return;
            int current = Character.getNumericValue(name.charAt(1));
            int target = desired > 0 ? desired : Math.max(1, current - 1);
            PsiMutationUtils.changeTagName(project, tag, "h" + target);
        }
    }
}
