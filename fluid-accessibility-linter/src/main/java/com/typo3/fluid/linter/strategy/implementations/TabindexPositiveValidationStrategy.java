package com.typo3.fluid.linter.strategy.implementations;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiRecursiveElementVisitor;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlTag;
import com.typo3.fluid.linter.strategy.ValidationResult;
import com.typo3.fluid.linter.strategy.ValidationStrategy;

import java.util.ArrayList;
import java.util.List;

/**
 * Flags positive tabindex values which disrupt natural focus order.
 */
public class TabindexPositiveValidationStrategy implements ValidationStrategy {
    @Override
    public List<ValidationResult> validate(PsiFile file, String content) {
        List<ValidationResult> results = new ArrayList<>();
        file.accept(new PsiRecursiveElementVisitor() {
            @Override
            public void visitElement(@org.jetbrains.annotations.NotNull PsiElement element) {
                if (element instanceof XmlTag) {
                    XmlTag tag = (XmlTag) element;
                    XmlAttribute attr = tag.getAttribute("tabindex");
                    if (attr != null) {
                        String val = attr.getValue();
                        try {
                            if (val != null && Integer.parseInt(val.trim()) > 0) {
                                int s = tag.getTextRange().getStartOffset();
                                int e = tag.getTextRange().getEndOffset();
                                results.add(new ValidationResult(s, e,
                                    "Avoid positive tabindex values; use natural DOM order or tabindex=0"));
                            }
                        } catch (NumberFormatException ignored) { }
                    }
                }
                super.visitElement(element);
            }
        });
        return results;
    }

    @Override
    public int getPriority() { return 35; }
}

