package com.typo3.fluid.linter;

import com.intellij.psi.tree.IElementType;
import com.intellij.psi.TokenType;

public class FluidTokenTypes {
    public static final IElementType TEXT = new IElementType("TEXT", FluidLanguage.INSTANCE);
    public static final IElementType WHITE_SPACE = TokenType.WHITE_SPACE;
    public static final IElementType COMMENT = new IElementType("COMMENT", FluidLanguage.INSTANCE);
    public static final IElementType TAG_START = new IElementType("TAG_START", FluidLanguage.INSTANCE);
    public static final IElementType TAG_END = new IElementType("TAG_END", FluidLanguage.INSTANCE);
    public static final IElementType VIEWHELPER = new IElementType("VIEWHELPER", FluidLanguage.INSTANCE);
    public static final IElementType ATTRIBUTE = new IElementType("ATTRIBUTE", FluidLanguage.INSTANCE);
    public static final IElementType VARIABLE = new IElementType("VARIABLE", FluidLanguage.INSTANCE);
}