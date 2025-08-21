package com.typo3.fluid.linter.inspections;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.*;

public class ListSemanticInspection extends FluidAccessibilityInspection {
    
    protected static final Pattern LIST_PATTERN = Pattern.compile(
        "<(ul|ol|dl)\\s*([^>]*)>(.*?)</\\1>",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    
    private static final Pattern LI_PATTERN = Pattern.compile(
        "<li\\s*[^>]*>",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern DT_DD_PATTERN = Pattern.compile(
        "<(dt|dd)\\s*[^>]*>",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern NESTED_LIST_PATTERN = Pattern.compile(
        "<li[^>]*>.*?(<(?:ul|ol)\\s*[^>]*>.*?</(?:ul|ol)>)(?:(?!</li>).)*",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    
    private static final Pattern ROLE_LIST_PATTERN = Pattern.compile(
        "role\\s*=\\s*[\"']list[\"']",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern EMPTY_LIST_ITEM_PATTERN = Pattern.compile(
        "<li[^>]*>\\s*(?:<br\\s*/?>\\s*)?</li>",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern DIVIDER_PATTERN = Pattern.compile(
        "(<li[^>]*>\\s*[-–—•·]{1,3}\\s*</li>)|" +
        "(<li[^>]*class\\s*=\\s*[\"'][^\"']*divider[^\"']*[\"'][^>]*>\\s*</li>)",
        Pattern.CASE_INSENSITIVE
    );
    
    @NotNull
    @Override
    public String getDisplayName() {
        return "Enhanced list structure and semantic issues";
    }
    
    @NotNull
    @Override
    public String getShortName() {
        return "ListSemantic";
    }
    
    @Override
    protected void inspectFile(@NotNull PsiFile file, @NotNull ProblemsHolder holder) {
        String content = file.getText();
        
        checkListStructure(content, file, holder);
        checkDescriptionListStructure(content, file, holder);
        checkNestedLists(content, file, holder);
        checkEmptyListItems(content, file, holder);
        checkListRoles(content, file, holder);
        checkListMisuse(content, file, holder);
        
        // Enhanced context-aware semantic analysis
        analyzeListContentSemantics(content, file, holder);
        detectNavigationLists(content, file, holder);
        validateListNestingContext(content, file, holder);
        checkListItemContentQuality(content, file, holder);
    }
    
    private void checkListStructure(String content, PsiFile file, ProblemsHolder holder) {
        Matcher listMatcher = LIST_PATTERN.matcher(content);
        
        while (listMatcher.find()) {
            String listType = listMatcher.group(1).toLowerCase();
            String listContent = listMatcher.group(3);
            
            if (listType.equals("ul") || listType.equals("ol")) {
                String cleanContent = removeFluidControlFlow(listContent);
                cleanContent = removeComments(cleanContent);
                
                if (!LI_PATTERN.matcher(cleanContent).find()) {
                    registerProblem(holder, file, listMatcher.start(), listMatcher.end(),
                        "List <" + listType + "> has no <li> elements",
                        null);
                }
                
                Pattern invalidChildPattern = Pattern.compile(
                    "<(?!li|ul|ol|script|template|style)([a-zA-Z]+)\\s*[^>]*>",
                    Pattern.CASE_INSENSITIVE
                );
                
                Matcher invalidMatcher = invalidChildPattern.matcher(cleanContent);
                if (invalidMatcher.find()) {
                    String invalidTag = invalidMatcher.group(1);
                    if (!isFluidViewHelper("f:" + invalidTag)) {
                        registerProblem(holder, file, listMatcher.start() + invalidMatcher.start(),
                            listMatcher.start() + invalidMatcher.end(),
                            "Invalid direct child <" + invalidTag + "> in <" + listType + ">. Only <li> elements are allowed",
                            new WrapInListItemFix(invalidTag));
                    }
                }
            }
        }
    }
    
    private void checkDescriptionListStructure(String content, PsiFile file, ProblemsHolder holder) {
        Pattern dlPattern = Pattern.compile(
            "<dl\\s*[^>]*>(.*?)</dl>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );
        
        Matcher dlMatcher = dlPattern.matcher(content);
        
        while (dlMatcher.find()) {
            String dlContent = dlMatcher.group(1);
            
            Matcher dtddMatcher = DT_DD_PATTERN.matcher(dlContent);
            int dtCount = 0;
            int ddCount = 0;
            String lastTag = null;
            
            while (dtddMatcher.find()) {
                String tag = dtddMatcher.group(1).toLowerCase();
                if (tag.equals("dt")) {
                    dtCount++;
                    if ("dt".equals(lastTag) && ddCount < dtCount - 1) {
                        registerProblem(holder, file, dlMatcher.start() + dtddMatcher.start(),
                            dlMatcher.start() + dtddMatcher.end(),
                            "Multiple <dt> elements without corresponding <dd> elements",
                            null);
                    }
                } else if (tag.equals("dd")) {
                    ddCount++;
                    if (dtCount == 0) {
                        registerProblem(holder, file, dlMatcher.start() + dtddMatcher.start(),
                            dlMatcher.start() + dtddMatcher.end(),
                            "<dd> element without preceding <dt> element",
                            null);
                    }
                }
                lastTag = tag;
            }
            
            if (dtCount == 0 || ddCount == 0) {
                registerProblem(holder, file, dlMatcher.start(), dlMatcher.end(),
                    "Description list <dl> must contain both <dt> and <dd> elements",
                    null);
            }
        }
    }
    
    private void checkNestedLists(String content, PsiFile file, ProblemsHolder holder) {
        Matcher nestedMatcher = NESTED_LIST_PATTERN.matcher(content);
        
        while (nestedMatcher.find()) {
            String match = nestedMatcher.group(0);
            if (!match.trim().endsWith("</li>")) {
                registerProblem(holder, file, nestedMatcher.start(), nestedMatcher.end(),
                    "Nested list should be inside a list item <li>",
                    null);
            }
        }
        
        Pattern improperNestingPattern = Pattern.compile(
            "</(?:ul|ol)>\\s*<(?:ul|ol)\\s*[^>]*>",
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher improperMatcher = improperNestingPattern.matcher(content);
        while (improperMatcher.find()) {
            registerProblem(holder, file, improperMatcher.start(), improperMatcher.end(),
                "Lists should not be siblings. Nest the second list inside a list item",
                null);
        }
    }
    
    private void checkEmptyListItems(String content, PsiFile file, ProblemsHolder holder) {
        Matcher emptyMatcher = EMPTY_LIST_ITEM_PATTERN.matcher(content);
        
        while (emptyMatcher.find()) {
            registerProblem(holder, file, emptyMatcher.start(), emptyMatcher.end(),
                "Empty list item found. List items should contain meaningful content",
                new RemoveEmptyListItemFix());
        }
        
        Matcher dividerMatcher = DIVIDER_PATTERN.matcher(content);
        while (dividerMatcher.find()) {
            registerProblem(holder, file, dividerMatcher.start(), dividerMatcher.end(),
                "List items used as visual dividers should be replaced with CSS styling",
                null);
        }
    }
    
    private void checkListRoles(String content, PsiFile file, ProblemsHolder holder) {
        Pattern ulWithRolePattern = Pattern.compile(
            "<ul\\s+[^>]*" + ROLE_LIST_PATTERN.pattern() + "[^>]*>",
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher roleMatcher = ulWithRolePattern.matcher(content);
        while (roleMatcher.find()) {
            registerProblem(holder, file, roleMatcher.start(), roleMatcher.end(),
                "Redundant role='list' on <ul> element",
                new RemoveRedundantRoleFix());
        }
        
        Pattern divListPattern = Pattern.compile(
            "<div\\s+[^>]*role\\s*=\\s*[\"']list[\"'][^>]*>",
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher divListMatcher = divListPattern.matcher(content);
        while (divListMatcher.find()) {
            registerProblem(holder, file, divListMatcher.start(), divListMatcher.end(),
                "Consider using semantic <ul> or <ol> instead of div with role='list'",
                null);
        }
    }
    
    private void checkListMisuse(String content, PsiFile file, ProblemsHolder holder) {
        Pattern singleItemListPattern = Pattern.compile(
            "<(ul|ol)\\s*[^>]*>\\s*(?:<f:[^>]+>.*?</f:[^>]+>\\s*)*<li[^>]*>.*?</li>\\s*(?:<f:[^>]+>.*?</f:[^>]+>\\s*)*</\\1>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );
        
        Matcher singleMatcher = singleItemListPattern.matcher(content);
        while (singleMatcher.find()) {
            String listContent = singleMatcher.group(0);
            int liCount = countOccurrences(listContent, "<li");
            if (liCount == 1) {
                registerProblem(holder, file, singleMatcher.start(), singleMatcher.end(),
                    "List with only one item. Consider if a list is appropriate here",
                    null);
            }
        }
    }
    
    private String removeFluidControlFlow(String content) {
        for (String viewHelper : CONTROL_FLOW_VIEWHELPERS) {
            content = content.replaceAll("<" + viewHelper + "[^>]*>.*?</" + viewHelper + ">", "");
        }
        return content;
    }
    
    private String removeComments(String content) {
        return content.replaceAll("<!--.*?-->", "").replaceAll("<f:comment>.*?</f:comment>", "");
    }
    
    private int countOccurrences(String text, String search) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(search, index)) != -1) {
            count++;
            index += search.length();
        }
        return count;
    }
    
    protected void registerProblem(ProblemsHolder holder, PsiFile file, int start, int end,
                                  String message, LocalQuickFix fix) {
        PsiElement element = file.findElementAt(start);
        if (element != null) {
            if (fix != null) {
                holder.registerProblem(element, message, fix);
            } else {
                holder.registerProblem(element, message);
            }
        }
    }
    
    private static class WrapInListItemFix implements LocalQuickFix {
        private final String tagName;
        
        WrapInListItemFix(String tagName) {
            this.tagName = tagName;
        }
        
        @NotNull
        @Override
        public String getFamilyName() {
            return "Wrap <" + tagName + "> in <li> element";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would wrap element in li
        }
    }
    
    private static class RemoveEmptyListItemFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Remove empty list item";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would remove empty li
        }
    }
    
    private static class RemoveRedundantRoleFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Remove redundant role attribute";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would remove role attribute
        }
    }
    
    // Enhanced context-aware semantic analysis methods
    private void analyzeListContentSemantics(String content, PsiFile file, ProblemsHolder holder) {
        Matcher listMatcher = LIST_PATTERN.matcher(content);
        
        while (listMatcher.find()) {
            String listType = listMatcher.group(1).toLowerCase();
            String listContent = listMatcher.group(3);
            int listStart = listMatcher.start();
            
            // Analyze list item content for semantic appropriateness
            List<String> itemContents = extractListItemContents(listContent);
            
            if (itemContents.size() >= 3) { // Only analyze lists with sufficient items
                boolean hasSequentialContent = analyzeSequentialContent(itemContents);
                boolean hasRankingContent = analyzeRankingContent(itemContents);
                boolean hasStepContent = analyzeStepContent(itemContents);
                boolean hasChronologicalContent = analyzeChronologicalContent(itemContents);
                
                // Check ordered vs unordered appropriateness
                if ("ul".equals(listType)) {
                    if (hasSequentialContent || hasRankingContent || hasStepContent || hasChronologicalContent) {
                        registerProblem(holder, file, listStart, listStart + 100,
                            String.format("Consider using <ol> instead of <ul> for %s content",
                                getContentTypeDescription(hasSequentialContent, hasRankingContent, hasStepContent, hasChronologicalContent)),
                            new ChangeToOrderedListFix());
                    }
                } else if ("ol".equals(listType)) {
                    if (!hasSequentialContent && !hasRankingContent && !hasStepContent && !hasChronologicalContent) {
                        // Check if content suggests unordered nature
                        boolean hasUnorderedIndicators = itemContents.stream().anyMatch(item -> 
                            item.toLowerCase().contains("feature") ||
                            item.toLowerCase().contains("benefit") ||
                            item.toLowerCase().contains("option") ||
                            item.toLowerCase().contains("advantage"));
                        
                        if (hasUnorderedIndicators) {
                            registerProblem(holder, file, listStart, listStart + 100,
                                "Consider using <ul> instead of <ol> for features, benefits, or options that don't have inherent order",
                                new ChangeToUnorderedListFix());
                        }
                    }
                }
                
                // Check for mixed content types within single list
                boolean hasMixedContent = (hasSequentialContent ? 1 : 0) +
                                         (hasRankingContent ? 1 : 0) +
                                         (hasStepContent ? 1 : 0) > 1;
                
                if (hasMixedContent) {
                    registerProblem(holder, file, listStart, listStart + 100,
                        "List contains mixed content types (steps, rankings, sequences). Consider splitting into separate lists",
                        new SplitMixedListFix());
                }
                
                // Check for appropriate list length
                if (itemContents.size() > 15) {
                    registerProblem(holder, file, listStart, listStart + 100,
                        String.format("Long list with %d items. Consider grouping into sublists or using pagination for better usability",
                            itemContents.size()),
                        new SuggestListGroupingFix());
                }
            }
        }
    }
    
    private void detectNavigationLists(String content, PsiFile file, ProblemsHolder holder) {
        // Find lists that appear to be navigation
        Pattern navContextPattern = Pattern.compile(
            "<(?:nav|header|aside)[^>]*>.*?<(?:ul|ol)[^>]*>(.*?)</(?:ul|ol)>.*?</(?:nav|header|aside)>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );
        
        Matcher navMatcher = navContextPattern.matcher(content);
        
        while (navMatcher.find()) {
            String listContent = navMatcher.group(1);
            
            // Check for navigation indicators
            boolean hasLinks = listContent.contains("<a href");
            boolean hasNavKeywords = listContent.toLowerCase().contains("home") ||
                                    listContent.toLowerCase().contains("about") ||
                                    listContent.toLowerCase().contains("contact") ||
                                    listContent.toLowerCase().contains("menu");
            
            if (hasLinks && hasNavKeywords) {
                // This is likely navigation - check for proper landmarks
                String beforeList = content.substring(Math.max(0, navMatcher.start() - 100), navMatcher.start());
                
                if (!beforeList.contains("<nav") && !beforeList.contains("role=\"navigation\"")) {
                    registerProblem(holder, file, navMatcher.start(), navMatcher.start() + 100,
                        "Navigation list should be wrapped in <nav> element or have role='navigation'",
                        new AddNavigationLandmarkFix());
                }
                
                // Check for accessibility features
                if (!listContent.contains("aria-current") && !listContent.contains("aria-expanded")) {
                    registerProblem(holder, file, navMatcher.start(), navMatcher.start() + 100,
                        "Navigation list should indicate current page with aria-current='page' or expanded state with aria-expanded",
                        new AddNavigationStatesFix());
                }
            }
        }
        
        // Check for breadcrumb patterns
        Pattern breadcrumbPattern = Pattern.compile(
            "<(?:ol|ul)[^>]*>(?:(?!<(?:ol|ul)).)*(?:home|start).*?(?:current|active).*?</(?:ol|ul)>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );
        
        Matcher breadcrumbMatcher = breadcrumbPattern.matcher(content);
        
        while (breadcrumbMatcher.find()) {
            String listTag = content.substring(breadcrumbMatcher.start(), 
                content.indexOf('>', breadcrumbMatcher.start()) + 1);
            
            if (!listTag.contains("aria-label") && !listTag.contains("aria-labelledby")) {
                registerProblem(holder, file, breadcrumbMatcher.start(), breadcrumbMatcher.start() + 100,
                    "Breadcrumb navigation should have aria-label='Breadcrumb' or similar for context",
                    new AddBreadcrumbLabelFix());
            }
            
            // Breadcrumbs should use ordered lists
            if (listTag.toLowerCase().contains("<ul")) {
                registerProblem(holder, file, breadcrumbMatcher.start(), breadcrumbMatcher.start() + 100,
                    "Breadcrumb navigation should use <ol> to indicate hierarchical sequence",
                    new ChangeToOrderedListFix());
            }
        }
    }
    
    private void validateListNestingContext(String content, PsiFile file, ProblemsHolder holder) {
        // Find nested lists and analyze their semantic relationship
        Pattern nestedListPattern = Pattern.compile(
            "<li[^>]*>(.*?)<(?:ul|ol)[^>]*>(.*?)</(?:ul|ol)>(.*?)</li>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );
        
        Matcher nestedMatcher = nestedListPattern.matcher(content);
        
        while (nestedMatcher.find()) {
            String parentContent = nestedMatcher.group(1).trim();
            String nestedContent = nestedMatcher.group(2);
            String afterNestedContent = nestedMatcher.group(3).trim();
            
            // Check if parent item has content to justify the nesting
            String parentTextContent = parentContent.replaceAll("<[^>]+>", "").trim();
            
            if (parentTextContent.isEmpty()) {
                registerProblem(holder, file, nestedMatcher.start(), nestedMatcher.start() + 100,
                    "Parent list item with nested list should have descriptive content",
                    new AddParentItemContentFix());
            } else {
                // Analyze semantic relationship
                boolean parentIsCategory = parentTextContent.toLowerCase().matches(".*(category|group|section|type).*");
                boolean parentIsParent = parentTextContent.toLowerCase().matches(".*(contains|includes|has).*");
                
                if (!parentIsCategory && !parentIsParent) {
                    // Check if nested content relates to parent
                    String nestedTextContent = nestedContent.replaceAll("<[^>]+>", "").toLowerCase();
                    
                    boolean hasSemanticRelation = nestedTextContent.contains(parentTextContent.toLowerCase().substring(0, 
                        Math.min(parentTextContent.length(), 10)));
                    
                    if (!hasSemanticRelation) {
                        registerProblem(holder, file, nestedMatcher.start(), nestedMatcher.start() + 100,
                            "Nested list items should be semantically related to their parent item",
                            new ReviewNestingStructureFix());
                    }
                }
            }
            
            // Check for content after nested list
            if (!afterNestedContent.isEmpty() && afterNestedContent.length() > 10) {
                registerProblem(holder, file, nestedMatcher.start(), nestedMatcher.start() + 100,
                    "Consider moving content that appears after nested list to before the nested list for better structure",
                    new ReorganizeNestedContentFix());
            }
        }
        
        // Check for deeply nested lists
        checkNestedListDepth(content, file, holder, 0, 0);
    }
    
    private void checkListItemContentQuality(String content, PsiFile file, ProblemsHolder holder) {
        // Find all list items and analyze their content quality
        Pattern listItemPattern = Pattern.compile(
            "<li[^>]*>(.*?)</li>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );
        
        Matcher liMatcher = listItemPattern.matcher(content);
        
        while (liMatcher.find()) {
            String itemContent = liMatcher.group(1);
            int itemStart = liMatcher.start();
            
            // Extract text content
            String textContent = itemContent.replaceAll("<[^>]+>", "").trim();
            
            if (!textContent.isEmpty()) {
                // Check for single character or number items (might be visual formatting)
                if (textContent.matches("^[•·\\-+*]$")) {
                    registerProblem(holder, file, itemStart, liMatcher.end(),
                        "List item contains only a bullet character. Use CSS for visual bullets instead",
                        new RemoveBulletCharacterFix());
                }
                
                // Check for items that are just numbers (in unordered lists)
                if (textContent.matches("^\\d+\\.?$")) {
                    // Check if this is in an unordered list
                    int ulStart = content.lastIndexOf("<ul", itemStart);
                    int olStart = content.lastIndexOf("<ol", itemStart);
                    
                    if (ulStart > olStart) {
                        registerProblem(holder, file, itemStart, liMatcher.end(),
                            "List item contains only a number in unordered list. Consider using <ol> or adding descriptive content",
                            new FixNumberedItemFix());
                    }
                }
                
                // Check for very short items that might need more context
                if (textContent.length() < 3 && !textContent.matches("^[A-Z]{1,3}$")) { // Allow abbreviations
                    registerProblem(holder, file, itemStart, liMatcher.end(),
                        "Very short list item might need more descriptive content for accessibility",
                        null);
                }
                
                // Check for inconsistent formatting within same list
                // This would require collecting all items in the same list first
                // For now, check individual items for common issues
                
                // Check for items ending with punctuation inconsistency
                boolean endsWithPeriod = textContent.endsWith(".");
                boolean endsWithColon = textContent.endsWith(":");
                boolean isQuestion = textContent.endsWith("?");
                
                if (endsWithColon && !itemContent.contains("<ul") && !itemContent.contains("<ol")) {
                    registerProblem(holder, file, itemStart, liMatcher.end(),
                        "List item ends with colon but has no nested list. Consider removing colon or adding nested content",
                        new FixColonUsageFix());
                }
            }
            
            // Check for interactive elements without proper labeling
            if (itemContent.contains("<input") || itemContent.contains("<button") || itemContent.contains("<select")) {
                boolean hasLabel = itemContent.contains("<label") || 
                                 itemContent.contains("aria-label") ||
                                 itemContent.contains("aria-labelledby");
                
                if (!hasLabel) {
                    registerProblem(holder, file, itemStart, liMatcher.end(),
                        "Interactive elements in list items should have proper labels",
                        new AddInteractiveElementLabelFix());
                }
            }
        }
    }
    
    private void checkNestedListDepth(String content, PsiFile file, ProblemsHolder holder, int startPos, int depth) {
        if (depth > 4) {
            registerProblem(holder, file, startPos, startPos + 100,
                String.format("List nesting is %d levels deep. Consider flattening for better accessibility and usability", depth),
                new FlattenDeepNestingFix());
            return;
        }
        
        Pattern listPattern = Pattern.compile("<(?:ul|ol)[^>]*>", Pattern.CASE_INSENSITIVE);
        Matcher listMatcher = listPattern.matcher(content);
        listMatcher.region(startPos, content.length());
        
        while (listMatcher.find()) {
            int listEnd = findElementEnd(content, listMatcher.start());
            checkNestedListDepth(content, file, holder, listMatcher.start(), depth + 1);
        }
    }
    
    // Helper methods for content analysis
    private List<String> extractListItemContents(String listContent) {
        List<String> items = new ArrayList<>();
        Matcher liMatcher = Pattern.compile("<li[^>]*>(.*?)</li>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(listContent);
        
        while (liMatcher.find()) {
            String itemContent = liMatcher.group(1).replaceAll("<[^>]+>", "").trim();
            if (!itemContent.isEmpty()) {
                items.add(itemContent);
            }
        }
        
        return items;
    }
    
    private boolean analyzeSequentialContent(List<String> items) {
        int sequentialCount = 0;
        for (String item : items) {
            String lower = item.toLowerCase();
            if (lower.matches(".*(first|second|third|fourth|fifth|sixth|next|then|finally|last).*") ||
                lower.matches("^\\d+\\..*") || lower.matches(".*(step \\d+).*")) {
                sequentialCount++;
            }
        }
        return sequentialCount >= items.size() * 0.6; // 60% threshold
    }
    
    private boolean analyzeRankingContent(List<String> items) {
        int rankingCount = 0;
        for (String item : items) {
            String lower = item.toLowerCase();
            if (lower.matches(".*(best|worst|top|bottom|highest|lowest|rank|rating|score).*") ||
                lower.matches(".*(#\\d+|number \\d+).*")) {
                rankingCount++;
            }
        }
        return rankingCount >= items.size() * 0.5;
    }
    
    private boolean analyzeStepContent(List<String> items) {
        int stepCount = 0;
        for (String item : items) {
            String lower = item.toLowerCase();
            if (lower.matches(".*(step|phase|stage|procedure|process|method).*") ||
                lower.startsWith("click") || lower.startsWith("select") || lower.startsWith("enter")) {
                stepCount++;
            }
        }
        return stepCount >= items.size() * 0.6;
    }
    
    private boolean analyzeChronologicalContent(List<String> items) {
        int chronoCount = 0;
        for (String item : items) {
            String lower = item.toLowerCase();
            if (lower.matches(".*(before|after|during|while|when|year|month|day|time|date).*") ||
                lower.matches(".*(\\d{4}|january|february|march|april|may|june|july|august|september|october|november|december).*")) {
                chronoCount++;
            }
        }
        return chronoCount >= items.size() * 0.5;
    }
    
    private String getContentTypeDescription(boolean sequential, boolean ranking, boolean step, boolean chronological) {
        if (sequential) return "sequential";
        if (ranking) return "ranking/priority";
        if (step) return "step-by-step";
        if (chronological) return "chronological";
        return "ordered";
    }
    
    // Additional quick fixes for enhanced functionality
    private static class ChangeToOrderedListFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Change to ordered list <ol>";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would change ul to ol
        }
    }
    
    private static class ChangeToUnorderedListFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Change to unordered list <ul>";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would change ol to ul
        }
    }
    
    private static class SplitMixedListFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Split mixed content into separate lists";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would split list
        }
    }
    
    private static class SuggestListGroupingFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Suggest grouping long list into sublists";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would suggest grouping
        }
    }
    
    private static class AddNavigationLandmarkFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Wrap navigation list in <nav> element";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add nav wrapper
        }
    }
    
    private static class AddNavigationStatesFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add aria-current or aria-expanded to navigation";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add navigation states
        }
    }
    
    private static class AddBreadcrumbLabelFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add aria-label to breadcrumb navigation";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add breadcrumb label
        }
    }
    
    private static class AddParentItemContentFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add descriptive content to parent list item";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add parent content
        }
    }
    
    private static class ReviewNestingStructureFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Review nesting structure for semantic relationship";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would help review structure
        }
    }
    
    private static class ReorganizeNestedContentFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Move content to before nested list";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would reorganize content
        }
    }
    
    private static class RemoveBulletCharacterFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Remove bullet character (use CSS instead)";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would remove bullet character
        }
    }
    
    private static class FixNumberedItemFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Fix numbered item in unordered list";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would fix numbered item
        }
    }
    
    private static class FixColonUsageFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Fix colon usage in list item";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would fix colon usage
        }
    }
    
    private static class AddInteractiveElementLabelFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add label to interactive element in list";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add labels
        }
    }
    
    private static class FlattenDeepNestingFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Suggest flattening deep list nesting";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would suggest flattening
        }
    }
}