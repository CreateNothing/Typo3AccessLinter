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
        "<(ul|ol|dl)\\b([^>]*)>(.*?)</\\1>",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    
    private static final Pattern LI_PATTERN = Pattern.compile(
        "<li\\b[^>]*>",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern DT_DD_PATTERN = Pattern.compile(
        "<(dt|dd)\\b[^>]*>",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern NESTED_LIST_PATTERN = Pattern.compile(
        "<li[^>]*>.*?<(?:ul|ol)[^>]*>.*?</(?:ul|ol)>.*?</li>",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    
    private static final Pattern ROLE_LIST_PATTERN = Pattern.compile(
        "role\\s*=\\s*\"list\"",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern EMPTY_LIST_ITEM_PATTERN = Pattern.compile(
        "<li[^>]*>\\s*(?:<br\\s*/?>\\s*)?</li>",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern DIVIDER_PATTERN = Pattern.compile(
        "((<li[^>]*>\\s*[-–—•·]{1,3}\\s*</li>))|((<li[^>]*class\\s*=\\s*\"[^\"]*divider[^\"]*\"[^>]*>\\s*</li>))",
        Pattern.CASE_INSENSITIVE
    );
    
    // Enhanced patterns for sophisticated list analysis
    private static final Pattern MENU_LIST_PATTERN = Pattern.compile(
        "<(?:ul|ol)[^>]*(?:class\\s*=\\s*\"[^\"]*(?:menu|nav|navigation)[^\"]*\"|role\\s*=\\s*\"(?:menubar|menu)\")",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern BREADCRUMB_PATTERN = Pattern.compile(
        "<(?:ul|ol|nav)[^>]*(?:class\\s*=\\s*\"[^\"]*breadcrumb[^\"]*\"|aria-label\\s*=\\s*\"[^\"]*breadcrumb[^\"]*\")",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern DATA_LIST_PATTERN = Pattern.compile(
        "<(?:ul|ol)[^>]*class\\s*=\\s*\"[^\"]*(?:data|result|item|product)[^\"]*\"",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern SOCIAL_LIST_PATTERN = Pattern.compile(
        "<(?:ul|ol)[^>]*class\\s*=\\s*\"[^\"]*(?:social|share|follow)[^\"]*\"",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern TAG_LIST_PATTERN = Pattern.compile(
        "<(?:ul|ol)[^>]*class\\s*=\\s*\"[^\"]*(?:tag|label|chip|badge)[^\"]*\"",
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
        
        // Enhanced list-specific validations
        analyzeListPurposeAndContext(content, file, holder);
        validateListContentConsistency(content, file, holder);
        checkListAccessibilityEnhancements(content, file, holder);
        analyzeListInteractionPatterns(content, file, holder);
        validateListPerformanceConsiderations(content, file, holder);
        checkListResponsiveDesign(content, file, holder);
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
                    "<(?!li|ul|ol|script|template|style)([a-zA-Z]+)\\s*[^\\\"]*>",
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
            "<dl\\s*[^\\\"]*>(.*?)<\\/dl>",
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
                    "Put nested lists inside a list item (<li>)",
                    null);
            }
        }
        
        Pattern improperNestingPattern = Pattern.compile(
            "<\\/(?:ul|ol)>\\s*<(?:ul|ol)\\s*[^\\\"]*>",
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher improperMatcher = improperNestingPattern.matcher(content);
        while (improperMatcher.find()) {
            registerProblem(holder, file, improperMatcher.start(), improperMatcher.end(),
                "Avoid side‑by‑side lists; nest the second list inside a list item",
                null);
        }
    }
    
    private void checkEmptyListItems(String content, PsiFile file, ProblemsHolder holder) {
        Matcher emptyMatcher = EMPTY_LIST_ITEM_PATTERN.matcher(content);
        
        while (emptyMatcher.find()) {
            registerProblem(holder, file, emptyMatcher.start(), emptyMatcher.end(),
                "Remove empty list items; each <li> should have content",
                new RemoveEmptyListItemFix());
        }
        
        Matcher dividerMatcher = DIVIDER_PATTERN.matcher(content);
        while (dividerMatcher.find()) {
            registerProblem(holder, file, dividerMatcher.start(), dividerMatcher.end(),
                "Don’t use list items as visual dividers; use CSS instead",
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
            "<div\\s+[^>]*role\\s*=\\s*\"list\"[^>]*>",
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
            "<(ul|ol)\\b[^>]*>\\s*(?:<f:[^>]*>.*?</f:[^>]*>\\s*)*<li[^>]*>.*?</li>\\s*(?:<f:[^>]*>.*?</f:[^>]*>\\s*)*</\\1>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );
        
        Matcher singleMatcher = singleItemListPattern.matcher(content);
        while (singleMatcher.find()) {
            String listContent = singleMatcher.group(0);
            int liCount = countOccurrences(listContent, "<li");
            if (liCount == 1) {
                LocalQuickFix[] fixes = new LocalQuickFix[] {
                    new ConvertSingleItemListFix(),
                    new ConvertSingleItemListToParagraphFix()
                };
                registerProblems(holder, file, singleMatcher.start(), singleMatcher.end(),
                    "List with only one item. Consider if a list is appropriate here",
                    fixes);
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
            PsiElement element = descriptor.getPsiElement();
            if (element == null) return;
            PsiFile file = element.getContainingFile();
            if (file == null) return;

            String content = file.getText();
            int start = element.getTextRange().getStartOffset();

            // Find the start of the offending tag
            ListSemanticInspection self = new ListSemanticInspection();
            int elemStart = self.findElementStart(content, start);
            int elemEnd = self.findElementEnd(content, elemStart);
            if (elemStart < 0 || elemEnd <= elemStart) return;

            String before = content.substring(0, elemStart);
            String middle = content.substring(elemStart, elemEnd);
            String after = content.substring(elemEnd);

            String replacement = "<li>" + middle + "</li>";
            String newContent = before + replacement + after;

            com.intellij.psi.PsiFile newFile = com.intellij.psi.PsiFileFactory.getInstance(project)
                .createFileFromText(file.getName(), file.getFileType(), newContent);
            file.getNode().replaceAllChildrenToChildrenOf(newFile.getNode());
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
            PsiElement element = descriptor.getPsiElement();
            if (element == null) return;
            PsiFile file = element.getContainingFile();
            if (file == null) return;

            String content = file.getText();
            int caret = element.getTextOffset();
            int liStart = content.lastIndexOf("<li", caret);
            if (liStart < 0) return;
            ListSemanticInspection self = new ListSemanticInspection();
            int liEnd = self.findElementEnd(content, liStart);
            if (liEnd <= liStart) return;

            String newContent = content.substring(0, liStart) + content.substring(liEnd);
            com.intellij.psi.PsiFile newFile = com.intellij.psi.PsiFileFactory.getInstance(project)
                .createFileFromText(file.getName(), file.getFileType(), newContent);
            file.getNode().replaceAllChildrenToChildrenOf(newFile.getNode());
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
            PsiElement element = descriptor.getPsiElement();
            if (element == null) return;
            PsiFile file = element.getContainingFile();
            if (file == null) return;

            String content = file.getText();
            int caret = element.getTextOffset();
            int listStart = Math.max(content.lastIndexOf("<ul", caret), content.lastIndexOf("<ol", caret));
            if (listStart < 0) return;
            int tagEnd = content.indexOf('>', listStart);
            if (tagEnd < 0) return;
            String openTag = content.substring(listStart, tagEnd + 1);
            String updated = openTag.replaceAll("\\srole\\s*=\\s*\"list\"", "").replaceAll("\\srole\\s*=\\s*'list'", "");
            String newContent = content.substring(0, listStart) + updated + content.substring(tagEnd + 1);
            com.intellij.psi.PsiFile newFile = com.intellij.psi.PsiFileFactory.getInstance(project)
                .createFileFromText(file.getName(), file.getFileType(), newContent);
            file.getNode().replaceAllChildrenToChildrenOf(newFile.getNode());
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
            "<(?:nav|header|aside)[^\\\"]*>.*?<(?:ul|ol)[^\\\"]*>(.*?)<\\/(?:ul|ol)>.*?<\\/(?:nav|header|aside)>",
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
                        "Wrap navigation lists in <nav> (or add role='navigation')",
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
            "<(?:ol|ul)[^\\\"]*>(?:(?!<(?:ol|ul)).)*(?:home|start).*?(?:current|active).*?<\\/(?:ol|ul)>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );
        
        Matcher breadcrumbMatcher = breadcrumbPattern.matcher(content);
        
        while (breadcrumbMatcher.find()) {
            String listTag = content.substring(breadcrumbMatcher.start(), 
                content.indexOf('>', breadcrumbMatcher.start()) + 1);
            
            if (!listTag.contains("aria-label") && !listTag.contains("aria-labelledby")) {
                registerProblem(holder, file, breadcrumbMatcher.start(), breadcrumbMatcher.start() + 100,
                    "Add a short label to the breadcrumb (e.g., aria-label='Breadcrumb')",
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
            "<li[^\\\"]*>(.*?)<(?:ul|ol)[^\\\"]*>(.*?)<\\/(?:ul|ol)>(.*?)<\\/li>",
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
                boolean parentIsCategory = parentTextContent.toLowerCase().matches(".*(category|group|section|type).*\s*");
                boolean parentIsParent = parentTextContent.toLowerCase().matches(".*(contains|includes|has).*\s*");
                
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
            "<li[^\\\"]*>(.*?)<\\/li>",
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
            LocalQuickFix[] fixes = new LocalQuickFix[] {
                new FlattenDeepNestingFix(),
                new FlattenDeepNestingToItemsFix()
            };
            registerProblems(holder, file, startPos, startPos + 100,
                String.format("List nesting is %d levels deep. Consider flattening for better accessibility and usability", depth),
                fixes);
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
            if (lower.matches(".*(first|second|third|fourth|fifth|sixth|next|then|finally|last).*\\s*") ||
                lower.matches("^\\d+\\.?.*\\s*") || lower.matches(".*(step \\d+).*\\s*")) {
                sequentialCount++;
            }
        }
        return sequentialCount >= items.size() * 0.6; // 60% threshold
    }
    
    private boolean analyzeRankingContent(List<String> items) {
        int rankingCount = 0;
        for (String item : items) {
            String lower = item.toLowerCase();
            if (lower.matches(".*(best|worst|top|bottom|highest|lowest|rank|rating|score).*\\s*") ||
                lower.matches(".*(#\\d+|number \\d+).*\\s*")) {
                rankingCount++;
            }
        }
        return rankingCount >= items.size() * 0.5;
    }
    
    private boolean analyzeStepContent(List<String> items) {
        int stepCount = 0;
        for (String item : items) {
            String lower = item.toLowerCase();
            if (lower.matches(".*(step|phase|stage|procedure|process|method).*\\s*") ||
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
            if (lower.matches(".*(before|after|during|while|when|year|month|day|time|date).*\\s*") ||
                lower.matches(".*(\\d{4}|january|february|march|april|may|june|july|august|september|october|november|december).*\\s*")) {
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
            PsiElement element = descriptor.getPsiElement();
            if (element == null) return;
            PsiFile file = element.getContainingFile();
            if (file == null) return;

            String content = file.getText();
            int caret = element.getTextOffset();
            int ulStart = content.lastIndexOf("<ul", caret);
            if (ulStart < 0) return;
            ListSemanticInspection self = new ListSemanticInspection();
            int listEnd = self.findElementEnd(content, ulStart);
            if (listEnd <= ulStart) return;
            String outer = content.substring(ulStart, listEnd);
            String converted = outer.replaceFirst("(?i)^<ul", "<ol").replaceFirst("(?i)</ul>$", "</ol>");
            String newContent = content.substring(0, ulStart) + converted + content.substring(listEnd);
            com.intellij.psi.PsiFile newFile = com.intellij.psi.PsiFileFactory.getInstance(project)
                .createFileFromText(file.getName(), file.getFileType(), newContent);
            file.getNode().replaceAllChildrenToChildrenOf(newFile.getNode());
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
            PsiElement element = descriptor.getPsiElement();
            if (element == null) return;
            PsiFile file = element.getContainingFile();
            if (file == null) return;

            String content = file.getText();
            int caret = element.getTextOffset();
            int olStart = content.lastIndexOf("<ol", caret);
            if (olStart < 0) return;
            ListSemanticInspection self = new ListSemanticInspection();
            int listEnd = self.findElementEnd(content, olStart);
            if (listEnd <= olStart) return;
            String outer = content.substring(olStart, listEnd);
            String converted = outer.replaceFirst("(?i)^<ol", "<ul").replaceFirst("(?i)</ol>$", "</ul>");
            String newContent = content.substring(0, olStart) + converted + content.substring(listEnd);
            com.intellij.psi.PsiFile newFile = com.intellij.psi.PsiFileFactory.getInstance(project)
                .createFileFromText(file.getName(), file.getFileType(), newContent);
            file.getNode().replaceAllChildrenToChildrenOf(newFile.getNode());
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
            PsiElement element = descriptor.getPsiElement();
            if (element == null) return;
            PsiFile file = element.getContainingFile();
            if (file == null) return;

            String content = file.getText();
            int caret = element.getTextOffset();
            int liStart = content.lastIndexOf("<li", caret);
            if (liStart < 0) return;
            ListSemanticInspection self = new ListSemanticInspection();
            int liEnd = self.findElementEnd(content, liStart);
            if (liEnd <= liStart) return;
            String li = content.substring(liStart, liEnd);

            // Remove leading bullet-like characters inside the <li> content
            String cleaned = li.replaceFirst("(?is)(<li[^>]*>\\s*)([•·\\-+*–—]{1,3}\\s*)(?=\\S)", "$1");
            String newContent = content.substring(0, liStart) + cleaned + content.substring(liEnd);
            com.intellij.psi.PsiFile newFile = com.intellij.psi.PsiFileFactory.getInstance(project)
                .createFileFromText(file.getName(), file.getFileType(), newContent);
            file.getNode().replaceAllChildrenToChildrenOf(newFile.getNode());
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
            PsiElement element = descriptor.getPsiElement();
            if (element == null) return;
            PsiFile file = element.getContainingFile();
            if (file == null) return;

            String content = file.getText();
            int caret = element.getTextOffset();
            // Convert surrounding <ul> to <ol>
            int ulStart = content.lastIndexOf("<ul", caret);
            if (ulStart < 0) return;
            ListSemanticInspection self = new ListSemanticInspection();
            int listEnd = self.findElementEnd(content, ulStart);
            if (listEnd <= ulStart) return;
            String outer = content.substring(ulStart, listEnd);
            String converted = outer.replaceFirst("(?i)^<ul", "<ol").replaceFirst("(?i)</ul>$", "</ol>");
            String newContent = content.substring(0, ulStart) + converted + content.substring(listEnd);
            com.intellij.psi.PsiFile newFile = com.intellij.psi.PsiFileFactory.getInstance(project)
                .createFileFromText(file.getName(), file.getFileType(), newContent);
            file.getNode().replaceAllChildrenToChildrenOf(newFile.getNode());
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
            PsiElement element = descriptor.getPsiElement();
            if (element == null) return;
            PsiFile file = element.getContainingFile();
            if (file == null) return;

            String content = file.getText();
            int caret = element.getTextOffset();
            int liStart = content.lastIndexOf("<li", caret);
            if (liStart < 0) return;
            ListSemanticInspection self = new ListSemanticInspection();
            int liEnd = self.findElementEnd(content, liStart);
            if (liEnd <= liStart) return;
            String li = content.substring(liStart, liEnd);
            // Remove trailing colon before </li> if no nested lists inside
            if (!li.matches("(?is).*<(?:ul|ol)[^>]*>.*")) {
                String cleaned = li.replaceFirst("(?is):\\s*</li>\s*$", "</li>");
                String newContent = content.substring(0, liStart) + cleaned + content.substring(liEnd);
                com.intellij.psi.PsiFile newFile = com.intellij.psi.PsiFileFactory.getInstance(project)
                    .createFileFromText(file.getName(), file.getFileType(), newContent);
                file.getNode().replaceAllChildrenToChildrenOf(newFile.getNode());
            }
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
            PsiElement element = descriptor.getPsiElement();
            if (element == null) return;
            PsiFile file = element.getContainingFile();
            if (file == null) return;

            String content = file.getText();
            int caret = element.getTextOffset();
            int liStart = content.lastIndexOf("<li", caret);
            if (liStart < 0) return;
            ListSemanticInspection self = new ListSemanticInspection();
            int liEnd = self.findElementEnd(content, liStart);
            if (liEnd <= liStart) return;
            String li = content.substring(liStart, liEnd);

            // Add aria-label="Action" to first interactive element lacking accessible name
            java.util.regex.Pattern interactive = java.util.regex.Pattern.compile(
                "<(input|button|select)([^>]*)>", java.util.regex.Pattern.CASE_INSENSITIVE);
            java.util.regex.Matcher m = interactive.matcher(li);
            if (m.find()) {
                String attrs = m.group(2) != null ? m.group(2) : "";
                boolean hasLabel = attrs.matches("(?is).*aria-label\\s*=.*|.*aria-labelledby\\s*=.*|.*title\\s*=.*");
                if (!hasLabel) {
                    String before = li.substring(0, m.start(2));
                    String after = li.substring(m.end(2));
                    String newAttrs = attrs + " aria-label=\"Action\"";
                    li = before + newAttrs + after;
                    String newContent = content.substring(0, liStart) + li + content.substring(liEnd);
                    com.intellij.psi.PsiFile newFile = com.intellij.psi.PsiFileFactory.getInstance(project)
                        .createFileFromText(file.getName(), file.getFileType(), newContent);
                    file.getNode().replaceAllChildrenToChildrenOf(newFile.getNode());
                }
            }
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
            PsiElement element = descriptor.getPsiElement();
            if (element == null) return;
            PsiFile file = element.getContainingFile();
            if (file == null) return;

            String content = file.getText();
            int caret = element.getTextOffset();

            // Find nearest enclosing list start (<ul or <ol>) before caret
            int ulStart = content.lastIndexOf("<ul", caret);
            int olStart = content.lastIndexOf("<ol", caret);
            int listStart = Math.max(ulStart, olStart);
            if (listStart < 0) return;

            // Determine end of this list
            ListSemanticInspection self = new ListSemanticInspection();
            int listEnd = self.findElementEnd(content, listStart);
            if (listEnd <= listStart) return;

            String outer = content.substring(listStart, Math.min(listEnd, content.length()));

            // Capture parent opening tag and attributes
            java.util.regex.Matcher parentOpen = java.util.regex.Pattern
                .compile("^<(ul|ol)([^>]*)>", java.util.regex.Pattern.CASE_INSENSITIVE | java.util.regex.Pattern.DOTALL)
                .matcher(outer);
            if (!parentOpen.find()) return;
            String parentTag = parentOpen.group(1);
            String parentAttrs = parentOpen.group(2) != null ? parentOpen.group(2) : "";

            java.util.LinkedHashSet<String> mergedClasses = new java.util.LinkedHashSet<>();
            addClassesFromAttr(parentAttrs, mergedClasses);
            java.util.Map<String,String> inheritedAria = new java.util.HashMap<>();

            // Scan nested list opening tags to collect classes/ARIA
            java.util.regex.Matcher nestedOpen = java.util.regex.Pattern
                .compile("<(ul|ol)([^>]*)>", java.util.regex.Pattern.CASE_INSENSITIVE | java.util.regex.Pattern.DOTALL)
                .matcher(outer);
            boolean skippedParent = false;
            while (nestedOpen.find()) {
                if (!skippedParent) { skippedParent = true; continue; }
                String attrs = nestedOpen.group(2) != null ? nestedOpen.group(2) : "";
                addClassesFromAttr(attrs, mergedClasses);
                inheritIfAbsent(attrs, "role", inheritedAria);
                inheritIfAbsent(attrs, "aria-label", inheritedAria);
                inheritIfAbsent(attrs, "aria-labelledby", inheritedAria);
                inheritIfAbsent(attrs, "aria-describedby", inheritedAria);
            }

            // Repeatedly lift nested <ul>/<ol> <li> items into the parent list
            String flattened = outer;
            java.util.regex.Pattern nested = java.util.regex.Pattern.compile(
                "(<li[^>]*>)(.*?)<(?:ul|ol)[^>]*>(.*?)</(?:ul|ol)>(.*?)</li>",
                java.util.regex.Pattern.CASE_INSENSITIVE | java.util.regex.Pattern.DOTALL
            );

            boolean changed = true;
            int passes = 0;
            while (changed && passes < 10) {
                java.util.regex.Matcher m = nested.matcher(flattened);
                if (m.find()) {
                    // Replace each nested list with: <li>prefix</li> + inner <li>...
                    flattened = m.replaceAll("$1$2</li>$3");
                } else {
                    changed = false;
                }
                passes++;
            }

            // Rebuild parent opening tag with merged class/ARIA
            String mergedAttrStr = parentAttrs;
            if (!mergedClasses.isEmpty()) {
                String merged = String.join(" ", mergedClasses);
                mergedAttrStr = replaceOrAppendAttr(mergedAttrStr, "class", merged);
            }
            for (java.util.Map.Entry<String,String> e : inheritedAria.entrySet()) {
                // Only add if not already present on parent
                if (!hasAttr(mergedAttrStr, e.getKey())) {
                    mergedAttrStr = replaceOrAppendAttr(mergedAttrStr, e.getKey(), e.getValue());
                }
            }
            String newParentOpen = "<" + parentTag + mergedAttrStr + ">";
            flattened = newParentOpen + flattened.substring(parentOpen.end());

            // Write back to file
            String newContent = content.substring(0, listStart) + flattened + content.substring(listEnd);
            com.intellij.psi.PsiFile newFile = com.intellij.psi.PsiFileFactory.getInstance(project)
                .createFileFromText(file.getName(), file.getFileType(), newContent);
            file.getNode().replaceAllChildrenToChildrenOf(newFile.getNode());
        }

        private static void addClassesFromAttr(String attrs, java.util.Set<String> out) {
            java.util.regex.Matcher cm = java.util.regex.Pattern
                .compile("\\bclass\\s*=\\s*\"([^\"]*)\"|\\bclass\\s*=\\s*'([^']*)'", java.util.regex.Pattern.CASE_INSENSITIVE)
                .matcher(attrs);
            if (cm.find()) {
                String val = cm.group(1) != null ? cm.group(1) : cm.group(2);
                if (val != null) {
                    for (String c : val.trim().split("\\s+")) {
                        if (!c.isEmpty()) out.add(c);
                    }
                }
            }
        }

        private static void inheritIfAbsent(String attrs, String name, java.util.Map<String,String> out) {
            String v = getAttr(attrs, name);
            if (v != null && !out.containsKey(name)) out.put(name, v);
        }

        private static String getAttr(String attrs, String name) {
            java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("\\b" + java.util.regex.Pattern.quote(name) + "\\s*=\\s*\"([^\"]*)\"|\\b" + java.util.regex.Pattern.quote(name) + "\\s*=\\s*'([^']*)'",
                        java.util.regex.Pattern.CASE_INSENSITIVE)
                .matcher(attrs);
            if (m.find()) return m.group(1) != null ? m.group(1) : m.group(2);
            return null;
        }

        private static boolean hasAttr(String attrs, String name) {
            return java.util.regex.Pattern.compile("\\b" + java.util.regex.Pattern.quote(name) + "\\s*=", java.util.regex.Pattern.CASE_INSENSITIVE)
                .matcher(attrs).find();
        }

        private static String replaceOrAppendAttr(String attrs, String name, String value) {
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(
                "(\\b" + java.util.regex.Pattern.quote(name) + "\\s*=\\s*)(\"[^\"]*\"|'[^']*')",
                java.util.regex.Pattern.CASE_INSENSITIVE);
            java.util.regex.Matcher m = p.matcher(attrs);
            if (m.find()) {
                return m.replaceFirst(m.group(1) + '"' + java.util.regex.Matcher.quoteReplacement(value) + '"');
            }
            // append with a leading space
            return attrs + " " + name + "=\"" + value.replace("\"", "&quot;") + "\"";
        }
    }

    private static class FlattenDeepNestingToItemsFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Flatten nesting and move classes to items";
        }

        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            PsiElement element = descriptor.getPsiElement();
            if (element == null) return;
            PsiFile file = element.getContainingFile();
            if (file == null) return;

            String content = file.getText();
            int caret = element.getTextOffset();

            // Locate current list
            int ulStart = content.lastIndexOf("<ul", caret);
            int olStart = content.lastIndexOf("<ol", caret);
            int listStart = Math.max(ulStart, olStart);
            if (listStart < 0) return;

            ListSemanticInspection self = new ListSemanticInspection();
            int listEnd = self.findElementEnd(content, listStart);
            if (listEnd <= listStart) return;

            String outer = content.substring(listStart, Math.min(listEnd, content.length()));

            // Capture parent opening tag and inner segment
            java.util.regex.Matcher parentOpen = java.util.regex.Pattern
                .compile("^<(ul|ol)([^>]*)>", java.util.regex.Pattern.CASE_INSENSITIVE | java.util.regex.Pattern.DOTALL)
                .matcher(outer);
            if (!parentOpen.find()) return;
            int openEnd = parentOpen.end();
            String parentAttrs = parentOpen.group(2) != null ? parentOpen.group(2) : "";

            // Collect classes from nested lists (excluding the parent)
            java.util.LinkedHashSet<String> mergedClasses = new java.util.LinkedHashSet<>();
            // Do NOT add parent's classes; we want parent minimal
            java.util.regex.Matcher nestedOpen = java.util.regex.Pattern
                .compile("<(ul|ol)([^>]*)>", java.util.regex.Pattern.CASE_INSENSITIVE | java.util.regex.Pattern.DOTALL)
                .matcher(outer);
            boolean skippedParent = false;
            while (nestedOpen.find()) {
                if (!skippedParent) { skippedParent = true; continue; }
                String attrs = nestedOpen.group(2) != null ? nestedOpen.group(2) : "";
                addClassesFromAttr(attrs, mergedClasses);
            }

            // Flatten nested lists by lifting inner <li> items
            String flattened = outer;
            java.util.regex.Pattern nested = java.util.regex.Pattern.compile(
                "(<li[^>]*>)(.*?)<(?:ul|ol)[^>]*>(.*?)</(?:ul|ol)>(.*?)</li>",
                java.util.regex.Pattern.CASE_INSENSITIVE | java.util.regex.Pattern.DOTALL
            );
            boolean changed = true; int passes = 0;
            while (changed && passes < 10) {
                java.util.regex.Matcher m = nested.matcher(flattened);
                if (m.find()) {
                    flattened = m.replaceAll("$1$2</li>$3");
                } else { changed = false; }
                passes++;
            }

            // Push merged classes down to first-level <li> children
            if (!mergedClasses.isEmpty()) {
                String merged = String.join(" ", mergedClasses);
                String beforeItems = flattened.substring(0, openEnd);
                String itemsAndClose = flattened.substring(openEnd);
                // Replace immediate <li ...> occurrences (best-effort regex)
                java.util.regex.Matcher lim = java.util.regex.Pattern
                    .compile("<li([^>]*)>", java.util.regex.Pattern.CASE_INSENSITIVE)
                    .matcher(itemsAndClose);
                StringBuffer sb = new StringBuffer();
                while (lim.find()) {
                    String liAttrs = lim.group(1) != null ? lim.group(1) : "";
                    String newAttrs = replaceOrAppendAttr(liAttrs, "class", mergeClassVals(getAttr(liAttrs, "class"), merged));
                    lim.appendReplacement(sb, "<li" + java.util.regex.Matcher.quoteReplacement(newAttrs) + ">");
                }
                lim.appendTail(sb);
                flattened = beforeItems + sb.toString();
            }

            // Keep parent minimal — optionally could remove redundant class attr spaces
            String newContent = content.substring(0, listStart) + flattened + content.substring(listEnd);
            com.intellij.psi.PsiFile newFile = com.intellij.psi.PsiFileFactory.getInstance(project)
                .createFileFromText(file.getName(), file.getFileType(), newContent);
            file.getNode().replaceAllChildrenToChildrenOf(newFile.getNode());
        }

        private static void addClassesFromAttr(String attrs, java.util.Set<String> out) {
            java.util.regex.Matcher cm = java.util.regex.Pattern
                .compile("\\bclass\\s*=\\s*\"([^\"]*)\"|\\bclass\\s*=\\s*'([^']*)'", java.util.regex.Pattern.CASE_INSENSITIVE)
                .matcher(attrs);
            if (cm.find()) {
                String val = cm.group(1) != null ? cm.group(1) : cm.group(2);
                if (val != null) {
                    for (String c : val.trim().split("\\s+")) {
                        if (!c.isEmpty()) out.add(c);
                    }
                }
            }
        }

        private static String replaceOrAppendAttr(String attrs, String name, String value) {
            if (value == null || value.trim().isEmpty()) return attrs; // nothing to add
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(
                "(\\b" + java.util.regex.Pattern.quote(name) + "\\s*=\\s*)(\"[^\"]*\"|'[^']*')",
                java.util.regex.Pattern.CASE_INSENSITIVE);
            java.util.regex.Matcher m = p.matcher(attrs);
            if (m.find()) {
                return m.replaceFirst(m.group(1) + '"' + java.util.regex.Matcher.quoteReplacement(value) + '"');
            }
            return attrs + " " + name + "=\"" + value.replace("\"", "&quot;") + "\"";
        }

        private static String getAttr(String attrs, String name) {
            java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("\\b" + java.util.regex.Pattern.quote(name) + "\\s*=\\s*\"([^\"]*)\"|\\b" + java.util.regex.Pattern.quote(name) + "\\s*=\\s*'([^']*)'",
                        java.util.regex.Pattern.CASE_INSENSITIVE)
                .matcher(attrs);
            if (m.find()) return m.group(1) != null ? m.group(1) : m.group(2);
            return null;
        }

        private static String mergeClassVals(String existing, String add) {
            java.util.LinkedHashSet<String> set = new java.util.LinkedHashSet<>();
            if (existing != null) {
                for (String c : existing.trim().split("\\s+")) if (!c.isEmpty()) set.add(c);
            }
            if (add != null) {
                for (String c : add.trim().split("\\s+")) if (!c.isEmpty()) set.add(c);
            }
            if (set.isEmpty()) return existing != null ? existing : "";
            return " " + String.join(" ", set); // leading space keeps spacing when reinserted
        }
    }

    private static class ConvertSingleItemListFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Convert single-item list to plain content";
        }

        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            PsiElement element = descriptor.getPsiElement();
            if (element == null) return;
            PsiFile file = element.getContainingFile();
            if (file == null) return;

            String content = file.getText();
            int caret = element.getTextOffset();

            int ulStart = content.lastIndexOf("<ul", caret);
            int olStart = content.lastIndexOf("<ol", caret);
            int listStart = Math.max(ulStart, olStart);
            if (listStart < 0) return;

            ListSemanticInspection self = new ListSemanticInspection();
            int listEnd = self.findElementEnd(content, listStart);
            if (listEnd <= listStart) return;

            String outer = content.substring(listStart, Math.min(listEnd, content.length()));
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("<li[^>]*>(.*?)</li>", java.util.regex.Pattern.CASE_INSENSITIVE | java.util.regex.Pattern.DOTALL).matcher(outer);
            if (!m.find()) return;
            String inner = m.group(1);

            // Replace the whole list with inner content (unwrapped). Keep whitespace intact around.
            String newContent = content.substring(0, listStart) + inner + content.substring(listEnd);
            com.intellij.psi.PsiFile newFile = com.intellij.psi.PsiFileFactory.getInstance(project)
                .createFileFromText(file.getName(), file.getFileType(), newContent);
            file.getNode().replaceAllChildrenToChildrenOf(newFile.getNode());
        }
    }

    private static class ConvertSingleItemListToParagraphFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Convert single-item list to <p> paragraph";
        }

        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            PsiElement element = descriptor.getPsiElement();
            if (element == null) return;
            PsiFile file = element.getContainingFile();
            if (file == null) return;

            String content = file.getText();
            int caret = element.getTextOffset();

            int ulStart = content.lastIndexOf("<ul", caret);
            int olStart = content.lastIndexOf("<ol", caret);
            int listStart = Math.max(ulStart, olStart);
            if (listStart < 0) return;

            ListSemanticInspection self = new ListSemanticInspection();
            int listEnd = self.findElementEnd(content, listStart);
            if (listEnd <= listStart) return;

            String outer = content.substring(listStart, Math.min(listEnd, content.length()));
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("<li[^>]*>(.*?)</li>", java.util.regex.Pattern.CASE_INSENSITIVE | java.util.regex.Pattern.DOTALL).matcher(outer);
            if (!m.find()) return;
            String inner = m.group(1).trim();

            // If inner already contains block-level elements, fall back to plain unwrap
            if (inner.matches("(?is).*</?(p|div|section|article|header|footer|ul|ol|table|figure|nav)[^>]*>.*")) {
                String newContentFallback = content.substring(0, listStart) + inner + content.substring(listEnd);
                com.intellij.psi.PsiFile newFileFallback = com.intellij.psi.PsiFileFactory.getInstance(project)
                    .createFileFromText(file.getName(), file.getFileType(), newContentFallback);
                file.getNode().replaceAllChildrenToChildrenOf(newFileFallback.getNode());
                return;
            }

            String wrapped = "<p>" + inner + "</p>";
            String newContent = content.substring(0, listStart) + wrapped + content.substring(listEnd);
            com.intellij.psi.PsiFile newFile = com.intellij.psi.PsiFileFactory.getInstance(project)
                .createFileFromText(file.getName(), file.getFileType(), newContent);
            file.getNode().replaceAllChildrenToChildrenOf(newFile.getNode());
        }
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
    private enum ListPurpose { NAVIGATION, MENU, BREADCRUMB, DATA, RESULTS, SOCIAL, TAGS, 
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
            this.wordCount = textContent.split("\\s+\\s*").length;
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
        return content.substring(start, Math.min(end, content.length()));
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
    
    // Additional specialized fixes
    private static class StandardizeListItemsFix implements LocalQuickFix {
        private final String suggestion;
        
        StandardizeListItemsFix(String suggestion) {
            this.suggestion = suggestion;
        }
        
        @NotNull
        @Override
        public String getName() { return getFamilyName(); }
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
        public String getName() { return getFamilyName(); }
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
        public String getName() { return getFamilyName(); }
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
        public String getName() { return getFamilyName(); }
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
        public String getName() { return getFamilyName(); }
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
    
    // More specialized fixes... (duplicates removed)
    
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
}
