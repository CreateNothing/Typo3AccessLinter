package com.typo3.fluid.linter;

import com.intellij.lang.Language;

public class FluidLanguage extends Language {
    public static final FluidLanguage INSTANCE = new FluidLanguage();
    
    private FluidLanguage() {
        super("Fluid");
    }
}