package com.typo3.fluid.linter.strategy.implementations;

import com.intellij.psi.PsiFile;
import com.typo3.fluid.linter.strategy.ValidationResult;
import com.typo3.fluid.linter.strategy.ValidationStrategy;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Static CSS scan to flag removal of focus outlines.
 */
public class FocusIndicatorCssValidationStrategy implements ValidationStrategy {
    private static final Pattern OUTLINE_NONE = Pattern.compile(
        "(?is)(:focus[^}]*outline\\s*:\\s*none)|(:focus-visible[^}]*outline\\s*:\\s*none)|(\\*:focus[^}]*outline\\s*:\\s*none)"
    );

    @Override
    public List<ValidationResult> validate(PsiFile file, String content) {
        List<ValidationResult> results = new ArrayList<>();
        Matcher m = OUTLINE_NONE.matcher(content);
        if (m.find()) {
            int s = Math.max(0, m.start());
            int e = Math.min(content.length(), m.end());
            results.add(new ValidationResult(s, e,
                "Do not remove focus outline. Provide a visible :focus-visible indicator"));
        }
        return results;
    }

    @Override
    public int getPriority() { return 30; }
}
