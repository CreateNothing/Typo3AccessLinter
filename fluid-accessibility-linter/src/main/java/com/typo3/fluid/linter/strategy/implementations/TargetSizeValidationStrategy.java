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
 * Static check for very small inline target sizes (<24x24).
 */
public class TargetSizeValidationStrategy implements ValidationStrategy {
    @Override
    public List<ValidationResult> validate(PsiFile file, String content) {
        List<ValidationResult> results = new ArrayList<>();
        file.accept(new PsiRecursiveElementVisitor() {
            @Override
            public void visitElement(@org.jetbrains.annotations.NotNull PsiElement element) {
                if (element instanceof XmlTag) {
                    XmlTag tag = (XmlTag) element;
                    String name = tag.getName().toLowerCase();
                    if (name.equals("a") || name.equals("button")) {
                        String style = tag.getAttributeValue("style");
                        if (style != null) {
                            int w = extractPx(style, "width");
                            int h = extractPx(style, "height");
                            int mw = extractPx(style, "min-width");
                            int mh = extractPx(style, "min-height");
                            // If explicit width/height smaller than 24 in either dimension
                            if ((w > 0 && w < 24) || (h > 0 && h < 24) || (mw > 0 && mw < 24) || (mh > 0 && mh < 24)) {
                                int s = tag.getTextRange().getStartOffset();
                                int e = tag.getTextRange().getEndOffset();
                                results.add(new ValidationResult(s, e,
                                    "Interactive target is smaller than 24x24 CSS px. Increase size or spacing"));
                            }
                        }
                    }
                }
                super.visitElement(element);
            }
        });
        return results;
    }

    private int extractPx(String style, String prop) {
        String low = style.toLowerCase();
        int idx = low.indexOf(prop + ":");
        if (idx < 0) return -1;
        int end = low.indexOf(';', idx);
        String seg = end > idx ? low.substring(idx, end) : low.substring(idx);
        int px = seg.indexOf("px");
        if (px < 0) return -1;
        String num = seg.replaceAll(".*:" , "").replace("px", "").trim();
        try { return Integer.parseInt(num); } catch (Exception e) { return -1; }
    }

    @Override
    public int getPriority() { return 22; }
}

