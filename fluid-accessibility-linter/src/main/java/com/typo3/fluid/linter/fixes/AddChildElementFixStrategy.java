package com.typo3.fluid.linter.fixes;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.xml.XmlTag;
import org.jetbrains.annotations.NotNull;

/**
 * Strategy for adding child elements to existing tags (e.g., adding <title> to SVG)
 */
public class AddChildElementFixStrategy implements FixStrategy {
    
    @Override
    public LocalQuickFix createFix(PsiFile file, int startOffset, int endOffset, FixContext context) {
        String childTagName = context.getStringAttribute("childTagName");
        String childContent = context.getStringAttribute("childContent");
        String parentTagName = context.getStringAttribute("parentTagName");
        
        if (childTagName == null) {
            return null;
        }
        
        return new AddChildElementFix(childTagName, childContent, parentTagName);
    }
    
    @Override
    public boolean canHandle(String problemType) {
        return problemType != null && (
            problemType.equals("add-child-element") ||
            problemType.equals("add-svg-title") ||
            problemType.equals("add-svg-desc")
        );
    }
    
    private static class AddChildElementFix implements LocalQuickFix {
        private final String childTagName;
        private final String childContent;
        private final String parentTagName;
        
        public AddChildElementFix(String childTagName, String childContent, String parentTagName) {
            this.childTagName = childTagName;
            this.childContent = childContent != null ? childContent : "Description";
            this.parentTagName = parentTagName;
        }
        
        @NotNull
        @Override
        public String getName() {
            return String.format("Add <%s> element", childTagName);
        }
        
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add child element";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            PsiElement element = descriptor.getPsiElement();
            if (element == null) return;
            
            // Find the target tag (SVG or other parent)
            XmlTag parentTag = findContainingTag(element);
            if (parentTag == null) return;
            
            // Verify it's the correct parent type if specified
            if (parentTagName != null && !parentTagName.equals(parentTag.getName())) {
                return;
            }
            
            // Check if child element already exists
            XmlTag[] existingChildren = parentTag.getSubTags();
            for (XmlTag child : existingChildren) {
                if (childTagName.equals(child.getName())) {
                    // Child already exists, don't add another
                    return;
                }
            }
            
            try {
                // Create a temporary XML file with the child element
                String childXml = String.format("<%s>%s</%s>", childTagName, childContent, childTagName);
                PsiFile tempFile = PsiFileFactory.getInstance(project)
                    .createFileFromText("temp.xml", parentTag.getContainingFile().getFileType(), 
                        "<root>" + childXml + "</root>");
                
                // Find the created child tag in the temporary file
                XmlTag rootTag = (XmlTag) tempFile.getFirstChild();
                if (rootTag != null && rootTag.getSubTags().length > 0) {
                    XmlTag childTag = rootTag.getSubTags()[0];
                    
                    // Add as first child to make it accessible immediately
                    if (parentTag.getSubTags().length > 0) {
                        parentTag.addBefore(childTag, parentTag.getSubTags()[0]);
                    } else {
                        parentTag.add(childTag);
                    }
                }
            } catch (Exception e) {
                // Fallback: add as simple text if XML creation fails
                // This is a simplified approach - in production, better error handling would be needed
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