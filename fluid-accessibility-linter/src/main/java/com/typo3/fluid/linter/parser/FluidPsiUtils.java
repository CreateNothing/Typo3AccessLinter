package com.typo3.fluid.linter.parser;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiRecursiveElementVisitor;
import com.intellij.psi.xml.XmlTag;
import com.intellij.psi.xml.XmlText;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Fluid-aware PSI utilities: view helper detection and control-flow unwrapping.
 */
public final class FluidPsiUtils {
    private static final Set<String> CONTROL_FLOW_NAMES = new HashSet<>(Arrays.asList(
            "if", "then", "else", "for", "switch", "case", "alias"
    ));

    private FluidPsiUtils() {}

    /**
     * Return true if the tag represents a Fluid ViewHelper (e.g., f:if, v:xyz:tag).
     */
    public static boolean isFluidViewHelper(PsiElement element) {
        if (element instanceof XmlTag) {
            String name = ((XmlTag) element).getName();
            return name.contains(":");
        }
        return false;
    }

    /**
     * Returns true if the tag is a control-flow ViewHelper that should be unwrapped for structural checks.
     */
    public static boolean isControlFlowViewHelper(XmlTag tag) {
        String name = tag.getName();
        int idx = name.lastIndexOf(':');
        String local = idx >= 0 ? name.substring(idx + 1) : name;
        return CONTROL_FLOW_NAMES.contains(local.toLowerCase());
    }

    /**
     * Recursively collect the effective children for structural validation: unwrap known control-flow wrappers.
     */
    public static List<PsiElement> getEffectiveChildren(XmlTag container) {
        List<PsiElement> out = new ArrayList<>();
        for (PsiElement child : container.getChildren()) {
            if (child instanceof XmlText) {
                out.add(child);
                continue;
            }
            if (child instanceof XmlTag) {
                XmlTag tag = (XmlTag) child;
                if (isFluidViewHelper(tag) && isControlFlowViewHelper(tag)) {
                    out.addAll(getEffectiveChildren(tag));
                } else {
                    out.add(tag);
                }
            }
        }
        return out;
    }

    /**
     * Find XmlTags by exact name (case-insensitive).
     */
    public static List<XmlTag> findTagsByName(@NotNull PsiFile file, @NotNull String tagName) {
        List<XmlTag> tags = new ArrayList<>();
        file.accept(new PsiRecursiveElementVisitor() {
            @Override
            public void visitElement(@NotNull PsiElement element) {
                if (element instanceof XmlTag) {
                    XmlTag tag = (XmlTag) element;
                    if (tagName.equalsIgnoreCase(tag.getName())) {
                        tags.add(tag);
                    }
                }
                super.visitElement(element);
            }
        });
        return tags;
    }
}

