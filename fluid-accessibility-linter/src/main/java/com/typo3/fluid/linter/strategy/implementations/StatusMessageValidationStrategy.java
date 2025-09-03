package com.typo3.fluid.linter.strategy.implementations;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiRecursiveElementVisitor;
import com.intellij.psi.xml.XmlTag;
import com.typo3.fluid.linter.strategy.ValidationResult;
import com.typo3.fluid.linter.strategy.ValidationStrategy;

import java.util.ArrayList;
import java.util.List;

/**
 * Heuristic: if error markup is hidden and no role='alert' present, warn about status messages.
 */
public class StatusMessageValidationStrategy implements ValidationStrategy {
    @Override
    public List<ValidationResult> validate(PsiFile file, String content) {
        List<ValidationResult> results = new ArrayList<>();
        final boolean[] hasAlert = {content.toLowerCase().contains("role='alert") || content.toLowerCase().contains("role=\"alert")};
        file.accept(new PsiRecursiveElementVisitor() {
            @Override
            public void visitElement(@org.jetbrains.annotations.NotNull PsiElement element) {
                if (element instanceof XmlTag) {
                    XmlTag tag = (XmlTag) element;
                    String cls = val(tag.getAttributeValue("class")).toLowerCase();
                    String hidden = val(tag.getAttributeValue("hidden")).toLowerCase();
                    String style = val(tag.getAttributeValue("style")).toLowerCase();
                    boolean looksHidden = hidden.equals("hidden") || style.contains("display:none") || style.contains("visibility:hidden");
                    if ((cls.contains("error") || cls.contains("invalid") || cls.contains("alert")) && looksHidden && !hasAlert[0]) {
                        int s = tag.getTextRange().getStartOffset();
                        int e = tag.getTextRange().getEndOffset();
                        results.add(new ValidationResult(s, e,
                            "Status messages should be announced. Consider role='alert' when errors appear"));
                    }
                }
                super.visitElement(element);
            }
        });
        return results;
    }

    private String val(String v) { return v == null ? "" : v; }

    @Override
    public int getPriority() { return 18; }
}

