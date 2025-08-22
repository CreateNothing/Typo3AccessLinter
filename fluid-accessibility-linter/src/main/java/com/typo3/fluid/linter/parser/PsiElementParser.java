package com.typo3.fluid.linter.parser;

import com.intellij.psi.*;
import com.intellij.psi.html.HtmlTag;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlTag;
import com.intellij.psi.xml.XmlToken;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * PSI-based parser for HTML/Fluid elements.
 * Replaces regex patterns with proper AST parsing.
 */
public class PsiElementParser {
    
    /**
     * Find all elements of a specific tag type
     */
    public static List<PsiElement> findElementsByTagName(@NotNull PsiFile file, @NotNull String tagName) {
        List<PsiElement> elements = new ArrayList<>();
        file.accept(new PsiRecursiveElementVisitor() {
            @Override
            public void visitElement(@NotNull PsiElement element) {
                if (element instanceof XmlTag) {
                    XmlTag tag = (XmlTag) element;
                    if (tagName.equalsIgnoreCase(tag.getName())) {
                        elements.add(element);
                    }
                } else if (element instanceof HtmlTag) {
                    HtmlTag tag = (HtmlTag) element;
                    if (tagName.equalsIgnoreCase(tag.getName())) {
                        elements.add(element);
                    }
                }
                super.visitElement(element);
            }
        });
        return elements;
    }
    
    /**
     * Find all elements matching a predicate
     */
    public static List<PsiElement> findElements(@NotNull PsiFile file, @NotNull ElementPredicate predicate) {
        List<PsiElement> elements = new ArrayList<>();
        file.accept(new PsiRecursiveElementVisitor() {
            @Override
            public void visitElement(@NotNull PsiElement element) {
                if (predicate.test(element)) {
                    elements.add(element);
                }
                super.visitElement(element);
            }
        });
        return elements;
    }
    
    /**
     * Get attribute value from an element
     */
    @Nullable
    public static String getAttributeValue(@NotNull PsiElement element, @NotNull String attributeName) {
        if (element instanceof XmlTag) {
            XmlTag tag = (XmlTag) element;
            XmlAttribute attribute = tag.getAttribute(attributeName);
            return attribute != null ? attribute.getValue() : null;
        } else if (element instanceof HtmlTag) {
            HtmlTag tag = (HtmlTag) element;
            String value = tag.getAttributeValue(attributeName);
            return value;
        }
        return null;
    }
    
    /**
     * Check if element has attribute
     */
    public static boolean hasAttribute(@NotNull PsiElement element, @NotNull String attributeName) {
        return getAttributeValue(element, attributeName) != null;
    }
    
    /**
     * Get all attributes of an element
     */
    public static List<String> getAttributeNames(@NotNull PsiElement element) {
        List<String> names = new ArrayList<>();
        if (element instanceof XmlTag) {
            XmlTag tag = (XmlTag) element;
            for (XmlAttribute attr : tag.getAttributes()) {
                names.add(attr.getName());
            }
        }
        return names;
    }
    
    /**
     * Get direct children of a specific type
     */
    public static <T extends PsiElement> List<T> getChildrenOfType(@NotNull PsiElement element, @NotNull Class<T> type) {
        List<T> children = new ArrayList<>();
        for (PsiElement child : element.getChildren()) {
            if (type.isInstance(child)) {
                children.add(type.cast(child));
            }
        }
        return children;
    }
    
    /**
     * Find parent element of type
     */
    @Nullable
    public static <T extends PsiElement> T getParentOfType(@NotNull PsiElement element, @NotNull Class<T> type) {
        PsiElement parent = element.getParent();
        while (parent != null) {
            if (type.isInstance(parent)) {
                return type.cast(parent);
            }
            parent = parent.getParent();
        }
        return null;
    }
    
    /**
     * Check if element is a Fluid ViewHelper
     */
    public static boolean isFluidViewHelper(@NotNull PsiElement element) {
        if (element instanceof XmlTag) {
            XmlTag tag = (XmlTag) element;
            String name = tag.getName();
            return name.contains(":") || name.startsWith("f:");
        }
        return false;
    }
    
    /**
     * Get tag name from element
     */
    @Nullable
    public static String getTagName(@NotNull PsiElement element) {
        if (element instanceof XmlTag) {
            return ((XmlTag) element).getName();
        } else if (element instanceof HtmlTag) {
            return ((HtmlTag) element).getName();
        }
        return null;
    }
    
    /**
     * Functional interface for element predicates
     */
    @FunctionalInterface
    public interface ElementPredicate {
        boolean test(PsiElement element);
    }
}