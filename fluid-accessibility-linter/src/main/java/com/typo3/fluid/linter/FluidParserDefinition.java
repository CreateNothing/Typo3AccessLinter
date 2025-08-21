package com.typo3.fluid.linter;

import com.intellij.lang.ASTNode;
import com.intellij.lang.ParserDefinition;
import com.intellij.lang.PsiParser;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;

public class FluidParserDefinition implements ParserDefinition {
    public static final IFileElementType FILE = new IFileElementType(FluidLanguage.INSTANCE);
    public static final TokenSet WHITE_SPACES = TokenSet.create(FluidTokenTypes.WHITE_SPACE);
    
    @NotNull
    @Override
    public Lexer createLexer(Project project) {
        return new FluidLexerAdapter();
    }
    
    @Override
    public PsiParser createParser(Project project) {
        return new FluidParser();
    }
    
    @Override
    public IFileElementType getFileNodeType() {
        return FILE;
    }
    
    @NotNull
    @Override
    public TokenSet getCommentTokens() {
        return TokenSet.create(FluidTokenTypes.COMMENT);
    }
    
    @NotNull
    @Override
    public TokenSet getWhitespaceTokens() {
        return WHITE_SPACES;
    }
    
    @NotNull
    @Override
    public TokenSet getStringLiteralElements() {
        return TokenSet.EMPTY;
    }
    
    @NotNull
    @Override
    public PsiElement createElement(ASTNode node) {
        return new FluidPsiElement(node);
    }
    
    @Override
    public PsiFile createFile(FileViewProvider viewProvider) {
        return new FluidFile(viewProvider);
    }
    
    @Override
    public ParserDefinition.SpaceRequirements spaceExistenceTypeBetweenTokens(ASTNode left, ASTNode right) {
        return ParserDefinition.SpaceRequirements.MAY;
    }
}