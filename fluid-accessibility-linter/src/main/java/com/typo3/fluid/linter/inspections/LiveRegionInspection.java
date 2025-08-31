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
import java.util.stream.Collectors;

public class LiveRegionInspection extends FluidAccessibilityInspection {
    
    private static final Set<String> LIVE_REGION_ROLES = new HashSet<>(Arrays.asList(
        "alert", "alertdialog", "log", "marquee", "status", "timer"
    ));
    
    private static final Set<String> VALID_ARIA_LIVE_VALUES = new HashSet<>(Arrays.asList(
        "polite", "assertive", "off"
    ));
    
    private static final Set<String> VALID_ARIA_RELEVANT_VALUES = new HashSet<>(Arrays.asList(
        "additions", "removals", "text", "all",
        "additions text", "additions removals", "additions removals text",
        "removals text", "text additions", "text removals",
        "removals additions", "removals additions text"
    ));
    
    protected static final Pattern ARIA_LIVE_PATTERN = Pattern.compile(
        "\\baria-live\\s*=\\s*[\"']([^\"']+)[\"']",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern ARIA_ATOMIC_PATTERN = Pattern.compile(
        "\\baria-atomic\\s*=\\s*[\"']([^\"']+)[\"']",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern ARIA_RELEVANT_PATTERN = Pattern.compile(
        "\\baria-relevant\\s*=\\s*[\"']([^\"']+)[\"']",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern ARIA_BUSY_PATTERN = Pattern.compile(
        "\\baria-busy\\s*=\\s*[\"']([^\"']+)[\"']",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern ROLE_PATTERN = Pattern.compile(
        "\\brole\\s*=\\s*[\"']([^\"']+)[\"']",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern LIVE_REGION_ELEMENT_PATTERN = Pattern.compile(
        "<[^>]+(?:aria-live\\s*=\\s*[\"'][^\"']+[\"']|role\\s*=\\s*[\"'](?:alert|status|log)[\"'])[^>]*>",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern STATUS_MESSAGE_PATTERN = Pattern.compile(
        "<[^>]*(?:class\\s*=\\s*[\"'][^\"']*(?:alert|message|notification|status|error|warning|success|info)[^\"']*[\"'])[^>]*>",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern ARIA_LABEL_PATTERN = Pattern.compile(
        "\\baria-label\\s*=\\s*[\"']([^\"']+)[\"']",
        Pattern.CASE_INSENSITIVE
    );
    
    @NotNull
    @Override
    public String getDisplayName() {
        return "Live region and dynamic content announcement issues";
    }
    
    @NotNull
    @Override
    public String getShortName() {
        return "LiveRegion";
    }
    
    @Override
    protected void inspectFile(@NotNull PsiFile file, @NotNull ProblemsHolder holder) {
        String content = file.getText();
        
        // Check aria-live attributes
        checkAriaLiveAttributes(content, file, holder);
        
        // Check live region roles
        checkLiveRegionRoles(content, file, holder);
        
        // Check aria-atomic usage
        checkAriaAtomic(content, file, holder);
        
        // Check aria-relevant usage
        checkAriaRelevant(content, file, holder);
        
        // Check aria-busy usage
        checkAriaBusy(content, file, holder);
        
        // Check for status messages without live regions
        checkStatusMessages(content, file, holder);
        
        // Check for conflicting live region settings
        checkConflictingLiveRegions(content, file, holder);

        // Enhanced context-aware checks
        prioritizeContentUpdates(content, file, holder);
        detectCompetingLiveRegions(content, file, holder);
        validateLiveRegionAppropriateness(content, file, holder);
        checkStatusMessagePatterns(content, file, holder);

        // Fluid-specific: flash messages should be inside a live region
        checkFlashMessagesAnnouncement(content, file, holder);
    }

    private void checkFlashMessagesAnnouncement(String content, PsiFile file, ProblemsHolder holder) {
        Pattern flashPattern = Pattern.compile("<f:flashMessages\\b[^>]*/?>", Pattern.CASE_INSENSITIVE);
        Matcher m = flashPattern.matcher(content);
        while (m.find()) {
            int start = m.start();
            int end = m.end();

            int contextStart = Math.max(0, start - 300);
            int contextEnd = Math.min(content.length(), end + 300);
            String context = content.substring(contextStart, contextEnd).toLowerCase();

            boolean hasWrapper = context.contains("role=\"status\"") ||
                                 context.contains("aria-live=\"polite\"") ||
                                 context.contains("role=\"alert\"") ||
                                 context.contains("aria-live=\"assertive\"");

            if (!hasWrapper) {
                registerProblem(holder, file, start, end,
                    "Flash messages should be announced via aria-live or role='status'",
                    new AddStatusRoleFix());
            }
        }
    }
    
    private void checkAriaLiveAttributes(String content, PsiFile file, ProblemsHolder holder) {
        Matcher liveMatcher = ARIA_LIVE_PATTERN.matcher(content);
        
        while (liveMatcher.find()) {
            String value = liveMatcher.group(1).toLowerCase().trim();
            
            if (!VALID_ARIA_LIVE_VALUES.contains(value)) {
                registerProblem(holder, file, liveMatcher.start(), liveMatcher.end(),
                    "Invalid aria-live value '" + value + "'. Must be 'polite', 'assertive', or 'off'",
                    new FixAriaLiveValueFix());
            }
            
            // Check for overuse of assertive
            if ("assertive".equals(value)) {
                // Find the element context
                int elementStart = findElementStart(content, liveMatcher.start());
                String elementContext = content.substring(Math.max(0, elementStart), 
                    Math.min(liveMatcher.end() + 50, content.length()));
                
                // Warn if not actually critical
                if (!elementContext.contains("error") && !elementContext.contains("alert") && 
                    !elementContext.contains("critical") && !elementContext.contains("emergency")) {
                    registerProblem(holder, file, liveMatcher.start(), liveMatcher.end(),
                        "Consider using aria-live='polite' instead of 'assertive' for non-critical updates",
                        new ChangeToPoliteFix());
                }
            }
        }
    }
    
    private void checkLiveRegionRoles(String content, PsiFile file, ProblemsHolder holder) {
        Matcher roleMatcher = ROLE_PATTERN.matcher(content);
        
        while (roleMatcher.find()) {
            String role = roleMatcher.group(1).toLowerCase().trim();
            
            if (LIVE_REGION_ROLES.contains(role)) {
                // Find the element
                int elementStart = findElementStart(content, roleMatcher.start());
                int elementEnd = findElementEnd(content, elementStart);
                String element = content.substring(elementStart, 
                    Math.min(elementEnd, content.length()));
                
                // Check for redundant aria-live with implicit live roles
                if (role.equals("alert") || role.equals("status")) {
                    Matcher liveMatcher = ARIA_LIVE_PATTERN.matcher(element);
                    if (liveMatcher.find()) {
                        String liveValue = liveMatcher.group(1);
                        
                        if ((role.equals("alert") && "assertive".equals(liveValue)) ||
                            (role.equals("status") && "polite".equals(liveValue))) {
                            registerProblem(holder, file, liveMatcher.start() + elementStart, 
                                liveMatcher.end() + elementStart,
                                "Redundant aria-live with role='" + role + "' (already implicit)",
                                new RemoveRedundantAriaLiveFix());
                        }
                    }
                }
                
                // Check for proper labeling
                checkLiveRegionLabeling(element, role, file, holder, elementStart);
            }
        }
    }
    
    private void checkAriaAtomic(String content, PsiFile file, ProblemsHolder holder) {
        Matcher atomicMatcher = ARIA_ATOMIC_PATTERN.matcher(content);
        
        while (atomicMatcher.find()) {
            String value = atomicMatcher.group(1).toLowerCase().trim();
            
            if (!"true".equals(value) && !"false".equals(value)) {
                registerProblem(holder, file, atomicMatcher.start(), atomicMatcher.end(),
                    "aria-atomic must be 'true' or 'false', not '" + value + "'",
                    new FixAriaAtomicValueFix());
            }
        }
    }
    
    private void checkAriaRelevant(String content, PsiFile file, ProblemsHolder holder) {
        Matcher relevantMatcher = ARIA_RELEVANT_PATTERN.matcher(content);
        
        while (relevantMatcher.find()) {
            String value = relevantMatcher.group(1).toLowerCase().trim();
            
            // Check for valid values or combinations
            if (!VALID_ARIA_RELEVANT_VALUES.contains(value)) {
                // Check if it's a valid combination
                String[] parts = value.split("\\s+");
                Set<String> uniqueParts = new HashSet<>(Arrays.asList(parts));
                
                boolean allValid = uniqueParts.stream()
                    .allMatch(part -> part.equals("additions") || part.equals("removals") || 
                              part.equals("text") || part.equals("all"));
                
                if (!allValid || uniqueParts.contains("all") && uniqueParts.size() > 1) {
                    registerProblem(holder, file, relevantMatcher.start(), relevantMatcher.end(),
                        "Invalid aria-relevant value '" + value + "'. Use 'additions', 'removals', 'text', 'all', or valid combinations",
                        new FixAriaRelevantValueFix());
                }
            }
        }
    }
    
    private void checkAriaBusy(String content, PsiFile file, ProblemsHolder holder) {
        Matcher busyMatcher = ARIA_BUSY_PATTERN.matcher(content);
        
        while (busyMatcher.find()) {
            String value = busyMatcher.group(1).toLowerCase().trim();
            
            if (!"true".equals(value) && !"false".equals(value)) {
                registerProblem(holder, file, busyMatcher.start(), busyMatcher.end(),
                    "aria-busy must be 'true' or 'false', not '" + value + "'",
                    new FixAriaBusyValueFix());
            }
            
            // Warn about persistent aria-busy="true"
            if ("true".equals(value)) {
                registerProblem(holder, file, busyMatcher.start(), busyMatcher.end(),
                    "Ensure aria-busy='true' is removed when loading completes",
                    null);
            }
        }
    }
    
    private void checkStatusMessages(String content, PsiFile file, ProblemsHolder holder) {
        Matcher statusMatcher = STATUS_MESSAGE_PATTERN.matcher(content);
        
        while (statusMatcher.find()) {
            String element = content.substring(statusMatcher.start(), statusMatcher.end());
            
            // Check if it has live region attributes
            boolean hasAriaLive = ARIA_LIVE_PATTERN.matcher(element).find();
            boolean hasLiveRole = false;
            
            Matcher roleMatcher = ROLE_PATTERN.matcher(element);
            if (roleMatcher.find()) {
                hasLiveRole = LIVE_REGION_ROLES.contains(roleMatcher.group(1).toLowerCase());
            }
            
            if (!hasAriaLive && !hasLiveRole) {
                // Check the class name to determine severity
                String classMatch = element.toLowerCase();
                
                if (classMatch.contains("error") || classMatch.contains("alert")) {
                    registerProblem(holder, file, statusMatcher.start(), statusMatcher.end(),
                        "Error/alert message should have role='alert' or aria-live='assertive' for screen reader announcement",
                        new AddAlertRoleFix());
                } else if (classMatch.contains("success") || classMatch.contains("info") || 
                           classMatch.contains("status") || classMatch.contains("message")) {
                    registerProblem(holder, file, statusMatcher.start(), statusMatcher.end(),
                        "Status message should have role='status' or aria-live='polite' for screen reader announcement",
                        new AddStatusRoleFix());
                }
            }
        }
    }
    
    private void checkConflictingLiveRegions(String content, PsiFile file, ProblemsHolder holder) {
        Matcher liveRegionMatcher = LIVE_REGION_ELEMENT_PATTERN.matcher(content);
        
        while (liveRegionMatcher.find()) {
            String element = liveRegionMatcher.group();
            
            // Check for multiple aria-live attributes (shouldn't happen but check anyway)
            Matcher liveMatcher = ARIA_LIVE_PATTERN.matcher(element);
            int liveCount = 0;
            while (liveMatcher.find()) {
                liveCount++;
            }
            
            if (liveCount > 1) {
                registerProblem(holder, file, liveRegionMatcher.start(), liveRegionMatcher.end(),
                    "Element has multiple aria-live attributes",
                    null);
            }
            
            // Check for nested live regions
            int elementEnd = findElementEnd(content, liveRegionMatcher.start());
            String elementContent = content.substring(liveRegionMatcher.start(), 
                Math.min(elementEnd, content.length()));
            
            Matcher nestedLiveMatcher = LIVE_REGION_ELEMENT_PATTERN.matcher(
                elementContent.substring(liveRegionMatcher.group().length()));
            
            if (nestedLiveMatcher.find()) {
                registerProblem(holder, file, liveRegionMatcher.start(), liveRegionMatcher.end(),
                    "Avoid nesting live regions - it can cause duplicate announcements",
                    null);
            }
        }
    }
    
    private void checkLiveRegionLabeling(String element, String role, PsiFile file, 
                                          ProblemsHolder holder, int baseOffset) {
        // Some live regions benefit from labels
        if (role.equals("log") || role.equals("status") || role.equals("timer")) {
            boolean hasLabel = ARIA_LABEL_PATTERN.matcher(element).find();
            
            if (!hasLabel) {
                registerProblem(holder, file, baseOffset, baseOffset + Math.min(100, element.length()),
                    "Consider adding aria-label to provide context for the " + role + " region",
                    new AddLiveRegionLabelFix(role));
            }
        }
    }
    
    
    
    
    private static class FixAriaLiveValueFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getName() { return getFamilyName(); }
        @NotNull
        @Override
        public String getFamilyName() {
            return "Fix aria-live value";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would fix the value
        }
    }
    
    private static class ChangeToPoliteFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getName() { return getFamilyName(); }
        @NotNull
        @Override
        public String getFamilyName() {
            return "Change to aria-live='polite'";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would change to polite
        }
    }
    
    private static class RemoveRedundantAriaLiveFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getName() { return getFamilyName(); }
        @NotNull
        @Override
        public String getFamilyName() {
            return "Remove redundant aria-live";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would remove redundant aria-live
        }
    }
    
    private static class FixAriaAtomicValueFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getName() { return getFamilyName(); }
        @NotNull
        @Override
        public String getFamilyName() {
            return "Fix aria-atomic value";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would fix the value
        }
    }
    
    private static class FixAriaRelevantValueFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getName() { return getFamilyName(); }
        @NotNull
        @Override
        public String getFamilyName() {
            return "Fix aria-relevant value";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would fix the value
        }
    }
    
    private static class FixAriaBusyValueFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getName() { return getFamilyName(); }
        @NotNull
        @Override
        public String getFamilyName() {
            return "Fix aria-busy value";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would fix the value
        }
    }
    
    private static class AddAlertRoleFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getName() { return getFamilyName(); }
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add role='alert' for screen reader announcement";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add role='alert'
        }
    }
    
    private static class AddStatusRoleFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getName() { return getFamilyName(); }
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add role='status' for screen reader announcement";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add role='status'
        }
    }
    
    private static class AddLiveRegionLabelFix implements LocalQuickFix {
        private final String role;
        
        AddLiveRegionLabelFix(String role) {
            this.role = role;
        }
        
        @NotNull
        @Override
        public String getName() { return getFamilyName(); }
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add aria-label to " + role + " region";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add aria-label
        }
    }
    
    // Enhanced context-aware validation methods
    private void prioritizeContentUpdates(String content, PsiFile file, ProblemsHolder holder) {
        // Find all live regions and analyze their context for priority
        Matcher liveRegionMatcher = LIVE_REGION_ELEMENT_PATTERN.matcher(content);
        
        while (liveRegionMatcher.find()) {
            String element = liveRegionMatcher.group();
            int offset = liveRegionMatcher.start();
            
            // Analyze context for priority
            String liveValue = "polite"; // default
            Matcher liveMatcher = ARIA_LIVE_PATTERN.matcher(element);
            if (liveMatcher.find()) {
                liveValue = liveMatcher.group(1);
            } else {
                // Check for implicit live roles
                Matcher roleMatcher = ROLE_PATTERN.matcher(element);
                if (roleMatcher.find()) {
                    String role = roleMatcher.group(1).toLowerCase();
                    if ("alert".equals(role)) {
                        liveValue = "assertive";
                    } else if ("status".equals(role)) {
                        liveValue = "polite";
                    }
                }
            }
            
            // Analyze surrounding content for context clues
            int contextStart = Math.max(0, offset - 200);
            int contextEnd = Math.min(content.length(), offset + 200);
            String context = content.substring(contextStart, contextEnd).toLowerCase();
            
            // Check for high-priority scenarios
            boolean hasErrorContext = context.contains("error") || context.contains("failed") ||
                                    context.contains("invalid") || context.contains("required");
            boolean hasSuccessContext = context.contains("success") || context.contains("saved") ||
                                      context.contains("complete") || context.contains("submitted");
            boolean hasProgressContext = context.contains("loading") || context.contains("progress") ||
                                       context.contains("processing") || context.contains("%");
            boolean hasValidationContext = context.contains("form") && (context.contains("submit") || context.contains("validate"));
            
            // Validate appropriateness of live region priority
            if ("assertive".equals(liveValue)) {
                if (!hasErrorContext && !hasValidationContext) {
                    // Check for emergency/critical indicators
                    boolean isCritical = context.contains("emergency") || context.contains("critical") ||
                                       context.contains("urgent") || context.contains("immediate");
                    
                    if (!isCritical) {
                        registerProblem(holder, file, offset, offset + 100,
                            "aria-live='assertive' should be reserved for critical errors or urgent messages. Consider 'polite' for status updates",
                            new ChangeToPoliteWithReasonFix("Non-critical content"));
                    }
                }
            } else if ("polite".equals(liveValue)) {
                if (hasErrorContext && hasValidationContext) {
                    registerProblem(holder, file, offset, offset + 100,
                        "Form validation errors should use aria-live='assertive' or role='alert' for immediate attention",
                        new ChangeToAssertiveFix("Form validation error"));
                }
            }
            
            // Check for appropriate content timing
            if (hasProgressContext) {
                if (!element.contains("aria-busy")) {
                    registerProblem(holder, file, offset, offset + 100,
                        "Progress indicators should include aria-busy='true' during loading and remove it when complete",
                        new AddAriaBusyFix());
                }
                
                // Check for percentage updates (should be throttled)
                if (context.contains("%")) {
                    registerProblem(holder, file, offset, offset + 100,
                        "Progress percentage updates should be throttled (e.g., every 5-10%) to avoid overwhelming screen readers",
                        null);
                }
            }
            
            // Check for success message timing
            if (hasSuccessContext && "assertive".equals(liveValue)) {
                registerProblem(holder, file, offset, offset + 100,
                    "Success messages can usually use aria-live='polite' unless immediate attention is required",
                    new ChangeToPoliteWithReasonFix("Success message"));
            }
        }
    }
    
    private void detectCompetingLiveRegions(String content, PsiFile file, ProblemsHolder holder) {
        List<LiveRegionInfo> liveRegions = new ArrayList<>();
        
        // Find all live regions
        Matcher liveRegionMatcher = LIVE_REGION_ELEMENT_PATTERN.matcher(content);
        
        while (liveRegionMatcher.find()) {
            String element = liveRegionMatcher.group();
            int offset = liveRegionMatcher.start();
            
            String priority = "polite";
            Matcher liveMatcher = ARIA_LIVE_PATTERN.matcher(element);
            if (liveMatcher.find()) {
                priority = liveMatcher.group(1);
            } else {
                Matcher roleMatcher = ROLE_PATTERN.matcher(element);
                if (roleMatcher.find() && "alert".equals(roleMatcher.group(1))) {
                    priority = "assertive";
                }
            }
            
            liveRegions.add(new LiveRegionInfo(offset, priority, element));
        }
        
        // Check for competing assertive regions
        List<LiveRegionInfo> assertiveRegions = liveRegions.stream()
            .filter(lr -> "assertive".equals(lr.priority))
            .collect(java.util.stream.Collectors.toList());
        
        if (assertiveRegions.size() > 2) {
            for (LiveRegionInfo region : assertiveRegions) {
                registerProblem(holder, file, region.offset, region.offset + 100,
                    String.format("Multiple assertive live regions detected (%d total). Consider using only one for critical messages", assertiveRegions.size()),
                    new ReviewLiveRegionPriorityFix());
            }
        }
        
        // Check for nearby live regions that might conflict
        for (int i = 0; i < liveRegions.size() - 1; i++) {
            LiveRegionInfo current = liveRegions.get(i);
            LiveRegionInfo next = liveRegions.get(i + 1);
            
            if (Math.abs(current.offset - next.offset) < 500) { // Within 500 characters
                if ("assertive".equals(current.priority) && "assertive".equals(next.priority)) {
                    registerProblem(holder, file, current.offset, current.offset + 100,
                        "Multiple assertive live regions in close proximity may interrupt each other's announcements",
                        new ConsolidateLiveRegionsFix());
                }
            }
        }
        
        // Check for empty live regions (potential dynamic update targets)
        for (LiveRegionInfo region : liveRegions) {
            int elementEnd = findElementEnd(content, region.offset);
            String regionContent = content.substring(region.offset, Math.min(elementEnd, content.length()));
            
            // Extract content between tags
            String innerContent = regionContent.replaceAll("<[^>]+>", "").trim();
            
            if (innerContent.isEmpty() && !region.element.contains("aria-label")) {
                registerProblem(holder, file, region.offset, region.offset + 100,
                    "Empty live region detected - ensure it will be populated with meaningful content when updated",
                    new AddLiveRegionPlaceholderFix());
            }
        }
    }
    
    private void validateLiveRegionAppropriateness(String content, PsiFile file, ProblemsHolder holder) {
        // Check for overuse of live regions
        int liveRegionCount = 0;
        Matcher liveRegionMatcher = LIVE_REGION_ELEMENT_PATTERN.matcher(content);
        
        while (liveRegionMatcher.find()) {
            liveRegionCount++;
        }
        
        if (liveRegionCount > 5) {
            registerProblem(holder, file, 0, 100,
                String.format("High number of live regions detected (%d). Consider consolidating to avoid overwhelming screen readers", liveRegionCount),
                null);
        }
        
        // Check for inappropriate use of live regions on static content
        Pattern staticContentPattern = Pattern.compile(
            "<(?:h[1-6]|p|div|span)[^>]*(?:aria-live|role=\"(?:alert|status)\")[^>]*>(?:[^<])+</(?:h[1-6]|p|div|span)>",
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher staticMatcher = staticContentPattern.matcher(content);
        
        while (staticMatcher.find()) {
            String element = staticMatcher.group();
            
            // Check if content appears static (no placeholders or dynamic indicators)
            boolean isDynamic = element.contains("{{}") || element.contains("<%") ||
                              element.contains("${}") || element.contains("<f:") ||
                              element.contains("data-content") || element.contains("data-text");
            
            if (!isDynamic) {
                String textContent = element.replaceAll("<[^>]+>", "").trim();
                if (textContent.length() > 20 && !textContent.toLowerCase().contains("loading") && 
                    !textContent.toLowerCase().contains("updating")) {
                    
                    registerProblem(holder, file, staticMatcher.start(), staticMatcher.end(),
                        "Live region appears to contain static content. Live regions should only be used for dynamic updates",
                        new RemoveLiveRegionFromStaticFix());
                }
            }
        }
        
        // Check for missing live regions on dynamic form feedback
        Pattern formPattern = Pattern.compile(
            "<form[^>]*>.*?</form>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );
        
        Matcher formMatcher = formPattern.matcher(content);
        
        while (formMatcher.find()) {
            String formContent = formMatcher.group();
            
            // Check for validation message containers without live regions
            Pattern messagePattern = Pattern.compile(
                "<[^>]*class=\"[^\"]*(?:error|message|validation|feedback)[^\"]*\"[^>]*>",
                Pattern.CASE_INSENSITIVE
            );
            
            Matcher messageMatcher = messagePattern.matcher(formContent);
            
            while (messageMatcher.find()) {
                String messageElement = messageMatcher.group();
                
                if (!messageElement.contains("aria-live") && !messageElement.contains("role=\"alert\"") &&
                    !messageElement.contains("role=\"status\"")) {
                    
                    registerProblem(holder, file, formMatcher.start() + messageMatcher.start(),
                        formMatcher.start() + messageMatcher.end(),
                        "Form validation message container should have aria-live or appropriate role for screen reader announcement",
                        new AddLiveRegionToFormFeedbackFix());
                }
            }
        }
    }
    
    private void checkStatusMessagePatterns(String content, PsiFile file, ProblemsHolder holder) {
        // Check for common status message patterns and their appropriateness
        Map<String, String> statusPatterns = new HashMap<>();
        statusPatterns.put("toast", "polite");
        statusPatterns.put("notification", "polite");
        statusPatterns.put("alert", "assertive");
        statusPatterns.put("snackbar", "polite");
        statusPatterns.put("banner", "polite");
        statusPatterns.put("modal", "assertive");
        
        for (Map.Entry<String, String> entry : statusPatterns.entrySet()) {
            String pattern = entry.getKey();
            String recommendedLive = entry.getValue();
            
            Pattern elementPattern = Pattern.compile(
                "<[^>]*class=\"[^\"]*" + pattern + "[^\"]*\"[^>]*>",
                Pattern.CASE_INSENSITIVE
            );
            
            Matcher elementMatcher = elementPattern.matcher(content);
            
            while (elementMatcher.find()) {
                String element = elementMatcher.group();
                
                boolean hasLiveRegion = element.contains("aria-live") || 
                                       element.contains("role=\"alert\"") ||
                                       element.contains("role=\"status\"");
                
                if (!hasLiveRegion) {
                    String suggestion = "assertive".equals(recommendedLive) ? 
                        "role='alert'" : "aria-live='polite' or role='status'";
                    
                    registerProblem(holder, file, elementMatcher.start(), elementMatcher.end(),
                        String.format("%s component should have %s for screen reader announcement",
                            pattern.substring(0, 1).toUpperCase() + pattern.substring(1), suggestion),
                        new AddRecommendedLiveRegionFix(recommendedLive));
                } else {
                    // Check if the live region level is appropriate
                    Matcher liveMatcher = ARIA_LIVE_PATTERN.matcher(element);
                    if (liveMatcher.find()) {
                        String currentLive = liveMatcher.group(1);
                        
                        if (!recommendedLive.equals(currentLive)) {
                            if ("assertive".equals(currentLive) && "polite".equals(recommendedLive)) {
                                registerProblem(holder, file, elementMatcher.start(), elementMatcher.end(),
                                    String.format("%s components typically use aria-live='polite' rather than 'assertive' to avoid interrupting user tasks",
                                        pattern.substring(0, 1).toUpperCase() + pattern.substring(1)),
                                    new ChangeToPoliteWithReasonFix(pattern + " component"));
                            }
                        }
                    }
                }
            }
        }
        
        // Check for loading states
        Pattern loadingPattern = Pattern.compile(
            "<[^>]*(?:class=\"[^\"]*loading[^\"]*\"|data-loading)[^>]*>",
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher loadingMatcher = loadingPattern.matcher(content);
        
        while (loadingMatcher.find()) {
            String element = loadingMatcher.group();
            
            if (!element.contains("aria-busy") && !element.contains("aria-live")) {
                registerProblem(holder, file, loadingMatcher.start(), loadingMatcher.end(),
                    "Loading indicator should include aria-busy='true' and consider aria-live='polite' for status updates",
                    new AddLoadingAriaAttributesFix());
            }
        }
    }
    
    // Helper class for live region information
    private static class LiveRegionInfo {
        final int offset;
        final String priority;
        final String element;
        
        LiveRegionInfo(int offset, String priority, String element) {
            this.offset = offset;
            this.priority = priority;
            this.element = element;
        }
    }
    
    // Additional quick fixes for enhanced functionality
    private static class ChangeToPoliteWithReasonFix implements LocalQuickFix {
        private final String reason;
        
        ChangeToPoliteWithReasonFix(String reason) {
            this.reason = reason;
        }
        
        @NotNull
        @Override
        public String getName() { return getFamilyName(); }
        @NotNull
        @Override
        public String getFamilyName() {
            return "Change to aria-live='polite' (" + reason + ")";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would change to polite
        }
    }
    
    private static class ChangeToAssertiveFix implements LocalQuickFix {
        private final String reason;
        
        ChangeToAssertiveFix(String reason) {
            this.reason = reason;
        }
        
        @NotNull
        @Override
        public String getName() { return getFamilyName(); }
        @NotNull
        @Override
        public String getFamilyName() {
            return "Change to aria-live='assertive' (" + reason + ")";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would change to assertive
        }
    }
    
    private static class AddAriaBusyFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getName() { return getFamilyName(); }
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add aria-busy='true' for loading state";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add aria-busy
        }
    }
    
    private static class ReviewLiveRegionPriorityFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getName() { return getFamilyName(); }
        @NotNull
        @Override
        public String getFamilyName() {
            return "Review and adjust live region priorities";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would help review priorities
        }
    }
    
    private static class ConsolidateLiveRegionsFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getName() { return getFamilyName(); }
        @NotNull
        @Override
        public String getFamilyName() {
            return "Consolidate nearby live regions";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would consolidate regions
        }
    }
    
    private static class AddLiveRegionPlaceholderFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getName() { return getFamilyName(); }
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add descriptive aria-label to live region";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add placeholder
        }
    }
    
    private static class RemoveLiveRegionFromStaticFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getName() { return getFamilyName(); }
        @NotNull
        @Override
        public String getFamilyName() {
            return "Remove live region from static content";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would remove live region
        }
    }
    
    private static class AddLiveRegionToFormFeedbackFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getName() { return getFamilyName(); }
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add aria-live to form feedback container";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add live region
        }
    }
    
    private static class AddRecommendedLiveRegionFix implements LocalQuickFix {
        private final String priority;
        
        AddRecommendedLiveRegionFix(String priority) {
            this.priority = priority;
        }
        
        @NotNull
        @Override
        public String getName() { return getFamilyName(); }
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add recommended live region (" + priority + ")";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add recommended live region
        }
    }
    
    private static class AddLoadingAriaAttributesFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getName() { return getFamilyName(); }
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add aria-busy and aria-live to loading indicator";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add loading attributes
        }
    }
}
