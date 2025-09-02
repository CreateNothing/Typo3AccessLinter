package com.typo3.fluid.linter.fixes;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

/**
 * Strategy for wrapping elements with container elements
 */
public class WrapElementFixStrategy implements FixStrategy {
    
    @Override
    public LocalQuickFix createFix(PsiFile file, int startOffset, int endOffset, FixContext context) {
        String wrapperTag = context.getStringAttribute("wrapperTag");
        String wrapperAttributes = context.getStringAttribute("wrapperAttributes");
        
        if (wrapperTag == null) {
            return null;
        }
        
        return new WrapElementFix(wrapperTag, wrapperAttributes);
    }
    
    @Override
    public boolean canHandle(String problemType) {
        return problemType != null && (
            problemType.equals("wrap-element") ||
            problemType.equals("missing-container") ||
            problemType.equals("structure-issue")
        );
    }
    
    private static class WrapElementFix implements LocalQuickFix {
        private final String wrapperTag;
        private final String wrapperAttributes;
        
        public WrapElementFix(String wrapperTag, String wrapperAttributes) {
            this.wrapperTag = wrapperTag;
            this.wrapperAttributes = wrapperAttributes;
        }
        
        @NotNull
        @Override
        public String getName() {
            if (wrapperAttributes != null) {
                return String.format("Wrap with <%s %s>", wrapperTag, wrapperAttributes);
            }
            return String.format("Wrap with <%s>", wrapperTag);
        }
        
        @NotNull
        @Override
        public String getFamilyName() { return "Structure"; }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            PsiElement element = descriptor.getPsiElement();
            if (element == null) return;
            PsiFile file = element.getContainingFile();
            if (file == null) return;
            int start = element.getTextRange().getStartOffset();
            int end = element.getTextRange().getEndOffset();
            PsiMutationUtils.wrapRangeWithTag(project, file, start, end, wrapperTag, wrapperAttributes);
        }
    }
}
