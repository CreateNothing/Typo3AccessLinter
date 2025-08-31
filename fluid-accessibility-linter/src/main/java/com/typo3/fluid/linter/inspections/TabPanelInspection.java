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

public class TabPanelInspection extends FluidAccessibilityInspection {
    
    protected static final Pattern TABLIST_PATTERN = Pattern.compile(
        "<[^>]+\\brole\\s*=\\s*[\"']tablist[\"'][^>]*>",
        Pattern.CASE_INSENSITIVE
    );
    
    protected static final Pattern TAB_PATTERN = Pattern.compile(
        "<[^>]+\\brole\\s*=\\s*[\"']tab[\"'][^>]*>",
        Pattern.CASE_INSENSITIVE
    );
    
    protected static final Pattern TABPANEL_PATTERN = Pattern.compile(
        "<[^>]+\\brole\\s*=\\s*[\"']tabpanel[\"'][^>]*>",
        Pattern.CASE_INSENSITIVE
    );
    
    protected static final Pattern ARIA_SELECTED_PATTERN = Pattern.compile(
        "\\baria-selected\\s*=\\s*[\"']([^\"']+)[\"']",
        Pattern.CASE_INSENSITIVE
    );
    
    protected static final Pattern ARIA_CONTROLS_PATTERN = Pattern.compile(
        "\\baria-controls\\s*=\\s*[\"']([^\"']+)[\"']",
        Pattern.CASE_INSENSITIVE
    );
    
