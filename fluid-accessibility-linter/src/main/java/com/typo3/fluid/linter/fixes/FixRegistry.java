package com.typo3.fluid.linter.fixes;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.psi.PsiFile;
import java.util.ArrayList;
import java.util.List;

/**
 * Registry for managing fix strategies
 */
public class FixRegistry {
    private static final FixRegistry INSTANCE = new FixRegistry();
    private final List<FixStrategy> strategies = new ArrayList<>();
    private static final ExtensionPointName<FixStrategy> FIX_STRATEGY_EP =
            ExtensionPointName.create("com.typo3.fluid.linter.fixStrategy");
    
    private FixRegistry() {
        // Register default strategies
        registerDefaultStrategies();
    }
    
    public static FixRegistry getInstance() {
        return INSTANCE;
    }
    
    public void register(FixStrategy strategy) {
        strategies.add(strategy);
    }
    
    public LocalQuickFix[] getFixes(PsiFile file, int startOffset, int endOffset, FixContext context) {
        List<LocalQuickFix> fixes = new ArrayList<>();
        
        for (FixStrategy strategy : strategies) {
            if (strategy.canHandle(context.getProblemType())) {
                LocalQuickFix fix = strategy.createFix(file, startOffset, endOffset, context);
                if (fix != null) {
                    fixes.add(fix);
                }
            }
        }
        
        return fixes.toArray(new LocalQuickFix[0]);
    }
    
    private void registerDefaultStrategies() {
        // Try to load from EP first
        try {
            FixStrategy[] epStrategies = FIX_STRATEGY_EP.getExtensions();
            if (epStrategies != null && epStrategies.length > 0) {
                for (FixStrategy fs : epStrategies) {
                    register(fs);
                }
            }
        } catch (Throwable ignored) {
            // EP may be unavailable depending on context; fall back below
        }

        if (strategies.isEmpty()) {
            // Fallback built-ins
            register(new AddAttributeFixStrategy());
            register(new ReplaceTextFixStrategy());
            register(new WrapElementFixStrategy());
            register(new ChangeAttributeFixStrategy());
            register(new AddChildElementFixStrategy());
            register(new MarkImageDecorativeFixStrategy());
            register(new DeriveAltFromFilenameFixStrategy());
            register(new GenerateTableHeaderScaffoldFixStrategy());
            register(new GenerateLabelForInputFixStrategy());
            register(new AddAriaRequiredAttributesFixStrategy());
            register(new AddSkipLinkFixStrategy());
            register(new AdjustHeadingLevelFixStrategy());
        }
    }
}
