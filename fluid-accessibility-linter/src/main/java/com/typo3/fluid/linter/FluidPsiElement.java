package com.typo3.fluid.linter;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;

public class FluidPsiElement extends ASTWrapperPsiElement {
    public FluidPsiElement(@NotNull ASTNode node) {
        super(node);
    }
}