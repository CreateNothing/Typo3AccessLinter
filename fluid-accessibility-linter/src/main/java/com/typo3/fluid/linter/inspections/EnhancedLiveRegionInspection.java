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

public class EnhancedLiveRegionInspection extends LiveRegionInspection {
    
    // Enhanced patterns for sophisticated live region analysis
    private static final Pattern DYNAMIC_CONTENT_PATTERN = Pattern.compile(
        "<[^>]*(?:data-bind|ng-|v-|x-data|class=\"[^\"]*js-dynamic[^\"]*\")[^>]*>",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern AJAX_PATTERN = Pattern.compile(
        "(?:fetch\\(|\\$\\.ajax|XMLHttpRequest|axios\\.|ajax:|data-url)",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern FORM_FEEDBACK_PATTERN = Pattern.compile(
        "<[^>]*class=\"[^\"]*(?:feedback|validation|form-error|form-success)[^\"]*\"[^>]*>",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern LOADING_INDICATOR_PATTERN = Pattern.compile(
        "<[^>]*(?:class=\"[^\"]*(?:loading|spinner|progress)[^\"]*\"|data-loading)[^>]*>",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern REAL_TIME_PATTERN = Pattern.compile(
        "(?:websocket|socket\\.io|sse|eventSource|setInterval|setTimeout)",
        Pattern.CASE_INSENSITIVE
    );

    @NotNull
    @Override
    public String getDisplayName() {
        return "Enhanced live region and dynamic content accessibility";
    }

    @NotNull
    @Override
    public String getShortName() {
        return "EnhancedLiveRegion";
    }

    @Override
    protected void inspectFile(@NotNull PsiFile file, @NotNull ProblemsHolder holder) {
        // Call parent implementation first
        super.inspectFile(file, holder);
        
        String content = file.getText();
        
        // Enhanced live region validations
        analyzeContentUpdatePatterns(content, file, holder);
        validateLiveRegionHierarchy(content, file, holder);
        checkUserInteractionContext(content, file, holder);
        analyzeUpdateFrequencyPatterns(content, file, holder);
        validateCriticalMessageHandling(content, file, holder);
        checkMultiModalAnnouncements(content, file, holder);
    }
    
    private void analyzeContentUpdatePatterns(String content, PsiFile file, ProblemsHolder holder) {
        // Identify dynamic content areas that might need live region support
        Matcher dynamicMatcher = DYNAMIC_CONTENT_PATTERN.matcher(content);
        
        while (dynamicMatcher.find()) {
            int dynamicStart = dynamicMatcher.start();
            int dynamicEnd = findElementEnd(content, dynamicStart);
            String dynamicContent = content.substring(dynamicStart, Math.min(dynamicEnd, content.length()));
            
            // Check if this dynamic area has appropriate live region attributes
            boolean hasLiveRegion = dynamicContent.contains("aria-live") ||
                                   dynamicContent.contains("role=\"alert\"") ||
                                   dynamicContent.contains("role=\"status\"");
            
            if (!hasLiveRegion) {
                // Analyze the type of dynamic content
                String contentType = analyzeDynamicContentType(dynamicContent, content, dynamicStart);
                
                if (!contentType.equals("decorative")) {
                    String recommendedLiveRegion = getRecommendedLiveRegion(contentType);
                    
                    // TODO: registerProblem call needs to be implemented properly
        // registerProblem(holder, file, dynamicStart, dynamicStart + 100, message, fix);
                }
            } else {
                // Validate appropriateness of existing live region
                // TODO: validateLiveRegionAppropriateness(dynamicContent, content, file, holder, dynamicStart);
            }
        }
        
        // Check for AJAX content updates without live regions
        if (AJAX_PATTERN.matcher(content).find()) {
            checkAjaxLiveRegionSupport(content, file, holder);
        }
    }
    
    private void validateLiveRegionHierarchy(String content, PsiFile file, ProblemsHolder holder) {
        // Find all live regions and analyze their relationships
        List<LiveRegionInfo> liveRegions = identifyAllLiveRegions(content);
        
        if (liveRegions.size() > 1) {
            // Check for hierarchical relationships
            for (int i = 0; i < liveRegions.size(); i++) {
                LiveRegionInfo current = liveRegions.get(i);
                
                for (int j = 0; j < liveRegions.size(); j++) {
                    if (i != j) {
                        LiveRegionInfo other = liveRegions.get(j);
                        
                        // Check if one live region is nested within another
                        // TODO: fix containsOffset method call
        if (current.startOffset <= other.startOffset && current.endOffset >= other.startOffset) {
                            // TODO: registerProblem call
                // registerProblem for nested live regions
                        }
                        
                        // Check for competing priority levels
                        if (Math.abs(current.startOffset - other.startOffset) < 300 &&
                            current.priority.equals("assertive") && other.priority.equals("assertive")) {
                            
                            registerProblem(holder, file, current.startOffset, current.startOffset + 100,
                                "Multiple assertive live regions in close proximity may interrupt each other",
                                null);// TODO: fix
                        }
                    }
                }
            }
            
            // Check for missing coordination between related live regions
            Map<String, List<LiveRegionInfo>> regionsByContext = groupRegionsByContext(liveRegions, content);
            
            for (Map.Entry<String, List<LiveRegionInfo>> entry : regionsByContext.entrySet()) {
                if (entry.getValue().size() > 2) {
                    String context = entry.getKey();
                    registerProblem(holder, file, entry.getValue().get(0).startOffset, 
                        entry.getValue().get(0).startOffset + 100,
                        String.format("Multiple live regions detected in %s context. Consider coordinating announcements or consolidating",
                            context),
                        null);// TODO: fix
                }
            }
        }
    }
    
    private void checkUserInteractionContext(String content, PsiFile file, ProblemsHolder holder) {
        // Analyze live regions in context of user interactions
        List<LiveRegionInfo> liveRegions = identifyAllLiveRegions(content);
        
        for (LiveRegionInfo region : liveRegions) {
            String surroundingContext = getSurroundingContext(content, region.startOffset, 400);
            
            // Check for form interaction context
            boolean isNearForm = surroundingContext.contains("<form") || 
                               surroundingContext.contains("<input") ||
                               surroundingContext.contains("<button");
            
            if (isNearForm) {
                analyzeFormRelatedLiveRegion(region, surroundingContext, file, holder);
            }
            
            // Check for search/filter context
            boolean isSearchContext = surroundingContext.toLowerCase().contains("search") ||
                                     surroundingContext.toLowerCase().contains("filter") ||
                                     surroundingContext.contains("type=\"search\"");
            
            if (isSearchContext) {
                analyzeSearchRelatedLiveRegion(region, surroundingContext, file, holder);
            }
            
            // Check for shopping/e-commerce context
            boolean isCommerceContext = surroundingContext.toLowerCase().contains("cart") ||
                                       surroundingContext.toLowerCase().contains("price") ||
                                       surroundingContext.toLowerCase().contains("checkout");
            
            if (isCommerceContext) {
                analyzeCommerceRelatedLiveRegion(region, surroundingContext, file, holder);
            }
            
            // Check for data visualization context
            boolean isDataContext = surroundingContext.contains("chart") ||
                                   surroundingContext.contains("graph") ||
                                   surroundingContext.contains("data-");
            
            if (isDataContext) {
                analyzeDataVisualizationLiveRegion(region, surroundingContext, file, holder);
            }
        }
    }
    
    private void analyzeUpdateFrequencyPatterns(String content, PsiFile file, ProblemsHolder holder) {
        // Check for high-frequency update patterns that might overwhelm users
        boolean hasRealTimeUpdates = REAL_TIME_PATTERN.matcher(content).find();
        
        if (hasRealTimeUpdates) {
            List<LiveRegionInfo> liveRegions = identifyAllLiveRegions(content);
            
            for (LiveRegionInfo region : liveRegions) {
                String regionContent = getRegionContent(content, region);
                
                // Check for indicators of frequent updates
                boolean hasTimestamp = regionContent.toLowerCase().contains("time") ||
                                      regionContent.contains("Date") ||
                                      regionContent.contains("timestamp");
                
                boolean hasCounters = regionContent.matches(".*\\d+.*") &&
                                     (regionContent.contains("count") || regionContent.contains("total"));
                
                boolean hasProgressIndicators = regionContent.contains("%") ||
                                               regionContent.contains("progress") ||
                                               regionContent.contains("loading");
                
                if (hasTimestamp || hasCounters || hasProgressIndicators) {
                    if (region.priority.equals("assertive")) {
                        registerProblem(holder, file, region.startOffset, region.startOffset + 100,
                            "Frequently updating content with assertive live region may overwhelm screen readers. Consider 'polite' or throttling",
                            null);// TODO: fix
                    }
                    
                    boolean hasThrottling = regionContent.contains("throttle") ||
                                          regionContent.contains("debounce") ||
                                          regionContent.contains("rate-limit");
                    
                    if (!hasThrottling) {
                        registerProblem(holder, file, region.startOffset, region.startOffset + 100,
                            "Rapidly updating content should implement throttling to prevent announcement spam",
                            null);// TODO: fix
                    }
                }
            }
            
            // Check for pause/resume functionality
            boolean hasPauseControl = content.contains("pause") || content.contains("stop");
            
            if (!hasPauseControl && liveRegions.size() > 0) {
                registerProblem(holder, file, 0, 100,
                    "Real-time updating content should provide user control to pause/resume announcements",
                    null);// TODO: fix
            }
        }
    }
    
    private void validateCriticalMessageHandling(String content, PsiFile file, ProblemsHolder holder) {
        // Identify critical message patterns and validate their handling
        Pattern criticalPattern = Pattern.compile(
            "<[^>]*(?:class=\"[^\"]*(?:critical|emergency|urgent|error|alert|danger)[^\"]*\")[^>]*>",
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher criticalMatcher = criticalPattern.matcher(content);
        
        while (criticalMatcher.find()) {
            String criticalElement = criticalMatcher.group();
            int offset = criticalMatcher.start();
            
            // Check for appropriate live region priority
            boolean hasAssertiveLiveRegion = criticalElement.contains("aria-live=\"assertive\"") ||
                                           criticalElement.contains("role=\"alert\"");
            
            if (!hasAssertiveLiveRegion) {
                String className = getAttributeValue(criticalElement, "class");
                registerProblem(holder, file, offset, offset + 100,
                    String.format("Critical message with class '%s' should use aria-live='assertive' or role='alert' for immediate attention",
                        className),
                    null);// TODO: fix
            }
            
            // Check for additional accessibility enhancements
            boolean hasVisualEnhancement = criticalElement.contains("icon") ||
                                          criticalElement.contains("symbol") ||
                                          criticalElement.contains("color");
            
            boolean hasAudioCue = criticalElement.contains("audio") ||
                                 criticalElement.contains("sound") ||
                                 criticalElement.contains("beep");
            
            if (!hasVisualEnhancement && !hasAudioCue) {
                registerProblem(holder, file, offset, offset + 100,
                    "Critical messages should have visual or audio cues in addition to ARIA announcements",
                    null);// TODO: fix
            }
        }
        
        // Check for error message patterns
        Pattern errorPattern = Pattern.compile(
            "<[^>]*(?:class=\"[^\"]*(?:error|invalid|required)[^\"]*\"|aria-invalid=\"true\")[^>]*>",
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher errorMatcher = errorPattern.matcher(content);
        Set<String> processedForms = new HashSet<>();
        
        while (errorMatcher.find()) {
            String errorElement = errorMatcher.group();
            int offset = errorMatcher.start();
            
            // Find associated form
            String formId = findAssociatedForm(content, offset);
            
            if (formId != null && !processedForms.contains(formId)) {
                processedForms.add(formId);
                
                // Check for form-level error summary
                boolean hasErrorSummary = content.contains("error-summary") ||
                                         content.contains("validation-summary");
                
                if (!hasErrorSummary) {
                    registerProblem(holder, file, offset, offset + 100,
                        "Form with validation errors should have an error summary with live region for screen readers",
                        null);// TODO: fix
                }
            }
        }
    }
    
    private void checkMultiModalAnnouncements(String content, PsiFile file, ProblemsHolder holder) {
        // Check for multi-modal announcement opportunities
        List<LiveRegionInfo> liveRegions = identifyAllLiveRegions(content);
        
        for (LiveRegionInfo region : liveRegions) {
            String regionContent = getRegionContent(content, region);
            
            // Check for success messages that could benefit from positive feedback
            boolean isSuccessMessage = regionContent.toLowerCase().contains("success") ||
                                      regionContent.toLowerCase().contains("saved") ||
                                      regionContent.toLowerCase().contains("complete");
            
            if (isSuccessMessage && region.priority.equals("assertive")) {
                // Success messages usually don't need to be assertive unless they're critical
                registerProblem(holder, file, region.startOffset, region.startOffset + 100,
                    "Success messages typically use aria-live='polite' unless immediate attention is required",
                    null);// TODO: fix
            }
            
            // Check for progress messages
            boolean isProgressMessage = regionContent.contains("%") ||
                                       regionContent.toLowerCase().contains("progress") ||
                                       regionContent.toLowerCase().contains("loading");
            
            if (isProgressMessage) {
                boolean hasVisualProgress = getSurroundingContext(content, region.startOffset, 200)
                    .contains("progress");
                
                if (!hasVisualProgress) {
                    registerProblem(holder, file, region.startOffset, region.startOffset + 100,
                        "Progress announcements should be accompanied by visual progress indicators",
                        null);// TODO: fix
                }
                
                // Check for completion announcements
                boolean hasCompletionAnnouncement = regionContent.toLowerCase().contains("complete") ||
                                                   regionContent.contains("100%") ||
                                                   regionContent.toLowerCase().contains("finished");
                
                if (!hasCompletionAnnouncement && regionContent.contains("%")) {
                    registerProblem(holder, file, region.startOffset, region.startOffset + 100,
                        "Progress indicators should announce completion clearly",
                        null);// TODO: fix
                }
            }
            
            // Check for quantity/count updates
            boolean hasQuantityUpdates = regionContent.matches(".*\\b\\d+\\s*(?:item|product|result|match).*");
            
            if (hasQuantityUpdates && !regionContent.toLowerCase().contains("found") && 
                !regionContent.toLowerCase().contains("available") && !regionContent.toLowerCase().contains("selected")) {
                
                registerProblem(holder, file, region.startOffset, region.startOffset + 100,
                    "Quantity updates should provide context (e.g., '5 items found', '3 products selected')",
                    null);// TODO: fix
            }
        }
    }
    
    // Helper methods for enhanced functionality
    private String analyzeDynamicContentType(String dynamicContent, String fullContent, int offset) {
        String lowerContent = dynamicContent.toLowerCase();
        String surroundingContext = getSurroundingContext(fullContent, offset, 300).toLowerCase();
        
        if (lowerContent.contains("error") || lowerContent.contains("invalid") || 
            surroundingContext.contains("validation")) {
            return "error";
        }
        
        if (lowerContent.contains("success") || lowerContent.contains("saved") || 
            lowerContent.contains("complete")) {
            return "success";
        }
        
        if (lowerContent.contains("loading") || lowerContent.contains("progress") || 
            lowerContent.contains("processing")) {
            return "status";
        }
        
        if (lowerContent.contains("count") || lowerContent.contains("result") || 
            lowerContent.contains("total")) {
            return "count";
        }
        
        if (surroundingContext.contains("search") || surroundingContext.contains("filter")) {
            return "search-result";
        }
        
        if (lowerContent.contains("chat") || lowerContent.contains("message") || 
            lowerContent.contains("notification")) {
            return "notification";
        }
        
        // Check if it's likely decorative (animations, visual effects)
        if (lowerContent.contains("fade") || lowerContent.contains("slide") || 
            lowerContent.contains("animation") || dynamicContent.length() < 10) {
            return "decorative";
        }
        
        return "general-update";
    }
    
    private String getRecommendedLiveRegion(String contentType) {
        switch (contentType) {
            case "error":
                return "aria-live='assertive' or role='alert'";
            case "success":
            case "status":
            case "count":
            case "search-result":
                return "aria-live='polite' or role='status'";
            case "notification":
                return "aria-live='polite'";
            case "general-update":
                return "aria-live='polite'";
            default:
                return "appropriate live region";
        }
    }
    
    private void validateLiveRegionAppropriateness(String regionContent, String fullContent, 
                                                  PsiFile file, ProblemsHolder holder, int offset) {
        
        String priority = "polite"; // default
        
        if (regionContent.contains("aria-live=\"assertive\"") || regionContent.contains("role=\"alert\"")) {
            priority = "assertive";
        }
        
        String contentType = analyzeDynamicContentType(regionContent, fullContent, offset);
        String recommendedRegion = getRecommendedLiveRegion(contentType);
        
        if (priority.equals("assertive") && 
            (contentType.equals("success") || contentType.equals("count") || contentType.equals("search-result"))) {
            
            registerProblem(holder, file, offset, offset + 100,
                String.format("%s content typically doesn't require assertive live region. Consider %s",
                    contentType, recommendedRegion),
                null);// TODO: fix
        }
        
        if (priority.equals("polite") && contentType.equals("error")) {
            registerProblem(holder, file, offset, offset + 100,
                String.format("%s content should use assertive live region for immediate attention. Consider %s",
                    contentType, recommendedRegion),
                null);// TODO: fix
        }
    }
    
    private void checkAjaxLiveRegionSupport(String content, PsiFile file, ProblemsHolder holder) {
        // Find AJAX patterns and check for corresponding live regions
        Matcher ajaxMatcher = AJAX_PATTERN.matcher(content);
        Set<String> checkedContainers = new HashSet<>();
        
        while (ajaxMatcher.find()) {
            int ajaxOffset = ajaxMatcher.start();
            
            // Find the container that will receive the AJAX content
            String containerId = findAjaxTargetContainer(content, ajaxOffset);
            
            if (containerId != null && !checkedContainers.contains(containerId)) {
                checkedContainers.add(containerId);
                
                boolean containerHasLiveRegion = checkContainerForLiveRegion(content, containerId);
                
                if (!containerHasLiveRegion) {
                    registerProblem(holder, file, ajaxOffset, ajaxOffset + 100,
                        String.format("AJAX content updates to '%s' should include live region for screen reader announcements",
                            containerId),
                        null);// TODO: fix
                }
            }
        }
    }
    
    private List<LiveRegionInfo> identifyAllLiveRegions(String content) {
        List<LiveRegionInfo> regions = new ArrayList<>();
        
        // Find explicit aria-live regions
        Matcher liveMatcher = ARIA_LIVE_PATTERN.matcher(content);
        while (liveMatcher.find()) {
            int start = findElementStart(content, liveMatcher.start());
            int end = findElementEnd(content, start);
            
            regions.add(new LiveRegionInfo(start, end, liveMatcher.group(1), 
                content.substring(start, Math.min(end, content.length()))));
        }
        
        // Find implicit live regions (role="alert", role="status", etc.)
        Pattern implicitPattern = Pattern.compile(
            "<[^>]*role\\s*=\\s*[\"'](alert|status|log)[\"'][^>]*>",
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher implicitMatcher = implicitPattern.matcher(content);
        while (implicitMatcher.find()) {
            int start = implicitMatcher.start();
            int end = findElementEnd(content, start);
            String role = implicitMatcher.group(1);
            String priority = role.equals("alert") ? "assertive" : "polite";
            
            regions.add(new LiveRegionInfo(start, end, priority, 
                content.substring(start, Math.min(end, content.length()))));
        }
        
        return regions;
    }
    
    private Map<String, List<LiveRegionInfo>> groupRegionsByContext(List<LiveRegionInfo> regions, String content) {
        Map<String, List<LiveRegionInfo>> groups = new HashMap<>();
        
        for (LiveRegionInfo region : regions) {
            String context = determineRegionContext(content, region.startOffset);
            groups.computeIfAbsent(context, k -> new ArrayList<>()).add(region);
        }
        
        return groups;
    }
    
    private String determineRegionContext(String content, int offset) {
        String surroundingContext = getSurroundingContext(content, offset, 300).toLowerCase();
        
        if (surroundingContext.contains("form")) return "form";
        if (surroundingContext.contains("search")) return "search";
        if (surroundingContext.contains("cart") || surroundingContext.contains("checkout")) return "commerce";
        if (surroundingContext.contains("chat") || surroundingContext.contains("message")) return "messaging";
        if (surroundingContext.contains("nav")) return "navigation";
        
        return "general";
    }
    
    private String getSurroundingContext(String content, int offset, int range) {
        int start = Math.max(0, offset - range);
        int end = Math.min(content.length(), offset + range);
        return content.substring(start, end);
    }
    
    private void analyzeFormRelatedLiveRegion(LiveRegionInfo region, String context, PsiFile file, ProblemsHolder holder) {
        boolean isValidationFeedback = context.toLowerCase().contains("error") ||
                                      context.toLowerCase().contains("invalid") ||
                                      context.contains("required");
        
        if (isValidationFeedback && !region.priority.equals("assertive")) {
            registerProblem(holder, file, region.startOffset, region.startOffset + 100,
                "Form validation feedback should use assertive live region for immediate user attention",
                null);// TODO: fix
        }
        
        boolean hasFieldAssociation = context.contains("aria-describedby") ||
                                     context.contains("for=");
        
        if (!hasFieldAssociation && isValidationFeedback) {
            registerProblem(holder, file, region.startOffset, region.startOffset + 100,
                "Form validation messages should be associated with their form fields using aria-describedby",
                null);// TODO: fix
        }
    }
    
    private void analyzeSearchRelatedLiveRegion(LiveRegionInfo region, String context, PsiFile file, ProblemsHolder holder) {
        boolean hasResultCount = context.matches(".*\\d+.*(?:result|match|found).*");
        
        if (!hasResultCount) {
            registerProblem(holder, file, region.startOffset, region.startOffset + 100,
                "Search result announcements should include result count for user context",
                null);// TODO: fix
        }
        
        if (region.priority.equals("assertive") && !context.toLowerCase().contains("error")) {
            registerProblem(holder, file, region.startOffset, region.startOffset + 100,
                "Search result updates typically use polite live region unless there's an error",
                null);// TODO: fix
        }
    }
    
    private void analyzeCommerceRelatedLiveRegion(LiveRegionInfo region, String context, PsiFile file, ProblemsHolder holder) {
        boolean isCartUpdate = context.toLowerCase().contains("cart") ||
                              context.toLowerCase().contains("added") ||
                              context.toLowerCase().contains("removed");
        
        if (isCartUpdate) {
            boolean hasQuantityInfo = context.matches(".*\\d+.*");
            
            if (!hasQuantityInfo) {
                registerProblem(holder, file, region.startOffset, region.startOffset + 100,
                    "Shopping cart updates should include quantity information",
                    null);// TODO: fix
            }
        }
        
        boolean isPriceUpdate = context.toLowerCase().contains("price") ||
                               context.contains("$") ||
                               context.contains("€") ||
                               context.contains("£");
        
        if (isPriceUpdate && region.priority.equals("assertive")) {
            registerProblem(holder, file, region.startOffset, region.startOffset + 100,
                "Price updates typically don't require assertive announcements unless there's an error",
                null);// TODO: fix
        }
    }
    
    private void analyzeDataVisualizationLiveRegion(LiveRegionInfo region, String context, PsiFile file, ProblemsHolder holder) {
        boolean hasDataDescription = context.toLowerCase().contains("chart") ||
                                    context.toLowerCase().contains("graph") ||
                                    context.toLowerCase().contains("data");
        
        if (hasDataDescription) {
            boolean hasTextualAlternative = context.contains("alt=") ||
                                           context.contains("aria-label") ||
                                           context.contains("aria-describedby");
            
            if (!hasTextualAlternative) {
                registerProblem(holder, file, region.startOffset, region.startOffset + 100,
                    "Data visualizations with live updates should provide textual descriptions",
                    null);// TODO: fix
            }
        }
    }
    
    private String getRegionContent(String content, LiveRegionInfo region) {
        return content.substring(region.startOffset, Math.min(region.endOffset, content.length()));
    }
    
    private String findAssociatedForm(String content, int errorOffset) {
        // Look backwards for form element
        int searchStart = Math.max(0, errorOffset - 1000);
        String searchContent = content.substring(searchStart, errorOffset);
        
        Pattern formPattern = Pattern.compile("<form[^>]*(?:id\\s*=\\s*[\"']([^\"']+)[\"'])?[^>]*>", 
            Pattern.CASE_INSENSITIVE);
        Matcher matcher = formPattern.matcher(searchContent);
        
        String formId = null;
        while (matcher.find()) {
            formId = matcher.group(1);
        }
        
        return formId != null ? formId : "form";
    }
    
    private String findAjaxTargetContainer(String content, int ajaxOffset) {
        // Simple heuristic to find target container ID from AJAX call context
        String ajaxContext = content.substring(Math.max(0, ajaxOffset - 100), 
            Math.min(content.length(), ajaxOffset + 100));
        
        Pattern targetPattern = Pattern.compile("(?:target|container)[\"']?:\\s*[\"']#?([^\"',\\s]+)[\"']?", 
            Pattern.CASE_INSENSITIVE);
        Matcher matcher = targetPattern.matcher(ajaxContext);
        
        return matcher.find() ? matcher.group(1) : null;
    }
    
    private boolean checkContainerForLiveRegion(String content, String containerId) {
        Pattern containerPattern = Pattern.compile(
            "<[^>]*id\\s*=\\s*[\"']" + Pattern.quote(containerId) + "[\"'][^>]*(?:aria-live|role\\s*=\\s*[\"'](?:alert|status))",
            Pattern.CASE_INSENSITIVE
        );
        
        return containerPattern.matcher(content).find();
    }
    
    // Enhanced LiveRegionInfo class
    private static class LiveRegionInfo {
        final int startOffset;
        final int endOffset;
        final String priority;
        final String content;
        
        LiveRegionInfo(int startOffset, int endOffset, String priority, String content) {
            this.startOffset = startOffset;
            this.endOffset = endOffset;
            this.priority = priority;
            this.content = content;
        }
        
        boolean containsOffset(int offset) {
            return offset >= startOffset && offset <= endOffset;
        }
    }
    
    // Enhanced quick fixes
    private static class AddContextualLiveRegionFix implements LocalQuickFix {
        private final String contentType;
        private final String recommendedRegion;
        
        AddContextualLiveRegionFix(String contentType, String recommendedRegion) {
            this.contentType = contentType;
            this.recommendedRegion = recommendedRegion;
        }
        
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add " + recommendedRegion + " for " + contentType + " content";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add contextual live region
        }
    }
    
    private static class RestructureNestedLiveRegionsFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Restructure nested live regions";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would restructure nested regions
        }
    }
    
    private static class ConsolidateAssertiveRegionsFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Consolidate competing assertive live regions";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would consolidate assertive regions
        }
    }
    
    private static class CoordinateLiveRegionsFix implements LocalQuickFix {
        private final String context;
        
        CoordinateLiveRegionsFix(String context) {
            this.context = context;
        }
        
        @NotNull
        @Override
        public String getFamilyName() {
            return "Coordinate live regions in " + context + " context";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would coordinate regions
        }
    }
    
    private static class ThrottleUpdatesFix implements LocalQuickFix {
        private final String currentPriority;
        
        ThrottleUpdatesFix(String currentPriority) {
            this.currentPriority = currentPriority;
        }
        
        @NotNull
        @Override
        public String getFamilyName() {
            return "Throttle frequent updates or change to polite";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would throttle updates
        }
    }
    
    private static class ImplementThrottlingFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Implement update throttling for rapid changes";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would implement throttling
        }
    }
    
