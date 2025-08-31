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

public class SkipLinksInspection extends FluidAccessibilityInspection {
    
    private static final Pattern SKIP_LINK_PATTERN = Pattern.compile(
        "<a\\s+[^>]*(?:class\\s*=\\s*[\"'][^\"']*skip[^\"']*[\"']|" +
        "href\\s*=\\s*[\"']#(?:main|content|nav)[\"'])[^>]*>",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern BODY_START_PATTERN = Pattern.compile(
        "<body[^>]*>\\s*(?:<[^>]+>\\s*)*",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern MAIN_CONTENT_ID_PATTERN = Pattern.compile(
        "id\\s*=\\s*[\"'](?:main|content|main-content)[\"']",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern HIDDEN_SKIP_LINK_PATTERN = Pattern.compile(
        "<a\\s+[^>]*class\\s*=\\s*[\"']([^\"']*(?:sr-only|visually-hidden|screen-reader)[^\"']*)[\"'][^>]*>.*?skip.*?</a>",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    
    private static final Pattern FIRST_FOCUSABLE_PATTERN = Pattern.compile(
        "<body[^>]*>\\s*(?:<(?:script|style|meta|link)[^>]*>.*?</(?:script|style|meta|link)>\\s*)*" +
        "(<(?:a|button|input|select|textarea)\\s+[^>]*>)",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    
    @NotNull
    @Override
    public String getDisplayName() {
        return "Missing or improperly implemented skip navigation links";
    }
    
    @NotNull
    @Override
    public String getShortName() {
        return "SkipLinks";
    }
    
    @Override
    protected void inspectFile(@NotNull PsiFile file, @NotNull ProblemsHolder holder) {
        String content = file.getText();
        
        if (!isMainTemplate(content)) {
            return;
        }
        
        checkSkipLinkPresence(content, file, holder);
        checkSkipLinkTarget(content, file, holder);
        checkSkipLinkVisibility(content, file, holder);
        checkSkipLinkPlacement(content, file, holder);
    }
    
    private boolean isMainTemplate(String content) {
        return content.contains("<body") || content.contains("<header") || 
               content.contains("<nav") || content.contains("<main");
    }
    
    private void checkSkipLinkPresence(String content, PsiFile file, ProblemsHolder holder) {
        boolean hasSkipLink = SKIP_LINK_PATTERN.matcher(content).find();
        boolean hasHiddenSkipLink = HIDDEN_SKIP_LINK_PATTERN.matcher(content).find();
        
        if (!hasSkipLink && !hasHiddenSkipLink) {
            boolean hasNav = content.contains("<nav") || content.contains("role=\"navigation\"");
            boolean hasMain = content.contains("<main") || content.contains("role=\"main\"");
            
            if (hasNav && hasMain) {
                registerProblem(holder, file, 0, 100,
                    "Page with navigation should have skip navigation links for keyboard users",
                    new AddSkipLinkFix());
            }
        }
    }
    
    private void checkSkipLinkTarget(String content, PsiFile file, ProblemsHolder holder) {
        Pattern skipHrefPattern = Pattern.compile(
            "<a\\s+[^>]*href\\s*=\\s*[\"']#([^\"']+)[\"'][^>]*>.*?(?:skip|jump).*?</a>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );
        
        Matcher skipMatcher = skipHrefPattern.matcher(content);
        while (skipMatcher.find()) {
            String targetId = skipMatcher.group(1);
            
            Pattern targetPattern = Pattern.compile(
                "id\\s*=\\s*[\"']" + Pattern.quote(targetId) + "[\"']",
                Pattern.CASE_INSENSITIVE
            );
            
            if (!targetPattern.matcher(content).find()) {
                registerProblem(holder, file, skipMatcher.start(), skipMatcher.end(),
                    "Skip link target '#" + targetId + "' does not exist in the document",
                    null);
            }
            
            if (!targetId.matches("(?i)(main|content|navigation|nav|search).*")) {
                registerProblem(holder, file, skipMatcher.start(), skipMatcher.end(),
                    "Skip link target ID '" + targetId + "' is not descriptive. Use IDs like 'main-content', 'navigation', etc.",
                    null);
            }
        }
    }
    
    private void checkSkipLinkVisibility(String content, PsiFile file, ProblemsHolder holder) {
        Matcher hiddenMatcher = HIDDEN_SKIP_LINK_PATTERN.matcher(content);
        
        while (hiddenMatcher.find()) {
            String classes = hiddenMatcher.group(1);
            
            Pattern focusPattern = Pattern.compile(
                "\\.(" + Pattern.quote(classes.split("\\s+")[0]) + ")(?::focus|:focus-visible)\\s*\\{[^}]*(?:position\\s*:\\s*(?:static|relative)|" +
                "clip\\s*:\\s*auto|width\\s*:\\s*auto|height\\s*:\\s*auto)",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL
            );
            
            boolean hasFocusStyles = focusPattern.matcher(content).find();
            
            if (!hasFocusStyles && (classes.contains("sr-only") || classes.contains("visually-hidden"))) {
                registerProblem(holder, file, hiddenMatcher.start(), hiddenMatcher.end(),
                    "Skip link with class '" + classes + "' should become visible on focus",
                    new AddFocusStylesFix());
            }
        }
    }
    
    private void checkSkipLinkPlacement(String content, PsiFile file, ProblemsHolder holder) {
        Matcher firstFocusableMatcher = FIRST_FOCUSABLE_PATTERN.matcher(content);
        
        if (firstFocusableMatcher.find()) {
            String firstElement = firstFocusableMatcher.group(1);
            
            boolean isSkipLink = firstElement.toLowerCase().contains("skip") || 
                                  firstElement.toLowerCase().contains("jump");
            
            if (!isSkipLink) {
                Matcher bodyMatcher = BODY_START_PATTERN.matcher(content);
                if (bodyMatcher.find()) {
                    int bodyEnd = bodyMatcher.end();
                    String afterBody = content.substring(bodyEnd, Math.min(bodyEnd + 500, content.length()));
                    
                    if (!SKIP_LINK_PATTERN.matcher(afterBody).find()) {
                        registerProblem(holder, file, firstFocusableMatcher.start(), firstFocusableMatcher.end(),
                            "Skip links should be the first focusable element in the page",
                            null);
                    }
                }
            }
        }
    }
    
    
    private static class AddSkipLinkFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getName() { return getFamilyName(); }
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add skip navigation link";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add skip link at beginning of body
        }
    }
    
    private static class AddFocusStylesFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getName() { return getFamilyName(); }
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add focus styles to make skip link visible";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add CSS for focus visibility
        }
    }
}
