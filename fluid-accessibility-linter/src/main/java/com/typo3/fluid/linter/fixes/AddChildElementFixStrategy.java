package com.typo3.fluid.linter.fixes;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.openapi.command.WriteCommandAction;
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
        public String getFamilyName() { return "Structure"; }
        
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
            
            String childXml = String.format("<%s>%s</%s>", childTagName, childContent, childTagName);
            WriteCommandAction.runWriteCommandAction(project, "Add child element", null, () -> {
                try {
                    // Create a temporary file with a root wrapper to parse the child tag
                    String wrapped = "<root>" + childXml + "</root>";
                    PsiFile temp = PsiFileFactory.getInstance(project)
                            .createFileFromText("temp.xml", parentTag.getContainingFile().getFileType(), wrapped);
                    XmlTag root = firstXmlTag(temp);
                    if (root == null) return;
                    XmlTag childTag = root.findFirstSubTag(childTagName);
                    if (childTag == null) return;
                    if (parentTag.getSubTags().length > 0) {
                        parentTag.addBefore(childTag, parentTag.getSubTags()[0]);
                    } else {
                        parentTag.add(childTag);
                    }
                } catch (Throwable ignored) {
                    // Best-effort; leave unchanged on parse failure
                }
            });
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

        private XmlTag firstXmlTag(PsiElement element) {
            PsiElement cur = element.getFirstChild();
            while (cur != null) {
                if (cur instanceof XmlTag) return (XmlTag) cur;
                XmlTag nested = firstXmlTag(cur);
                if (nested != null) return nested;
                cur = cur.getNextSibling();
            }
            return null;
        }
    }
}
