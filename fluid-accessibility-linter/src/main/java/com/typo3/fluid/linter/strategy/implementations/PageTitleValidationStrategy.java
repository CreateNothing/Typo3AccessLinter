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
 * Validate page has a descriptive <title> element.
 */
public class PageTitleValidationStrategy implements ValidationStrategy {
    @Override
    public List<ValidationResult> validate(PsiFile file, String content) {
        List<ValidationResult> results = new ArrayList<>();
        final XmlTag[] titleTag = {null};
        file.accept(new PsiRecursiveElementVisitor() {
            @Override
            public void visitElement(@org.jetbrains.annotations.NotNull PsiElement element) {
                if (element instanceof XmlTag) {
                    XmlTag tag = (XmlTag) element;
                    if ("title".equalsIgnoreCase(tag.getName())) {
                        titleTag[0] = tag;
                    }
                }
                super.visitElement(element);
            }
        });
        if (titleTag[0] == null) {
            results.add(new ValidationResult(0, Math.min(content.length(), 1), "Missing <title> element"));
        } else {
            String text = titleTag[0].getValue().getText().trim();
            if (text.isEmpty() || text.equalsIgnoreCase("home") || text.equalsIgnoreCase("untitled")) {
                int s = titleTag[0].getTextRange().getStartOffset();
                int e = titleTag[0].getTextRange().getEndOffset();
                results.add(new ValidationResult(s, e, "Page title should be descriptive and unique"));
            }
        }
        return results;
    }

    @Override
    public int getPriority() { return 15; }
}

