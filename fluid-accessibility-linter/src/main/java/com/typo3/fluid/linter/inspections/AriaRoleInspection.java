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

/**
 * Enhanced ARIA Role Inspection with sophisticated context-aware intelligence.
 * Features:
 * - Implicit role detection and unnecessary explicit role identification
 * - Role appropriateness validation for context
 * - Conflicting semantic meaning detection
 * - Role change accessibility contract validation
 * - Context-sensitive role recommendations
 */
public class AriaRoleInspection extends FluidAccessibilityInspection {
    
    private static final Set<String> VALID_ROLES = new HashSet<>(Arrays.asList(
        // Widget roles
        "button", "checkbox", "combobox", "dialog", "gridcell", "link", "listbox", 
        "menu", "menubar", "menuitem", "menuitemcheckbox", "menuitemradio", 
        "option", "progressbar", "radio", "scrollbar", "searchbox", "separator",
        "slider", "spinbutton", "switch", "tab", "tablist", "tabpanel", "textbox",
        "timer", "tooltip", "tree", "treegrid", "treeitem",
        
        // Document structure roles
        "article", "application", "blockquote", "caption", "cell", "code",
        "columnheader", "definition", "deletion", "directory", "document", 
        "emphasis", "feed", "figure", "generic", "group", "heading", "img", 
        "insertion", "list", "listitem", "log", "main", "marquee", "math", 
        "meter", "navigation", "none", "note", "paragraph", "presentation", 
        "region", "row", "rowgroup", "rowheader", "separator", "status", 
        "strong", "subscript", "superscript", "table", "term", "time", "toolbar",
        
        // Landmark roles
        "banner", "complementary", "contentinfo", "form", "main", "navigation",
        "region", "search",
        
        // Live region roles
        "alert", "alertdialog", "log", "marquee", "status", "timer"
    ));
    
    private static final Set<String> ABSTRACT_ROLES = new HashSet<>(Arrays.asList(
        "command", "composite", "input", "landmark", "range", "roletype", 
        "section", "sectionhead", "select", "structure", "widget", "window"
    ));
    
    private static final Map<String, Set<String>> REQUIRED_PROPERTIES = new HashMap<>();
    private static final Map<String, Set<String>> REQUIRED_STATES = new HashMap<>();
    
    // Implicit roles for HTML elements
    private static final Map<String, String> IMPLICIT_ROLES = new HashMap<>();
    
    // Context-sensitive role mappings
    private static final Map<String, Set<String>> CONTEXT_APPROPRIATE_ROLES = new HashMap<>();
    
    // Roles that change element semantics significantly
    private static final Set<String> SEMANTIC_CHANGING_ROLES = new HashSet<>();
    
    static {
        // Required properties for specific roles
        REQUIRED_PROPERTIES.put("checkbox", Set.of("aria-checked"));
        REQUIRED_PROPERTIES.put("combobox", Set.of("aria-expanded"));
        REQUIRED_PROPERTIES.put("heading", Set.of("aria-level"));
        REQUIRED_PROPERTIES.put("menuitemcheckbox", Set.of("aria-checked"));
        REQUIRED_PROPERTIES.put("menuitemradio", Set.of("aria-checked"));
        REQUIRED_PROPERTIES.put("option", Set.of("aria-selected"));
        REQUIRED_PROPERTIES.put("radio", Set.of("aria-checked"));
        REQUIRED_PROPERTIES.put("slider", Set.of("aria-valuenow", "aria-valuemin", "aria-valuemax"));
        REQUIRED_PROPERTIES.put("spinbutton", Set.of("aria-valuenow", "aria-valuemin", "aria-valuemax"));
        REQUIRED_PROPERTIES.put("switch", Set.of("aria-checked"));
        REQUIRED_PROPERTIES.put("tab", Set.of("aria-selected"));
        REQUIRED_PROPERTIES.put("tree", Set.of("aria-orientation"));
        REQUIRED_PROPERTIES.put("treegrid", Set.of("aria-orientation"));
        
        // Required states for specific roles
        REQUIRED_STATES.put("dialog", Set.of("aria-labelledby", "aria-label"));
        REQUIRED_STATES.put("alertdialog", Set.of("aria-labelledby", "aria-label"));
        REQUIRED_STATES.put("progressbar", Set.of("aria-valuenow"));
        REQUIRED_STATES.put("scrollbar", Set.of("aria-controls", "aria-valuenow", "aria-valuemin", "aria-valuemax"));
        
        // Initialize implicit roles
        initializeImplicitRoles();
        
        // Initialize context-appropriate roles
        initializeContextRoles();
        
        // Initialize semantic-changing roles
        initializeSemanticChangingRoles();
    }
    