    private static class AddUpdateControlsFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add pause/resume controls for real-time updates";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add update controls
        }
    }
    
    private static class UpgradeToCriticalLiveRegionFix implements LocalQuickFix {
        private final String className;
        
        UpgradeToCriticalLiveRegionFix(String className) {
            this.className = className;
        }
        
        @NotNull
        @Override
        public String getFamilyName() {
            return "Upgrade to assertive live region for critical message";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would upgrade to assertive
        }
    }
    
    private static class AddMultiModalAlertFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add visual/audio cues for critical message";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add multi-modal alerts
        }
    }
    
    private static class AddErrorSummaryFix implements LocalQuickFix {
        private final String formId;
        
        AddErrorSummaryFix(String formId) {
            this.formId = formId;
        }
        
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add error summary with live region to form";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add error summary
        }
    }
    
    private static class AdjustSuccessMessagePriorityFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Change success message to polite live region";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would adjust priority
        }
    }
    
    private static class AddVisualProgressIndicatorFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add visual progress indicator";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add visual progress
        }
    }
    
    private static class EnhanceCompletionAnnouncementFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Enhance progress completion announcement";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would enhance completion announcement
        }
    }
    
    private static class AddQuantityContextFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add context to quantity updates";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add quantity context
        }
    }
    
    private static class AdjustLiveRegionPriorityFix implements LocalQuickFix {
        private final String contentType;
        private final String recommendedPriority;
        
        AdjustLiveRegionPriorityFix(String contentType, String recommendedPriority) {
            this.contentType = contentType;
            this.recommendedPriority = recommendedPriority;
        }
        
        @NotNull
        @Override
        public String getFamilyName() {
            return "Change " + contentType + " to " + recommendedPriority + " live region";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would adjust priority
        }
    }
    
    private static class AddAjaxLiveRegionFix implements LocalQuickFix {
        private final String containerId;
        
        AddAjaxLiveRegionFix(String containerId) {
            this.containerId = containerId;
        }
        
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add live region to AJAX target container";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add AJAX live region
        }
    }
    
    private static class UpgradeFormValidationLiveRegionFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Upgrade form validation to assertive live region";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would upgrade validation live region
        }
    }
    
    private static class AssociateValidationWithFieldFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Associate validation message with form field";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would associate validation with field
        }
    }
    
    private static class AddSearchResultCountFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add result count to search announcements";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add result count
        }
    }
    
    private static class AdjustSearchLiveRegionFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Change search updates to polite live region";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would adjust search live region
        }
    }
    
    private static class AddCartQuantityInfoFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add quantity information to cart updates";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add cart quantity info
        }
    }
    
    private static class AdjustPriceUpdateLiveRegionFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Change price updates to polite live region";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would adjust price update live region
        }
    }
    
    private static class AddDataDescriptionFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add textual description for data visualization";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add data description
        }
    }
}