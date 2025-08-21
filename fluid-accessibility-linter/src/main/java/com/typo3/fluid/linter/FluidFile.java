package com.typo3.fluid.linter;

import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.FileViewProvider;
import org.jetbrains.annotations.NotNull;

public class FluidFile extends PsiFileBase {
    public FluidFile(@NotNull FileViewProvider viewProvider) {
        super(viewProvider, FluidLanguage.INSTANCE);
    }
    
    @NotNull
    @Override
    public FileType getFileType() {
        return FluidFileType.INSTANCE;
    }
    
    @Override
    public String toString() {
        return "Fluid File";
    }
    
    @Override
    public boolean isWritable() {
        return getViewProvider().isEventSystemEnabled();
    }
}