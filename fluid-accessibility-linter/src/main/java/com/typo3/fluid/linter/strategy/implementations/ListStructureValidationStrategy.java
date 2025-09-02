package com.typo3.fluid.linter.strategy.implementations;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.psi.xml.XmlText;
import com.typo3.fluid.linter.fixes.FixContext;
import com.typo3.fluid.linter.fixes.FixRegistry;
import com.typo3.fluid.linter.parser.PsiElementParser;
import com.typo3.fluid.linter.parser.FluidPsiUtils;
import com.typo3.fluid.linter.strategy.ValidationResult;

import java.util.ArrayList;
import java.util.List;

/**
 * PSI-first validation: <ul>/<ol> must have only <li> as direct children.
 * - Flags non-empty direct text nodes under lists.
 * - Flags non-<li> element children under lists (skips Fluid ViewHelpers).
 * - Suggests wrapping offending content with <li>.
 */
public class ListStructureValidationStrategy extends BaseValidationStrategy {

    @Override
    public List<ValidationResult> validate(PsiFile file, String content) {
        List<ValidationResult> results = new ArrayList<>();

        // Find all <ul> and <ol> tags
        List<PsiElement> lists = new ArrayList<>();
        lists.addAll(PsiElementParser.findElementsByTagName(file, "ul"));
        lists.addAll(PsiElementParser.findElementsByTagName(file, "ol"));

        for (PsiElement listEl : lists) {
            if (!(listEl instanceof XmlTag)) continue;
            XmlTag listTag = (XmlTag) listEl;

            // Use Fluid-aware effective children (unwrap control-flow VH like f:if)
            for (PsiElement child : FluidPsiUtils.getEffectiveChildren(listTag)) {
                // Non-empty text directly inside list -> invalid
                if (child instanceof XmlText) {
                    String text = ((XmlText) child).getValue();
                    if (text != null && !text.trim().isEmpty()) {
                        results.add(buildWrapWithLiResult(file, child.getTextRange().getStartOffset(), child.getTextRange().getEndOffset(),
                                "Lists must not contain direct text; wrap content in <li>"));
                    }
                    continue;
                }

                // Element children: only <li> are allowed as direct children
                if (child instanceof XmlTag) {
                    XmlTag childTag = (XmlTag) child;
                    String name = childTag.getName();
                    if ("li".equalsIgnoreCase(name)) {
                        continue; // valid
                    }

                    results.add(buildWrapWithLiResult(file, childTag.getTextRange().getStartOffset(), childTag.getTextRange().getEndOffset(),
                            "Only <li> elements are allowed as direct children of lists"));
                }
            }
        }

        return results;
    }

    private ValidationResult buildWrapWithLiResult(PsiFile file, int start, int end, String message) {
        FixContext ctx = new FixContext("structure-issue");
        ctx.setAttribute("wrapperTag", "li");
        LocalQuickFix[] fixes = FixRegistry.getInstance().getFixes(file, start, end, ctx);
        return new ValidationResult(start, end, message, fixes);
    }

    @Override
    public int getPriority() {
        return 60;
    }
}
