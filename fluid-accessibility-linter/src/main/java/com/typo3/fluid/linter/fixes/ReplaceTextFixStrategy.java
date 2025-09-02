package com.typo3.fluid.linter.fixes;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

/**
 * Strategy for replacing text content
 */
public class ReplaceTextFixStrategy implements FixStrategy {
    
    @Override
    public LocalQuickFix createFix(PsiFile file, int startOffset, int endOffset, FixContext context) {
        String oldText = context.getStringAttribute("oldText");
        String newText = context.getStringAttribute("newText");
        String description = context.getStringAttribute("description");
        
        if (oldText == null || newText == null) {
            return null;
        }
        
        return new ReplaceTextFix(oldText, newText, description);
    }
    
    @Override
    public boolean canHandle(String problemType) {
        return problemType != null && (
            problemType.equals("replace-text") ||
            problemType.equals("invalid-value") ||
            problemType.equals("deprecated-syntax")
        );
    }
    
    private static class ReplaceTextFix implements LocalQuickFix {
        private final String oldText;
        private final String newText;
        private final String description;
        
        public ReplaceTextFix(String oldText, String newText, String description) {
            this.oldText = oldText;
            this.newText = newText;
            this.description = description;
        }
        
        @NotNull
        @Override
        public String getName() {
            if (description != null) {
                return description;
            }
            return String.format("Replace with %s", newText);
        }
        
        @NotNull
        @Override
        public String getFamilyName() { return "Accessibility"; }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would replace the text
            // This is simplified - actual implementation would use PSI manipulation
        }
    }
}
