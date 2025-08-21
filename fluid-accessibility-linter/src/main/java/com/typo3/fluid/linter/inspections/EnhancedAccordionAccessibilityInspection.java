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

public class EnhancedAccordionAccessibilityInspection extends AccordionAccessibilityInspection {
    
    // Enhanced patterns for better detection
    private static final Pattern DYNAMIC_ACCORDION_PATTERN = Pattern.compile(
        "<[^>]*(?:data-bs-toggle\\s*=\\s*[\"']collapse[\"']|" +
        "data-toggle\\s*=\\s*[\"']collapse[\"']|" +
        "class\\s*=\\s*[\"'][^\"']*(?:accordion|collapsible)[^\"']*[\"'])[^>]*>",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern SEMANTIC_ACCORDION_PATTERN = Pattern.compile(
        "<(?:details|summary)[^>]*>.*?</(?:details|summary)>",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    
    private static final Pattern KEYBOARD_EVENT_PATTERN = Pattern.compile(
        "(?:onkeydown|onkeyup|onkeypress|addEventListener\\s*\\([\"']key)",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern FOCUS_MANAGEMENT_PATTERN = Pattern.compile(
        "(?:\\.focus\\(\\)|tabindex\\s*=|data-focus)",
        Pattern.CASE_INSENSITIVE
    );

    @NotNull
    @Override
    public String getDisplayName() {
        return "Enhanced accordion and collapsible content accessibility";
    }

    @NotNull
    @Override
    public String getShortName() {
        return "EnhancedAccordionAccessibility";
    }

    @Override
    protected void inspectFile(@NotNull PsiFile file, @NotNull ProblemsHolder holder) {
        // Call parent implementation first
        super.inspectFile(file, holder);
        
        String content = file.getText();
        
        // Enhanced accordion-specific validations
        analyzeAccordionSemantics(content, file, holder);
        validateAccordionInteractionPatterns(content, file, holder);
        checkAccordionGroupCoherence(content, file, holder);
        analyzeAccordionUsabilityPatterns(content, file, holder);
        validateResponsiveAccordionBehavior(content, file, holder);
    }
    
    private void analyzeAccordionSemantics(String content, PsiFile file, ProblemsHolder holder) {
        // Analyze accordion context for semantic appropriateness
        Matcher accordionMatcher = ACCORDION_PATTERN.matcher(content);
        
        while (accordionMatcher.find()) {
            int accordionStart = accordionMatcher.start();
            int accordionEnd = findElementEnd(content, accordionStart);
            String accordionContent = content.substring(accordionStart, Math.min(accordionEnd, content.length()));
            
            // Analyze surrounding context
            int contextStart = Math.max(0, accordionStart - 300);
            int contextEnd = Math.min(content.length(), accordionEnd + 300);
            String surroundingContext = content.substring(contextStart, contextEnd);
            
            // Check for FAQ patterns
            boolean isFAQ = surroundingContext.toLowerCase().contains("faq") ||
                           surroundingContext.toLowerCase().contains("frequently") ||
                           accordionContent.toLowerCase().contains("question");
            
            // Check for navigation patterns (inappropriate for accordion)
            boolean hasNavigationContext = surroundingContext.contains("<nav") ||
                                          surroundingContext.contains("role=\"navigation\"") ||
                                          accordionContent.contains("<a href");
            
            if (hasNavigationContext && !isFAQ) {
                registerProblem(holder, file, accordionStart, accordionStart + 100,
                    "Accordion pattern detected within navigation context. Consider using disclosure widgets or menus instead",
                    new SuggestAlternativeNavigationFix());
            }
            
            // Check for appropriate accordion content structure
            List<String> panelContents = extractAccordionPanelContents(accordionContent);
            
            if (panelContents.size() > 1) {
                boolean hasConsistentStructure = validatePanelContentConsistency(panelContents);
                
                if (!hasConsistentStructure) {
                    registerProblem(holder, file, accordionStart, accordionStart + 100,
                        "Accordion panels have inconsistent content structure. Consider standardizing panel content format",
                        new StandardizePanelContentFix());
                }
                
                // Check for overly complex panel content
                for (int i = 0; i < panelContents.size(); i++) {
                    String panelContent = panelContents.get(i);
                    
                    if (panelContent.length() > 2000) {
                        registerProblem(holder, file, accordionStart, accordionStart + 100,
                            String.format("Accordion panel %d contains very long content (%d characters). Consider breaking into sub-sections or separate pages",
                                i + 1, panelContent.length()),
                            new SuggestContentBreakdownFix());
                    }
                    
                    // Check for nested interactive elements that might complicate keyboard navigation
                    int interactiveElementCount = countInteractiveElements(panelContent);
                    
                    if (interactiveElementCount > 10) {
                        registerProblem(holder, file, accordionStart, accordionStart + 100,
                            String.format("Accordion panel %d contains many interactive elements (%d). Ensure proper focus management and tab order",
                                i + 1, interactiveElementCount),
                            new ReviewFocusManagementFix());
                    }
                }
            }
        }
    }
    
    private void validateAccordionInteractionPatterns(String content, PsiFile file, ProblemsHolder holder) {
        // Check for modern accordion interaction patterns
        Matcher accordionMatcher = ACCORDION_PATTERN.matcher(content);
        
        while (accordionMatcher.find()) {
            int accordionStart = accordionMatcher.start();
            int accordionEnd = findElementEnd(content, accordionStart);
            String accordionContent = content.substring(accordionStart, Math.min(accordionEnd, content.length()));
            
            // Check for single vs multiple panel expansion capability
            boolean allowsMultipleOpen = accordionContent.contains("data-allow-multiple") ||
                                        accordionContent.contains("data-multi-expand") ||
                                        !accordionContent.contains("data-parent");
            
            List<String> triggers = extractAccordionTriggers(accordionContent);
            
            if (triggers.size() > 3 && !allowsMultipleOpen) {
                registerProblem(holder, file, accordionStart, accordionStart + 100,
                    "Multi-panel accordion with single-expansion behavior may frustrate users. Consider allowing multiple panels to be open simultaneously",
                    new AllowMultipleExpansionFix());
            }
            
            // Check for smooth animation hints
            boolean hasAnimationControl = accordionContent.contains("transition") ||
                                         accordionContent.contains("data-duration") ||
                                         accordionContent.contains("prefers-reduced-motion");
            
            if (!hasAnimationControl && triggers.size() > 1) {
                registerProblem(holder, file, accordionStart, accordionStart + 100,
                    "Accordion should respect user motion preferences and provide smooth, interruptible animations",
                    new AddMotionPreferencesSupportFix());
            }
            
            // Check for loading state management
            boolean hasLoadingStates = accordionContent.contains("aria-busy") ||
                                      accordionContent.contains("data-loading") ||
                                      accordionContent.contains("loading");
            
            if (!hasLoadingStates && accordionContent.contains("ajax")) {
                registerProblem(holder, file, accordionStart, accordionStart + 100,
                    "Accordion with dynamic content loading should manage loading states with aria-busy",
                    new AddLoadingStateManagementFix());
            }
        }
    }
    
    private void checkAccordionGroupCoherence(String content, PsiFile file, ProblemsHolder holder) {
        // Find all accordion groups on the page and analyze their relationships
        List<AccordionGroup> accordionGroups = identifyAccordionGroups(content);
        
        if (accordionGroups.size() > 1) {
            for (int i = 0; i < accordionGroups.size() - 1; i++) {
                AccordionGroup current = accordionGroups.get(i);
                AccordionGroup next = accordionGroups.get(i + 1);
                
                // Check if accordions are close together and might be related
                if (Math.abs(current.endOffset - next.startOffset) < 500) {
                    boolean semanticallyRelated = analyzeAccordionRelationship(current, next, content);
                    
                    if (semanticallyRelated) {
                        registerProblem(holder, file, current.startOffset, current.startOffset + 100,
                            "Multiple related accordions detected. Consider combining them into a single accordion group with consistent behavior",
                            new CombineRelatedAccordionsFix());
                    }
                }
            }
            
            // Check for accordion nesting that might confuse users
            for (AccordionGroup group : accordionGroups) {
                for (AccordionGroup other : accordionGroups) {
                    if (group != other && group.containsOffset(other.startOffset)) {
                        registerProblem(holder, file, group.startOffset, group.startOffset + 100,
                            "Nested accordion groups detected. This may create confusing interaction patterns for users",
                            new SimplifyNestedAccordionsFix());
                    }
                }
            }
        }
        
        // Check for page-level accordion density
        if (accordionGroups.size() > 5) {
            registerProblem(holder, file, 0, 100,
                String.format("Page contains many accordion groups (%d). Consider if all content truly benefits from accordion organization",
                    accordionGroups.size()),
                new ReviewAccordionNecessityFix());
        }
    }
    
    private void analyzeAccordionUsabilityPatterns(String content, PsiFile file, ProblemsHolder holder) {
        // Check for mobile-specific accordion considerations
        Matcher viewportMatcher = Pattern.compile(
            "<meta[^>]*name\\s*=\\s*[\"']viewport[\"'][^>]*>",
            Pattern.CASE_INSENSITIVE
        ).matcher(content);
        
        boolean isMobileOptimized = viewportMatcher.find();
        
        if (isMobileOptimized) {
            Matcher accordionMatcher = ACCORDION_PATTERN.matcher(content);
            
            while (accordionMatcher.find()) {
                int accordionStart = accordionMatcher.start();
                String accordionTag = content.substring(accordionStart, 
                    Math.min(accordionStart + 200, content.length()));
                
                // Check for touch-friendly trigger sizing
                boolean hasTouchOptimization = accordionTag.contains("min-height") ||
                                              accordionTag.contains("padding") ||
                                              accordionTag.contains("touch-friendly");
                
                if (!hasTouchOptimization) {
                    registerProblem(holder, file, accordionStart, accordionStart + 100,
                        "Mobile-optimized page should ensure accordion triggers have adequate touch targets (minimum 44px)",
                        new OptimizeForTouchFix());
                }
            }
        }
        
        // Check for accordion trigger clarity
        List<String> accordionTriggers = extractAllAccordionTriggers(content);
        
        for (int i = 0; i < accordionTriggers.size(); i++) {
            String trigger = accordionTriggers.get(i);
            String triggerText = trigger.replaceAll("<[^>]+>", "").trim();
            
            // Check for vague trigger text
            if (triggerText.toLowerCase().matches(".*(?:more|details|info|click here|expand).*") &&
                triggerText.length() < 20) {
                
                registerProblem(holder, file, 0, 100,
                    String.format("Accordion trigger text '%s' is vague. Use descriptive text that indicates what will be revealed",
                        triggerText.substring(0, Math.min(triggerText.length(), 30))),
                    new ImproveTriggertextFix(i));
            }
            
            // Check for consistent trigger formatting
            if (i > 0) {
                String previousTrigger = accordionTriggers.get(i - 1);
                boolean hasConsistentFormat = checkTriggerFormatConsistency(trigger, previousTrigger);
                
                if (!hasConsistentFormat) {
                    registerProblem(holder, file, 0, 100,
                        "Accordion triggers have inconsistent formatting. Maintain consistent style for better usability",
                        new StandardizeTriggerFormatFix());
                }
            }
        }
    }
    
    private void validateResponsiveAccordionBehavior(String content, PsiFile file, ProblemsHolder holder) {
        // Check for responsive accordion patterns
        boolean hasMediaQueries = content.contains("@media") || content.contains("media=");
        boolean hasResponsiveClasses = content.contains("responsive") || 
                                      content.contains("d-") || // Bootstrap responsive classes
                                      content.contains("hidden-");
        
        if (hasMediaQueries || hasResponsiveClasses) {
            Matcher accordionMatcher = ACCORDION_PATTERN.matcher(content);
            
            while (accordionMatcher.find()) {
                int accordionStart = accordionMatcher.start();
                int accordionEnd = findElementEnd(content, accordionStart);
                String accordionContent = content.substring(accordionStart, Math.min(accordionEnd, content.length()));
                
                // Check for responsive behavior documentation
                boolean hasResponsiveBehaviorHints = accordionContent.contains("data-breakpoint") ||
                                                    accordionContent.contains("data-responsive") ||
                                                    accordionContent.contains("mobile-accordion");
                
                if (!hasResponsiveBehaviorHints) {
                    registerProblem(holder, file, accordionStart, accordionStart + 100,
                        "Responsive page should consider accordion behavior across different screen sizes",
                        new AddResponsiveBehaviorFix());
                }
                
                // Check for desktop-alternative patterns
                List<String> panelContents = extractAccordionPanelContents(accordionContent);
                
                if (panelContents.size() <= 3) {
                    registerProblem(holder, file, accordionStart, accordionStart + 100,
                        "Consider showing all content expanded on larger screens when there are few accordion panels",
                        new SuggestDesktopExpansionFix());
                }
            }
        }
    }
    
    // Helper methods for enhanced functionality
    private List<String> extractAccordionPanelContents(String accordionContent) {
        List<String> contents = new ArrayList<>();
        Pattern panelPattern = Pattern.compile(
            "<[^>]*(?:class\\s*=\\s*[\"'][^\"']*(?:panel|collapse)[^\"']*[\"'])[^>]*>(.*?)</[^>]+>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );
        
        Matcher matcher = panelPattern.matcher(accordionContent);
        while (matcher.find()) {
            contents.add(matcher.group(1));
        }
        
        return contents;
    }
    
    private boolean validatePanelContentConsistency(List<String> panelContents) {
        if (panelContents.size() < 2) return true;
        
        // Check if panels have similar structure (headings, paragraphs, lists, etc.)
        List<String> firstPanelStructure = analyzeContentStructure(panelContents.get(0));
        
        for (int i = 1; i < panelContents.size(); i++) {
            List<String> currentPanelStructure = analyzeContentStructure(panelContents.get(i));
            
            if (!structuresAreSimilar(firstPanelStructure, currentPanelStructure)) {
                return false;
            }
        }
        
        return true;
    }
    
    private List<String> analyzeContentStructure(String content) {
        List<String> structure = new ArrayList<>();
        
        // Extract tag patterns
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
        
        // Consider similar if they share at least 60% of structural elements
        return union.isEmpty() || (double) intersection.size() / union.size() >= 0.6;
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
    
    private List<String> extractAccordionTriggers(String accordionContent) {
        List<String> triggers = new ArrayList<>();
        Pattern triggerPattern = Pattern.compile(
            "<(?:button|[^>]+role\\s*=\\s*[\"']button[\"'])[^>]*>.*?</(?:button|[^>]+)>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );
        
        Matcher matcher = triggerPattern.matcher(accordionContent);
        while (matcher.find()) {
            triggers.add(matcher.group());
        }
        
        return triggers;
    }
    
    private List<String> extractAllAccordionTriggers(String content) {
        List<String> allTriggers = new ArrayList<>();
        Matcher accordionMatcher = ACCORDION_PATTERN.matcher(content);
        
        while (accordionMatcher.find()) {
            int accordionStart = accordionMatcher.start();
            int accordionEnd = findElementEnd(content, accordionStart);
            String accordionContent = content.substring(accordionStart, Math.min(accordionEnd, content.length()));
            
            allTriggers.addAll(extractAccordionTriggers(accordionContent));
        }
        
        return allTriggers;
    }
    
    private List<AccordionGroup> identifyAccordionGroups(String content) {
        List<AccordionGroup> groups = new ArrayList<>();
        Matcher accordionMatcher = ACCORDION_PATTERN.matcher(content);
        
        while (accordionMatcher.find()) {
            int start = accordionMatcher.start();
            int end = findElementEnd(content, start);
            String accordionContent = content.substring(start, Math.min(end, content.length()));
            
            groups.add(new AccordionGroup(start, end, accordionContent));
        }
        
        return groups;
    }
    
    private boolean analyzeAccordionRelationship(AccordionGroup group1, AccordionGroup group2, String content) {
        // Check if accordions are semantically related based on surrounding content and structure
        String betweenContent = content.substring(group1.endOffset, group2.startOffset);
        
        // If there's minimal content between them, they might be related
        String cleanBetweenContent = betweenContent.replaceAll("<[^>]+>", "").trim();
        
        return cleanBetweenContent.length() < 100;
    }
    
    private boolean checkTriggerFormatConsistency(String trigger1, String trigger2) {
        // Extract styling patterns
        boolean trigger1HasIcon = trigger1.contains("icon") || trigger1.contains("fa-") || trigger1.contains("glyphicon");
        boolean trigger2HasIcon = trigger2.contains("icon") || trigger2.contains("fa-") || trigger2.contains("glyphicon");
        
        return trigger1HasIcon == trigger2HasIcon;
    }
    
    // Helper class for accordion group information
    private static class AccordionGroup {
        final int startOffset;
        final int endOffset;
        final String content;
        
        AccordionGroup(int startOffset, int endOffset, String content) {
            this.startOffset = startOffset;
            this.endOffset = endOffset;
            this.content = content;
        }
        
        boolean containsOffset(int offset) {
            return offset >= startOffset && offset <= endOffset;
        }
    }
    
    // Enhanced quick fixes
    private static class SuggestAlternativeNavigationFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Consider menu or disclosure widget for navigation";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would suggest navigation alternatives
        }
    }
    
    private static class StandardizePanelContentFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Standardize accordion panel content structure";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would help standardize panel content
        }
    }
    
    private static class SuggestContentBreakdownFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Consider breaking down long accordion content";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would suggest content breakdown strategies
        }
    }
    
