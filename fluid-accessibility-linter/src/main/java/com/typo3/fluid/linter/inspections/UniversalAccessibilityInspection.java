package com.typo3.fluid.linter.inspections;

import com.intellij.codeInspection.*;
import com.intellij.psi.PsiFile;
import com.typo3.fluid.linter.fixes.FixContext;
import com.typo3.fluid.linter.fixes.FixRegistry;
import com.typo3.fluid.linter.rules.RuleEngine;
import com.typo3.fluid.linter.rules.RuleViolation;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Universal accessibility inspection that uses the rule engine.
 * This replaces all individual inspection classes with a single, configurable entry point.
 */
public class UniversalAccessibilityInspection extends FluidAccessibilityInspection {
    
    private final RuleEngine ruleEngine = RuleEngine.getInstance();
    private final FixRegistry fixRegistry = FixRegistry.getInstance();
    
    @Override
    protected void inspectFile(@NotNull PsiFile file, @NotNull ProblemsHolder holder) {
        // Execute all enabled rules
        List<RuleViolation> violations = ruleEngine.execute(file);
        
        // Register problems for each violation
        for (RuleViolation violation : violations) {
            registerViolation(file, holder, violation);
        }
    }
    
    private void registerViolation(PsiFile file, ProblemsHolder holder, RuleViolation violation) {
        // Create fix context
        FixContext fixContext = new FixContext(violation.getRule().getId());
        fixContext.setAttribute("rule", violation.getRule());
        fixContext.setAttribute("result", violation.getResult());
        
        // Get applicable fixes
        LocalQuickFix[] fixes = fixRegistry.getFixes(
            file,
            violation.getResult().getStartOffset(),
            violation.getResult().getEndOffset(),
            fixContext
        );
        
        // Register the problem
        registerProblem(
            holder,
            file,
            violation.getResult().getStartOffset(),
            violation.getResult().getEndOffset(),
            violation.getMessage(),
            violation.getRule().getSeverity().getHighlightType(),
            fixes.length > 0 ? fixes[0] : null
        );
    }
    
    @Override
    @NotNull
    public String getShortName() {
        return "FluidUniversalAccessibility";
    }
    
    @Override
    @NotNull
    public String getDisplayName() {
        return "Universal Fluid Accessibility Check";
    }
    
    @Override
    @NotNull
    public String getGroupDisplayName() {
        return "Fluid Accessibility";
    }
    
    @Override
    public boolean isEnabledByDefault() {
        return true;
    }
    
    @Override
    @NotNull
    public String[] getGroupPath() {
        return new String[]{"HTML", "Accessibility"};
    }
}