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

public class EnhancedListSemanticInspection extends ListSemanticInspection {
    
    // Enhanced patterns for sophisticated list analysis
    private static final Pattern MENU_LIST_PATTERN = Pattern.compile(
        "<(?:ul|ol)[^>]*(?:class\\s*=\\s*[\"'][^\"']*(?:menu|nav|navigation)[^\"']*[\"']|role\\s*=\\s*[\"'](?:menubar|menu)[\"'])",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern BREADCRUMB_PATTERN = Pattern.compile(
        "<(?:ul|ol|nav)[^>]*(?:class\\s*=\\s*[\"'][^\"']*breadcrumb[^\"']*[\"']|aria-label\\s*=\\s*[\"'][^\"']*breadcrumb[^\"']*[\"'])",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern DATA_LIST_PATTERN = Pattern.compile(
        "<(?:ul|ol)[^>]*(?:class\\s*=\\s*[\"'][^\"']*(?:data|result|item|product)[^\"']*[\"'])",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern SOCIAL_LIST_PATTERN = Pattern.compile(
        "<(?:ul|ol)[^>]*(?:class\\s*=\\s*[\"'][^\"']*(?:social|share|follow)[^\"']*[\"'])",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern TAG_LIST_PATTERN = Pattern.compile(
        "<(?:ul|ol)[^>]*(?:class\\s*=\\s*[\"'][^\"']*(?:tag|label|chip|badge)[^\"']*[\"'])",
        Pattern.CASE_INSENSITIVE
    );

    @NotNull
    @Override
    public String getDisplayName() {
        return "Enhanced list structure, semantics, and content analysis";
    }

    @NotNull
    @Override
    public String getShortName() {
        return "EnhancedListSemantic";
    }

    @Override
    protected void inspectFile(@NotNull PsiFile file, @NotNull ProblemsHolder holder) {
        // Call parent implementation first
        super.inspectFile(file, holder);
        
        String content = file.getText();
        
        // Enhanced list-specific validations
        analyzeListPurposeAndContext(content, file, holder);
        validateListContentConsistency(content, file, holder);
        checkListAccessibilityEnhancements(content, file, holder);
        analyzeListInteractionPatterns(content, file, holder);
        validateListPerformanceConsiderations(content, file, holder);
        checkListResponsiveDesign(content, file, holder);
    }
    
    private void analyzeListPurposeAndContext(String content, PsiFile file, ProblemsHolder holder) {
        // Analyze different types of lists and their appropriateness
        Matcher listMatcher = LIST_PATTERN.matcher(content);
        
        while (listMatcher.find()) {
            String listType = listMatcher.group(1).toLowerCase();
            String listAttributes = listMatcher.group(2);
            String listContent = listMatcher.group(3);
            int listStart = listMatcher.start();
            
            ListPurpose purpose = analyzeListPurpose(listAttributes, listContent, content, listStart);
            
            // Validate list type appropriateness
            validateListTypeForPurpose(purpose, listType, file, holder, listStart);
            
            // Check for missing semantic markup
            checkMissingSemanticMarkup(purpose, listAttributes, listContent, file, holder, listStart);
            
            // Analyze list item content quality
            // TODO: analyzeListItemContentQuality(listContent, purpose, file, holder, listStart);
            
            // Check for accessibility improvements
            // TODO: suggestAccessibilityEnhancements(purpose, listAttributes, listContent, file, holder, listStart);
        }
    }
    
    private void validateListContentConsistency(String content, PsiFile file, ProblemsHolder holder) {
        // Find all lists and analyze their content patterns
        Matcher listMatcher = LIST_PATTERN.matcher(content);
        
        while (listMatcher.find()) {
            String listContent = listMatcher.group(3);
            int listStart = listMatcher.start();
            
            List<ListItem> items = extractListItems(listContent);
            
            if (items.size() > 2) {
                // Check content consistency
                ConsistencyAnalysis analysis = analyzeContentConsistency(items);
                
                if (!analysis.isConsistent()) {
                    registerProblem(holder, file, listStart, listStart + 100,
                        String.format("List items have inconsistent content structure. %s", 
                            analysis.getInconsistencyDescription()),
                        null); // TODO: new StandardizeListItemsFix(analysis.getSuggestion()));
                }
                
                // Check for mixed content types
                if (analysis.hasMixedContentTypes()) {
                    registerProblem(holder, file, listStart, listStart + 100,
                        "List contains mixed content types. Consider separating into multiple lists or using consistent formatting",
                        null); // TODO: new SeparateMixedContentFix());
                }
                
                // Check for appropriate list length
                if (items.size() > 20) {
                    registerProblem(holder, file, listStart, listStart + 100,
                        String.format("Long list with %d items. Consider pagination, grouping, or virtual scrolling for better usability",
                            items.size()),
                        null); // TODO: new OptimizeLongListFix(items.size()));
                }
                
                // Check for empty or minimal content items
                int lowContentItems = 0;
                for (ListItem item : items) {
                    if (item.textContent.length() < 3) {
                        lowContentItems++;
                    }
                }
                
                if (lowContentItems > items.size() * 0.3) { // More than 30% are low content
                    registerProblem(holder, file, listStart, listStart + 100,
                        String.format("List has many items (%d/%d) with minimal content. Consider if list structure is appropriate",
                            lowContentItems, items.size()),
                        null); // TODO: new ReviewListStructureFix());
                }
            }
        }
    }
    
    private void checkListAccessibilityEnhancements(String content, PsiFile file, ProblemsHolder holder) {
        // Check for advanced accessibility features in different list contexts
        
        // Navigation lists
        Matcher menuMatcher = MENU_LIST_PATTERN.matcher(content);
        while (menuMatcher.find()) {
            validateNavigationListAccessibility(content, menuMatcher, file, holder);
        }
        
        // Breadcrumb lists
        Matcher breadcrumbMatcher = BREADCRUMB_PATTERN.matcher(content);
        while (breadcrumbMatcher.find()) {
            validateBreadcrumbAccessibility(content, breadcrumbMatcher, file, holder);
        }
        
        // Data/result lists
        Matcher dataListMatcher = DATA_LIST_PATTERN.matcher(content);
        while (dataListMatcher.find()) {
            validateDataListAccessibility(content, dataListMatcher, file, holder);
        }
        
        // Social media lists
        Matcher socialMatcher = SOCIAL_LIST_PATTERN.matcher(content);
        while (socialMatcher.find()) {
            validateSocialListAccessibility(content, socialMatcher, file, holder);
        }
        
        // Tag/label lists
        Matcher tagMatcher = TAG_LIST_PATTERN.matcher(content);
        while (tagMatcher.find()) {
            validateTagListAccessibility(content, tagMatcher, file, holder);
        }
    }
    
    private void analyzeListInteractionPatterns(String content, PsiFile file, ProblemsHolder holder) {
        // Check for interactive list patterns and their accessibility
        Matcher listMatcher = LIST_PATTERN.matcher(content);
        
        while (listMatcher.find()) {
            String listContent = listMatcher.group(3);
            int listStart = listMatcher.start();
            
            // Check for interactive elements within list items
            int interactiveItemCount = countInteractiveListItems(listContent);
            int totalItems = countListItems(listContent);
            
            if (interactiveItemCount > 0) {
                // Check for proper keyboard navigation
                boolean hasKeyboardSupport = checkForKeyboardNavigationSupport(listContent);
                
                if (interactiveItemCount > 3 && !hasKeyboardSupport) {
                    registerProblem(holder, file, listStart, listStart + 100,
                        String.format("Interactive list with %d clickable items should support keyboard navigation (arrow keys, enter/space)",
                            interactiveItemCount),
                        null);// TODO: fix quick fix
                }
                
                // Check for selection patterns
                boolean hasSelectionPattern = listContent.contains("selected") ||
                                             listContent.contains("active") ||
                                             listContent.contains("aria-selected");
                
                if (hasSelectionPattern) {
                    validateSelectionListAccessibility(listContent, file, holder, listStart);
                }
                
                // Check for drag-and-drop patterns
                boolean hasDragDrop = listContent.contains("draggable") ||
                                     listContent.contains("drag") ||
                                     listContent.contains("sortable");
                
                if (hasDragDrop) {
                    validateDragDropListAccessibility(listContent, file, holder, listStart);
                }
            }
            
            // Check for list filtering/searching
            boolean hasFiltering = content.toLowerCase().contains("filter") ||
                                  content.toLowerCase().contains("search") ||
                                  listContent.contains("data-filter");
            
            if (hasFiltering && totalItems > 10) {
                checkFilteringAccessibility(listContent, file, holder, listStart);
            }
        }
    }
    
    private void validateListPerformanceConsiderations(String content, PsiFile file, ProblemsHolder holder) {
        // Check for performance implications in large lists
        Matcher listMatcher = LIST_PATTERN.matcher(content);
        
        while (listMatcher.find()) {
            String listContent = listMatcher.group(3);
            int listStart = listMatcher.start();
            
            int itemCount = countListItems(listContent);
            
            if (itemCount > 50) {
                // Check for virtual scrolling or pagination
                boolean hasVirtualScrolling = listContent.contains("virtual") ||
                                             listContent.contains("lazy") ||
                                             listContent.contains("viewport");
                
                boolean hasPagination = content.contains("pagination") ||
                                       content.contains("page-") ||
                                       content.contains("load-more");
                
                if (!hasVirtualScrolling && !hasPagination) {
                    registerProblem(holder, file, listStart, listStart + 100,
                        String.format("Large list with %d items should implement virtual scrolling or pagination for better performance",
                            itemCount),
                        null);// TODO: fix quick fix
                }
                
                // Check for heavy content in list items
                boolean hasHeavyContent = listContent.contains("<img") ||
                                         listContent.contains("<video") ||
                                         listContent.contains("<iframe");
                
                if (hasHeavyContent) {
                    boolean hasLazyLoading = listContent.contains("loading=\"lazy\"") ||
                                            listContent.contains("data-src");
                    
                    if (!hasLazyLoading) {
                        registerProblem(holder, file, listStart, listStart + 100,
                            "Large list with media content should implement lazy loading",
                            null);// TODO: fix quick fix
                    }
                }
            }
            
            // Check for excessive DOM manipulation
            if (listContent.contains("data-bind") || listContent.contains("ng-repeat") ||
                listContent.contains("v-for") || listContent.contains("f:for")) {
                
                if (itemCount > 100) {
                    registerProblem(holder, file, listStart, listStart + 100,
                        String.format("Data-bound list with %d items may cause performance issues. Consider windowing or server-side pagination",
                            itemCount),
                        null);// TODO: fix quick fix
                }
            }
        }
    }
    
    private void checkListResponsiveDesign(String content, PsiFile file, ProblemsHolder holder) {
        // Check for responsive design considerations
        boolean isMobileOptimized = content.contains("viewport") && content.contains("device-width");
        
        if (isMobileOptimized) {
            Matcher listMatcher = LIST_PATTERN.matcher(content);
            
            while (listMatcher.find()) {
                String listType = listMatcher.group(1).toLowerCase();
                String listAttributes = listMatcher.group(2);
                String listContent = listMatcher.group(3);
                int listStart = listMatcher.start();
                
                // Check horizontal lists on mobile
                if (listAttributes.contains("horizontal") || listAttributes.contains("inline") ||
                    listContent.contains("display: inline") || listContent.contains("flex-direction: row")) {
                    
                    int itemCount = countListItems(listContent);
                    if (itemCount > 4) {
                        registerProblem(holder, file, listStart, listStart + 100,
                            String.format("Horizontal list with %d items may not fit on mobile screens. Consider responsive layout",
                                itemCount),
                            null);// TODO: fix quick fix
                    }
                }
                
                // Check for touch-friendly interactive elements
                int interactiveItems = countInteractiveListItems(listContent);
                if (interactiveItems > 0) {
                    boolean hasTouchOptimization = listContent.contains("touch") ||
                                                  listContent.contains("min-height") ||
                                                  listAttributes.contains("touch-friendly");
                    
                    if (!hasTouchOptimization) {
                        registerProblem(holder, file, listStart, listStart + 100,
                            "Interactive list items should have adequate touch targets (minimum 44px) for mobile users",
                            null);// TODO: fix quick fix
                    }
                }
                
                // Check for swipe gestures on mobile
                ListPurpose purpose = analyzeListPurpose(listAttributes, listContent, content, listStart);
                if (purpose == ListPurpose.GALLERY || purpose == ListPurpose.CAROUSEL) {
                    boolean hasSwipeSupport = listContent.contains("swipe") ||
                                             listContent.contains("touch") ||
                                             listContent.contains("gesture");
                    
                    if (!hasSwipeSupport) {
                        registerProblem(holder, file, listStart, listStart + 100,
                            "Gallery/carousel lists should support swipe gestures on mobile devices",
                            null);// TODO: fix quick fix
                    }
                }
            }
        }
    }
    
    // Helper methods and classes
    private enum ListPurpose {
        NAVIGATION, MENU, BREADCRUMB, DATA, RESULTS, SOCIAL, TAGS, 
        GALLERY, CAROUSEL, STEPS, TIMELINE, FAQ, GENERAL
    }
    
    private ListPurpose analyzeListPurpose(String attributes, String content, String fullContent, int offset) {
        String lowerAttrib = attributes.toLowerCase();
        String lowerContent = content.toLowerCase();
        String surroundingContext = getSurroundingContext(fullContent, offset, 300).toLowerCase();
        
        if (lowerAttrib.contains("nav") || lowerAttrib.contains("menu") || 
            surroundingContext.contains("navigation")) {
            return ListPurpose.NAVIGATION;
        }
        
        if (lowerAttrib.contains("breadcrumb") || lowerContent.contains("home") &&
            (lowerContent.contains("current") || lowerContent.contains("active"))) {
            return ListPurpose.BREADCRUMB;
        }
        
        if (lowerAttrib.contains("social") || lowerAttrib.contains("share")) {
            return ListPurpose.SOCIAL;
        }
        
        if (lowerAttrib.contains("tag") || lowerAttrib.contains("label") || 
            lowerAttrib.contains("chip")) {
            return ListPurpose.TAGS;
        }
        
        if (lowerAttrib.contains("gallery") || lowerAttrib.contains("carousel") ||
            lowerContent.contains("<img")) {
            return ListPurpose.GALLERY;
        }
        
        if (lowerContent.contains("step") && lowerContent.contains("next")) {
            return ListPurpose.STEPS;
        }
        
        if (lowerContent.contains("question") || lowerContent.contains("answer") ||
            surroundingContext.contains("faq")) {
            return ListPurpose.FAQ;
        }
        
        if (lowerAttrib.contains("result") || lowerAttrib.contains("data") ||
            lowerAttrib.contains("product") || lowerAttrib.contains("item")) {
            return ListPurpose.DATA;
        }
        
        return ListPurpose.GENERAL;
    }
    
    private void validateListTypeForPurpose(ListPurpose purpose, String listType, 
                                           PsiFile file, ProblemsHolder holder, int offset) {
        
        switch (purpose) {
            case BREADCRUMB:
            case STEPS:
            case TIMELINE:
                if (listType.equals("ul")) {
                    registerProblem(holder, file, offset, offset + 100,
                        String.format("%s should use <ol> to indicate sequence/hierarchy", 
                            purpose.name().toLowerCase()),
                        null);// TODO: fix quick fix
                }
                break;
                
            case NAVIGATION:
            case MENU:
            case SOCIAL:
            case TAGS:
                if (listType.equals("ol")) {
                    registerProblem(holder, file, offset, offset + 100,
                        String.format("%s typically uses <ul> as order is not semantically important", 
                            purpose.name().toLowerCase()),
                        null);// TODO: fix quick fix
                }
                break;
        }
    }
    
    private void checkMissingSemanticMarkup(ListPurpose purpose, String attributes, String content,
                                           PsiFile file, ProblemsHolder holder, int offset) {
        
        switch (purpose) {
            case NAVIGATION:
            case MENU:
                if (!attributes.contains("role") && !content.contains("<nav")) {
                    registerProblem(holder, file, offset, offset + 100,
                        "Navigation list should be wrapped in <nav> element or have role='navigation'",
                        null);// TODO: fix quick fix
                }
                break;
                
            case BREADCRUMB:
                if (!attributes.contains("aria-label") && !attributes.contains("aria-labelledby")) {
                    registerProblem(holder, file, offset, offset + 100,
                        "Breadcrumb list should have aria-label='Breadcrumb' for context",
                        null);// TODO: fix quick fix
                }
                break;
                
            case DATA:
            case RESULTS:
                if (!attributes.contains("aria-label") && !attributes.contains("aria-labelledby")) {
                    registerProblem(holder, file, offset, offset + 100,
                        "Data/results list should have descriptive aria-label",
                        null);// TODO: fix quick fix
                }
                break;
        }
    }
    
    private static class ListItem {
        final String htmlContent;
        final String textContent;
        final boolean isInteractive;
        final boolean hasMedia;
        final int wordCount;
        
        ListItem(String htmlContent, String textContent, boolean isInteractive, boolean hasMedia) {
            this.htmlContent = htmlContent;
            this.textContent = textContent;
            this.isInteractive = isInteractive;
            this.hasMedia = hasMedia;
            this.wordCount = textContent.split("\\s+").length;
        }
    }
    
    private static class ConsistencyAnalysis {
        final boolean consistent;
        final boolean mixedContentTypes;
        final String inconsistencyDescription;
        final String suggestion;
        
        ConsistencyAnalysis(boolean consistent, boolean mixedContentTypes, 
                           String inconsistencyDescription, String suggestion) {
            this.consistent = consistent;
            this.mixedContentTypes = mixedContentTypes;
            this.inconsistencyDescription = inconsistencyDescription;
            this.suggestion = suggestion;
        }
        
        boolean isConsistent() { return consistent; }
        boolean hasMixedContentTypes() { return mixedContentTypes; }
        String getInconsistencyDescription() { return inconsistencyDescription; }
        String getSuggestion() { return suggestion; }
    }
    
    private List<ListItem> extractListItems(String listContent) {
        List<ListItem> items = new ArrayList<>();
        
        Pattern liPattern = Pattern.compile("<li[^>]*>(.*?)</li>", 
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher matcher = liPattern.matcher(listContent);
        
        while (matcher.find()) {
            String htmlContent = matcher.group(1);
            String textContent = htmlContent.replaceAll("<[^>]+>", "").trim();
            boolean isInteractive = htmlContent.contains("<a") || htmlContent.contains("<button") ||
                                   htmlContent.contains("onclick") || htmlContent.contains("href");
            boolean hasMedia = htmlContent.contains("<img") || htmlContent.contains("<video") ||
                              htmlContent.contains("<audio");
            
            items.add(new ListItem(htmlContent, textContent, isInteractive, hasMedia));
        }
        
        return items;
    }
    
    private ConsistencyAnalysis analyzeContentConsistency(List<ListItem> items) {
        if (items.size() < 2) {
            return new ConsistencyAnalysis(true, false, "", "");
        }
        
        // Analyze word count consistency
        int[] wordCounts = items.stream().mapToInt(item -> item.wordCount).toArray();
        double avgWords = Arrays.stream(wordCounts).average().orElse(0);
        double wordVariance = Arrays.stream(wordCounts)
            .mapToDouble(count -> Math.pow(count - avgWords, 2))
            .average().orElse(0);
        
        boolean wordCountConsistent = Math.sqrt(wordVariance) < avgWords * 0.5; // Low coefficient of variation
        
        // Analyze content type consistency
        boolean hasInteractive = items.stream().anyMatch(item -> item.isInteractive);
        boolean hasNonInteractive = items.stream().anyMatch(item -> !item.isInteractive);
        boolean mixedInteractivity = hasInteractive && hasNonInteractive;
        
        boolean hasMedia = items.stream().anyMatch(item -> item.hasMedia);
        boolean hasTextOnly = items.stream().anyMatch(item -> !item.hasMedia);
        boolean mixedMedia = hasMedia && hasTextOnly;
        
        // Analyze structural consistency
        Set<String> structures = new HashSet<>();
        for (ListItem item : items) {
            structures.add(analyzeItemStructure(item.htmlContent));
        }
        boolean structurallyConsistent = structures.size() <= 2; // Allow some variation
        
        boolean overallConsistent = wordCountConsistent && structurallyConsistent && 
                                   !mixedInteractivity && !mixedMedia;
        
        String description = "";
        String suggestion = "";
        
        if (!overallConsistent) {
            List<String> issues = new ArrayList<>();
            
            if (!wordCountConsistent) {
                issues.add("varying content lengths");
                suggestion = "Standardize content length";
            }
            if (mixedInteractivity) {
                issues.add("mixed interactive/non-interactive items");
                suggestion = "Separate interactive and static content";
            }
            if (mixedMedia) {
                issues.add("mixed media/text items");
                suggestion = "Use consistent item format";
            }
            if (!structurallyConsistent) {
                issues.add("inconsistent HTML structure");
                suggestion = "Standardize item markup structure";
            }
            
            description = String.join(", ", issues);
        }
        
        return new ConsistencyAnalysis(overallConsistent, mixedInteractivity || mixedMedia, 
                                     description, suggestion);
    }
    
    private String analyzeItemStructure(String htmlContent) {
        // Extract tag pattern
        Pattern tagPattern = Pattern.compile("<(\\w+)[^>]*>", Pattern.CASE_INSENSITIVE);
        Matcher matcher = tagPattern.matcher(htmlContent);
        
        List<String> tags = new ArrayList<>();
        while (matcher.find()) {
            tags.add(matcher.group(1).toLowerCase());
        }
        
        return String.join("-", tags);
    }
    
    private void validateNavigationListAccessibility(String content, Matcher matcher, 
                                                    PsiFile file, ProblemsHolder holder) {
        int offset = matcher.start();
        String listContent = extractListContentFromMatcher(content, matcher);
        
        // Check for current page indication
        boolean hasCurrentIndicator = listContent.contains("aria-current") ||
                                     listContent.contains("current") ||
                                     listContent.contains("active");
        
        if (!hasCurrentIndicator) {
            registerProblem(holder, file, offset, offset + 100,
                "Navigation list should indicate current page with aria-current='page' or visual styling",
                null);// TODO: fix quick fix
        }
        
        // Check for keyboard navigation support
        int interactiveItems = countInteractiveListItems(listContent);
        if (interactiveItems > 5) {
            boolean hasKeyboardSupport = listContent.contains("keydown") ||
                                        listContent.contains("keyup") ||
                                        listContent.contains("accesskey");
            
            if (!hasKeyboardSupport) {
                registerProblem(holder, file, offset, offset + 100,
                    "Large navigation menu should support keyboard navigation (arrow keys, escape)",
                    null);// TODO: fix quick fix
            }
        }
    }
    
    private void validateBreadcrumbAccessibility(String content, Matcher matcher,
                                               PsiFile file, ProblemsHolder holder) {
        int offset = matcher.start();
        String listContent = extractListContentFromMatcher(content, matcher);
        
        // Check for proper separator handling
        boolean hasAriaHiddenSeparators = listContent.contains("aria-hidden=\"true\"");
        boolean hasVisibleSeparators = listContent.contains(">") || listContent.contains("/") ||
                                      listContent.contains("→");
        
        if (hasVisibleSeparators && !hasAriaHiddenSeparators) {
            registerProblem(holder, file, offset, offset + 100,
                "Breadcrumb separators should be hidden from screen readers with aria-hidden='true'",
                null);// TODO: fix quick fix
        }
        
        // Check for structured data
        boolean hasStructuredData = listContent.contains("itemscope") ||
                                   listContent.contains("vocab") ||
                                   content.contains("application/ld+json");
        
        if (!hasStructuredData) {
            registerProblem(holder, file, offset, offset + 100,
                "Breadcrumbs should include structured data (JSON-LD or microdata) for better SEO",
                null);// TODO: fix quick fix
        }
    }
    
    private void validateDataListAccessibility(String content, Matcher matcher,
                                             PsiFile file, ProblemsHolder holder) {
        int offset = matcher.start();
        String listContent = extractListContentFromMatcher(content, matcher);
        
        int itemCount = countListItems(listContent);
        
        // Check for loading states
        if (itemCount > 20) {
            boolean hasLoadingState = listContent.contains("loading") ||
                                     listContent.contains("aria-busy") ||
                                     listContent.contains("skeleton");
            
            if (!hasLoadingState) {
                registerProblem(holder, file, offset, offset + 100,
                    "Large data list should provide loading state indicators",
                    null);// TODO: fix quick fix
            }
        }
        
        // Check for empty state handling
        boolean hasEmptyStateHandling = content.contains("no-results") ||
                                       content.contains("empty") ||
                                       content.contains("no data");
        
        if (!hasEmptyStateHandling) {
            registerProblem(holder, file, offset, offset + 100,
                "Data list should handle empty state with appropriate messaging",
                null);// TODO: fix quick fix
        }
    }
    
    private void validateSocialListAccessibility(String content, Matcher matcher,
                                               PsiFile file, ProblemsHolder holder) {
        int offset = matcher.start();
        String listContent = extractListContentFromMatcher(content, matcher);
        
        // Check for proper link labeling
        Pattern socialLinkPattern = Pattern.compile("<a[^>]*>", Pattern.CASE_INSENSITIVE);
        Matcher linkMatcher = socialLinkPattern.matcher(listContent);
        
        while (linkMatcher.find()) {
            String link = linkMatcher.group();
            
            boolean hasAccessibleLabel = link.contains("aria-label") ||
                                        link.contains("aria-labelledby") ||
                                        listContent.contains("sr-only");
            
            if (!hasAccessibleLabel) {
                registerProblem(holder, file, offset, offset + 100,
                    "Social media links should have accessible labels (not just icons)",
                    null);// TODO: fix quick fix
                break; // Report once per list
            }
        }
        
        // Check for external link indication
        boolean hasExternalLinkIndication = listContent.contains("external") ||
                                           listContent.contains("target=\"_blank\"");
        
        if (hasExternalLinkIndication && !listContent.contains("aria-label")) {
            registerProblem(holder, file, offset, offset + 100,
                "External social links should indicate they open in new window/tab",
                null);// TODO: fix quick fix
        }
    }
    
    private void validateTagListAccessibility(String content, Matcher matcher,
                                            PsiFile file, ProblemsHolder holder) {
        int offset = matcher.start();
        String listContent = extractListContentFromMatcher(content, matcher);
        
        // Check for removable tags
        boolean hasRemovableTag = listContent.contains("remove") ||
                                 listContent.contains("delete") ||
                                 listContent.contains("×");
        
        if (hasRemovableTag) {
            boolean hasProperLabeling = listContent.contains("aria-label") ||
                                       listContent.contains("sr-only");
            
            if (!hasProperLabeling) {
                registerProblem(holder, file, offset, offset + 100,
                    "Removable tags should have accessible labels like 'Remove [tag name] tag'",
                    null);// TODO: fix quick fix
            }
        }
        
        // Check for tag filtering/selection
        boolean hasInteractiveTag = listContent.contains("onclick") ||
                                   listContent.contains("button") ||
                                   listContent.contains("href");
        
        if (hasInteractiveTag) {
            boolean hasSelectionState = listContent.contains("aria-pressed") ||
                                       listContent.contains("aria-selected");
            
            if (!hasSelectionState) {
                registerProblem(holder, file, offset, offset + 100,
                    "Interactive tags should indicate selection state with aria-pressed or aria-selected",
                    null);// TODO: fix quick fix
            }
        }
    }
    
    private String getSurroundingContext(String content, int offset, int range) {
        int start = Math.max(0, offset - range);
        int end = Math.min(content.length(), offset + range);
        return content.substring(start, end);
    }
    
    private String extractListContentFromMatcher(String content, Matcher matcher) {
        int listStart = matcher.start();
        int listEnd = findElementEnd(content, listStart);
        return content.substring(listStart, Math.min(listEnd, content.length()));
    }
    
    private int countListItems(String listContent) {
        Pattern liPattern = Pattern.compile("<li[^>]*>", Pattern.CASE_INSENSITIVE);
        Matcher matcher = liPattern.matcher(listContent);
        int count = 0;
        while (matcher.find()) count++;
        return count;
    }
    
    private int countInteractiveListItems(String listContent) {
        Pattern interactivePattern = Pattern.compile(
            "<li[^>]*>.*?(?:<a|<button|onclick|href).*?</li>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );
        
        Matcher matcher = interactivePattern.matcher(listContent);
        int count = 0;
        while (matcher.find()) count++;
        return count;
    }
    
    private boolean checkForKeyboardNavigationSupport(String content) {
        return content.contains("keydown") || content.contains("keyup") ||
               content.contains("addEventListener") || content.contains("tabindex");
    }
    
    private void validateSelectionListAccessibility(String content, PsiFile file, 
                                                   ProblemsHolder holder, int offset) {
        // Check for proper ARIA selection attributes
        boolean hasAriaSelected = content.contains("aria-selected");
        boolean hasAriaPressed = content.contains("aria-pressed");
        
        if (!hasAriaSelected && !hasAriaPressed) {
            registerProblem(holder, file, offset, offset + 100,
                "Selectable list items should use aria-selected or aria-pressed to indicate state",
                null);// TODO: fix quick fix
        }
        
        // Check for keyboard selection support
        boolean hasKeyboardSelection = content.contains("space") || content.contains("enter");
        
        if (!hasKeyboardSelection) {
            registerProblem(holder, file, offset, offset + 100,
                "Selectable list should support keyboard selection (Space/Enter keys)",
                null);// TODO: fix quick fix
        }
    }
    
    private void validateDragDropListAccessibility(String content, PsiFile file,
                                                  ProblemsHolder holder, int offset) {
        // Check for keyboard alternatives to drag-and-drop
        boolean hasKeyboardAlternative = content.contains("move up") ||
                                        content.contains("move down") ||
                                        content.contains("reorder");
        
        if (!hasKeyboardAlternative) {
            registerProblem(holder, file, offset, offset + 100,
                "Drag-and-drop lists should provide keyboard alternatives for reordering",
                null);// TODO: fix quick fix
        }
        
        // Check for live region updates
        boolean hasLiveRegion = content.contains("aria-live") || content.contains("role=\"status\"");
        
        if (!hasLiveRegion) {
            registerProblem(holder, file, offset, offset + 100,
                "Drag-and-drop operations should announce changes to screen readers",
                null);// TODO: fix quick fix
        }
    }
    
    private void checkFilteringAccessibility(String content, PsiFile file, 
                                           ProblemsHolder holder, int offset) {
        // Check for filter result announcements
        boolean hasResultAnnouncement = content.contains("aria-live") ||
                                       content.contains("results found") ||
                                       content.contains("filtered");
        
        if (!hasResultAnnouncement) {
            registerProblem(holder, file, offset, offset + 100,
                "Filtered lists should announce result counts to screen readers",
                null);// TODO: fix quick fix
        }
        
        // Check for no results handling
        boolean hasNoResultsHandling = content.contains("no results") ||
                                      content.contains("no matches");
        
        if (!hasNoResultsHandling) {
            registerProblem(holder, file, offset, offset + 100,
                "Filtered lists should handle 'no results' state appropriately",
                null);// TODO: fix quick fix
        }
    }
    
    // Enhanced Quick Fixes
    private static class StandardizeListItemsFix implements LocalQuickFix {
        private final String suggestion;
        
        StandardizeListItemsFix(String suggestion) {
            this.suggestion = suggestion;
        }
        
        @NotNull
        @Override
        public String getFamilyName() {
            return "Standardize list items: " + suggestion;
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would standardize list items
        }
    }
    
    private static class SeparateMixedContentFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Separate mixed content types into different lists";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would separate content types
        }
    }
    
    private static class OptimizeLongListFix implements LocalQuickFix {
        private final int itemCount;
        
        OptimizeLongListFix(int itemCount) {
            this.itemCount = itemCount;
        }
        
        @NotNull
        @Override
        public String getFamilyName() {
            return "Optimize long list with " + itemCount + " items";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would optimize long list
        }
    }
    
    private static class ReviewListStructureFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Review list structure for minimal content items";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would review structure
        }
    }
    
    private static class AddKeyboardNavigationFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add keyboard navigation support to interactive list";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add keyboard navigation
        }
    }
    