    private static final Pattern ROLE_PATTERN = Pattern.compile(
        "\\brole\\s*=\\s*[\"']([^\"']+)[\"']",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern ELEMENT_WITH_ROLE_PATTERN = Pattern.compile(
        "<([^>]+\\brole\\s*=\\s*[\"'][^\"']+[\"'][^>]*)>",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern ARIA_ATTRIBUTE_PATTERN = Pattern.compile(
        "\\b(aria-[\\w-]+)\\s*=\\s*[\"']([^\"']*)[\"']",
        Pattern.CASE_INSENSITIVE
    );
    
    @NotNull
    @Override
    public String getDisplayName() {
        return "Invalid or misused ARIA roles";
    }
    
    @NotNull
    @Override
    public String getShortName() {
        return "AriaRole";
    }
    
    @Override
    protected void inspectFile(@NotNull PsiFile file, @NotNull ProblemsHolder holder) {
        String content = file.getText();
        
        // Check for invalid or abstract roles
        checkInvalidRoles(content, file, holder);
        
        // Check for required properties
        checkRequiredProperties(content, file, holder);
        
        // Check for redundant ARIA
        checkRedundantAria(content, file, holder);
        
        // Check for conflicting ARIA
        checkConflictingAria(content, file, holder);
        
        // Enhanced context-aware checks
        checkImplicitRoles(content, file, holder);
        checkRoleAppropriateness(content, file, holder);
        checkSemanticConsistency(content, file, holder);
        checkAccessibilityContracts(content, file, holder);
    }
    
    private void checkInvalidRoles(String content, PsiFile file, ProblemsHolder holder) {
        Matcher roleMatcher = ROLE_PATTERN.matcher(content);
        
        while (roleMatcher.find()) {
            String role = roleMatcher.group(1).toLowerCase().trim();
            
            if (ABSTRACT_ROLES.contains(role)) {
                registerProblem(holder, file, roleMatcher.start(), roleMatcher.end(),
                    "Abstract ARIA role '" + role + "' should not be used directly",
                    new RemoveRoleFix(role));
            } else if (!VALID_ROLES.contains(role)) {
                // Check for multiple roles (space-separated)
                String[] roles = role.split("\\s+");
                if (roles.length > 1) {
                    registerProblem(holder, file, roleMatcher.start(), roleMatcher.end(),
                        "Multiple ARIA roles are not allowed. Use a single role",
                        new UseFirstRoleFix(roles[0]));
                } else {
                    registerProblem(holder, file, roleMatcher.start(), roleMatcher.end(),
                        "Invalid ARIA role '" + role + "'",
                        new RemoveRoleFix(role));
                }
            }
        }
    }
    
    private void checkRequiredProperties(String content, PsiFile file, ProblemsHolder holder) {
        Matcher elementMatcher = ELEMENT_WITH_ROLE_PATTERN.matcher(content);
        
        while (elementMatcher.find()) {
            String elementContent = elementMatcher.group(1);
            
            // Extract role
            Matcher roleMatcher = ROLE_PATTERN.matcher(elementContent);
            if (roleMatcher.find()) {
                String role = roleMatcher.group(1).toLowerCase().trim();
                
                // Check required properties
                Set<String> requiredProps = REQUIRED_PROPERTIES.get(role);
                if (requiredProps != null) {
                    Set<String> presentProps = extractAriaAttributes(elementContent);
                    
                    for (String required : requiredProps) {
                        if (!presentProps.contains(required)) {
                            registerProblem(holder, file, elementMatcher.start(), elementMatcher.end(),
                                "Role '" + role + "' requires '" + required + "' property",
                                new AddAriaPropertyFix(required, role));
                        }
                    }
                }
                
                // Check required states (at least one must be present)
                Set<String> requiredStates = REQUIRED_STATES.get(role);
                if (requiredStates != null) {
                    Set<String> presentProps = extractAriaAttributes(elementContent);
                    boolean hasRequiredState = requiredStates.stream()
                        .anyMatch(presentProps::contains);
                    
                    if (!hasRequiredState) {
                        String stateList = String.join(" or ", requiredStates);
                        registerProblem(holder, file, elementMatcher.start(), elementMatcher.end(),
                            "Role '" + role + "' requires " + stateList,
                            new AddAriaPropertyFix(requiredStates.iterator().next(), role));
                    }
                }
            }
        }
    }
    
    private void checkRedundantAria(String content, PsiFile file, ProblemsHolder holder) {
        // Check for semantic HTML with redundant ARIA roles
        Pattern redundantPatterns = Pattern.compile(
            "<(button|nav|main|header|footer|aside|section|article|form|img|ul|ol|li|a|input|select|textarea)" +
            "\\s+[^>]*\\brole\\s*=\\s*[\"']\\1[\"'][^>]*>",
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher redundantMatcher = redundantPatterns.matcher(content);
        while (redundantMatcher.find()) {
            String element = redundantMatcher.group(1);
            registerProblem(holder, file, redundantMatcher.start(), redundantMatcher.end(),
                "Redundant ARIA role on <" + element + "> element. The element already has this semantic meaning",
                new RemoveRedundantRoleFix());
        }
        
        // Special cases for redundant ARIA
        checkRedundantNavRole(content, file, holder);
        checkRedundantButtonRole(content, file, holder);
    }
    
    private void checkRedundantNavRole(String content, PsiFile file, ProblemsHolder holder) {
        Pattern navPattern = Pattern.compile(
            "<nav\\s+[^>]*\\brole\\s*=\\s*[\"']navigation[\"'][^>]*>",
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher matcher = navPattern.matcher(content);
        while (matcher.find()) {
            registerProblem(holder, file, matcher.start(), matcher.end(),
                "Redundant role='navigation' on <nav> element",
                new RemoveRedundantRoleFix());
        }
    }
    
    private void checkRedundantButtonRole(String content, PsiFile file, ProblemsHolder holder) {
        Pattern buttonPattern = Pattern.compile(
            "<button\\s+[^>]*\\brole\\s*=\\s*[\"']button[\"'][^>]*>",
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher matcher = buttonPattern.matcher(content);
        while (matcher.find()) {
            registerProblem(holder, file, matcher.start(), matcher.end(),
                "Redundant role='button' on <button> element",
                new RemoveRedundantRoleFix());
        }
    }
    
    private void checkConflictingAria(String content, PsiFile file, ProblemsHolder holder) {
        // Check for conflicting ARIA attributes
        Pattern hiddenWithInteractive = Pattern.compile(
            "<[^>]+aria-hidden\\s*=\\s*[\"']true[\"'][^>]*(?:tabindex\\s*=\\s*[\"']0[\"']|role\\s*=\\s*[\"'](?:button|link|checkbox|radio)[\"'])[^>]*>",
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher conflictMatcher = hiddenWithInteractive.matcher(content);
        while (conflictMatcher.find()) {
            registerProblem(holder, file, conflictMatcher.start(), conflictMatcher.end(),
                "Element with aria-hidden='true' should not be interactive",
                new RemoveAriaHiddenFix());
        }
    }
    
    private Set<String> extractAriaAttributes(String elementContent) {
        Set<String> attributes = new HashSet<>();
        Matcher ariaMatcher = ARIA_ATTRIBUTE_PATTERN.matcher(elementContent);
        
        while (ariaMatcher.find()) {
            attributes.add(ariaMatcher.group(1).toLowerCase());
        }
        
        return attributes;
    }
    
    
    private static class RemoveRoleFix implements LocalQuickFix {
        private final String role;
        
        RemoveRoleFix(String role) {
            this.role = role;
        }
        
        @NotNull
        @Override
        public String getFamilyName() {
            return "Remove invalid ARIA role";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would remove the role attribute
        }
    }
    
    private static class UseFirstRoleFix implements LocalQuickFix {
        private final String firstRole;
        
        UseFirstRoleFix(String firstRole) {
            this.firstRole = firstRole;
        }
        
        @NotNull
        @Override
        public String getFamilyName() {
            return "Use only the first role: " + firstRole;
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would keep only the first role
        }
    }
    
    private static class AddAriaPropertyFix implements LocalQuickFix {
        private final String property;
        private final String role;
        
        AddAriaPropertyFix(String property, String role) {
            this.property = property;
            this.role = role;
        }
        
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add required " + property + " for role='" + role + "'";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add the required ARIA property
        }
    }
    
    private static class RemoveRedundantRoleFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Remove redundant ARIA role";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would remove the redundant role attribute
        }
    }
    
    private static class RemoveAriaHiddenFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Remove aria-hidden from interactive element";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would remove aria-hidden attribute
        }
    }
    
    private static void initializeImplicitRoles() {
        IMPLICIT_ROLES.put("a", "link");
        IMPLICIT_ROLES.put("button", "button");
        IMPLICIT_ROLES.put("nav", "navigation");
        IMPLICIT_ROLES.put("main", "main");
        IMPLICIT_ROLES.put("header", "banner");
        IMPLICIT_ROLES.put("footer", "contentinfo");
        IMPLICIT_ROLES.put("aside", "complementary");
        IMPLICIT_ROLES.put("article", "article");
        IMPLICIT_ROLES.put("section", "region");
        IMPLICIT_ROLES.put("form", "form");
        IMPLICIT_ROLES.put("img", "img");
        IMPLICIT_ROLES.put("ul", "list");
        IMPLICIT_ROLES.put("ol", "list");
        IMPLICIT_ROLES.put("li", "listitem");
        IMPLICIT_ROLES.put("h1", "heading");
        IMPLICIT_ROLES.put("h2", "heading");
        IMPLICIT_ROLES.put("h3", "heading");
        IMPLICIT_ROLES.put("h4", "heading");
        IMPLICIT_ROLES.put("h5", "heading");
        IMPLICIT_ROLES.put("h6", "heading");
        IMPLICIT_ROLES.put("input", "textbox"); // varies by type
        IMPLICIT_ROLES.put("textarea", "textbox");
        IMPLICIT_ROLES.put("select", "listbox");
    }
    
    private static void initializeContextRoles() {
        // Navigation context
        CONTEXT_APPROPRIATE_ROLES.put("nav", Set.of("navigation", "menu", "menubar", "tablist"));
        
        // Form context
        CONTEXT_APPROPRIATE_ROLES.put("form", Set.of("form", "search", "group"));
        
        // Interactive context
        CONTEXT_APPROPRIATE_ROLES.put("interactive", Set.of("button", "link", "tab", "menuitem", "option"));
        
        // Content context
        CONTEXT_APPROPRIATE_ROLES.put("content", Set.of("article", "region", "complementary", "banner", "contentinfo"));
        
        // List context
        CONTEXT_APPROPRIATE_ROLES.put("list", Set.of("list", "listbox", "menu", "menubar", "tablist", "tree"));
    }
    
    private static void initializeSemanticChangingRoles() {
        SEMANTIC_CHANGING_ROLES.addAll(Set.of(
            "button", "link", "tab", "menuitem", "option", "checkbox", "radio",
            "textbox", "combobox", "listbox", "tree", "grid", "dialog", "alertdialog",
            "application", "document", "presentation", "none"
        ));
    }
    
    /**
     * Detect implicit roles and identify unnecessary explicit roles
     */
    private void checkImplicitRoles(String content, PsiFile file, ProblemsHolder holder) {
        Pattern elementWithRolePattern = Pattern.compile(
            "<(\\w+)[^>]*\\brole\\s*=\\s*[\"']([^\"']+)[\"'][^>]*>",
            Pattern.CASE_INSENSITIVE);
        
        Matcher matcher = elementWithRolePattern.matcher(content);
        
        while (matcher.find()) {
            String elementName = matcher.group(1).toLowerCase();
            String explicitRole = matcher.group(2).toLowerCase().trim();
            String implicitRole = IMPLICIT_ROLES.get(elementName);
            
            if (implicitRole != null) {
                if (implicitRole.equals(explicitRole)) {
                    registerProblem(holder, file, matcher.start(), matcher.end(),
                        "Redundant role='" + explicitRole + "' on <" + elementName + "> element. This role is implicit",
                        new RemoveRedundantRoleQuickFix());
                } else if (isRoleConflicting(implicitRole, explicitRole)) {
                    registerProblem(holder, file, matcher.start(), matcher.end(),
                        "Role='" + explicitRole + "' conflicts with implicit role '" + implicitRole + "' of <" + elementName + "> element",
                        new ResolveRoleConflictQuickFix(implicitRole, explicitRole));
                }
            }
            
            // Special case: input elements have type-dependent implicit roles
            if ("input".equals(elementName)) {
                checkInputTypeRoleConsistency(matcher.group(0), file, holder, matcher.start(), matcher.end());
            }
        }
    }
    
    private boolean isRoleConflicting(String implicitRole, String explicitRole) {
        // Define role conflicts based on semantic incompatibility
        Map<String, Set<String>> conflicts = Map.of(
            "button", Set.of("link", "tab", "menuitem"),
            "link", Set.of("button", "tab"),
            "navigation", Set.of("main", "banner", "contentinfo"),
            "main", Set.of("navigation", "banner", "contentinfo", "complementary"),
            "form", Set.of("navigation", "main", "banner"),
            "list", Set.of("table", "tree", "grid")
        );
        
        Set<String> conflictingRoles = conflicts.get(implicitRole);
        return conflictingRoles != null && conflictingRoles.contains(explicitRole);
    }
    
    private void checkInputTypeRoleConsistency(String inputElement, PsiFile file, ProblemsHolder holder, int start, int end) {
        String type = extractAttributeValue(inputElement, "type");
        String role = extractAttributeValue(inputElement, "role");
        
        if (type != null && role != null) {
            String expectedRole = getExpectedRoleForInputType(type);
            if (expectedRole != null && !expectedRole.equals(role.toLowerCase())) {
                registerProblem(holder, file, start, end,
                    "Role='" + role + "' is inappropriate for input type='" + type + "'. Consider '" + expectedRole + "' or remove role",
                    new FixInputRoleQuickFix(expectedRole));
            }
        }
    }
    
    private String getExpectedRoleForInputType(String type) {
        switch (type.toLowerCase()) {
            case "button":
            case "submit":
            case "reset":
                return "button";
            case "checkbox":
                return "checkbox";
            case "radio":
                return "radio";
            case "range":
                return "slider";
            case "search":
                return "searchbox";
            case "email":
            case "tel":
            case "url":
            case "text":
            case "password":
                return "textbox";
            default:
                return null;
        }
    }
    
    private String extractAttributeValue(String element, String attribute) {
        Pattern pattern = Pattern.compile(attribute + "\\s*=\\s*[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(element);
        return matcher.find() ? matcher.group(1) : null;
    }
    
    /**
     * Validate role appropriateness for context
     */
    private void checkRoleAppropriateness(String content, PsiFile file, ProblemsHolder holder) {
        Pattern contextPattern = Pattern.compile(
            "<(nav|form|section|article|aside|main|header|footer)[^>]*>(.*?)</\\1>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        
        Matcher contextMatcher = contextPattern.matcher(content);
        
        while (contextMatcher.find()) {
            String contextElement = contextMatcher.group(1).toLowerCase();
            String contextContent = contextMatcher.group(2);
            int contextStart = contextMatcher.start();
            
            Pattern rolePattern = Pattern.compile(
                "<[^>]+\\brole\\s*=\\s*[\"']([^\"']+)[\"'][^>]*>",
                Pattern.CASE_INSENSITIVE);
            
            Matcher roleMatcher = rolePattern.matcher(contextContent);
            
            while (roleMatcher.find()) {
                String role = roleMatcher.group(1).toLowerCase().trim();
                
                if (!isRoleAppropriateForContext(role, contextElement)) {
                    registerProblem(holder, file, contextStart + roleMatcher.start(), contextStart + roleMatcher.end(),
                        "Role '" + role + "' may be inappropriate within <" + contextElement + "> context. Consider a more suitable role",
                        new SuggestContextAppropriateRoleQuickFix(contextElement));
                }
            }
        }
    }
    
    private boolean isRoleAppropriateForContext(String role, String context) {
        Set<String> appropriateRoles = CONTEXT_APPROPRIATE_ROLES.get(context);
        if (appropriateRoles != null && appropriateRoles.contains(role)) {
            return true;
        }
        
        // Additional context-specific logic
        switch (context) {
            case "nav":
                return role.equals("menu") || role.equals("menubar") || role.equals("tablist") || 
                       role.equals("list") || role.equals("link") || role.equals("button");
            case "form":
                return !role.equals("navigation") && !role.equals("main") && !role.equals("banner");
            case "main":
                return !role.equals("navigation") && !role.equals("banner") && !role.equals("contentinfo");
            default:
                return true; // Allow most roles in general contexts
        }
    }
    
    /**
     * Check for conflicting semantic meanings
     */
    private void checkSemanticConsistency(String content, PsiFile file, ProblemsHolder holder) {
        // Check for elements with conflicting visual and semantic indicators
        Pattern visualButtonPattern = Pattern.compile(
            "<(?!button)[^>]+(?:class\\s*=\\s*[\"'][^\"']*btn[^\"']*[\"']|onclick)[^>]*\\brole\\s*=\\s*[\"']([^\"']+)[\"'][^>]*>",
            Pattern.CASE_INSENSITIVE);
        
        Matcher matcher = visualButtonPattern.matcher(content);
        
        while (matcher.find()) {
            String role = matcher.group(1).toLowerCase();
            
            if (!role.equals("button")) {
                registerProblem(holder, file, matcher.start(), matcher.end(),
                    "Element appears to be styled as a button but has role='" + role + "'. This creates conflicting semantics",
                    new FixSemanticConflictQuickFix("button"));
            }
        }
        
        // Check for links that look like buttons
        Pattern linkButtonPattern = Pattern.compile(
            "<a[^>]+class\\s*=\\s*[\"'][^\"']*(?:btn|button)[^\"']*[\"'][^>]*>",
            Pattern.CASE_INSENSITIVE);
        
        Matcher linkButtonMatcher = linkButtonPattern.matcher(content);
        
        while (linkButtonMatcher.find()) {
            String linkElement = linkButtonMatcher.group();
            if (!linkElement.contains("role=")) {
                registerProblem(holder, file, linkButtonMatcher.start(), linkButtonMatcher.end(),
                    "Link styled as button should have role='button' or be changed to <button> element",
                    new FixLinkButtonSemanticQuickFix());
            }
        }
    }
    
    /**
     * Detect role changes that break accessibility contracts
     */
    private void checkAccessibilityContracts(String content, PsiFile file, ProblemsHolder holder) {
        // Check for semantic elements with presentation role
        Pattern semanticWithPresentationPattern = Pattern.compile(
            "<(h[1-6]|button|a|input|select|textarea|ul|ol|table)[^>]*\\brole\\s*=\\s*[\"'](?:presentation|none)[\"'][^>]*>",
            Pattern.CASE_INSENSITIVE);
        
        Matcher matcher = semanticWithPresentationPattern.matcher(content);
        
        while (matcher.find()) {
            String element = matcher.group(1).toLowerCase();
            
            registerProblem(holder, file, matcher.start(), matcher.end(),
                "Using role='presentation' on semantic element <" + element + "> removes accessibility information. Consider restructuring",
                new RemovePresentationRoleQuickFix());
        }
        
        // Check for interactive elements with non-interactive roles
        Pattern interactiveWithNonInteractivePattern = Pattern.compile(
            "<(button|a|input)[^>]*\\brole\\s*=\\s*[\"'](article|region|text|img|figure)[\"'][^>]*>",
            Pattern.CASE_INSENSITIVE);
        
        Matcher interactiveMatcher = interactiveWithNonInteractivePattern.matcher(content);
        
        while (interactiveMatcher.find()) {
            String element = interactiveMatcher.group(1);
            String role = interactiveMatcher.group(2);
            
            registerProblem(holder, file, interactiveMatcher.start(), interactiveMatcher.end(),
                "Interactive element <" + element + "> with role='" + role + "' may not be accessible to assistive technology",
                new RestoreInteractiveRoleQuickFix(element));
        }
    }
    
    // Enhanced Quick Fixes
    private static class ResolveRoleConflictQuickFix implements LocalQuickFix {
        private final String implicitRole;
        private final String explicitRole;
        
        ResolveRoleConflictQuickFix(String implicitRole, String explicitRole) {
            this.implicitRole = implicitRole;
            this.explicitRole = explicitRole;
        }
        
        @NotNull
        @Override
        public String getFamilyName() {
            return "Resolve role conflict - use implicit role '" + implicitRole + "'";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would remove conflicting role or suggest element change
        }
    }
    
    private static class FixInputRoleQuickFix implements LocalQuickFix {
        private final String expectedRole;
        
        FixInputRoleQuickFix(String expectedRole) {
            this.expectedRole = expectedRole;
        }
        
        @NotNull
        @Override
        public String getFamilyName() {
            return "Change role to '" + expectedRole + "' for input type";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would update role attribute
        }
    }
    
    private static class SuggestContextAppropriateRoleQuickFix implements LocalQuickFix {
        private final String context;
        
        SuggestContextAppropriateRoleQuickFix(String context) {
            this.context = context;
        }
        
        @NotNull
        @Override
        public String getFamilyName() {
            return "Suggest appropriate role for " + context + " context";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would suggest context-appropriate roles
        }
    }
    
    private static class FixSemanticConflictQuickFix implements LocalQuickFix {
        private final String suggestedRole;
        
        FixSemanticConflictQuickFix(String suggestedRole) {
            this.suggestedRole = suggestedRole;
        }
        
        @NotNull
        @Override
        public String getFamilyName() {
            return "Change role to '" + suggestedRole + "' to match visual appearance";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would update role to match visual semantics
        }
    }
    
    private static class FixLinkButtonSemanticQuickFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add role='button' to link styled as button";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add role='button' to link
        }
    }
    
    private static class RemovePresentationRoleQuickFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Remove presentation role from semantic element";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would remove presentation role
        }
    }
    
    private static class RestoreInteractiveRoleQuickFix implements LocalQuickFix {
        private final String element;
        
        RestoreInteractiveRoleQuickFix(String element) {
            this.element = element;
        }
        
        @NotNull
        @Override
        public String getFamilyName() {
            return "Restore appropriate interactive role for <" + element + ">";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would restore appropriate interactive role
        }
    }
    
    private static class RemoveRedundantRoleQuickFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Remove redundant ARIA role";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would remove the redundant role attribute
        }
    }
}