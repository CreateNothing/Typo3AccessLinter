package com.typo3.fluid.linter.strategy.implementations;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiRecursiveElementVisitor;
import com.intellij.psi.xml.XmlTag;
import com.typo3.fluid.linter.fixes.FixContext;
import com.typo3.fluid.linter.fixes.FixRegistry;
import com.typo3.fluid.linter.strategy.ValidationResult;

import java.util.*;

/**
 * Sanity checks for aria-label usage that are not covered by role validity rules.
 *
 * - Warn on aria-label present on non-interactive elements (e.g., p, div, span) without an explicit role.
 * - Warn on empty aria-label values.
 * - Warn when aria-label is used on aria-hidden="true" elements (ignored by AT).
 */
public class AriaLabelSanityStrategy extends BaseValidationStrategy {

    private static final Set<String> NON_INTERACTIVE_ELEMENTS = new HashSet<>(Arrays.asList(
            "p", "div", "span", "section", "article", "header", "footer",
            "h1", "h2", "h3", "h4", "h5", "h6", "ul", "ol", "li", "dl", "dt", "dd",
            "blockquote", "pre", "code", "em", "strong", "b", "i", "u",
            "table", "tr", "td", "th", "tbody", "thead", "tfoot"
    ));

    @Override
    public List<ValidationResult> validate(PsiFile file, String content) {
        List<ValidationResult> results = new ArrayList<>();

        file.accept(new PsiRecursiveElementVisitor() {
            @Override
            public void visitElement(@org.jetbrains.annotations.NotNull PsiElement element) {
                if (element instanceof XmlTag) {
                    XmlTag tag = (XmlTag) element;
                    String ariaLabel = tag.getAttributeValue("aria-label");
                    if (ariaLabel != null) {
                        String name = tag.getName().toLowerCase();
                        String role = tag.getAttributeValue("role");
                        int start = tag.getTextRange().getStartOffset();
                        int end = tag.getTextRange().getEndOffset();

                        // Empty aria-label provides no accessible name
                        if (ariaLabel.trim().isEmpty()) {
                            results.add(new ValidationResult(start, end,
                                    "Empty aria-label provides no accessible name",
                                    removeAriaLabelFix(file, start, end)));
                        }

                        // aria-label on aria-hidden elements is ignored
                        String ariaHidden = tag.getAttributeValue("aria-hidden");
                        if (ariaHidden != null && "true".equalsIgnoreCase(ariaHidden.trim())) {
                            results.add(new ValidationResult(start, end,
                                    "aria-label on element with aria-hidden='true' will be ignored",
                                    removeAriaLabelFix(file, start, end)));
                        }

                        // Unnecessary aria-label on non-interactive elements without explicit role
                        if (role == null && NON_INTERACTIVE_ELEMENTS.contains(name)) {
                            results.add(new ValidationResult(start, end,
                                    "Unnecessary aria-label on non-interactive <" + name + "> element. Screen readers already read the content",
                                    removeAriaLabelFix(file, start, end)));
                        }
                    }
                }
                super.visitElement(element);
            }
        });

        return results;
    }

    private LocalQuickFix[] removeAriaLabelFix(PsiFile file, int start, int end) {
        FixContext ctx = new FixContext("remove-attribute");
        ctx.setAttribute("attributeName", "aria-label");
        LocalQuickFix[] fixes = FixRegistry.getInstance().getFixes(file, start, end, ctx);
        return fixes != null ? fixes : new LocalQuickFix[0];
    }

    @Override
    public int getPriority() {
        // Run after generic ARIA role validity checks
        return 70;
    }
}

