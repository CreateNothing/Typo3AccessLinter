package com.typo3.fluid.linter;

import com.intellij.openapi.fileTypes.LanguageFileType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import javax.swing.Icon;

public class FluidFileType extends LanguageFileType {
    public static final FluidFileType INSTANCE = new FluidFileType();
    
    private FluidFileType() {
        super(FluidLanguage.INSTANCE);
    }
    
    @NotNull
    @Override
    public String getName() {
        return "Fluid Template";
    }
    
    @NotNull
    @Override
    public String getDescription() {
        return "TYPO3 Fluid template file";
    }
    
    @NotNull
    @Override
    public String getDefaultExtension() {
        return "html";
    }
    
    @Nullable
    @Override
    public Icon getIcon() {
        return null; // You can add a custom icon later
    }
}