    private static class AddVirtualScrollingFix implements LocalQuickFix {
        private final int itemCount;
        
        AddVirtualScrollingFix(int itemCount) {
            this.itemCount = itemCount;
        }
        
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add virtual scrolling for " + itemCount + " items";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add virtual scrolling
        }
    }
    
    private static class AddLazyLoadingFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add lazy loading for media content";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add lazy loading
        }
    }
    
    private static class OptimizeDataBoundListFix implements LocalQuickFix {
        private final int itemCount;
        
        OptimizeDataBoundListFix(int itemCount) {
            this.itemCount = itemCount;
        }
        
        @NotNull
        @Override
        public String getFamilyName() {
            return "Optimize data-bound list with " + itemCount + " items";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would optimize data binding
        }
    }
    
    private static class MakeListResponsiveFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Make horizontal list responsive for mobile";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would make list responsive
        }
    }
    
    private static class OptimizeForTouchFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Optimize list for touch interaction";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would optimize for touch
        }
    }
    
    private static class AddSwipeGesturesFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add swipe gesture support for mobile";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add swipe gestures
        }
    }
    
    // Additional specialized fixes
    private static class ChangeToOrderedListFix implements LocalQuickFix {
        private final String purpose;
        
        ChangeToOrderedListFix(String purpose) {
            this.purpose = purpose;
        }
        
        @NotNull
        @Override
        public String getFamilyName() {
            return "Change to <ol> for " + purpose;
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would change to ordered list
        }
    }
    
    private static class ChangeToUnorderedListFix implements LocalQuickFix {
        private final String purpose;
        
        ChangeToUnorderedListFix(String purpose) {
            this.purpose = purpose;
        }
        
        @NotNull
        @Override
        public String getFamilyName() {
            return "Change to <ul> for " + purpose;
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would change to unordered list
        }
    }
    
    private static class AddNavigationSemanticsFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add navigation semantics (nav element or role)";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add navigation semantics
        }
    }
    
    private static class AddBreadcrumbLabelFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add aria-label='Breadcrumb' to breadcrumb list";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add breadcrumb label
        }
    }
    
    private static class AddDataListLabelFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add descriptive aria-label to data list";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add data list label
        }
    }
    
    // More specialized accessibility fixes...
    private static class AddCurrentPageIndicatorFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add current page indicator to navigation";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add current page indicator
        }
    }
    
    private static class AddMenuKeyboardSupportFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add keyboard support to navigation menu";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add keyboard support
        }
    }
    
    private static class HideBreadcrumbSeparatorsFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Hide breadcrumb separators from screen readers";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would hide separators
        }
    }
    
    private static class AddBreadcrumbStructuredDataFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add structured data to breadcrumbs";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add structured data
        }
    }
    
    private static class AddLoadingStateFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add loading state indicators to data list";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add loading states
        }
    }
    
    private static class AddEmptyStateHandlingFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add empty state handling to data list";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add empty state handling
        }
    }
    
    private static class AddSocialLinkLabelsFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add accessible labels to social media links";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add social link labels
        }
    }
    
    private static class AddExternalLinkIndicationFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add external link indication to social links";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add external link indication
        }
    }
    
    private static class AddRemoveTagLabelsFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add accessible labels to removable tags";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add remove tag labels
        }
    }
    
    private static class AddTagSelectionStateFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add selection state to interactive tags";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add tag selection state
        }
    }
    
    private static class AddSelectionAriaFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add ARIA selection attributes to list items";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add selection ARIA
        }
    }
    
    private static class AddKeyboardSelectionFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add keyboard selection support to list";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add keyboard selection
        }
    }
    
    private static class AddKeyboardReorderingFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add keyboard alternatives for drag-and-drop reordering";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add keyboard reordering
        }
    }
    
    private static class AddDragDropLiveRegionFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add live region for drag-and-drop announcements";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add drag-drop live region
        }
    }
    
    private static class AddFilterResultAnnouncementFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add filter result announcements";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add filter result announcements
        }
    }
    
    private static class AddNoResultsHandlingFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add no results state handling";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add no results handling
        }
    }
}