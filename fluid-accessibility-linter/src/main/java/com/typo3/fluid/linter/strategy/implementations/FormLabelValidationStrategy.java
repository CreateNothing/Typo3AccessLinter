package com.typo3.fluid.linter.strategy.implementations;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiRecursiveElementVisitor;
import com.intellij.psi.xml.XmlTag;
import com.typo3.fluid.linter.strategy.ValidationResult;
import com.typo3.fluid.linter.strategy.ValidationStrategy;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * PSI-first strategy for validating that form inputs have labels.
 */
public class FormLabelValidationStrategy implements ValidationStrategy {

    private static final Set<String> LABELED_INPUT_TYPES = new HashSet<>();
    static {
        String[] types = {"text","email","password","tel","number","date","search","url"};
        for (String t : types) LABELED_INPUT_TYPES.add(t);
    }

    @Override
    public List<ValidationResult> validate(PsiFile file, String content) {
        List<ValidationResult> results = new ArrayList<>();
        List<XmlTag> labels = new ArrayList<>();
        List<XmlTag> inputs = new ArrayList<>();

        file.accept(new PsiRecursiveElementVisitor() {
            @Override
            public void visitElement(@org.jetbrains.annotations.NotNull PsiElement element) {
                if (element instanceof XmlTag) {
                    XmlTag tag = (XmlTag) element;
                    String name = tag.getName().toLowerCase();
                    if (name.equals("label")) labels.add(tag);
                    if (name.equals("input")) inputs.add(tag);
                }
                super.visitElement(element);
            }
        });

        for (XmlTag input : inputs) {
            // Compute range early as it is used in multiple branches
            int s = input.getTextRange().getStartOffset();
            int e = input.getTextRange().getEndOffset();

            String type = value(input.getAttributeValue("type")).toLowerCase();
            if (!type.isEmpty() && !LABELED_INPUT_TYPES.contains(type)) continue;

            String ariaLabel = input.getAttributeValue("aria-label");
            String ariaLabelledby = input.getAttributeValue("aria-labelledby");

            // Accept aria-label directly
            if (ariaLabel != null && !ariaLabel.trim().isEmpty()) {
                continue; // accessible name provided
            }

            // Accept aria-labelledby only if it references at least one existing element id
            if (ariaLabelledby != null && !ariaLabelledby.trim().isEmpty()) {
                if (anyReferencedIdExists(file, ariaLabelledby)) {
                    continue; // valid programmatic label in place
                } else {
                    results.add(new ValidationResult(s, e,
                            buildMissingIdrefMessage(ariaLabelledby)));
                    continue;
                }
            }

            String id = input.getAttributeValue("id");
            

            if (id == null || id.trim().isEmpty()) {
                // Provide fixes: add <label for> (generating id) OR add aria-label
                com.typo3.fluid.linter.fixes.FixContext labelCtx = new com.typo3.fluid.linter.fixes.FixContext("form-input-missing-id");
                com.intellij.codeInspection.LocalQuickFix[] fixesLabel = com.typo3.fluid.linter.fixes.FixRegistry.getInstance().getFixes(file, s, e, labelCtx);
                // Provide a friendly aria-label fix via GenerateLabelForInputFixStrategy
                com.typo3.fluid.linter.fixes.FixContext ariaFriendly = new com.typo3.fluid.linter.fixes.FixContext("form-input-missing-label");
                ariaFriendly.setAttribute("labelFixKind", "aria");
                com.intellij.codeInspection.LocalQuickFix[] fixesAria = com.typo3.fluid.linter.fixes.FixRegistry.getInstance().getFixes(file, s, e, ariaFriendly);
                com.intellij.codeInspection.LocalQuickFix[] fixes = concat(fixesLabel, fixesAria);
                results.add(new ValidationResult(s, e, "Form input missing id attribute and label", fixes));
                continue;
            }

            boolean associated = isLabelFor(labels, id);
            if (!associated) {
                // Provide fixes: add <label for> or add aria-label
                com.typo3.fluid.linter.fixes.FixContext labelCtx = new com.typo3.fluid.linter.fixes.FixContext("form-input-missing-label");
                com.intellij.codeInspection.LocalQuickFix[] fixesLabel = com.typo3.fluid.linter.fixes.FixRegistry.getInstance().getFixes(file, s, e, labelCtx);
                // Friendly aria-label fix
                com.typo3.fluid.linter.fixes.FixContext ariaFriendly = new com.typo3.fluid.linter.fixes.FixContext("form-input-missing-label");
                ariaFriendly.setAttribute("labelFixKind", "aria");
                com.intellij.codeInspection.LocalQuickFix[] fixesAria = com.typo3.fluid.linter.fixes.FixRegistry.getInstance().getFixes(file, s, e, ariaFriendly);
                com.intellij.codeInspection.LocalQuickFix[] fixes = concat(fixesLabel, fixesAria);
                String detail = id != null && !id.isBlank() ? " (<label for='" + id + "'>)" : "";
                results.add(new ValidationResult(s, e, "Form input missing associated label" + detail, fixes));
            }
        }

        return results;
    }

    private boolean anyReferencedIdExists(PsiFile file, String ariaLabelledby) {
        java.util.Set<String> want = new java.util.HashSet<>();
        for (String id : ariaLabelledby.trim().split("\\s+")) {
            if (!id.isBlank()) want.add(id.trim());
        }
        if (want.isEmpty()) return false;

        java.util.Set<String> have = new java.util.HashSet<>();
        file.accept(new PsiRecursiveElementVisitor() {
            @Override
            public void visitElement(@org.jetbrains.annotations.NotNull PsiElement element) {
                if (element instanceof XmlTag) {
                    XmlTag tag = (XmlTag) element;
                    String id = tag.getAttributeValue("id");
                    if (id != null && !id.isBlank()) {
                        have.add(id);
                    }
                }
                super.visitElement(element);
            }
        });
        for (String id : want) {
            if (have.contains(id)) return true; // at least one exists
        }
        return false;
    }

    private String buildMissingIdrefMessage(String ariaLabelledby) {
        String[] parts = ariaLabelledby.trim().split("\\s+");
        StringBuilder missing = new StringBuilder();
        for (String id : parts) {
            if (!id.isBlank()) {
                if (missing.length() > 0) missing.append(", ");
                missing.append("'").append(id.trim()).append("'");
            }
        }
        if (missing.length() == 0) {
            return "aria-labelledby is present but empty; add valid id(s) or use aria-label";
        }
        return "aria-labelledby references non-existent ID(s): " + missing;
    }

    private boolean isLabelFor(List<XmlTag> labels, String id) {
        for (XmlTag label : labels) {
            String forAttr = label.getAttributeValue("for");
            if (forAttr != null && forAttr.equals(id)) return true;
        }
        return false;
    }

    private String value(String s) { return s == null ? "" : s; }

    private com.intellij.codeInspection.LocalQuickFix[] concat(com.intellij.codeInspection.LocalQuickFix[] a, com.intellij.codeInspection.LocalQuickFix[] b) {
        int al = a != null ? a.length : 0;
        int bl = b != null ? b.length : 0;
        com.intellij.codeInspection.LocalQuickFix[] out = new com.intellij.codeInspection.LocalQuickFix[al + bl];
        if (al > 0) System.arraycopy(a, 0, out, 0, al);
        if (bl > 0) System.arraycopy(b, 0, out, al, bl);
        return out;
    }

    @Override
    public int getPriority() {
        return 90;
    }
}
