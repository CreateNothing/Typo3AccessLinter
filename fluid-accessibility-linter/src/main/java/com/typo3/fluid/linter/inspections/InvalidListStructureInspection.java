package com.typo3.fluid.linter.inspections;

import com.intellij.codeInspection.*;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.xml.*;
import com.intellij.xml.util.HtmlUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class InvalidListStructureInspection extends LocalInspectionTool {
    private static final String DESCRIPTION = "Lists should only contain <li> elements as direct children";
    
    // Control flow ViewHelpers that don't generate HTML output
    private static final Set<String> CONTROL_FLOW_VIEWHELPERS = new HashSet<>(Arrays.asList(
        "for", "if", "else", "then", "switch", "case", "defaultCase",
        "groupedFor", "cycle", "variable", "alias", "comment", "spaceless",
        "cObject", "debug", "render", "section", "layout"
    ));
    
    // Fluid namespace patterns
    private static final String FLUID_NAMESPACE = "http://typo3.org/ns/TYPO3/CMS/Fluid/ViewHelpers";
    
    @NotNull
    @Override
    public String getDisplayName() {
        return "Invalid list structure";
    }
    
    @NotNull
    @Override
    public String getShortName() {
        return "InvalidListStructure";
    }
    
    @NotNull
    @Override
    public String getGroupDisplayName() {
        return "Fluid Accessibility";
    }
    
    @Override
    public boolean isEnabledByDefault() {
        return true;
    }
    
    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new XmlElementVisitor() {
            @Override
            public void visitXmlFile(XmlFile file) {
                // Only process HTML files
                if (file.getName().endsWith(".html")) {
                    super.visitXmlFile(file);
                }
            }
            
            @Override
            public void visitXmlTag(XmlTag tag) {
                String tagName = tag.getLocalName();
                if (tagName != null) {
                    tagName = tagName.toLowerCase();
                    
                    // Check if this is a list tag (ul or ol)
                    if ("ul".equals(tagName) || "ol".equals(tagName)) {
                        checkListStructure(tag, holder);
                    }
                }
                
                super.visitXmlTag(tag);
            }
        };
    }
    
    private void checkListStructure(XmlTag listTag, ProblemsHolder holder) {
        String listType = listTag.getLocalName().toLowerCase();
        
        // Get direct children only
        XmlTag[] directChildren = listTag.getSubTags();
        
        for (XmlTag child : directChildren) {
            if (!isValidListChild(child)) {
                // Get the actual tag name for error message
                String childDescription = getTagDescription(child);
                String message = String.format(
                    "<%s> should only contain <li> elements or control flow ViewHelpers as direct children, found: %s",
                    listType, childDescription
                );
                
                // Register problem at the start of the child tag
                PsiElement problemElement = child.getNavigationElement();
                if (problemElement == null) {
                    problemElement = child;
                }
                
                holder.registerProblem(
                    problemElement,
                    message,
                    ProblemHighlightType.WARNING,
                    new WrapInListItemQuickFix(child.getName())
                );
            } else if (isControlFlowViewHelper(child)) {
                // If it's a control flow ViewHelper, check its content recursively
                checkViewHelperContent(child, listType, holder);
            }
        }
        
        // Check for direct text content (non-whitespace)
        checkForDirectTextContent(listTag, holder);
    }
    
    private boolean isValidListChild(XmlTag tag) {
        String tagName = tag.getLocalName();
        if (tagName == null) {
            return false;
        }
        
        tagName = tagName.toLowerCase();
        
        // Check if it's an <li> element
        if ("li".equals(tagName)) {
            return true;
        }
        
        // Check if it's a control flow ViewHelper
        return isControlFlowViewHelper(tag);
    }
    
    private boolean isControlFlowViewHelper(XmlTag tag) {
        // Check if it's a Fluid ViewHelper
        String tagName = tag.getName();
        String localName = tag.getLocalName();
        
        // Check for f: prefix
        if (tagName.startsWith("f:")) {
            String viewHelperName = tagName.substring(2);
            return CONTROL_FLOW_VIEWHELPERS.contains(viewHelperName);
        }
        
        // Check for namespace
        String namespace = tag.getNamespace();
        if (FLUID_NAMESPACE.equals(namespace)) {
            return CONTROL_FLOW_VIEWHELPERS.contains(localName);
        }
        
        // Check for any namespace prefix followed by known control flow names
        if (tagName.contains(":")) {
            String[] parts = tagName.split(":", 2);
            if (parts.length == 2) {
                return CONTROL_FLOW_VIEWHELPERS.contains(parts[1]);
            }
        }
        
        return false;
    }
    
    private void checkViewHelperContent(XmlTag viewHelperTag, String listType, ProblemsHolder holder) {
        // Check the direct children of the ViewHelper
        XmlTag[] viewHelperChildren = viewHelperTag.getSubTags();
        
        for (XmlTag child : viewHelperChildren) {
            if (!isValidListChild(child)) {
                // ViewHelper content should also follow list rules
                String childDescription = getTagDescription(child);
                String message = String.format(
                    "<%s> should only contain <li> elements or control flow ViewHelpers as direct children (inside <%s>), found: %s",
                    listType, viewHelperTag.getName(), childDescription
                );
                
                PsiElement problemElement = child.getNavigationElement();
                if (problemElement == null) {
                    problemElement = child;
                }
                
                holder.registerProblem(
                    problemElement,
                    message,
                    ProblemHighlightType.WARNING,
                    new WrapInListItemQuickFix(child.getName())
                );
            } else if (isControlFlowViewHelper(child)) {
                // Recursively check nested ViewHelpers
                checkViewHelperContent(child, listType, holder);
            }
        }
    }
    
    private void checkForDirectTextContent(XmlTag listTag, ProblemsHolder holder) {
        // Check for direct text nodes
        PsiElement[] children = listTag.getChildren();
        
        for (PsiElement child : children) {
            if (child instanceof XmlText) {
                XmlText textNode = (XmlText) child;
                String text = textNode.getValue().trim();
                
                // Ignore whitespace-only text nodes
                if (!text.isEmpty()) {
                    String listType = listTag.getLocalName().toLowerCase();
                    String message = String.format(
                        "<%s> contains direct text content. Text must be wrapped in <li> elements",
                        listType
                    );
                    
                    holder.registerProblem(
                        textNode,
                        message,
                        ProblemHighlightType.WARNING
                    );
                }
            }
        }
    }
    
    private String getTagDescription(XmlTag tag) {
        String tagName = tag.getName();
        if (tagName.startsWith("f:")) {
            return "<" + tagName + ">";
        } else {
            String localName = tag.getLocalName();
            if (localName != null) {
                return "<" + localName.toLowerCase() + ">";
            }
            return "<" + tagName + ">";
        }
    }
    
    private static class WrapInListItemQuickFix implements LocalQuickFix {
        private final String tagName;
        
        WrapInListItemQuickFix(String tagName) {
            this.tagName = tagName;
        }
        
        @NotNull
        @Override
        public String getName() {
            return "Wrap in <li> element";
        }
        
        @NotNull
        @Override
        public String getFamilyName() {
            return "Fix list structure";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            PsiElement element = descriptor.getPsiElement();
            if (!(element instanceof XmlTag)) {
                element = element.getParent();
                if (!(element instanceof XmlTag)) {
                    return;
                }
            }
            
            XmlTag tag = (XmlTag) element;
            
            // Create a new <li> element wrapping the current tag
            String wrappedContent = "<li>" + tag.getText() + "</li>";
            
            // Replace the tag with the wrapped version
            XmlTag newTag = XmlElementFactory.getInstance(project).createHTMLTagFromText(wrappedContent);
            if (newTag != null) {
                tag.replace(newTag);
            }
        }
    }
}