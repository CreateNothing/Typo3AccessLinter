package com.typo3.fluid.linter.inspections;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AccordionAccessibilityInspection extends FluidAccessibilityInspection {
    
    protected static final Pattern ACCORDION_PATTERN = Pattern.compile(
        "<[^>]*(?:class\\s*=\\s*[\"'][^\"']*(?:accordion|collapsible|expandable|toggle)[^\"']*[\"']" +
        "|data-toggle\\s*=\\s*[\"'](?:accordion|collapse)[\"'])[^>]*>",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern BUTTON_PATTERN = Pattern.compile(
        "<(button|[^>]+\\brole\\s*=\\s*[\"']button[\"'])[^>]*>",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern ARIA_EXPANDED_PATTERN = Pattern.compile(
        "\\baria-expanded\\s*=\\s*[\"']([^\"']+)[\"']",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern ARIA_CONTROLS_PATTERN = Pattern.compile(
        "\\baria-controls\\s*=\\s*[\"']([^\"']+)[\"']",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern ID_PATTERN = Pattern.compile(
        "\\bid\\s*=\\s*[\"']([^\"']+)[\"']",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern ARIA_LABELLEDBY_PATTERN = Pattern.compile(
        "\\baria-labelledby\\s*=\\s*[\"']([^\"']+)[\"']",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern ROLE_REGION_PATTERN = Pattern.compile(
        "\\brole\\s*=\\s*[\"']region[\"']",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern TABINDEX_PATTERN = Pattern.compile(
        "\\btabindex\\s*=\\s*[\"']([^\"']+)[\"']",
        Pattern.CASE_INSENSITIVE
    );
    
    @NotNull
    @Override
    public String getDisplayName() {
        return "Accordion and collapsible content accessibility issues";
    }
    
    @NotNull
    @Override
    public String getShortName() {
        return "AccordionAccessibility";
    }
    
    @Override
    protected void inspectFile(@NotNull PsiFile file, @NotNull ProblemsHolder holder) {
        String content = file.getText();
        
        // Check accordion containers
        checkAccordionStructure(content, file, holder);
        
        // Check accordion buttons/triggers
        checkAccordionButtons(content, file, holder);
        
        // Check panel associations
        checkPanelAssociations(content, file, holder);
        
        // Check for common accordion patterns
        checkCommonAccordionPatterns(content, file, holder);
        
        // Enhanced context-aware checks
        validateStateManagement(content, file, holder);
        checkKeyboardNavigation(content, file, holder);
        validateNestedAccordions(content, file, holder);
        checkAccordionGrouping(content, file, holder);
    }
    
    private void checkAccordionStructure(String content, PsiFile file, ProblemsHolder holder) {
        Matcher accordionMatcher = ACCORDION_PATTERN.matcher(content);
        
        while (accordionMatcher.find()) {
            int accordionStart = accordionMatcher.start();
            int accordionEnd = findElementEnd(content, accordionStart);
            
            if (accordionEnd > accordionStart) {
                String accordionContent = content.substring(accordionStart, 
                    Math.min(accordionEnd, content.length()));
                
                // Check for proper button triggers
                checkAccordionTriggers(accordionContent, file, holder, accordionStart);
                
                // Check for proper panel structure
                checkAccordionPanels(accordionContent, file, holder, accordionStart);
            }
        }
    }
    
    private void checkAccordionButtons(String content, PsiFile file, ProblemsHolder holder) {
        // Find all potential accordion buttons
        Pattern accordionButtonPattern = Pattern.compile(
            "<(button|[^>]+\\brole\\s*=\\s*[\"']button[\"'])[^>]*" +
            "(?:class\\s*=\\s*[\"'][^\"']*accordion[^\"']*[\"']|" +
            "data-toggle\\s*=\\s*[\"']collapse[\"'])[^>]*>",
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher buttonMatcher = accordionButtonPattern.matcher(content);
        
        while (buttonMatcher.find()) {
            String buttonTag = buttonMatcher.group();
            int buttonStart = buttonMatcher.start();
            int buttonEnd = buttonMatcher.end();
            
            // Check for aria-expanded
            checkAriaExpanded(buttonTag, file, holder, buttonStart, buttonEnd);
            
            // Check for aria-controls
            checkAriaControls(buttonTag, content, file, holder, buttonStart, buttonEnd);
            
            // Check button semantics
            checkButtonSemantics(buttonTag, file, holder, buttonStart, buttonEnd);
        }
    }
    
    private void checkAccordionTriggers(String accordionContent, PsiFile file, ProblemsHolder holder,
                                         int baseOffset) {
        Matcher buttonMatcher = BUTTON_PATTERN.matcher(accordionContent);
        
        if (!buttonMatcher.find()) {
            registerProblem(holder, file, baseOffset, baseOffset + Math.min(100, accordionContent.length()),
                "Accordion should use <button> elements or role='button' for triggers",
                new AddButtonRoleFix());
        }
    }
    
    private void checkAccordionPanels(String accordionContent, PsiFile file, ProblemsHolder holder,
                                       int baseOffset) {
        // Check for panel elements with proper IDs
        Pattern panelPattern = Pattern.compile(
            "<div[^>]*(?:class\\s*=\\s*[\"'][^\"']*(?:panel|content|collapse)[^\"']*[\"'])[^>]*>",
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher panelMatcher = panelPattern.matcher(accordionContent);
        Set<String> panelIds = new HashSet<>();
        
        while (panelMatcher.find()) {
            String panelTag = accordionContent.substring(panelMatcher.start(), panelMatcher.end());
            Matcher idMatcher = ID_PATTERN.matcher(panelTag);
            
            if (!idMatcher.find()) {
                registerProblem(holder, file, baseOffset + panelMatcher.start(), 
                    baseOffset + panelMatcher.end(),
                    "Accordion panel should have an id for aria-controls reference",
                    new AddPanelIdFix());
            } else {
                panelIds.add(idMatcher.group(1));
            }
            
            // Check for region role with aria-labelledby
            if (ROLE_REGION_PATTERN.matcher(panelTag).find()) {
                if (!ARIA_LABELLEDBY_PATTERN.matcher(panelTag).find()) {
                    registerProblem(holder, file, baseOffset + panelMatcher.start(),
                        baseOffset + panelMatcher.end(),
                        "Accordion panel with role='region' should have aria-labelledby",
                        new AddAriaLabelledByFix());
                }
            }
        }
    }
    
    private void checkAriaExpanded(String buttonTag, PsiFile file, ProblemsHolder holder,
                                    int start, int end) {
        Matcher expandedMatcher = ARIA_EXPANDED_PATTERN.matcher(buttonTag);
        
        if (!expandedMatcher.find()) {
            registerProblem(holder, file, start, end,
                "Accordion button must have aria-expanded attribute",
                new AddAriaExpandedFix());
        } else {
            String value = expandedMatcher.group(1);
            if (!"true".equals(value) && !"false".equals(value)) {
                registerProblem(holder, file, start, end,
                    "aria-expanded must be 'true' or 'false', not '" + value + "'",
                    new FixAriaExpandedValueFix());
            }
        }
    }
    
    private void checkAriaControls(String buttonTag, String content, PsiFile file, 
                                    ProblemsHolder holder, int start, int end) {
        Matcher controlsMatcher = ARIA_CONTROLS_PATTERN.matcher(buttonTag);
        
        if (!controlsMatcher.find()) {
            registerProblem(holder, file, start, end,
                "Accordion button should have aria-controls pointing to panel id",
                new AddAriaControlsFix());
        } else {
            String controlsId = controlsMatcher.group(1);
            
            // Verify the referenced element exists
            Pattern targetPattern = Pattern.compile(
                "\\bid\\s*=\\s*[\"']" + Pattern.quote(controlsId) + "[\"']",
                Pattern.CASE_INSENSITIVE
            );
            
            if (!targetPattern.matcher(content).find()) {
                registerProblem(holder, file, start, end,
                    "aria-controls references non-existent element id: " + controlsId,
                    null);
            }
        }
    }
    
    private void checkButtonSemantics(String buttonTag, PsiFile file, ProblemsHolder holder,
                                       int start, int end) {
        // Check if it's a native button or has role="button"
        boolean isButton = buttonTag.toLowerCase().startsWith("<button");
        boolean hasButtonRole = buttonTag.contains("role") && buttonTag.contains("button");
        
        if (!isButton && !hasButtonRole) {
            // Check if it's a link being used as button
            if (buttonTag.toLowerCase().startsWith("<a")) {
                registerProblem(holder, file, start, end,
                    "Links used as accordion triggers should have role='button'",
                    new AddButtonRoleFix());
                
                // Check for href="#" which is problematic
                if (buttonTag.contains("href=\"#\"") || buttonTag.contains("href='#'")) {
                    registerProblem(holder, file, start, end,
                        "Accordion trigger should not use href='#'. Use a button element instead",
                        null);
                }
            }
        }
        
        // Check tabindex for non-button elements
        if (!isButton) {
            Matcher tabindexMatcher = TABINDEX_PATTERN.matcher(buttonTag);
            if (!tabindexMatcher.find()) {
                registerProblem(holder, file, start, end,
                    "Non-button accordion trigger should have tabindex='0' for keyboard access",
                    new AddTabindexFix());
            }
        }
    }
    
    private void checkPanelAssociations(String content, PsiFile file, ProblemsHolder holder) {
        // Find panels that should be associated with triggers
        Pattern panelWithIdPattern = Pattern.compile(
            "<[^>]+\\bid\\s*=\\s*[\"']([^\"']+)[\"'][^>]*" +
            "class\\s*=\\s*[\"'][^\"']*(?:panel|collapse|content)[^\"']*[\"'][^>]*>",
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher panelMatcher = panelWithIdPattern.matcher(content);
        Set<String> referencedPanels = new HashSet<>();
        
        // Collect all aria-controls references
        Matcher controlsMatcher = ARIA_CONTROLS_PATTERN.matcher(content);
        while (controlsMatcher.find()) {
            referencedPanels.add(controlsMatcher.group(1));
        }
        
        // Check if panels are referenced
        while (panelMatcher.find()) {
            String panelId = panelMatcher.group(1);
            if (!referencedPanels.contains(panelId)) {
                registerProblem(holder, file, panelMatcher.start(), panelMatcher.end(),
                    "Panel with id='" + panelId + "' is not referenced by any aria-controls",
                    null);
            }
        }
    }
    
    private void checkCommonAccordionPatterns(String content, PsiFile file, ProblemsHolder holder) {
        // Check for details/summary pattern (native accordion)
        Pattern detailsPattern = Pattern.compile(
            "<details[^>]*>\\s*<summary[^>]*>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );
        
        Matcher detailsMatcher = detailsPattern.matcher(content);
        while (detailsMatcher.find()) {
            String detailsTag = content.substring(detailsMatcher.start(), 
                Math.min(detailsMatcher.end() + 100, content.length()));
            
            // Details/summary is naturally accessible but check for enhancements
            if (detailsTag.contains("aria-expanded")) {
                registerProblem(holder, file, detailsMatcher.start(), detailsMatcher.end(),
                    "<details> element doesn't need aria-expanded (it's built-in)",
                    new RemoveRedundantAriaFix());
            }
        }
    }
    
    protected int findElementEnd(String content, int start) {
        int depth = 0;
        int i = start;
        boolean inTag = false;
        boolean inQuote = false;
        char quoteChar = ' ';
        
        while (i < content.length()) {
            char c = content.charAt(i);
            
            if (!inQuote && (c == '"' || c == '\'')) {
                inQuote = true;
                quoteChar = c;
            } else if (inQuote && c == quoteChar) {
                inQuote = false;
            } else if (!inQuote) {
                if (c == '<') {
                    inTag = true;
                    if (i + 1 < content.length() && content.charAt(i + 1) == '/') {
                        depth--;
                        if (depth < 0) {
                            return i;
                        }
                    } else {
                        depth++;
                    }
                } else if (c == '>' && inTag) {
                    inTag = false;
                    if (i > 0 && content.charAt(i - 1) == '/') {
                        depth--;
                    }
                    if (depth == 0) {
                        return i + 1;
                    }
                }
            }
            i++;
        }
        
        return content.length();
    }
    
    
    private static class AddAriaExpandedFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add aria-expanded attribute";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add aria-expanded='false'
        }
    }
    
    private static class FixAriaExpandedValueFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Fix aria-expanded value";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would fix the value to 'true' or 'false'
        }
    }
    
    private static class AddAriaControlsFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add aria-controls attribute";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add aria-controls
        }
    }
    
    private static class AddButtonRoleFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add role='button' to accordion trigger";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add role='button'
        }
    }
    
    private static class AddPanelIdFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add id to accordion panel";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add unique id
        }
    }
    
    private static class AddAriaLabelledByFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add aria-labelledby to region";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add aria-labelledby
        }
    }
    
    private static class AddTabindexFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add tabindex='0' for keyboard access";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add tabindex='0'
        }
    }
    
    private static class RemoveRedundantAriaFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Remove redundant ARIA attribute";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would remove redundant ARIA
        }
    }
    
    // Enhanced context-aware validation methods
    private void validateStateManagement(String content, PsiFile file, ProblemsHolder holder) {
        // Find all accordion triggers with aria-expanded
        Pattern triggerWithExpandedPattern = Pattern.compile(
            "<([^>]*(?:data-toggle=\"collapse\"|class=\"[^\"]*accordion[^\"]*\")[^>]*aria-expanded=\"([^\"]+)\")[^>]*>(.*?)</[^>]+>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );
        
        Matcher triggerMatcher = triggerWithExpandedPattern.matcher(content);
        
        while (triggerMatcher.find()) {
            String triggerTag = triggerMatcher.group(1);
            String expandedValue = triggerMatcher.group(2);
            String triggerContent = triggerMatcher.group(3);
            int offset = triggerMatcher.start();
            
            // Check for state consistency indicators in trigger content
            boolean hasVisualIndicator = triggerContent.contains("icon") || 
                                       triggerContent.contains("arrow") ||
                                       triggerContent.contains("chevron") ||
                                       triggerContent.contains("+") ||
                                       triggerContent.contains("-");
            
            if (!hasVisualIndicator) {
                registerProblem(holder, file, offset, offset + 100,
                    "Accordion trigger should have visual state indicator (icon, arrow, +/- etc.) that changes with aria-expanded",
                    new AddVisualIndicatorFix());
            }
            
            // Check for conflicting CSS classes that might indicate wrong state
            if ("true".equals(expandedValue) && (triggerTag.contains("collapsed") || triggerTag.contains("closed"))) {
                registerProblem(holder, file, offset, offset + 100,
                    "CSS class suggests collapsed state but aria-expanded='true' - ensure visual state matches ARIA state",
                    null);
            }
            
            if ("false".equals(expandedValue) && (triggerTag.contains("expanded") || triggerTag.contains("open"))) {
                registerProblem(holder, file, offset, offset + 100,
                    "CSS class suggests expanded state but aria-expanded='false' - ensure visual state matches ARIA state",
                    null);
            }
        }
    }
    
    private void checkKeyboardNavigation(String content, PsiFile file, ProblemsHolder holder) {
        // Check for keyboard event handling patterns
        Pattern accordionContainerPattern = Pattern.compile(
            "<[^>]*(?:class=\"[^\"]*accordion[^\"]*\"|data-toggle=\"accordion\")[^>]*>",
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher containerMatcher = accordionContainerPattern.matcher(content);
        
        while (containerMatcher.find()) {
            int containerStart = containerMatcher.start();
            int containerEnd = findElementEnd(content, containerStart);
            String containerContent = content.substring(containerStart, Math.min(containerEnd, content.length()));
            
            // Check for multiple triggers within accordion
            Pattern triggerPattern = Pattern.compile(
                "<[^>]*(?:data-toggle=\"collapse\"|role=\"button\")[^>]*>",
                Pattern.CASE_INSENSITIVE
            );
            
            Matcher triggerMatcher = triggerPattern.matcher(containerContent);
            int triggerCount = 0;
            
            while (triggerMatcher.find()) {
                triggerCount++;
            }
            
            if (triggerCount > 1) {
                // Multiple triggers - should support arrow key navigation
                boolean hasKeyboardSupport = containerContent.contains("keydown") ||
                                            containerContent.contains("onkeydown") ||
                                            containerContent.contains("addEventListener") ||
                                            containerContent.contains("data-keyboard");
                
                if (!hasKeyboardSupport) {
                    registerProblem(holder, file, containerStart, containerStart + 100,
                        "Multi-panel accordion should support arrow key navigation between triggers for better keyboard accessibility",
                        new AddKeyboardSupportFix());
                }
                
                // Check for Home/End key support hint
                registerProblem(holder, file, containerStart, containerStart + 100,
                    "Consider adding Home/End key support to jump to first/last accordion panel",
                    null);
            }
        }
        
        // Check for focus management
        Pattern triggerPattern = Pattern.compile(
            "<[^>]*(?:data-toggle=\"collapse\"|class=\"[^\"]*accordion[^\"]*\")[^>]*>",
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher triggerMatcher = triggerPattern.matcher(content);
        
        while (triggerMatcher.find()) {
            String triggerTag = triggerMatcher.group();
            int offset = triggerMatcher.start();
            
            // Check for focus management indicators
            boolean hasFocusManagement = triggerTag.contains("data-focus") ||
                                       triggerTag.contains("focus()") ||
                                       triggerTag.contains("tabindex");
            
            if (!hasFocusManagement) {
                registerProblem(holder, file, offset, offset + 100,
                    "Accordion trigger should maintain focus after activation for smooth keyboard navigation",
                    new AddFocusManagementFix());
            }
        }
    }
    
    private void validateNestedAccordions(String content, PsiFile file, ProblemsHolder holder) {
        Pattern accordionPattern = Pattern.compile(
            "<[^>]*(?:class=\"[^\"]*accordion[^\"]*\"|data-toggle=\"accordion\")[^>]*>(.*?)</[^>]+>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );
        
        Matcher accordionMatcher = accordionPattern.matcher(content);
        
        while (accordionMatcher.find()) {
            String accordionContent = accordionMatcher.group(1);
            int accordionOffset = accordionMatcher.start();
            
            // Check for nested accordions
            Matcher nestedMatcher = accordionPattern.matcher(accordionContent);
            
            if (nestedMatcher.find()) {
                registerProblem(holder, file, accordionOffset, accordionOffset + 100,
                    "Nested accordions detected - ensure proper ARIA hierarchy and avoid conflicting keyboard navigation",
                    null);
                
                // Check if nested accordion has different aria-level or grouping
                String nestedContent = nestedMatcher.group(1);
                Pattern headingPattern = Pattern.compile("<h([1-6])[^>]*>", Pattern.CASE_INSENSITIVE);
                
                Matcher parentHeadingMatcher = headingPattern.matcher(accordionContent.substring(0, nestedMatcher.start()));
                Matcher nestedHeadingMatcher = headingPattern.matcher(nestedContent);
                
                if (parentHeadingMatcher.find() && nestedHeadingMatcher.find()) {
                    int parentLevel = Integer.parseInt(parentHeadingMatcher.group(1));
                    int nestedLevel = Integer.parseInt(nestedHeadingMatcher.group(1));
                    
                    if (nestedLevel <= parentLevel) {
                        registerProblem(holder, file, accordionOffset + nestedMatcher.start(), 
                            accordionOffset + nestedMatcher.start() + 100,
                            String.format("Nested accordion heading level h%d should be greater than parent level h%d for proper hierarchy",
                                nestedLevel, parentLevel),
                            new FixHeadingLevelFix(nestedLevel + 1));
                    }
                }
            }
        }
    }
    
    private void checkAccordionGrouping(String content, PsiFile file, ProblemsHolder holder) {
        // Find multiple accordions that might need grouping
        Pattern accordionPattern = Pattern.compile(
            "<[^>]*(?:class=\"[^\"]*accordion[^\"]*\"|data-toggle=\"accordion\")[^>]*>",
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher accordionMatcher = accordionPattern.matcher(content);
        List<Integer> accordionOffsets = new ArrayList<>();
        
        while (accordionMatcher.find()) {
            accordionOffsets.add(accordionMatcher.start());
        }
        
        if (accordionOffsets.size() > 2) {
            // Multiple accordions - check if they should be grouped
            for (int i = 0; i < accordionOffsets.size() - 1; i++) {
                int current = accordionOffsets.get(i);
                int next = accordionOffsets.get(i + 1);
                
                // If accordions are close together (within 200 chars), suggest grouping
                if (next - current < 200) {
                    registerProblem(holder, file, current, current + 100,
                        "Multiple accordions detected - consider wrapping related accordions in a container with appropriate ARIA landmarks",
                        new GroupAccordionsFix());
                    break; // Only report once
                }
            }
        }
        
        // Check for accordion groups without proper labeling
        Pattern accordionGroupPattern = Pattern.compile(
            "<div[^>]*class=\"[^\"]*accordion-group[^\"]*\"[^>]*>",
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher groupMatcher = accordionGroupPattern.matcher(content);
        
        while (groupMatcher.find()) {
            String groupTag = groupMatcher.group();
            
            if (!groupTag.contains("aria-label") && !groupTag.contains("aria-labelledby")) {
                registerProblem(holder, file, groupMatcher.start(), groupMatcher.end(),
                    "Accordion group should have aria-label or aria-labelledby to describe the group's purpose",
                    new AddGroupLabelFix());
            }
        }
    }
    
    // Additional quick fixes for enhanced functionality
    private static class AddVisualIndicatorFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add visual state indicator to accordion trigger";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add visual indicator
        }
    }
    
    private static class AddKeyboardSupportFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add keyboard navigation support";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add keyboard support
        }
    }
    
    private static class AddFocusManagementFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add focus management to accordion trigger";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add focus management
        }
    }
    
    private static class FixHeadingLevelFix implements LocalQuickFix {
        private final int newLevel;
        
        FixHeadingLevelFix(int newLevel) {
            this.newLevel = newLevel;
        }
        
        @NotNull
        @Override
        public String getFamilyName() {
            return "Fix heading level to h" + newLevel;
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would fix heading level
        }
    }
    
    private static class GroupAccordionsFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Wrap related accordions in labeled container";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would group accordions
        }
    }
    
    private static class AddGroupLabelFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add aria-label to accordion group";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add group label
        }
    }
}