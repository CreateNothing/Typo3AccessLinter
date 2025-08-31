package com.typo3.fluid.linter.fixes;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlTag;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

/**
 * Quick fix: add required ARIA attributes for a given role with sensible defaults.
 */
public class AddAriaRequiredAttributesFixStrategy implements FixStrategy {
    @Override
    public LocalQuickFix createFix(PsiFile file, int startOffset, int endOffset, FixContext context) {
        return new AddRequiredForRoleFix();
    }

    @Override
    public boolean canHandle(String problemType) {
        return "aria-missing-required".equals(problemType);
    }

    private static class AddRequiredForRoleFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getName() { return "Add required ARIA attributes for role"; }

        @NotNull
        @Override
        public String getFamilyName() { return "ARIA"; }

        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            PsiElement element = descriptor.getPsiElement();
            XmlTag tag = PsiMutationUtils.findNearestTag(element);
            if (tag == null) return;
            String role = tag.getAttributeValue("role");
            if (role == null) return;
            for (String attr : requiredAttributesForRole(role)) {
                if (attr.contains("|")) continue; // skip one-of here; other fixes can cover aria-label
                String def = defaultValueFor(attr);
                PsiMutationUtils.ensureAttribute(project, tag, attr, def);
            }
        }

        private List<String> requiredAttributesForRole(String role) {
            switch (role.toLowerCase()) {
                case "slider":
                case "scrollbar":
                    return Arrays.asList("aria-valuemin", "aria-valuemax", "aria-valuenow");
                case "spinbutton":
                    return Arrays.asList("aria-valuenow");
                case "progressbar":
                    return Arrays.asList("aria-valuemin", "aria-valuemax", "aria-valuenow");
                case "checkbox":
                case "radio":
                case "switch":
                case "menuitemcheckbox":
                case "menuitemradio":
                    return Arrays.asList("aria-checked");
                case "tab":
                    return Arrays.asList("aria-selected");
                case "combobox":
                    return Arrays.asList("aria-expanded");
                case "tabpanel":
                    return Arrays.asList("aria-label|aria-labelledby");
                default:
                    return java.util.Collections.emptyList();
            }
        }

        private String defaultValueFor(String attr) {
            switch (attr) {
                case "aria-valuemin": return "0";
                case "aria-valuemax": return "100";
                case "aria-valuenow": return "0";
                case "aria-checked": return "false";
                case "aria-selected": return "false";
                case "aria-expanded": return "false";
                default: return "";
            }
        }
    }
}

