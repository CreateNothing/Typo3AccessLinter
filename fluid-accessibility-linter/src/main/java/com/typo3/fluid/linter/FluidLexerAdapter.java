package com.typo3.fluid.linter;

import com.intellij.lexer.LexerBase;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FluidLexerAdapter extends LexerBase {
    private CharSequence buffer;
    private int startOffset;
    private int endOffset;
    private int tokenStart;
    private int tokenEnd;
    private IElementType currentToken;
    
    @Override
    public void start(@NotNull CharSequence buffer, int startOffset, int endOffset, int initialState) {
        this.buffer = buffer;
        this.startOffset = startOffset;
        this.endOffset = endOffset;
        this.tokenStart = startOffset;
        advance();
    }
    
    @Override
    public int getState() {
        return 0;
    }
    
    @Nullable
    @Override
    public IElementType getTokenType() {
        return currentToken;
    }
    
    @Override
    public int getTokenStart() {
        return tokenStart;
    }
    
    @Override
    public int getTokenEnd() {
        return tokenEnd;
    }
    
    @Override
    public void advance() {
        tokenStart = tokenEnd;
        
        if (tokenStart >= endOffset) {
            currentToken = null;
            return;
        }
        
        char c = buffer.charAt(tokenStart);
        
        // Handle whitespace
        if (Character.isWhitespace(c)) {
            tokenEnd = tokenStart + 1;
            while (tokenEnd < endOffset && Character.isWhitespace(buffer.charAt(tokenEnd))) {
                tokenEnd++;
            }
            currentToken = FluidTokenTypes.WHITE_SPACE;
            return;
        }
        
        // Handle HTML comments
        if (tokenStart + 3 < endOffset && 
            buffer.charAt(tokenStart) == '<' && 
            buffer.charAt(tokenStart + 1) == '!' &&
            buffer.charAt(tokenStart + 2) == '-' &&
            buffer.charAt(tokenStart + 3) == '-') {
            tokenEnd = tokenStart + 4;
            while (tokenEnd + 2 < endOffset) {
                if (buffer.charAt(tokenEnd) == '-' && 
                    buffer.charAt(tokenEnd + 1) == '-' &&
                    buffer.charAt(tokenEnd + 2) == '>') {
                    tokenEnd += 3;
                    break;
                }
                tokenEnd++;
            }
            if (tokenEnd + 2 >= endOffset) {
                tokenEnd = endOffset;
            }
            currentToken = FluidTokenTypes.COMMENT;
            return;
        }
        
        // Handle tags (both HTML and Fluid)
        if (c == '<') {
            tokenEnd = tokenStart + 1;
            // Check for Fluid ViewHelper
            if (tokenEnd < endOffset && buffer.charAt(tokenEnd) == 'f' &&
                tokenEnd + 1 < endOffset && buffer.charAt(tokenEnd + 1) == ':') {
                // Fluid ViewHelper
                while (tokenEnd < endOffset && buffer.charAt(tokenEnd) != '>') {
                    tokenEnd++;
                }
                if (tokenEnd < endOffset) {
                    tokenEnd++; // Include the >
                }
                currentToken = FluidTokenTypes.VIEWHELPER;
            } else {
                // Regular HTML tag
                while (tokenEnd < endOffset && buffer.charAt(tokenEnd) != '>') {
                    tokenEnd++;
                }
                if (tokenEnd < endOffset) {
                    tokenEnd++; // Include the >
                }
                currentToken = FluidTokenTypes.TAG_START;
            }
            return;
        }
        
        // Handle Fluid variables
        if (c == '{') {
            tokenEnd = tokenStart + 1;
            int braceCount = 1;
            while (tokenEnd < endOffset && braceCount > 0) {
                char ch = buffer.charAt(tokenEnd);
                if (ch == '{') braceCount++;
                else if (ch == '}') braceCount--;
                tokenEnd++;
            }
            currentToken = FluidTokenTypes.VARIABLE;
            return;
        }
        
        // Default to text for everything else
        tokenEnd = tokenStart + 1;
        while (tokenEnd < endOffset) {
            char ch = buffer.charAt(tokenEnd);
            if (ch == '<' || ch == '{' || Character.isWhitespace(ch)) {
                break;
            }
            tokenEnd++;
        }
        currentToken = FluidTokenTypes.TEXT;
    }
    
    @NotNull
    @Override
    public CharSequence getBufferSequence() {
        return buffer;
    }
    
    @Override
    public int getBufferEnd() {
        return endOffset;
    }
}