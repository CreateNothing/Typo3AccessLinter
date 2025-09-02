package com.typo3.fluid.linter.strategy.implementations;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlTag;
import com.typo3.fluid.linter.strategy.ValidationResult;
import com.typo3.fluid.linter.utils.AccessibilityUtils;
import com.typo3.fluid.linter.parser.PsiElementParser;
import com.intellij.psi.PsiRecursiveElementVisitor;

import java.util.ArrayList;
import java.util.List;

/**
 * PSI-first skip links validation: encourage presence of a skip navigation link
 * and validate that skip link targets exist in the document.
 */
public class SkipLinksValidationStrategy extends BaseValidationStrategy {

    @Override
    public List<ValidationResult> validate(PsiFile file, String content) {
        List<ValidationResult> results = new ArrayList<>();
        List<XmlTag> anchors = new ArrayList<>();
        for (PsiElement el : PsiElementParser.findElementsByTagName(file, "a")) {
            if (el instanceof XmlTag) anchors.add((XmlTag) el);
        }

        List<XmlTag> skipLinks = new ArrayList<>();
        for (XmlTag a : anchors) {
            String href = a.getAttributeValue("href");
            if (href == null || !href.startsWith("#") || href.length() < 2) continue;
            String text = AccessibilityUtils.extractTextContent(a.getValue().getText()).toLowerCase();
            String cls = valueOrEmpty(a.getAttributeValue("class")).toLowerCase();
            String aria = valueOrEmpty(a.getAttributeValue("aria-label")).toLowerCase();
            if (text.contains("skip") || cls.contains("skip") || aria.contains("skip")) {
                skipLinks.add(a);
                String target = href.substring(1);
                if (!elementWithIdExistsPsi(file, target)) {
                    int s = a.getTextRange().getStartOffset();
                    int e = a.getTextRange().getEndOffset();
                    com.typo3.fluid.linter.fixes.FixContext ctx = new com.typo3.fluid.linter.fixes.FixContext("missing-skip-target");
                    ctx.setAttribute("skipFixKind", "target");
                    ctx.setAttribute("targetId", target);
                    com.intellij.codeInspection.LocalQuickFix[] fixes = com.typo3.fluid.linter.fixes.FixRegistry.getInstance()
                            .getFixes(file, s, e, ctx);
                    results.add(new ValidationResult(s, e, "Skip link target '#" + target + "' not found", fixes));
                }
            }
        }

        if (skipLinks.isEmpty()) {
            int end = Math.min(1, content.length());
            com.typo3.fluid.linter.fixes.FixContext ctx = new com.typo3.fluid.linter.fixes.FixContext("missing-skip-link");
            com.intellij.codeInspection.LocalQuickFix[] fixes = com.typo3.fluid.linter.fixes.FixRegistry.getInstance()
                    .getFixes(file, 0, end, ctx);
            results.add(new ValidationResult(0, end, "Page should provide a skip navigation link", fixes));
        }

        return results;
    }

    private boolean elementWithIdExistsPsi(PsiFile file, String id) {
        final boolean[] found = {false};
        file.accept(new PsiRecursiveElementVisitor() {
            @Override
            public void visitElement(@org.jetbrains.annotations.NotNull PsiElement element) {
                if (found[0]) return;
                if (element instanceof XmlTag) {
                    XmlTag t = (XmlTag) element;
                    String val = t.getAttributeValue("id");
                    if (val != null && val.equals(id)) { found[0] = true; return; }
                }
                super.visitElement(element);
            }
        });
        return found[0];
    }

    private String valueOrEmpty(String s) { return s == null ? "" : s; }

    @Override
    public int getPriority() {
        return 40;
    }
}
