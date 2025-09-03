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
 * Flags anchors used as buttons and buttons used for navigation.
 */
public class LinkButtonSemanticsValidationStrategy implements ValidationStrategy {
    @Override
    public List<ValidationResult> validate(PsiFile file, String content) {
        List<ValidationResult> results = new ArrayList<>();
        file.accept(new PsiRecursiveElementVisitor() {
            @Override
            public void visitElement(@org.jetbrains.annotations.NotNull PsiElement element) {
                if (element instanceof XmlTag) {
                    XmlTag tag = (XmlTag) element;
                    String name = tag.getName().toLowerCase();
                    if (name.equals("a")) {
                        String href = tag.getAttributeValue("href");
                        String onclick = tag.getAttributeValue("onclick");
                        if ((href == null || href.equals("#") || href.isBlank()) && onclick != null && !onclick.isBlank()) {
                            add(results, tag, "Anchor used as button. Use <button> for actions or provide a valid href");
                        }
                    } else if (name.equals("button")) {
                        String onclick = tag.getAttributeValue("onclick");
                        if (onclick != null) {
                            String low = onclick.toLowerCase();
                            if (low.contains("window.location") || low.contains("location=") || low.contains("document.location")) {
                                add(results, tag, "Button used for navigation. Use <a href> for navigation");
                            }
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
    public int getPriority() { return 50; }
}

