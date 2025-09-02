package com.typo3.fluid.linter.fixes;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiRecursiveElementVisitor;
import com.intellij.psi.xml.XmlTag;
import org.jetbrains.annotations.NotNull;

/**
 * Quick fixes for skip links: add a skip link anchor and/or add missing target id.
 */
public class AddSkipLinkFixStrategy implements FixStrategy {
    @Override
    public LocalQuickFix createFix(PsiFile file, int startOffset, int endOffset, FixContext context) {
        String kind = context.getStringAttribute("skipFixKind");
        if ("target".equals(kind)) return new AddSkipTargetFix(context.getStringAttribute("targetId"));
        return new AddSkipLinkFix();
    }

    @Override
    public boolean canHandle(String problemType) {
        return "missing-skip-link".equals(problemType) || "missing-skip-target".equals(problemType);
    }

    private static class AddSkipLinkFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getName() { return "Add skip link at top"; }

        @NotNull
        @Override
        public String getFamilyName() { return "Navigation"; }

        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            PsiElement element = descriptor.getPsiElement();
            PsiFile file = element.getContainingFile();
            String skip = "<a class=\"visually-hidden-focusable\" href=\"#main\">Skip to main content</a>\n";
            PsiMutationUtils.insertBeforeElement(project, file, file.getFirstChild(), skip);
        }
    }

    private static class AddSkipTargetFix implements LocalQuickFix {
        private final String targetId;
        AddSkipTargetFix(String targetId) { this.targetId = targetId != null ? targetId : "main"; }

        @NotNull
        @Override
        public String getName() { return "Add missing skip target id to <main>"; }

        @NotNull
        @Override
        public String getFamilyName() { return "Navigation"; }

        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            PsiElement element = descriptor.getPsiElement();
            PsiFile file = element.getContainingFile();
            XmlTag main = findFirstTag(file, "main");
            if (main != null) {
                PsiMutationUtils.ensureAttribute(project, main, "id", targetId);
                return;
            }
            // As a fallback add an empty anchor at top
            String anchor = String.format("<a id=\"%s\"></a>\n", targetId);
            PsiMutationUtils.insertBeforeElement(project, file, file.getFirstChild(), anchor);
        }

        private XmlTag findFirstTag(PsiFile file, String name) {
            final XmlTag[] found = {null};
            file.accept(new PsiRecursiveElementVisitor() {
                @Override
                public void visitElement(@NotNull PsiElement element) {
                    if (found[0] != null) return;
                    if (element instanceof XmlTag) {
                        XmlTag tag = (XmlTag) element;
                        if (name.equalsIgnoreCase(tag.getName())) { found[0] = tag; return; }
                    }
                    super.visitElement(element);
                }
            });
            return found[0];
        }
    }
}

