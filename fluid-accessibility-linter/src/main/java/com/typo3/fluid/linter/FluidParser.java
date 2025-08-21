package com.typo3.fluid.linter;

import com.intellij.lang.ASTNode;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiParser;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

public class FluidParser implements PsiParser {
    @NotNull
    @Override
    public ASTNode parse(@NotNull IElementType root, @NotNull PsiBuilder builder) {
        PsiBuilder.Marker rootMarker = builder.mark();
        
        while (!builder.eof()) {
            IElementType tokenType = builder.getTokenType();
            
            if (tokenType == FluidTokenTypes.WHITE_SPACE || 
                tokenType == FluidTokenTypes.COMMENT) {
                // Skip whitespace and comments
                builder.advanceLexer();
            } else if (tokenType == FluidTokenTypes.TAG_START || 
                       tokenType == FluidTokenTypes.VIEWHELPER) {
                parseTag(builder);
            } else if (tokenType == FluidTokenTypes.VARIABLE) {
                parseVariable(builder);
            } else {
                // Text or other content
                builder.advanceLexer();
            }
        }
        
        rootMarker.done(root);
        return builder.getTreeBuilt();
    }
    
    private void parseTag(PsiBuilder builder) {
        PsiBuilder.Marker tagMarker = builder.mark();
        builder.advanceLexer(); // Consume the tag token
        tagMarker.done(FluidTokenTypes.TAG_START);
    }
    
    private void parseVariable(PsiBuilder builder) {
        PsiBuilder.Marker varMarker = builder.mark();
        builder.advanceLexer(); // Consume the variable token
        varMarker.done(FluidTokenTypes.VARIABLE);
    }
}