    private static class ReviewFocusManagementFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Review focus management for complex accordion content";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would provide focus management guidance
        }
    }
    
    private static class AllowMultipleExpansionFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Allow multiple accordion panels to be open";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would modify accordion to allow multiple panels
        }
    }
    
    private static class AddMotionPreferencesSupportFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add support for reduced motion preferences";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add motion preferences support
        }
    }
    
    private static class AddLoadingStateManagementFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add loading state management with aria-busy";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add loading state management
        }
    }
    
    private static class CombineRelatedAccordionsFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Consider combining related accordion groups";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would suggest accordion combination
        }
    }
    
    private static class SimplifyNestedAccordionsFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Simplify nested accordion structure";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would help simplify nesting
        }
    }
    
    private static class ReviewAccordionNecessityFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Review if all accordions are necessary";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would provide accordion necessity guidelines
        }
    }
    
    private static class OptimizeForTouchFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Optimize accordion triggers for touch interaction";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would optimize for touch
        }
    }
    
    private static class ImproveTriggertextFix implements LocalQuickFix {
        private final int triggerIndex;
        
        ImproveTriggertextFix(int triggerIndex) {
            this.triggerIndex = triggerIndex;
        }
        
        @NotNull
        @Override
        public String getFamilyName() {
            return "Improve accordion trigger text clarity";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would improve trigger text
        }
    }
    
    private static class StandardizeTriggerFormatFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Standardize accordion trigger formatting";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would standardize trigger format
        }
    }
    
    private static class AddResponsiveBehaviorFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add responsive accordion behavior";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add responsive behavior
        }
    }
    
    private static class SuggestDesktopExpansionFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Consider expanding all panels on larger screens";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would suggest desktop expansion
        }
    }
}