package com.typo3.fluid.linter.strategy.implementations;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlTag;
import com.typo3.fluid.linter.parser.PsiElementParser;
import com.intellij.psi.PsiRecursiveElementVisitor;
import com.typo3.fluid.linter.strategy.ValidationResult;

import java.util.ArrayList;
import java.util.List;

/**
 * PSI-first table accessibility validation: data tables should contain TH headers.
 * Skips tables explicitly marked as presentation/none.
 */
public class TableAccessibilityValidationStrategy extends BaseValidationStrategy {

    @Override
    public List<ValidationResult> validate(PsiFile file, String content) {
        List<ValidationResult> results = new ArrayList<>();
        List<XmlTag> tables = new ArrayList<>();
        for (PsiElement el : PsiElementParser.findElementsByTagName(file, "table")) {
            if (el instanceof XmlTag) tables.add((XmlTag) el);
        }

        for (XmlTag table : tables) {
            String role = safe(table.getAttributeValue("role")).toLowerCase();
            if (role.equals("presentation") || role.equals("none")) continue; // layout-only

            boolean hasTh = hasDescendantTag(table, "th");
            if (!hasTh) {
                int s = table.getTextRange().getStartOffset();
                int e = table.getTextRange().getEndOffset();
                // Provide quick fix to scaffold <thead>/<th>
                com.typo3.fluid.linter.fixes.FixContext ctx = new com.typo3.fluid.linter.fixes.FixContext("table-headers-missing");
                com.intellij.codeInspection.LocalQuickFix[] fixes = com.typo3.fluid.linter.fixes.FixRegistry.getInstance()
                        .getFixes(file, s, e, ctx);
                results.add(new ValidationResult(s, e, "Data tables must include TH header cells", fixes));
            }
        }

        return results;
    }

    private boolean hasDescendantTag(XmlTag root, String name) {
        final boolean[] found = {false};
        root.accept(new PsiRecursiveElementVisitor() {
            @Override
            public void visitElement(@org.jetbrains.annotations.NotNull PsiElement element) {
                if (found[0]) return;
                if (element instanceof XmlTag) {
                    XmlTag t = (XmlTag) element;
                    if (name.equalsIgnoreCase(t.getName())) { found[0] = true; return; }
                }
                super.visitElement(element);
            }
        });
        return found[0];
    }

    private String safe(String s) { return s == null ? "" : s; }

    @Override
    public int getPriority() {
        return 65;
    }
}
