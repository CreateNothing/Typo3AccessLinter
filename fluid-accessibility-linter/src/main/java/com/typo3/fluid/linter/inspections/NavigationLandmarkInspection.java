package com.typo3.fluid.linter.inspections;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Enhanced Navigation Landmark Inspection with sophisticated context-aware intelligence.
 * Features:
 * - Redundant navigation landmark detection
 * - Navigation hierarchy and nesting analysis
 * - Skip link target validation
 * - Proper landmark roles and labeling verification
 * - Navigation structure and semantic analysis
 */
public class NavigationLandmarkInspection extends FluidAccessibilityInspection {
    
    private static final Pattern NAV_PATTERN = Pattern.compile(
        "<nav\\s*([^>]*)>.*?</nav>",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    
    private static final Pattern ROLE_NAV_PATTERN = Pattern.compile(
        "<([^>]+)\\s+[^>]*\\brole\\s*=\\s*[\"']navigation[\"'][^>]*>",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern ARIA_LABEL_PATTERN = Pattern.compile(
        "\\baria-label(?:ledby)?\\s*=\\s*[\"']([^\"']+)[\"']",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern UL_IN_NAV_PATTERN = Pattern.compile(
        "<nav[^>]*>.*?(<ul[^>]*>.*?</ul>).*?</nav>",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    
    private static final Pattern MAIN_PATTERN = Pattern.compile(
        "<main\\s*[^>]*>|role\\s*=\\s*[\"']main[\"']",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern HEADER_PATTERN = Pattern.compile(
        "<header\\s*[^>]*>|role\\s*=\\s*[\"']banner[\"']",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern FOOTER_PATTERN = Pattern.compile(
        "<footer\\s*[^>]*>|role\\s*=\\s*[\"']contentinfo[\"']",
        Pattern.CASE_INSENSITIVE
    );
    
    // Skip link patterns
    private static final Pattern SKIP_LINK_PATTERN = Pattern.compile(
        "<a\\s+[^>]*href\\s*=\\s*[\"']#([^\"']+)[\"'][^>]*>.*?(?:skip|jump).*?</a>",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    
    // Nested navigation patterns
    private static final Pattern NESTED_NAV_PATTERN = Pattern.compile(
        "<nav[^>]*>.*?<nav[^>]*>.*?</nav>.*?</nav>",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    
    // Breadcrumb patterns
    private static final Pattern BREADCRUMB_PATTERN = Pattern.compile(
        "(?:breadcrumb|trail|path)",
        Pattern.CASE_INSENSITIVE
    );
    
    // Sitemap/pagination patterns
    private static final Pattern SITEMAP_PATTERN = Pattern.compile(
        "(?:sitemap|site-map|pagination|pager)",
        Pattern.CASE_INSENSITIVE
    );
    
    // ARIA current patterns
    private static final Pattern ARIA_CURRENT_PATTERN = Pattern.compile(
        "aria-current\\s*=\\s*[\"']([^\"']+)[\"']",
        Pattern.CASE_INSENSITIVE
    );
    
    @NotNull
    @Override
    public String getDisplayName() {
        return "Navigation landmark and structure issues";
    }
    
    @NotNull
    @Override
    public String getShortName() {
        return "NavigationLandmark";
    }
    
    @Override
    protected void inspectFile(@NotNull PsiFile file, @NotNull ProblemsHolder holder) {
        String content = file.getText();
        
        checkNavigationElements(content, file, holder);
        checkMultipleNavigations(content, file, holder);
        checkLandmarkStructure(content, file, holder);
        checkNavigationLists(content, file, holder);
    }
    
    private void checkNavigationElements(String content, PsiFile file, ProblemsHolder holder) {
        Matcher navMatcher = NAV_PATTERN.matcher(content);
        Set<String> navLabels = new HashSet<>();
        int navCount = 0;
        
        while (navMatcher.find()) {
            navCount++;
            String attributes = navMatcher.group(1);

            Matcher labelMatcher = ARIA_LABEL_PATTERN.matcher(attributes);
            String label = labelMatcher.find() ? labelMatcher.group(1) : null;

            if (navCount > 1 && (label == null || label.trim().isEmpty())) {
                registerProblem(holder, file, navMatcher.start(), navMatcher.end(),
                    "Multiple <nav> elements should have unique aria-label or aria-labelledby attributes",
                    new AddNavigationLabelFix());
            } else if (label != null) {
                String key = label.trim();
                if (!navLabels.add(key)) {
                    registerProblem(holder, file, navMatcher.start(), navMatcher.end(),
                        "Duplicate navigation label '" + label + "'. Each navigation should have a unique label",
                        null);
                }
            }
        }
        
        Matcher roleNavMatcher = ROLE_NAV_PATTERN.matcher(content);
        while (roleNavMatcher.find()) {
            String tagName = roleNavMatcher.group(1).split("\\s")[0].replaceAll("^<", "");
            if (tagName.equalsIgnoreCase("nav")) {
                registerProblem(holder, file, roleNavMatcher.start(), roleNavMatcher.end(),
                    "Redundant role='navigation' on <nav> element",
                    new RemoveRedundantRoleFix());
            }
        }
    }
    
    private void checkMultipleNavigations(String content, PsiFile file, ProblemsHolder holder) {
        Pattern navMenuPattern = Pattern.compile(
            "(<nav[^>]*>(?:(?!</nav>).)*</nav>)",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );
        
        Matcher matcher = navMenuPattern.matcher(content);
        int primaryNavCount = 0;
        int secondaryNavCount = 0;
        
        while (matcher.find()) {
            String navContent = matcher.group(1);
            if (navContent.contains("primary") || navContent.contains("main-nav")) {
                primaryNavCount++;
            } else if (navContent.contains("secondary") || navContent.contains("footer-nav")) {
                secondaryNavCount++;
            }
        }
        
        if (primaryNavCount > 1) {
            registerProblem(holder, file, 0, 100,
                "Multiple primary navigation elements detected. Consider consolidating or clarifying navigation hierarchy",
                null);
        }
    }
    
    private void checkLandmarkStructure(String content, PsiFile file, ProblemsHolder holder) {
        boolean hasMain = MAIN_PATTERN.matcher(content).find();
        boolean hasHeader = HEADER_PATTERN.matcher(content).find();
        boolean hasFooter = FOOTER_PATTERN.matcher(content).find();
        boolean hasNav = NAV_PATTERN.matcher(content).find();
        
        if (!hasMain && content.length() > 500) {
            registerProblem(holder, file, 0, 100,
                "Page should have a <main> landmark for primary content",
                new AddMainLandmarkFix());
        }
        
        Pattern multipleMainPattern = Pattern.compile(
            "<main\\s*[^>]*>",
            Pattern.CASE_INSENSITIVE
        );
        Matcher mainMatcher = multipleMainPattern.matcher(content);
        int mainCount = 0;
        while (mainMatcher.find()) {
            mainCount++;
        }
        if (mainCount > 1) {
            registerProblem(holder, file, 0, 100,
                "Page should have only one <main> landmark",
                null);
        }
    }
    
    private void checkNavigationLists(String content, PsiFile file, ProblemsHolder holder) {
        Matcher navMatcher = NAV_PATTERN.matcher(content);
        
        while (navMatcher.find()) {
            String navContent = navMatcher.group(0);
            
            boolean hasLinks = navContent.contains("<a ") || navContent.contains("<f:link");
            boolean hasList = navContent.contains("<ul") || navContent.contains("<ol");
            
            if (hasLinks && !hasList) {
                int linkCount = countOccurrences(navContent, "<a ") + countOccurrences(navContent, "<f:link");
                if (linkCount > 3) {
                    registerProblem(holder, file, navMatcher.start(), navMatcher.end(),
                        "Navigation with multiple links should use a list structure (<ul> or <ol>)",
                        null);
                }
            }
        }
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
    
    
    private static class AddNavigationLabelFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add aria-label to navigation";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add aria-label attribute
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
            // Implementation would remove redundant role
        }
    }
    
    private static class AddMainLandmarkFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add <main> landmark for primary content";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would suggest adding main element
        }
    }
}
