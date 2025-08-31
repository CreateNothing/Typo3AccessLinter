package com.typo3.fluid.linter.strategy.implementations;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiRecursiveElementVisitor;
import com.intellij.psi.xml.XmlTag;
import com.typo3.fluid.linter.strategy.ValidationResult;

import java.util.ArrayList;
import java.util.List;

/**
 * PSI-first heading hierarchy validation: detect level jumps (e.g., h1 -> h3).
 */
public class HeadingHierarchyValidationStrategy extends BaseValidationStrategy {

    @Override
    public List<ValidationResult> validate(PsiFile file, String content) {
        List<ValidationResult> results = new ArrayList<>();
        List<XmlTag> headings = new ArrayList<>();

        file.accept(new PsiRecursiveElementVisitor() {
            @Override
            public void visitElement(@org.jetbrains.annotations.NotNull PsiElement element) {
                if (element instanceof XmlTag) {
                    XmlTag tag = (XmlTag) element;
                    String n = tag.getName().toLowerCase();
                    if (n.equals("h1") || n.equals("h2") || n.equals("h3") || n.equals("h4") || n.equals("h5") || n.equals("h6")) {
                        headings.add(tag);
                    }
                }
                super.visitElement(element);
            }
        });

        Integer prev = null;
        for (XmlTag h : headings) {
            int level = Integer.parseInt(h.getName().substring(1));
            if (prev != null && level > prev + 1) {
                int s = h.getTextRange().getStartOffset();
                int e = h.getTextRange().getEndOffset();
                // Offer fix to adjust to the expected level (prev + 1)
                com.typo3.fluid.linter.fixes.FixContext ctx = new com.typo3.fluid.linter.fixes.FixContext("heading-level-jump");
                ctx.setAttribute("desiredLevel", String.valueOf(prev + 1));
                com.intellij.codeInspection.LocalQuickFix[] fixes = com.typo3.fluid.linter.fixes.FixRegistry.getInstance()
                        .getFixes(file, s, e, ctx);
                results.add(new ValidationResult(s, e,
                        String.format("Heading level jumps from h%d to h%d", prev, level), fixes));
            }
            prev = level;
        }

        return results;
    }

    @Override
    public int getPriority() {
        return 70;
    }
}
