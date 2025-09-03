package com.typo3.fluid.linter.fixes;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlTag;
import org.jetbrains.annotations.NotNull;

/**
 * Strategy for removing an attribute from the nearest XmlTag.
 * Context attributes:
 * - attributeName (required)
 */
public class RemoveAttributeFixStrategy implements FixStrategy {

    @Override
    public LocalQuickFix createFix(PsiFile file, int startOffset, int endOffset, FixContext context) {
        String attributeName = context.getStringAttribute("attributeName");
        if (attributeName == null || attributeName.isBlank()) return null;
        return new RemoveAttributeFix(attributeName);
    }

    @Override
    public boolean canHandle(String problemType) {
        return "remove-attribute".equals(problemType);
    }

    private static class RemoveAttributeFix implements LocalQuickFix {
        private final String attributeName;

        private RemoveAttributeFix(String attributeName) {
            this.attributeName = attributeName;
        }

        @NotNull
        @Override
        public String getName() {
            return "Remove " + attributeName + " attribute";
        }

        @NotNull
        @Override
        public String getFamilyName() { return "Accessibility"; }

        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            PsiElement element = descriptor.getPsiElement();
            if (element == null) return;

            XmlTag tag = PsiMutationUtils.findNearestTag(element);
            if (tag == null) return;

            XmlAttribute attr = tag.getAttribute(attributeName);
            if (attr != null) {
                // Removing the attribute via PSI
                attr.delete();
            }
        }
    }
}

