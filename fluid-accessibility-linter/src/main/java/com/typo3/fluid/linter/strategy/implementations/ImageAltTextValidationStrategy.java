package com.typo3.fluid.linter.strategy.implementations;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlTag;
import com.typo3.fluid.linter.fixes.FixContext;
import com.typo3.fluid.linter.fixes.FixRegistry;
import com.typo3.fluid.linter.parser.PsiElementParser;
import com.typo3.fluid.linter.strategy.ValidationResult;
import com.typo3.fluid.linter.strategy.ValidationStrategy;

import java.util.ArrayList;
import java.util.List;

/**
 * PSI-first strategy for validating alt text on images (HTML <img> and Fluid f:image).
 */
public class ImageAltTextValidationStrategy implements ValidationStrategy {

    @Override
    public List<ValidationResult> validate(PsiFile file, String content) {
        List<ValidationResult> results = new ArrayList<>();

        // HTML <img>
        for (PsiElement el : PsiElementParser.findElementsByTagName(file, "img")) {
            if (el instanceof XmlTag) {
                results.addAll(checkOneImageTag(file, (XmlTag) el));
            }
        }

        // Fluid f:image, f:media, namespaced :image/:media, VHS dotted .image
        file.accept(new com.intellij.psi.PsiRecursiveElementVisitor() {
            @Override
            public void visitElement(@org.jetbrains.annotations.NotNull PsiElement element) {
                if (element instanceof XmlTag) {
                    XmlTag tag = (XmlTag) element;
                    String name = tag.getName();
                    String lower = name.toLowerCase();
                    if (lower.equals("f:image") || lower.equals("f:media") || lower.endsWith(":image") || lower.endsWith(":media") || lower.endsWith(".image") || lower.contains(":image.")) {
                        results.addAll(checkOneImageTag(file, tag));
                    }
                }
                super.visitElement(element);
            }
        });

        return results;
    }

    private List<ValidationResult> checkOneImageTag(PsiFile file, XmlTag tag) {
        List<ValidationResult> out = new ArrayList<>();
        String alt = tag.getAttributeValue("alt");
        String role = tag.getAttributeValue("role");
        String ariaHidden = tag.getAttributeValue("aria-hidden");

        boolean decorative = role != null && role.equalsIgnoreCase("presentation");
        decorative = decorative || (ariaHidden != null && ariaHidden.equalsIgnoreCase("true"));

        int start = tag.getTextRange().getStartOffset();
        int end = tag.getTextRange().getEndOffset();

        if (!decorative && (alt == null || alt.isEmpty())) {
            FixContext ctx = new FixContext("missing-alt");
            ctx.setAttribute("attributeName", "alt");
            ctx.setAttribute("elementType", tag.getName());
            LocalQuickFix[] fixes = FixRegistry.getInstance().getFixes(file, start, end, ctx);
            out.add(new ValidationResult(start, end, "Image missing alt attribute", fixes));
            return out;
        }

        if (alt != null) {
            String trimmed = alt.trim();
            if (trimmed.isEmpty()) {
                out.add(new ValidationResult(start, end, "Alt attribute must not be empty (use role='presentation' for decorative images)"));
            } else if (isLowQualityAltText(trimmed)) {
                out.add(new ValidationResult(start, end, "Alt text appears to be low quality: " + trimmed));
            }
        }

        return out;
    }

    private boolean isLowQualityAltText(String altText) {
        String lower = altText.toLowerCase();
        return lower.equals("image") ||
                lower.equals("picture") ||
                lower.equals("photo") ||
                lower.matches("img\\d+") ||
                lower.matches("image\\d+");
    }

    @Override
    public int getPriority() {
        return 100; // High priority
    }
}
