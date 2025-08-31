package com.typo3.fluid.linter.inspections;

import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Detects risky content filters that may remove important semantics, e.g. f:format.stripTags
 * removing headings, lists, or table structure.
 */
public class ContentFilterSemanticsInspection extends FluidAccessibilityInspection {

    private static final Pattern STRIPTAGS_BLOCK = Pattern.compile(
        "<f:format\\.stripTags(?:\\s+[^>]*)?>(.*?)</f:format\\.stripTags>",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    @NotNull
    @Override
    public String getDisplayName() {
        return "Content filters may remove semantics";
    }

    @NotNull
    @Override
    public String getShortName() {
        return "ContentFilterSemantics";
    }

    @Override
    protected void inspectFile(@NotNull PsiFile file, @NotNull ProblemsHolder holder) {
        String content = file.getText();

        Matcher m = STRIPTAGS_BLOCK.matcher(content);
        while (m.find()) {
            String inner = m.group(1);
            if (inner == null) inner = "";
            String lower = inner.toLowerCase();

            boolean removesHeadings = lower.matches(".*<h[1-6][^>]*>.*");
            if (removesHeadings) {
                register(holder, file, m.start(), m.end(),
                    "Removing headings may harm accessibility", ProblemHighlightType.WARNING);
                continue;
            }

            boolean removesLists = lower.contains("<ul") || lower.contains("<ol") || lower.contains("<dl");
            if (removesLists) {
                register(holder, file, m.start(), m.end(),
                    "Removing lists may harm accessibility", ProblemHighlightType.INFORMATION);
            }
        }
    }

    private void register(ProblemsHolder holder, PsiFile file, int start, int end,
                          String message, ProblemHighlightType type) {
        PsiElement element = file.findElementAt(Math.max(0, Math.min(start, file.getTextLength()-1)));
        if (element != null) {
            holder.registerProblem(element, message, type);
        }
    }
}

