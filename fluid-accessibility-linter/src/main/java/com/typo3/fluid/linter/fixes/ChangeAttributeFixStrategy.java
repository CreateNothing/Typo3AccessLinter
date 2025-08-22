package com.typo3.fluid.linter.fixes;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlTag;
import com.typo3.fluid.linter.parser.PsiElementParser;
import org.jetbrains.annotations.NotNull;

/**
 * Strategy for changing existing attribute values in HTML/Fluid elements
 */
public class ChangeAttributeFixStrategy implements FixStrategy {
    
    @Override
    public LocalQuickFix createFix(PsiFile file, int startOffset, int endOffset, FixContext context) {
        String attributeName = context.getStringAttribute("attributeName");
        String newValue = context.getStringAttribute("newValue");
        String oldValue = context.getStringAttribute("oldValue");
        
        if (attributeName == null || newValue == null) {
            return null;
        }
        
        return new ChangeAttributeFix(attributeName, newValue, oldValue);
    }
    
    @Override
    public boolean canHandle(String problemType) {
        return problemType != null && (
            problemType.equals("change-attribute") ||
            problemType.equals("change-alt-empty") ||
            problemType.equals("fix-alt-text")
        );
    }
    
    private static class ChangeAttributeFix implements LocalQuickFix {
        private final String attributeName;
        private final String newValue;
        private final String oldValue;
        
        public ChangeAttributeFix(String attributeName, String newValue, String oldValue) {
            this.attributeName = attributeName;
            this.newValue = newValue;
            this.oldValue = oldValue;
        }
        
        @NotNull
        @Override
        public String getName() {
            if (newValue.isEmpty()) {
                return String.format("Change %s to empty (%s=\"\")", attributeName, attributeName);
            }
            return String.format("Change %s to \"%s\"", attributeName, newValue);
        }
        
        @NotNull
        @Override
        public String getFamilyName() {
            return "Change attribute value";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            PsiElement element = descriptor.getPsiElement();
            if (element == null) return;
            
            // Find the XML tag that contains this element
            XmlTag tag = findContainingTag(element);
            if (tag == null) return;
            
            // Get the attribute to change
            XmlAttribute attribute = tag.getAttribute(attributeName);
            if (attribute != null) {
                // Change the attribute value
                attribute.setValue(newValue);
            }
        }
        
        private XmlTag findContainingTag(PsiElement element) {
            PsiElement current = element;
            while (current != null) {
                if (current instanceof XmlTag) {
                    return (XmlTag) current;
                }
                current = current.getParent();
            }
            return null;
        }
    }
}