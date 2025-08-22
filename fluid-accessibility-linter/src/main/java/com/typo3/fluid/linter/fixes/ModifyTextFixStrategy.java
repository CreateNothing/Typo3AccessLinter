package com.typo3.fluid.linter.fixes;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlTag;
import org.jetbrains.annotations.NotNull;

/**
 * Strategy for modifying text content within attributes (e.g., removing redundant phrases from alt text)
 */
public class ModifyTextFixStrategy implements FixStrategy {
    
    @Override
    public LocalQuickFix createFix(PsiFile file, int startOffset, int endOffset, FixContext context) {
        String attributeName = context.getStringAttribute("attributeName");
        String textToRemove = context.getStringAttribute("textToRemove");
        String replacement = context.getStringAttribute("replacement");
        
        if (attributeName == null || textToRemove == null) {
            return null;
        }
        
        return new ModifyTextFix(attributeName, textToRemove, replacement);
    }
    
    @Override
    public boolean canHandle(String problemType) {
        return problemType != null && (
            problemType.equals("remove-redundant-phrase") ||
            problemType.equals("fix-alt-text") ||
            problemType.equals("modify-text-content")
        );
    }
    
    private static class ModifyTextFix implements LocalQuickFix {
        private final String attributeName;
        private final String textToRemove;
        private final String replacement;
        
        public ModifyTextFix(String attributeName, String textToRemove, String replacement) {
            this.attributeName = attributeName;
            this.textToRemove = textToRemove;
            this.replacement = replacement != null ? replacement : "";
        }
        
        @NotNull
        @Override
        public String getName() {
            if (replacement.isEmpty()) {
                return String.format("Remove '%s' from %s", textToRemove, attributeName);
            }
            return String.format("Replace '%s' with '%s' in %s", textToRemove, replacement, attributeName);
        }
        
        @NotNull
        @Override
        public String getFamilyName() {
            return "Modify text content";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            PsiElement element = descriptor.getPsiElement();
            if (element == null) return;
            
            // Find the XML tag that contains this element
            XmlTag tag = findContainingTag(element);
            if (tag == null) return;
            
            // Get the attribute to modify
            XmlAttribute attribute = tag.getAttribute(attributeName);
            if (attribute != null && attribute.getValue() != null) {
                String currentValue = attribute.getValue();
                String newValue;
                
                // Handle different text modification patterns
                if (currentValue.toLowerCase().startsWith(textToRemove.toLowerCase() + " ")) {
                    // Remove phrase and following space from beginning
                    newValue = currentValue.substring(textToRemove.length() + 1).trim();
                } else if (currentValue.toLowerCase().equals(textToRemove.toLowerCase())) {
                    // Replace entire value
                    newValue = replacement;
                } else {
                    // Replace all occurrences
                    newValue = currentValue.replaceAll("(?i)" + textToRemove, replacement).trim();
                    // Clean up multiple spaces
                    newValue = newValue.replaceAll("\\s+", " ");
                }
                if (!replacement.isEmpty() && newValue.isEmpty()) {
                    newValue = replacement;
                }
                
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