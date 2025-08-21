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

public class EnhancedTabPanelInspection extends TabPanelInspection {
    
    // Enhanced patterns for sophisticated tab analysis
    private static final Pattern TAB_CONTAINER_PATTERN = Pattern.compile(
        "<[^>]*(?:class\\s*=\\s*[\"'][^\"']*tab-container[^\"']*[\"']|data-tab-container)[^>]*>",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern DYNAMIC_TAB_PATTERN = Pattern.compile(
        "<[^>]*(?:data-toggle\\s*=\\s*[\"']tab[\"']|data-bs-toggle\\s*=\\s*[\"']tab[\"'])[^>]*>",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern NESTED_TAB_PATTERN = Pattern.compile(
        "<[^>]*role\\s*=\\s*[\"']tablist[\"'][^>]*>.*?<[^>]*role\\s*=\\s*[\"']tablist[\"'][^>]*>.*?</[^>]*>",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    
    private static final Pattern COMPLEX_TABPANEL_PATTERN = Pattern.compile(
        "<[^>]*role\\s*=\\s*[\"']tabpanel[\"'][^>]*>.*?(?:<form|<table|<iframe|<canvas)",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    @NotNull
    @Override
    public String getDisplayName() {
        return "Enhanced tab panel interface accessibility and usability";
    }

    @NotNull
    @Override
    public String getShortName() {
        return "EnhancedTabPanel";
    }

    @Override
    protected void inspectFile(@NotNull PsiFile file, @NotNull ProblemsHolder holder) {
        // Call parent implementation first
        super.inspectFile(file, holder);
        
        String content = file.getText();
        
        // Enhanced tab-specific validations
        analyzeTabSemantics(content, file, holder);
        validateTabInteractionComplexity(content, file, holder);
        checkTabContentRelationships(content, file, holder);
        analyzeTabUsabilityPatterns(content, file, holder);
        validateTabPersistence(content, file, holder);
        checkTabAccessibilityEnhancements(content, file, holder);
    }
    
    private void analyzeTabSemantics(String content, PsiFile file, ProblemsHolder holder) {
        // Analyze tab context for semantic appropriateness
        Matcher tablistMatcher = TABLIST_PATTERN.matcher(content);
        
        while (tablistMatcher.find()) {
            int tablistStart = tablistMatcher.start();
            int tablistEnd = findElementEnd(content, tablistStart);
            String tablistContent = content.substring(tablistStart, Math.min(tablistEnd, content.length()));
            
            // Analyze tab labels for semantic meaning
            List<TabInfo> tabs = extractTabInformation(tablistContent, tablistStart);
            
            if (tabs.size() > 1) {
                validateTabLabelSemantics(tabs, content, file, holder, tablistStart);
                checkTabContentPreview(tabs, content, file, holder);
                analyzeTabGrouping(tabs, file, holder, tablistStart);
            }
            
            // Check for inappropriate tab usage patterns
            boolean isInNavigation = isWithinNavigationContext(content, tablistStart);
            boolean isInForm = isWithinFormContext(content, tablistStart);
            
            if (isInNavigation && tabs.size() < 3) {
                registerProblem(holder, file, tablistStart, tablistStart + 100,
                    "Tab interface with few tabs in navigation context. Consider if tabs are the best pattern here",
                    new ConsiderAlternativeNavigationFix());
            }
            
            if (isInForm) {
                boolean hasFormValidation = checkForFormValidationInTabs(tablistContent);
                if (hasFormValidation) {
                    registerProblem(holder, file, tablistStart, tablistStart + 100,
                        "Form with tabs should handle validation errors across all tabs and focus appropriately",
                        new EnhanceFormTabValidationFix());
                }
            }
        }
    }
    
    private void validateTabInteractionComplexity(String content, PsiFile file, ProblemsHolder holder) {
        // Check for complex tab interaction patterns
        Matcher tablistMatcher = TABLIST_PATTERN.matcher(content);
        
        while (tablistMatcher.find()) {
            int tablistStart = tablistMatcher.start();
            int tablistEnd = findElementEnd(content, tablistStart);
            String tablistContent = content.substring(tablistStart, Math.min(tablistEnd, content.length()));
            
            List<TabInfo> tabs = extractTabInformation(tablistContent, tablistStart);
            
            if (tabs.size() > 8) {
                registerProblem(holder, file, tablistStart, tablistStart + 100,
                    String.format("Tab interface with many tabs (%d) may be difficult to navigate. Consider grouping or alternative organization",
                        tabs.size()),
                    new SuggestTabGroupingFix(tabs.size()));
            }
            
            // Check for tabs with complex interactions
            for (TabInfo tab : tabs) {
                String tabPanelContent = getTabPanelContent(content, tab.controlsId);
                
                if (tabPanelContent != null) {
                    int interactiveElementCount = countInteractiveElements(tabPanelContent);
                    
                    if (interactiveElementCount > 15) {
                        registerProblem(holder, file, tab.offset, tab.offset + 100,
                            String.format("Tab panel '%s' contains many interactive elements (%d). Consider sub-organization or separate pages",
                                tab.label, interactiveElementCount),
                            new SimplifyTabContentFix(tab.label));
                    }
                    
                    // Check for nested complex widgets
                    boolean hasComplexWidgets = tabPanelContent.contains("role=\"tablist\"") ||
                                               tabPanelContent.contains("role=\"dialog\"") ||
                                               tabPanelContent.contains("role=\"grid\"");
                    
                    if (hasComplexWidgets) {
                        registerProblem(holder, file, tab.offset, tab.offset + 100,
                            String.format("Tab panel '%s' contains complex widgets. Ensure proper focus management between nested components",
                                tab.label),
                            new ReviewNestedWidgetFocusFix());
                    }
                }
            }
            
            // Check for dynamic tab creation/removal
            boolean hasDynamicTabs = tablistContent.contains("addTab") ||
                                    tablistContent.contains("removeTab") ||
                                    tablistContent.contains("data-dynamic");
            
            if (hasDynamicTabs) {
                registerProblem(holder, file, tablistStart, tablistStart + 100,
                    "Dynamic tabs should maintain keyboard navigation and screen reader announcements when tabs are added/removed",
                    new EnhanceDynamicTabAccessibilityFix());
            }
        }
    }
    
    private void checkTabContentRelationships(String content, PsiFile file, ProblemsHolder holder) {
        // Analyze relationships between tab content
        Matcher tablistMatcher = TABLIST_PATTERN.matcher(content);
        
        while (tablistMatcher.find()) {
            int tablistStart = tablistMatcher.start();
            int tablistEnd = findElementEnd(content, tablistStart);
            String tablistContent = content.substring(tablistStart, Math.min(tablistEnd, content.length()));
            
            List<TabInfo> tabs = extractTabInformation(tablistContent, tablistStart);
            
            if (tabs.size() > 2) {
                Map<String, String> tabContents = new HashMap<>();
                
                for (TabInfo tab : tabs) {
                    String panelContent = getTabPanelContent(content, tab.controlsId);
                    if (panelContent != null) {
                        tabContents.put(tab.label, panelContent);
                    }
                }
                
                // Check for content interdependencies
                boolean hasInterdependencies = analyzeTabContentDependencies(tabContents);
                
                if (hasInterdependencies) {
                    registerProblem(holder, file, tablistStart, tablistStart + 100,
                        "Tab panels appear to have interdependent content. Consider if step-by-step wizard or single page would be better",
                        new ConsiderWizardPatternFix());
                }
                
                // Check for content overlap
                double overlapPercentage = calculateContentOverlap(tabContents);
                
                if (overlapPercentage > 0.4) {
                    registerProblem(holder, file, tablistStart, tablistStart + 100,
                        String.format("Tab panels have significant content overlap (%.0f%%). Consider consolidating or restructuring",
                            overlapPercentage * 100),
                        new ConsolidateOverlappingContentFix());
                }
                
                // Check for consistent content structure
                boolean hasConsistentStructure = analyzeTabContentStructure(tabContents);
                
                if (!hasConsistentStructure && tabs.size() > 3) {
                    registerProblem(holder, file, tablistStart, tablistStart + 100,
                        "Tab panels have inconsistent content structure. Standardizing layout improves user experience",
                        new StandardizeTabContentStructureFix());
                }
            }
        }
    }
    
    private void analyzeTabUsabilityPatterns(String content, PsiFile file, ProblemsHolder holder) {
        // Check for mobile/responsive tab considerations
        boolean isMobileOptimized = content.contains("viewport") && content.contains("device-width");
        
        if (isMobileOptimized) {
            Matcher tablistMatcher = TABLIST_PATTERN.matcher(content);
            
            while (tablistMatcher.find()) {
                int tablistStart = tablistMatcher.start();
                String tablistTag = content.substring(tablistStart, 
                    Math.min(tablistStart + 200, content.length()));
                
                // Check for horizontal scrolling prevention
                boolean hasScrollPrevention = tablistTag.contains("overflow") ||
                                             tablistTag.contains("scroll") ||
                                             tablistTag.contains("responsive-tabs");
                
                List<TabInfo> tabs = extractTabInformation(content.substring(tablistStart, 
                    findElementEnd(content, tablistStart)), tablistStart);
                
                if (tabs.size() > 4 && !hasScrollPrevention) {
                    registerProblem(holder, file, tablistStart, tablistStart + 100,
                        String.format("Mobile layout with %d tabs may require horizontal scrolling or alternative organization",
                            tabs.size()),
                        new OptimizeForMobileFix());
                }
                
                // Check for touch-friendly tab sizing
                for (TabInfo tab : tabs) {
                    if (tab.label.length() > 15) {
                        registerProblem(holder, file, tab.offset, tab.offset + 100,
                            String.format("Tab label '%s' is long for mobile interfaces. Consider shortening or using icons",
                                tab.label.length() > 30 ? tab.label.substring(0, 30) + "..." : tab.label),
                            new OptimizeTabLabelsForMobileFix());
                    }
                }
            }
        }
        
        // Check for tab loading performance
        Matcher tablistMatcher = TABLIST_PATTERN.matcher(content);
        
        while (tablistMatcher.find()) {
            int tablistStart = tablistMatcher.start();
            List<TabInfo> tabs = extractTabInformation(content.substring(tablistStart,
                findElementEnd(content, tablistStart)), tablistStart);
            
            for (TabInfo tab : tabs) {
                String panelContent = getTabPanelContent(content, tab.controlsId);
                
                if (panelContent != null) {
                    // Check for heavy content indicators
                    boolean hasHeavyContent = panelContent.contains("<img") ||
                                             panelContent.contains("<video") ||
                                             panelContent.contains("<iframe") ||
                                             panelContent.length() > 5000;
                    
                    boolean hasLazyLoading = panelContent.contains("lazy") ||
                                            panelContent.contains("data-src") ||
                                            panelContent.contains("loading=\"lazy\"");
                    
                    if (hasHeavyContent && !hasLazyLoading) {
                        registerProblem(holder, file, tab.offset, tab.offset + 100,
                            String.format("Tab panel '%s' contains heavy content. Consider lazy loading for inactive tabs",
                                tab.label),
                            new AddLazyLoadingFix(tab.controlsId));
                    }
                }
            }
        }
    }
    
    private void validateTabPersistence(String content, PsiFile file, ProblemsHolder holder) {
        // Check for tab state persistence patterns
        Matcher tablistMatcher = TABLIST_PATTERN.matcher(content);
        
        while (tablistMatcher.find()) {
            int tablistStart = tablistMatcher.start();
            int tablistEnd = findElementEnd(content, tablistStart);
            String tablistContent = content.substring(tablistStart, Math.min(tablistEnd, content.length()));
            
            List<TabInfo> tabs = extractTabInformation(tablistContent, tablistStart);
            
            if (tabs.size() > 3) {
                // Check for URL fragment support
                boolean hasUrlFragmentSupport = tablistContent.contains("location.hash") ||
                                               tablistContent.contains("hashchange") ||
                                               tabs.stream().anyMatch(tab -> 
                                                   content.contains("href=\"#" + tab.controlsId + "\""));
                
                if (!hasUrlFragmentSupport) {
                    registerProblem(holder, file, tablistStart, tablistStart + 100,
                        "Tab interface should support URL fragments for bookmarking and direct access",
                        new AddUrlFragmentSupportFix());
                }
                
                // Check for session persistence
                boolean hasSessionPersistence = tablistContent.contains("sessionStorage") ||
                                               tablistContent.contains("localStorage") ||
                                               tablistContent.contains("data-persist");
                
                if (!hasSessionPersistence && isWithinFormContext(content, tablistStart)) {
                    registerProblem(holder, file, tablistStart, tablistStart + 100,
                        "Form with tabs should consider persisting tab state to improve user experience",
                        new AddTabStatePersistenceFix());
                }
            }
        }
    }
    
    private void checkTabAccessibilityEnhancements(String content, PsiFile file, ProblemsHolder holder) {
        // Check for advanced accessibility features
        Matcher tablistMatcher = TABLIST_PATTERN.matcher(content);
        
        while (tablistMatcher.find()) {
            int tablistStart = tablistMatcher.start();
            int tablistEnd = findElementEnd(content, tablistStart);
            String tablistContent = content.substring(tablistStart, Math.min(tablistEnd, content.length()));
            
            List<TabInfo> tabs = extractTabInformation(tablistContent, tablistStart);
            
            // Check for progress indication in multi-step processes
            boolean isMultiStep = tabs.stream().anyMatch(tab -> 
                tab.label.toLowerCase().matches(".*(?:step|stage|phase).*"));
            
            if (isMultiStep) {
                boolean hasProgressIndication = tablistContent.contains("aria-current") ||
                                               tablistContent.contains("progress") ||
                                               tablistContent.contains("step-indicator");
                
                if (!hasProgressIndication) {
                    registerProblem(holder, file, tablistStart, tablistStart + 100,
                        "Multi-step tab interface should indicate current step and overall progress",
                        new AddProgressIndicationFix());
                }
            }
            
            // Check for tab count announcements for screen readers
            if (tabs.size() > 5) {
                boolean hasCountAnnouncement = tablistContent.contains("aria-setsize") ||
                                              tablistContent.contains("aria-posinset");
                
                if (!hasCountAnnouncement) {
                    registerProblem(holder, file, tablistStart, tablistStart + 100,
                        String.format("Tab interface with %d tabs should announce position and total count for screen readers",
                            tabs.size()),
                        new AddTabCountAnnouncementFix(tabs.size()));
                }
            }
            
            // Check for keyboard shortcut support
            boolean hasKeyboardShortcuts = tablistContent.contains("accesskey") ||
                                          tablistContent.contains("data-key") ||
                                          tablistContent.contains("shortcut");
            
            if (tabs.size() <= 10 && !hasKeyboardShortcuts) {
                registerProblem(holder, file, tablistStart, tablistStart + 100,
                    "Consider adding keyboard shortcuts (Alt+1, Alt+2, etc.) for frequently used tabs",
                    new AddKeyboardShortcutsFix());
            }
        }
    }
    
    // Helper methods for enhanced functionality
    private List<TabInfo> extractTabInformation(String tablistContent, int baseOffset) {
        List<TabInfo> tabs = new ArrayList<>();
        
        Matcher tabMatcher = TAB_PATTERN.matcher(tablistContent);
        
        while (tabMatcher.find()) {
            String tabTag = tabMatcher.group();
            
            String id = getAttributeValue(tabTag, "id");
            String controls = getAttributeValue(tabTag, "aria-controls");
            String selected = getAttributeValue(tabTag, "aria-selected");
            
            // Extract tab label text
            int tabEnd = findElementEnd(tablistContent, tabMatcher.start());
            String tabElement = tablistContent.substring(tabMatcher.start(), 
                Math.min(tabEnd, tablistContent.length()));
            
            String label = tabElement.replaceAll("<[^>]+>", "").trim();
            
            tabs.add(new TabInfo(
                baseOffset + tabMatcher.start(),
                id,
                controls,
                "true".equals(selected),
                label
            ));
        }
        
        return tabs;
    }
    
    private void validateTabLabelSemantics(List<TabInfo> tabs, String content, PsiFile file, 
                                          ProblemsHolder holder, int tablistStart) {
        
        for (TabInfo tab : tabs) {
            // Check for vague labels
            if (tab.label.toLowerCase().matches(".*(?:tab|panel|section|content|info|details).*") &&
                tab.label.length() < 10) {
                
                registerProblem(holder, file, tab.offset, tab.offset + 100,
                    String.format("Tab label '%s' is vague. Use descriptive labels that indicate the tab's purpose",
                        tab.label),
                    new ImproveTabLabelFix(tab.id));
            }
            
            // Check for overly long labels
            if (tab.label.length() > 25) {
                registerProblem(holder, file, tab.offset, tab.offset + 100,
                    String.format("Tab label is long (%d characters). Consider shortening for better usability",
                        tab.label.length()),
                    new ShortenTabLabelFix(tab.id));
            }
            
            // Check for technical jargon
            String[] jargonTerms = {"api", "config", "admin", "debug", "dev", "qa"};
            String lowerLabel = tab.label.toLowerCase();
            
            for (String term : jargonTerms) {
                if (lowerLabel.contains(term) && !isWithinAdminContext(content, tablistStart)) {
                    registerProblem(holder, file, tab.offset, tab.offset + 100,
                        String.format("Tab label '%s' contains technical jargon. Consider user-friendly terminology",
                            tab.label),
                        new SimplifyTabLabelFix(tab.id));
                    break;
                }
            }
        }
    }
    
    private void checkTabContentPreview(List<TabInfo> tabs, String content, PsiFile file, ProblemsHolder holder) {
        // Check if tabs could benefit from content previews or counts
        for (TabInfo tab : tabs) {
            String panelContent = getTabPanelContent(content, tab.controlsId);
            
            if (panelContent != null) {
                // Count items in the panel
                int itemCount = countListItems(panelContent) + countTableRows(panelContent);
                
                if (itemCount > 5 && !tab.label.matches(".*\\(\\d+\\).*")) {
                    registerProblem(holder, file, tab.offset, tab.offset + 100,
                        String.format("Tab '%s' contains %d items. Consider adding count in label for user context",
                            tab.label, itemCount),
                        new AddItemCountToTabFix(tab.id, itemCount));
                }
                
                // Check for form fields that could show validation status
                int formFieldCount = countFormFields(panelContent);
                boolean hasValidationErrors = panelContent.contains("error") || 
                                             panelContent.contains("invalid");
                
                if (formFieldCount > 3 && hasValidationErrors) {
                    registerProblem(holder, file, tab.offset, tab.offset + 100,
                        String.format("Tab '%s' with form fields should indicate validation status in the tab label",
                            tab.label),
                        new AddValidationStatusToTabFix(tab.id));
                }
            }
        }
    }
    
    private void analyzeTabGrouping(List<TabInfo> tabs, PsiFile file, ProblemsHolder holder, int tablistStart) {
        if (tabs.size() > 6) {
            // Analyze tab labels for potential grouping
            Map<String, List<TabInfo>> potentialGroups = new HashMap<>();
            
            for (TabInfo tab : tabs) {
                String[] words = tab.label.toLowerCase().split("\\s+");
                
                for (String word : words) {
                    if (word.length() > 3) { // Skip short words
                        potentialGroups.computeIfAbsent(word, k -> new ArrayList<>()).add(tab);
                    }
                }
            }
            
            // Find groups with multiple tabs
            for (Map.Entry<String, List<TabInfo>> entry : potentialGroups.entrySet()) {
                if (entry.getValue().size() > 2) {
                    registerProblem(holder, file, tablistStart, tablistStart + 100,
                        String.format("Multiple tabs contain '%s' - consider grouping related tabs or using nested tab structure",
                            entry.getKey()),
                        new SuggestTabSubGroupingFix(entry.getKey(), entry.getValue().size()));
                }
            }
        }
    }
    
    private boolean isWithinNavigationContext(String content, int offset) {
        int contextStart = Math.max(0, offset - 500);
        String context = content.substring(contextStart, offset);
        
        return context.contains("<nav") || context.contains("role=\"navigation\"");
    }
    
    private boolean isWithinFormContext(String content, int offset) {
        int contextStart = Math.max(0, offset - 500);
        int contextEnd = Math.min(content.length(), offset + 500);
        String context = content.substring(contextStart, contextEnd);
        
        return context.contains("<form");
    }
    
    private boolean isWithinAdminContext(String content, int offset) {
        int contextStart = Math.max(0, offset - 1000);
        String context = content.substring(contextStart, offset).toLowerCase();
        
        return context.contains("admin") || context.contains("dashboard") || 
               context.contains("settings") || context.contains("config");
    }
    
    private boolean checkForFormValidationInTabs(String tablistContent) {
        return tablistContent.contains("required") || tablistContent.contains("validation") ||
               tablistContent.contains("error") || tablistContent.contains("invalid");
    }
    
    private String getTabPanelContent(String content, String panelId) {
        if (panelId == null) return null;
        
        Pattern panelPattern = Pattern.compile(
            "<[^>]*id=\"" + Pattern.quote(panelId) + "\"[^>]*>(.*?)</[^>]+>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );
        
        Matcher matcher = panelPattern.matcher(content);
        return matcher.find() ? matcher.group(1) : null;
    }
    
    private int countInteractiveElements(String content) {
        String[] interactiveTags = {"input", "button", "select", "textarea", "a href"};
        int count = 0;
        
        for (String tag : interactiveTags) {
            Pattern pattern = Pattern.compile("<" + tag, Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(content);
            while (matcher.find()) {
                count++;
            }
        }
        
        return count;
    }
    
    private int countListItems(String content) {
        Matcher matcher = Pattern.compile("<li[^>]*>", Pattern.CASE_INSENSITIVE).matcher(content);
        int count = 0;
        while (matcher.find()) count++;
        return count;
    }
    
    private int countTableRows(String content) {
        Matcher matcher = Pattern.compile("<tr[^>]*>", Pattern.CASE_INSENSITIVE).matcher(content);
        int count = 0;
        while (matcher.find()) count++;
        return Math.max(0, count - 1); // Subtract header row if present
    }
    
    private int countFormFields(String content) {
        String[] fieldTags = {"input", "select", "textarea"};
        int count = 0;
        
        for (String tag : fieldTags) {
            Pattern pattern = Pattern.compile("<" + tag + "[^>]*>", Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(content);
            while (matcher.find()) count++;
        }
        
        return count;
    }
    
    private boolean analyzeTabContentDependencies(Map<String, String> tabContents) {
        // Simple heuristic: check if one tab's content references another tab's content
        for (Map.Entry<String, String> entry : tabContents.entrySet()) {
            String content = entry.getValue().toLowerCase();
            
            for (String otherTabLabel : tabContents.keySet()) {
                if (!otherTabLabel.equals(entry.getKey())) {
                    if (content.contains(otherTabLabel.toLowerCase()) ||
                        content.contains("previous") || content.contains("next") ||
                        content.contains("continue") || content.contains("back")) {
                        return true;
                    }
                }
            }
        }
        
        return false;
    }
    
    private double calculateContentOverlap(Map<String, String> tabContents) {
        if (tabContents.size() < 2) return 0.0;
        
        List<String> contents = new ArrayList<>(tabContents.values());
        double totalOverlap = 0.0;
        int comparisons = 0;
        
        for (int i = 0; i < contents.size() - 1; i++) {
            for (int j = i + 1; j < contents.size(); j++) {
                totalOverlap += calculateTextSimilarity(contents.get(i), contents.get(j));
                comparisons++;
            }
        }
        
        return comparisons > 0 ? totalOverlap / comparisons : 0.0;
    }
    
    private double calculateTextSimilarity(String text1, String text2) {
        // Simple word-based similarity
        Set<String> words1 = new HashSet<>(Arrays.asList(text1.toLowerCase().split("\\W+")));
        Set<String> words2 = new HashSet<>(Arrays.asList(text2.toLowerCase().split("\\W+")));
        
        Set<String> intersection = new HashSet<>(words1);
        intersection.retainAll(words2);
        
        Set<String> union = new HashSet<>(words1);
        union.addAll(words2);
        
        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }
    
    private boolean analyzeTabContentStructure(Map<String, String> tabContents) {
        if (tabContents.size() < 2) return true;
        
        List<List<String>> structures = new ArrayList<>();
        
        for (String content : tabContents.values()) {
            structures.add(analyzeContentStructure(content));
        }
        
        List<String> firstStructure = structures.get(0);
        
        for (int i = 1; i < structures.size(); i++) {
            if (!structuresAreSimilar(firstStructure, structures.get(i))) {
                return false;
            }
        }
        
        return true;
    }
    
    private List<String> analyzeContentStructure(String content) {
        List<String> structure = new ArrayList<>();
        
        Pattern tagPattern = Pattern.compile("<(\\w+)[^>]*>", Pattern.CASE_INSENSITIVE);
        Matcher matcher = tagPattern.matcher(content);
        
        while (matcher.find()) {
            String tagName = matcher.group(1).toLowerCase();
            if (!structure.contains(tagName)) {
                structure.add(tagName);
            }
        }
        
        return structure;
    }
    
    private boolean structuresAreSimilar(List<String> struct1, List<String> struct2) {
        Set<String> set1 = new HashSet<>(struct1);
        Set<String> set2 = new HashSet<>(struct2);
        
        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);
        
        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);
        
        return union.isEmpty() || (double) intersection.size() / union.size() >= 0.7;
    }
    
    // Enhanced TabInfo class
    private static class TabInfo {
        final int offset;
        final String id;
        final String controlsId;
        final boolean isSelected;
        final String label;
        
        TabInfo(int offset, String id, String controlsId, boolean isSelected, String label) {
            this.offset = offset;
            this.id = id;
            this.controlsId = controlsId;
            this.isSelected = isSelected;
            this.label = label != null ? label : "";
        }
    }
    
    // Enhanced quick fixes
    private static class ConsiderAlternativeNavigationFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Consider alternative navigation patterns for few tabs";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would suggest navigation alternatives
        }
    }
    
    private static class EnhanceFormTabValidationFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Enhance form validation across tabs";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would enhance form validation
        }
    }
    
    private static class SuggestTabGroupingFix implements LocalQuickFix {
        private final int tabCount;
        
        SuggestTabGroupingFix(int tabCount) {
            this.tabCount = tabCount;
        }
        
        @NotNull
        @Override
        public String getFamilyName() {
            return "Suggest grouping for " + tabCount + " tabs";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would suggest tab grouping strategies
        }
    }
    
    private static class SimplifyTabContentFix implements LocalQuickFix {
        private final String tabLabel;
        
        SimplifyTabContentFix(String tabLabel) {
            this.tabLabel = tabLabel;
        }
        
        @NotNull
        @Override
        public String getFamilyName() {
            return "Simplify content in '" + tabLabel + "' tab";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would suggest content simplification
        }
    }
    
    private static class ReviewNestedWidgetFocusFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Review focus management for nested widgets";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would review nested widget focus
        }
    }
    
    private static class EnhanceDynamicTabAccessibilityFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Enhance accessibility for dynamic tabs";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would enhance dynamic tab accessibility
        }
    }
    
    private static class ConsiderWizardPatternFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Consider wizard pattern for interdependent content";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would suggest wizard pattern
        }
    }
    
    private static class ConsolidateOverlappingContentFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Consolidate overlapping tab content";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would consolidate overlapping content
        }
    }
    
    private static class StandardizeTabContentStructureFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Standardize tab content structure";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would standardize content structure
        }
    }
    
    private static class OptimizeForMobileFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Optimize tab interface for mobile";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would optimize for mobile
        }
    }
    
    private static class OptimizeTabLabelsForMobileFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Optimize tab labels for mobile display";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would optimize tab labels for mobile
        }
    }
    
    private static class AddLazyLoadingFix implements LocalQuickFix {
        private final String panelId;
        
        AddLazyLoadingFix(String panelId) {
            this.panelId = panelId;
        }
        
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add lazy loading to heavy tab content";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add lazy loading
        }
    }
    
    private static class AddUrlFragmentSupportFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add URL fragment support for bookmarking";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add URL fragment support
        }
    }
    
    private static class AddTabStatePersistenceFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add tab state persistence";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add state persistence
        }
    }
    
    private static class AddProgressIndicationFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add progress indication for multi-step tabs";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add progress indication
        }
    }
    
    private static class AddTabCountAnnouncementFix implements LocalQuickFix {
        private final int tabCount;
        
        AddTabCountAnnouncementFix(int tabCount) {
            this.tabCount = tabCount;
        }
        
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add position/count announcements for " + tabCount + " tabs";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add count announcements
        }
    }
    
    private static class AddKeyboardShortcutsFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add keyboard shortcuts for tab navigation";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add keyboard shortcuts
        }
    }
    
    private static class ImproveTabLabelFix implements LocalQuickFix {
        private final String tabId;
        
        ImproveTabLabelFix(String tabId) {
            this.tabId = tabId;
        }
        
        @NotNull
        @Override
        public String getFamilyName() {
            return "Improve tab label clarity";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would improve tab label
        }
    }
    
    private static class ShortenTabLabelFix implements LocalQuickFix {
        private final String tabId;
        
        ShortenTabLabelFix(String tabId) {
            this.tabId = tabId;
        }
        
        @NotNull
        @Override
        public String getFamilyName() {
            return "Shorten tab label for better usability";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would shorten tab label
        }
    }
    
    private static class SimplifyTabLabelFix implements LocalQuickFix {
        private final String tabId;
        
        SimplifyTabLabelFix(String tabId) {
            this.tabId = tabId;
        }
        
        @NotNull
        @Override
        public String getFamilyName() {
            return "Simplify tab label terminology";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would simplify terminology
        }
    }
    
    private static class AddItemCountToTabFix implements LocalQuickFix {
        private final String tabId;
        private final int itemCount;
        
        AddItemCountToTabFix(String tabId, int itemCount) {
            this.tabId = tabId;
            this.itemCount = itemCount;
        }
        
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add item count (" + itemCount + ") to tab label";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add item count
        }
    }
    
    private static class AddValidationStatusToTabFix implements LocalQuickFix {
        private final String tabId;
        
        AddValidationStatusToTabFix(String tabId) {
            this.tabId = tabId;
        }
        
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add validation status indication to tab";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add validation status
        }
    }
    
    private static class SuggestTabSubGroupingFix implements LocalQuickFix {
        private final String commonTerm;
        private final int tabCount;
        
        SuggestTabSubGroupingFix(String commonTerm, int tabCount) {
            this.commonTerm = commonTerm;
            this.tabCount = tabCount;
        }
        
        @NotNull
        @Override
        public String getFamilyName() {
            return "Consider sub-grouping " + tabCount + " tabs related to '" + commonTerm + "'";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would suggest sub-grouping
        }
    }
}