    protected static final Pattern ARIA_LABELLEDBY_PATTERN = Pattern.compile(
        "\\baria-labelledby\\s*=\\s*[\"']([^\"']+)[\"']",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern ARIA_LABEL_PATTERN = Pattern.compile(
        "\\baria-label\\s*=\\s*[\"']([^\"']+)[\"']",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern ARIA_ORIENTATION_PATTERN = Pattern.compile(
        "\\baria-orientation\\s*=\\s*[\"']([^\"']+)[\"']",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern ID_PATTERN = Pattern.compile(
        "\\bid\\s*=\\s*[\"']([^\"']+)[\"']",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern TABINDEX_PATTERN = Pattern.compile(
        "\\btabindex\\s*=\\s*[\"']([^\"']+)[\"']",
        Pattern.CASE_INSENSITIVE
    );
    
    @NotNull
    @Override
    public String getDisplayName() {
        return "Tab panel interface accessibility issues";
    }
    
    @NotNull
    @Override
    public String getShortName() {
        return "TabPanel";
    }
    
    @Override
    protected void inspectFile(@NotNull PsiFile file, @NotNull ProblemsHolder holder) {
        String content = file.getText();
        
        // Check tablist containers
        checkTablistStructure(content, file, holder);
        
        // Check individual tabs
        checkTabElements(content, file, holder);
        
        // Check tabpanels
        checkTabPanels(content, file, holder);
        
        // Check tab/panel associations
        checkTabPanelAssociations(content, file, holder);
        
        // Check for common tab patterns without proper ARIA
        checkCommonTabPatterns(content, file, holder);
        
        // Enhanced context-aware checks
        validateKeyboardNavigationPatterns(content, file, holder);
        checkFocusManagement(content, file, holder);
        validateTabSelectionConsistency(content, file, holder);
        checkTabPanelVisibilityStates(content, file, holder);
    }
    
    private void checkTablistStructure(String content, PsiFile file, ProblemsHolder holder) {
        Matcher tablistMatcher = TABLIST_PATTERN.matcher(content);
        
        while (tablistMatcher.find()) {
            String tablistTag = content.substring(tablistMatcher.start(), tablistMatcher.end());
            int tablistStart = tablistMatcher.start();
            int tablistEnd = findElementEnd(content, tablistStart);
            
            if (tablistEnd > tablistStart) {
                String tablistContent = content.substring(tablistStart, 
                    Math.min(tablistEnd, content.length()));
                
                // Check for aria-label or aria-labelledby
                checkTablistLabeling(tablistTag, file, holder, tablistStart, tablistMatcher.end());
                
                // Check for tabs within tablist
                checkTabsInTablist(tablistContent, file, holder, tablistStart);
                
                // Check aria-orientation
                checkTablistOrientation(tablistTag, file, holder, tablistStart, tablistMatcher.end());
            }
        }
    }
    
    private void checkTabElements(String content, PsiFile file, ProblemsHolder holder) {
        Matcher tabMatcher = TAB_PATTERN.matcher(content);
        Set<String> tabIds = new HashSet<>();
        Set<String> controlledPanels = new HashSet<>();
        int selectedCount = 0;
        
        while (tabMatcher.find()) {
            String tabTag = content.substring(tabMatcher.start(), tabMatcher.end());
            int tabStart = tabMatcher.start();
            int tabEnd = tabMatcher.end();
            
            // Check for aria-selected
            Matcher selectedMatcher = ARIA_SELECTED_PATTERN.matcher(tabTag);
            if (!selectedMatcher.find()) {
                registerProblem(holder, file, tabStart, tabEnd,
                    "Tab must have aria-selected attribute",
                    new AddAriaSelectedFix());
            } else {
                String value = selectedMatcher.group(1);
                if ("true".equals(value)) {
                    selectedCount++;
                }
                if (!"true".equals(value) && !"false".equals(value)) {
                    registerProblem(holder, file, tabStart, tabEnd,
                        "aria-selected must be 'true' or 'false', not '" + value + "'",
                        new FixAriaSelectedValueFix());
                }
            }
            
            // Check for aria-controls
            Matcher controlsMatcher = ARIA_CONTROLS_PATTERN.matcher(tabTag);
            if (!controlsMatcher.find()) {
                registerProblem(holder, file, tabStart, tabEnd,
                    "Tab should have aria-controls pointing to its tabpanel",
                    new AddAriaControlsFix());
            } else {
                controlledPanels.add(controlsMatcher.group(1));
            }
            
            // Check for id
            Matcher idMatcher = ID_PATTERN.matcher(tabTag);
            if (idMatcher.find()) {
                tabIds.add(idMatcher.group(1));
            }
            
            // Check tabindex
            checkTabTabindex(tabTag, file, holder, tabStart, tabEnd);
        }
        
        // Check that only one tab is selected
        if (selectedCount > 1) {
            registerProblem(holder, file, 0, 100,
                "Only one tab should have aria-selected='true' at a time",
                null);
        }
    }
    
    private void checkTabPanels(String content, PsiFile file, ProblemsHolder holder) {
        Matcher panelMatcher = TABPANEL_PATTERN.matcher(content);
        Set<String> panelIds = new HashSet<>();
        
        while (panelMatcher.find()) {
            String panelTag = content.substring(panelMatcher.start(), panelMatcher.end());
            int panelStart = panelMatcher.start();
            int panelEnd = panelMatcher.end();
            
            // Check for id
            Matcher idMatcher = ID_PATTERN.matcher(panelTag);
            if (!idMatcher.find()) {
                registerProblem(holder, file, panelStart, panelEnd,
                    "Tabpanel must have an id for aria-controls reference",
                    new AddPanelIdFix());
            } else {
                panelIds.add(idMatcher.group(1));
            }
            
            // Check for aria-labelledby
            Matcher labelledbyMatcher = ARIA_LABELLEDBY_PATTERN.matcher(panelTag);
            if (!labelledbyMatcher.find()) {
                registerProblem(holder, file, panelStart, panelEnd,
                    "Tabpanel should have aria-labelledby pointing to its tab",
                    new AddAriaLabelledByFix());
            }
            
            // Check tabindex
            checkTabPanelTabindex(panelTag, file, holder, panelStart, panelEnd);
        }
    }
    
    private void checkTablistLabeling(String tablistTag, PsiFile file, ProblemsHolder holder,
                                       int start, int end) {
        boolean hasLabel = ARIA_LABEL_PATTERN.matcher(tablistTag).find();
        boolean hasLabelledBy = ARIA_LABELLEDBY_PATTERN.matcher(tablistTag).find();
        
        if (!hasLabel && !hasLabelledBy) {
            registerProblem(holder, file, start, end,
                "Tablist should have aria-label or aria-labelledby for context",
                new AddTablistLabelFix());
        }
    }
    
    private void checkTabsInTablist(String tablistContent, PsiFile file, ProblemsHolder holder,
                                     int baseOffset) {
        Matcher tabMatcher = TAB_PATTERN.matcher(tablistContent);
        
        if (!tabMatcher.find()) {
            registerProblem(holder, file, baseOffset, baseOffset + Math.min(100, tablistContent.length()),
                "Tablist must contain at least one element with role='tab'",
                new AddTabElementFix());
        }
    }
    
    private void checkTablistOrientation(String tablistTag, PsiFile file, ProblemsHolder holder,
                                          int start, int end) {
        Matcher orientationMatcher = ARIA_ORIENTATION_PATTERN.matcher(tablistTag);
        
        if (orientationMatcher.find()) {
            String value = orientationMatcher.group(1);
            if (!"horizontal".equals(value) && !"vertical".equals(value)) {
                registerProblem(holder, file, start, end,
                    "aria-orientation must be 'horizontal' or 'vertical', not '" + value + "'",
                    new FixOrientationValueFix());
            }
        }
        // Note: aria-orientation is optional, defaults to horizontal
    }
    
    private void checkTabTabindex(String tabTag, PsiFile file, ProblemsHolder holder,
                                   int start, int end) {
        Matcher tabindexMatcher = TABINDEX_PATTERN.matcher(tabTag);
        
        if (tabindexMatcher.find()) {
            String value = tabindexMatcher.group(1);
            try {
                int tabindex = Integer.parseInt(value);
                if (tabindex > 0) {
                    registerProblem(holder, file, start, end,
                        "Tabs should use tabindex='0' or '-1', not positive values",
                        new FixTabindexFix());
                }
            } catch (NumberFormatException e) {
                registerProblem(holder, file, start, end,
                    "Invalid tabindex value: " + value,
                    new FixTabindexFix());
            }
        }
    }
    
    private void checkTabPanelTabindex(String panelTag, PsiFile file, ProblemsHolder holder,
                                        int start, int end) {
        Matcher tabindexMatcher = TABINDEX_PATTERN.matcher(panelTag);
        
        if (!tabindexMatcher.find()) {
            registerProblem(holder, file, start, end,
                "Tabpanel should have tabindex='0' for keyboard accessibility",
                new AddTabPanelTabindexFix());
        } else {
            String value = tabindexMatcher.group(1);
            if (!"0".equals(value)) {
                registerProblem(holder, file, start, end,
                    "Tabpanel should have tabindex='0', not '" + value + "'",
                    new FixTabPanelTabindexFix());
            }
        }
    }
    
    private void checkTabPanelAssociations(String content, PsiFile file, ProblemsHolder holder) {
        // Collect all tabs with aria-controls
        Set<String> controlledPanelIds = new HashSet<>();
        Set<String> tabIds = new HashSet<>();
        
        Matcher tabMatcher = TAB_PATTERN.matcher(content);
        while (tabMatcher.find()) {
            String tabTag = content.substring(tabMatcher.start(), tabMatcher.end());
            
            Matcher controlsMatcher = ARIA_CONTROLS_PATTERN.matcher(tabTag);
            if (controlsMatcher.find()) {
                controlledPanelIds.add(controlsMatcher.group(1));
            }
            
            Matcher idMatcher = ID_PATTERN.matcher(tabTag);
            if (idMatcher.find()) {
                tabIds.add(idMatcher.group(1));
            }
        }
        
        // Check all tabpanels
        Matcher panelMatcher = TABPANEL_PATTERN.matcher(content);
        while (panelMatcher.find()) {
            String panelTag = content.substring(panelMatcher.start(), panelMatcher.end());
            
            Matcher idMatcher = ID_PATTERN.matcher(panelTag);
            if (idMatcher.find()) {
                String panelId = idMatcher.group(1);
                
                if (!controlledPanelIds.contains(panelId)) {
                    registerProblem(holder, file, panelMatcher.start(), panelMatcher.end(),
                        "Tabpanel with id='" + panelId + "' is not referenced by any tab's aria-controls",
                        null);
                }
            }
            
            Matcher labelledbyMatcher = ARIA_LABELLEDBY_PATTERN.matcher(panelTag);
            if (labelledbyMatcher.find()) {
                String tabId = labelledbyMatcher.group(1);
                
                if (!tabIds.contains(tabId)) {
                    registerProblem(holder, file, panelMatcher.start(), panelMatcher.end(),
                        "Tabpanel's aria-labelledby references non-existent tab id: " + tabId,
                        null);
                }
            }
        }
    }
    
    private void checkCommonTabPatterns(String content, PsiFile file, ProblemsHolder holder) {
        // Check for common tab patterns without proper ARIA roles
        Pattern commonTabPattern = Pattern.compile(
            "<[^>]*(?:class\\s*=\\s*[\"'][^\"']*(?:tab|nav-tabs|nav-pills)[^\"']*[\"']" +
            "|data-toggle\\s*=\\s*[\"']tab[\"'])[^>]*>",
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher tabPatternMatcher = commonTabPattern.matcher(content);
        
        while (tabPatternMatcher.find()) {
            String element = content.substring(tabPatternMatcher.start(), tabPatternMatcher.end());
            
            // Check if it has proper ARIA roles
            if (!element.contains("role")) {
                registerProblem(holder, file, tabPatternMatcher.start(), tabPatternMatcher.end(),
                    "Tab interface detected but missing ARIA roles. Add role='tablist', role='tab', and role='tabpanel'",
                    new AddTabRolesFix());
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
    
    
    private static class AddAriaSelectedFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getName() { return getFamilyName(); }
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add aria-selected attribute";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add aria-selected='false'
        }
    }
    
    private static class FixAriaSelectedValueFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getName() { return getFamilyName(); }
        @NotNull
        @Override
        public String getFamilyName() {
            return "Fix aria-selected value";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would fix the value
        }
    }
    
    private static class AddAriaControlsFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getName() { return getFamilyName(); }
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add aria-controls to tab";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add aria-controls
        }
    }
    
    private static class AddPanelIdFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getName() { return getFamilyName(); }
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add id to tabpanel";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add unique id
        }
    }
    
    private static class AddAriaLabelledByFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getName() { return getFamilyName(); }
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add aria-labelledby to tabpanel";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add aria-labelledby
        }
    }
    
    private static class AddTablistLabelFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getName() { return getFamilyName(); }
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add aria-label to tablist";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add aria-label
        }
    }
    
    private static class AddTabElementFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getName() { return getFamilyName(); }
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add tab element with role='tab'";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add tab element
        }
    }
    
    private static class FixOrientationValueFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getName() { return getFamilyName(); }
        @NotNull
        @Override
        public String getFamilyName() {
            return "Fix aria-orientation value";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would fix orientation value
        }
    }
    
    private static class FixTabindexFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getName() { return getFamilyName(); }
        @NotNull
        @Override
        public String getFamilyName() {
            return "Fix tabindex value";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would fix tabindex
        }
    }
    
    private static class AddTabPanelTabindexFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getName() { return getFamilyName(); }
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add tabindex='0' to tabpanel";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add tabindex='0'
        }
    }
    
    private static class FixTabPanelTabindexFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getName() { return getFamilyName(); }
        @NotNull
        @Override
        public String getFamilyName() {
            return "Change tabpanel tabindex to '0'";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would fix tabindex value
        }
    }
    
    private static class AddTabRolesFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getName() { return getFamilyName(); }
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add proper ARIA tab roles";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add ARIA roles
        }
    }
    
    // Enhanced context-aware validation methods
    private void validateKeyboardNavigationPatterns(String content, PsiFile file, ProblemsHolder holder) {
        Matcher tablistMatcher = TABLIST_PATTERN.matcher(content);
        
        while (tablistMatcher.find()) {
            int tablistStart = tablistMatcher.start();
            int tablistEnd = findElementEnd(content, tablistStart);
            String tablistContent = content.substring(tablistStart, Math.min(tablistEnd, content.length()));
            
            // Count tabs in this tablist
            Matcher tabMatcher = TAB_PATTERN.matcher(tablistContent);
            int tabCount = 0;
            
            while (tabMatcher.find()) {
                tabCount++;
            }
            
            if (tabCount > 1) {
                // Check for arrow key support
                boolean hasKeyboardSupport = tablistContent.contains("keydown") ||
                                            tablistContent.contains("onkeydown") ||
                                            tablistContent.contains("data-keyboard") ||
                                            tablistContent.contains("addEventListener");
                
                if (!hasKeyboardSupport) {
                    registerProblem(holder, file, tablistStart, tablistStart + 100,
                        "Tablist should support arrow key navigation (Left/Right or Up/Down based on orientation)",
                        new AddArrowKeyNavigationFix());
                }
                
                // Check orientation for proper key mapping
                Matcher orientationMatcher = ARIA_ORIENTATION_PATTERN.matcher(tablistContent);
                if (orientationMatcher.find()) {
                    String orientation = orientationMatcher.group(1);
                    
                    registerProblem(holder, file, tablistStart, tablistStart + 100,
                        String.format("With aria-orientation='%s', use %s arrow keys for navigation and consider Home/End keys for first/last tab",
                            orientation, 
                            "vertical".equals(orientation) ? "Up/Down" : "Left/Right"),
                        null);
                } else {
                    // Default horizontal orientation
                    registerProblem(holder, file, tablistStart, tablistStart + 100,
                        "Consider adding Home/End key support to jump to first/last tab in horizontal tablist",
                        new AddHomeEndKeysFix());
                }
                
                // Check for Ctrl+PageUp/PageDown support in complex applications
                if (tabCount > 5) {
                    registerProblem(holder, file, tablistStart, tablistStart + 100,
                        "For complex tab interfaces with many tabs, consider supporting Ctrl+PageUp/PageDown for tab switching",
                        null);
                }
            }
        }
    }
    
    private void checkFocusManagement(String content, PsiFile file, ProblemsHolder holder) {
        // Check tab to tabpanel focus relationships
        Matcher tabMatcher = TAB_PATTERN.matcher(content);
        
        while (tabMatcher.find()) {
            String tabTag = content.substring(tabMatcher.start(), tabMatcher.end());
            int tabStart = tabMatcher.start();
            
            // Check for proper tabindex management
            Matcher tabindexMatcher = TABINDEX_PATTERN.matcher(tabTag);
            if (tabindexMatcher.find()) {
                String tabindex = tabindexMatcher.group(1);
                
                // Check for roving tabindex pattern
                if ("-1".equals(tabindex)) {
                    // This is good - inactive tab should have tabindex="-1"
                    Matcher selectedMatcher = ARIA_SELECTED_PATTERN.matcher(tabTag);
                    if (selectedMatcher.find() && "true".equals(selectedMatcher.group(1))) {
                        registerProblem(holder, file, tabStart, tabMatcher.end(),
                            "Selected tab (aria-selected='true') should have tabindex='0', not '-1'",
                            new FixSelectedTabIndexFix());
                    }
                } else if ("0".equals(tabindex)) {
                    // Check if this tab is actually selected
                    Matcher selectedMatcher = ARIA_SELECTED_PATTERN.matcher(tabTag);
                    if (!selectedMatcher.find() || !"true".equals(selectedMatcher.group(1))) {
                        registerProblem(holder, file, tabStart, tabMatcher.end(),
                            "Only the selected tab should have tabindex='0'. Inactive tabs should have tabindex='-1'",
                            new FixInactiveTabIndexFix());
                    }
                }
            }
            
            // Check for focus management after tab activation
            Matcher controlsMatcher = ARIA_CONTROLS_PATTERN.matcher(tabTag);
            if (controlsMatcher.find()) {
                String panelId = controlsMatcher.group(1);
                
                // Look for the corresponding tabpanel
                Pattern panelPattern = Pattern.compile(
                    "<[^>]+id=\"" + Pattern.quote(panelId) + "\"[^>]*role=\"tabpanel\"[^>]*>",
                    Pattern.CASE_INSENSITIVE
                );
                
                Matcher panelMatcher = panelPattern.matcher(content);
                if (panelMatcher.find()) {
                    String panelTag = panelMatcher.group();
                    
                    // Check if panel is focusable
                    if (!TABINDEX_PATTERN.matcher(panelTag).find()) {
                        registerProblem(holder, file, panelMatcher.start(), panelMatcher.end(),
                            "Tabpanel should have tabindex='0' to be focusable after tab activation",
                            new AddTabPanelTabindexFix());
                    }
                    
                    // Check for focus management hints
                    boolean hasFocusManagement = panelTag.contains("data-focus") ||
                                               panelTag.contains("autofocus") ||
                                               content.substring(panelMatcher.end(), 
                                                   Math.min(panelMatcher.end() + 200, content.length()))
                                                   .contains(".focus()");
                    
                    if (!hasFocusManagement) {
                        registerProblem(holder, file, panelMatcher.start(), panelMatcher.end(),
                            "Consider managing focus to tabpanel or its first focusable element when tab is activated",
                            new AddFocusManagementHintFix());
                    }
                }
            }
        }
    }
    
    private void validateTabSelectionConsistency(String content, PsiFile file, ProblemsHolder holder) {
        // Find all tablists and check selection consistency within each
        Matcher tablistMatcher = TABLIST_PATTERN.matcher(content);
        
        while (tablistMatcher.find()) {
            int tablistStart = tablistMatcher.start();
            int tablistEnd = findElementEnd(content, tablistStart);
            String tablistContent = content.substring(tablistStart, Math.min(tablistEnd, content.length()));
            
            // Find all tabs within this tablist
            Matcher tabMatcher = TAB_PATTERN.matcher(tablistContent);
            List<TabInfo> tabs = new ArrayList<>();
            
            while (tabMatcher.find()) {
                String tabTag = tabMatcher.group();
                
                Matcher selectedMatcher = ARIA_SELECTED_PATTERN.matcher(tabTag);
                Matcher tabindexMatcher = TABINDEX_PATTERN.matcher(tabTag);
                Matcher controlsMatcher = ARIA_CONTROLS_PATTERN.matcher(tabTag);
                
                boolean isSelected = selectedMatcher.find() && "true".equals(selectedMatcher.group(1));
                String tabindex = tabindexMatcher.find() ? tabindexMatcher.group(1) : null;
                String controlsId = controlsMatcher.find() ? controlsMatcher.group(1) : null;
                
                tabs.add(new TabInfo(tablistStart + tabMatcher.start(), isSelected, tabindex, controlsId));
            }
            
            // Validate consistency
            int selectedCount = 0;
            int tabindexZeroCount = 0;
            
            for (TabInfo tab : tabs) {
                if (tab.isSelected) selectedCount++;
                if ("0".equals(tab.tabindex)) tabindexZeroCount++;
            }
            
            if (selectedCount != 1) {
                registerProblem(holder, file, tablistStart, tablistStart + 100,
                    String.format("Tablist should have exactly one selected tab, found %d", selectedCount),
                    new FixTabSelectionFix());
            }
            
            if (tabindexZeroCount != 1) {
                registerProblem(holder, file, tablistStart, tablistStart + 100,
                    String.format("Tablist should have exactly one tab with tabindex='0' (the selected one), found %d", tabindexZeroCount),
                    new FixTabindexConsistencyFix());
            }
            
            // Check corresponding tabpanels are properly hidden/shown
            for (TabInfo tab : tabs) {
                if (tab.controlsId != null) {
                    checkTabPanelVisibility(content, tab, file, holder);
                }
            }
        }
    }
    
    private void checkTabPanelVisibilityStates(String content, PsiFile file, ProblemsHolder holder) {
        // Find all tabpanels and check their visibility states
        Matcher panelMatcher = TABPANEL_PATTERN.matcher(content);
        
        while (panelMatcher.find()) {
            String panelTag = content.substring(panelMatcher.start(), panelMatcher.end());
            int panelStart = panelMatcher.start();
            
            Matcher idMatcher = ID_PATTERN.matcher(panelTag);
            if (idMatcher.find()) {
                String panelId = idMatcher.group(1);
                
                // Find the corresponding tab
                Pattern controllingTabPattern = Pattern.compile(
                    "<[^>]+role=\"tab\"[^>]*aria-controls=\"" + Pattern.quote(panelId) + "\"[^>]*>",
                    Pattern.CASE_INSENSITIVE
                );
                
                Matcher controllingTabMatcher = controllingTabPattern.matcher(content);
                if (controllingTabMatcher.find()) {
                    String tabTag = controllingTabMatcher.group();
                    
                    Matcher selectedMatcher = ARIA_SELECTED_PATTERN.matcher(tabTag);
                    boolean tabIsSelected = selectedMatcher.find() && "true".equals(selectedMatcher.group(1));
                    
                    // Check panel visibility consistency
                    boolean panelHidden = panelTag.contains("hidden") ||
                                        panelTag.contains("display:none") ||
                                        panelTag.contains("visibility:hidden") ||
                                        panelTag.contains("aria-hidden=\"true\"");
                    
                    if (tabIsSelected && panelHidden) {
                        registerProblem(holder, file, panelStart, panelMatcher.end(),
                            "Tabpanel should be visible when its corresponding tab is selected (aria-selected='true')",
                            new ShowTabPanelFix());
                    }
                    
                    if (!tabIsSelected && !panelHidden) {
                        // Check if it's actually visible (could be handled by CSS)
                        registerProblem(holder, file, panelStart, panelMatcher.end(),
                            "Tabpanel should be hidden when its corresponding tab is not selected. Consider adding hidden attribute or aria-hidden='true'",
                            new HideTabPanelFix());
                    }
                }
            }
            
            // Check for content that shouldn't be in inactive panels
            if (panelTag.contains("hidden") || panelTag.contains("aria-hidden=\"true\"")) {
                int panelEnd = findElementEnd(content, panelStart);
                String panelContent = content.substring(panelStart, Math.min(panelEnd, content.length()));
                
                // Check for interactive elements in hidden panels
                if (panelContent.contains("<input") || panelContent.contains("<button") || 
                    panelContent.contains("<select") || panelContent.contains("<textarea")) {
                    
                    registerProblem(holder, file, panelStart, panelMatcher.end(),
                        "Hidden tabpanel contains interactive elements - ensure they are not keyboard accessible when panel is hidden",
                        new ManageInteractiveElementsFix());
                }
            }
        }
    }
    
    private void checkTabPanelVisibility(String content, TabInfo tab, PsiFile file, ProblemsHolder holder) {
        if (tab.controlsId == null) return;
        
        Pattern panelPattern = Pattern.compile(
            "<[^>]+id=\"" + Pattern.quote(tab.controlsId) + "\"[^>]*>",
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher panelMatcher = panelPattern.matcher(content);
        if (panelMatcher.find()) {
            String panelTag = panelMatcher.group();
            boolean panelHidden = panelTag.contains("hidden") || panelTag.contains("aria-hidden=\"true\"");
            
            if (tab.isSelected && panelHidden) {
                registerProblem(holder, file, tab.offset, tab.offset + 100,
                    "Selected tab's panel should be visible",
                    null);
            } else if (!tab.isSelected && !panelHidden) {
                registerProblem(holder, file, tab.offset, tab.offset + 100,
                    "Unselected tab's panel should be hidden",
                    null);
            }
        }
    }
    
    // Helper class for tab information
    private static class TabInfo {
        final int offset;
        final boolean isSelected;
        final String tabindex;
        final String controlsId;
        
        TabInfo(int offset, boolean isSelected, String tabindex, String controlsId) {
            this.offset = offset;
            this.isSelected = isSelected;
            this.tabindex = tabindex;
            this.controlsId = controlsId;
        }
    }
    
    // Additional quick fixes for enhanced functionality
    private static class AddArrowKeyNavigationFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getName() { return getFamilyName(); }
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add arrow key navigation to tablist";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add arrow key support
        }
    }
    
    private static class AddHomeEndKeysFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getName() { return getFamilyName(); }
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add Home/End key support for first/last tab";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add Home/End support
        }
    }
    
    private static class FixSelectedTabIndexFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getName() { return getFamilyName(); }
        @NotNull
        @Override
        public String getFamilyName() {
            return "Change selected tab tabindex to '0'";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would fix tabindex
        }
    }
    
    private static class FixInactiveTabIndexFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getName() { return getFamilyName(); }
        @NotNull
        @Override
        public String getFamilyName() {
            return "Change inactive tab tabindex to '-1'";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would fix tabindex
        }
    }
    
    private static class AddFocusManagementHintFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getName() { return getFamilyName(); }
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add focus management to tabpanel";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add focus management hint
        }
    }
    
    private static class FixTabSelectionFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getName() { return getFamilyName(); }
        @NotNull
        @Override
        public String getFamilyName() {
            return "Fix tab selection consistency";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would fix selection
        }
    }
    
    private static class FixTabindexConsistencyFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getName() { return getFamilyName(); }
        @NotNull
        @Override
        public String getFamilyName() {
            return "Fix tabindex consistency in tablist";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would fix tabindex consistency
        }
    }
    
    private static class ShowTabPanelFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getName() { return getFamilyName(); }
        @NotNull
        @Override
        public String getFamilyName() {
            return "Show tabpanel for selected tab";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would show panel
        }
    }
    
    private static class HideTabPanelFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getName() { return getFamilyName(); }
        @NotNull
        @Override
        public String getFamilyName() {
            return "Hide tabpanel for unselected tab";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would hide panel
        }
    }
    
    private static class ManageInteractiveElementsFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Manage interactive elements in hidden tabpanel";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would manage interactive elements
        }
    }
}
