package com.typo3.fluid.linter.fixes;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

/**
 * Strategy for adding attributes to HTML/Fluid elements
 */
public class AddAttributeFixStrategy implements FixStrategy {
    
    @Override
    public LocalQuickFix createFix(PsiFile file, int startOffset, int endOffset, FixContext context) {
        String attributeName = context.getStringAttribute("attributeName");
        String attributeValue = context.getStringAttribute("attributeValue");
        String elementType = context.getStringAttribute("elementType");
        
        if (attributeName == null) {
            return null;
        }
        
        return new AddAttributeFix(attributeName, attributeValue, elementType);
    }
    
    @Override
    public boolean canHandle(String problemType) {
        return problemType != null && (
            problemType.equals("missing-attribute") ||
            problemType.equals("missing-alt") ||
            problemType.equals("missing-role") ||
            problemType.equals("missing-aria-label")
        );
    }
    
    private static class AddAttributeFix implements LocalQuickFix {
        private final String attributeName;
        private final String attributeValue;
        private final String elementType;
        
        public AddAttributeFix(String attributeName, String attributeValue, String elementType) {
            this.attributeName = attributeName;
            this.attributeValue = attributeValue;
            this.elementType = elementType;
        }
        
        @NotNull
        @Override
        public String getName() {
            if (attributeValue != null) {
                return String.format("Add %s=\"%s\"", attributeName, attributeValue);
            }
            return String.format("Add %s attribute", attributeName);
        }
        
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add attribute";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add the attribute to the element
            // This is simplified - actual implementation would use PSI manipulation
        }
    }
}