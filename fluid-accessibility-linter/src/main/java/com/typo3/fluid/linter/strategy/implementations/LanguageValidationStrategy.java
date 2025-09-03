package com.typo3.fluid.linter.strategy.implementations;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiRecursiveElementVisitor;
import com.intellij.psi.xml.XmlTag;
import com.typo3.fluid.linter.strategy.ValidationResult;
import com.typo3.fluid.linter.strategy.ValidationStrategy;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Validate presence of page language and obvious invalid language values.
 */
public class LanguageValidationStrategy implements ValidationStrategy {
    private static final Pattern BCP47_SIMPLE = Pattern.compile("^[a-zA-Z]{2,3}(-[a-zA-Z0-9]{2,8})*$");

    @Override
    public List<ValidationResult> validate(PsiFile file, String content) {
        List<ValidationResult> results = new ArrayList<>();
        file.accept(new PsiRecursiveElementVisitor() {
            @Override
            public void visitElement(@org.jetbrains.annotations.NotNull PsiElement element) {
                if (element instanceof XmlTag) {
                    XmlTag tag = (XmlTag) element;
                    if (tag.getAttribute("lang") != null) {
                        String v = tag.getAttributeValue("lang");
                        if (v != null && (v.equalsIgnoreCase("xx") || v.equalsIgnoreCase("invalid") || !BCP47_SIMPLE.matcher(v).matches())) {
                            add(results, tag, "Invalid language code '" + v + "'");
                        }
                    }
                }
                super.visitElement(element);
            }
        });
        return results;
    }

    private void add(List<ValidationResult> results, XmlTag tag, String msg) {
        int s = tag.getTextRange().getStartOffset();
        int e = tag.getTextRange().getEndOffset();
        results.add(new ValidationResult(s, e, msg));
    }

    @Override
    public int getPriority() { return 20; }
}
