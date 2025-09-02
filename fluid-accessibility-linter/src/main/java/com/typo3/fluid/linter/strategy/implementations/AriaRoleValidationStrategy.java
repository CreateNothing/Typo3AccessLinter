package com.typo3.fluid.linter.strategy.implementations;

import com.intellij.psi.PsiFile;
import com.typo3.fluid.linter.strategy.ValidationResult;
import com.typo3.fluid.linter.strategy.ValidationStrategy;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Validation strategy for ARIA role accessibility.
 * Migrated from AriaRoleInspection to work with the new strategy pattern.
 */
public class AriaRoleValidationStrategy implements ValidationStrategy {
    
    // Valid ARIA roles
    private static final Set<String> VALID_ROLES = new HashSet<>(Arrays.asList(
        // Widget roles
        "button", "checkbox", "gridcell", "link", "menuitem", "menuitemcheckbox",
        "menuitemradio", "option", "progressbar", "radio", "scrollbar", "searchbox",
        "separator", "slider", "spinbutton", "switch", "tab", "tabpanel", "textbox",
        "treeitem", "combobox", "grid", "listbox", "menu", "menubar", "radiogroup",
        "tablist", "tree", "treegrid",
        
        // Document structure roles
        "application", "article", "cell", "columnheader", "definition", "directory",
        "document", "feed", "figure", "group", "heading", "img", "list", "listitem",
        "math", "none", "note", "presentation", "region", "row", "rowgroup", "rowheader",
        "separator", "table", "term", "toolbar", "tooltip",
        
        // Landmark roles
        "banner", "complementary", "contentinfo", "form", "main", "navigation", "region", "search",
        
        // Live region roles
        "alert", "log", "marquee", "status", "timer",
        
        // Window roles
        "alertdialog", "dialog"
    ));
    
    // Abstract roles that should not be used directly
    private static final Set<String> ABSTRACT_ROLES = new HashSet<>(Arrays.asList(
        "command", "composite", "input", "landmark", "range", "roletype",
        "section", "sectionhead", "select", "structure", "widget", "window"
    ));
    
    // Required properties for specific roles
    private static final Map<String, List<String>> REQUIRED_PROPERTIES = new HashMap<>();
    static {
        REQUIRED_PROPERTIES.put("checkbox", Arrays.asList("aria-checked"));
        REQUIRED_PROPERTIES.put("option", Arrays.asList("aria-selected"));
        REQUIRED_PROPERTIES.put("radio", Arrays.asList("aria-checked"));
        REQUIRED_PROPERTIES.put("slider", Arrays.asList("aria-valuenow", "aria-valuemin", "aria-valuemax"));
        REQUIRED_PROPERTIES.put("spinbutton", Arrays.asList("aria-valuenow"));
        REQUIRED_PROPERTIES.put("switch", Arrays.asList("aria-checked"));
        REQUIRED_PROPERTIES.put("combobox", Arrays.asList("aria-expanded"));
        REQUIRED_PROPERTIES.put("scrollbar", Arrays.asList("aria-valuenow", "aria-orientation"));
    }
    
    // Roles that require labeling
    private static final Set<String> ROLES_REQUIRING_LABEL = new HashSet<>(Arrays.asList(
        "dialog", "alertdialog", "form", "region", "tabpanel", "application"
    ));
    
    // Implicit roles for HTML elements
    private static final Map<String, String> IMPLICIT_ROLES = new HashMap<>();
    static {
        IMPLICIT_ROLES.put("button", "button");
        IMPLICIT_ROLES.put("nav", "navigation");
        IMPLICIT_ROLES.put("aside", "complementary");
        IMPLICIT_ROLES.put("main", "main");
        IMPLICIT_ROLES.put("header", "banner");
        IMPLICIT_ROLES.put("footer", "contentinfo");
        IMPLICIT_ROLES.put("article", "article");
        IMPLICIT_ROLES.put("section", "region");
        IMPLICIT_ROLES.put("form", "form");
        IMPLICIT_ROLES.put("img", "img");
        IMPLICIT_ROLES.put("table", "table");
        IMPLICIT_ROLES.put("ul", "list");
        IMPLICIT_ROLES.put("ol", "list");
        IMPLICIT_ROLES.put("li", "listitem");
        IMPLICIT_ROLES.put("a", "link");
        IMPLICIT_ROLES.put("h1", "heading");
        IMPLICIT_ROLES.put("h2", "heading");
        IMPLICIT_ROLES.put("h3", "heading");
        IMPLICIT_ROLES.put("h4", "heading");
        IMPLICIT_ROLES.put("h5", "heading");
        IMPLICIT_ROLES.put("h6", "heading");
    }
    
    private static final Pattern ELEMENT_WITH_ROLE_PATTERN = Pattern.compile(
        "<([a-zA-Z][^>]*?)\\s+role\\s*=\\s*[\"']([^\"']+)[\"']([^>]*)>",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    
    private static final Pattern TAG_NAME_PATTERN = Pattern.compile(
        "^([a-zA-Z0-9]+)",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern ARIA_HIDDEN_PATTERN = Pattern.compile(
        "aria-hidden\\s*=\\s*[\"']true[\"']",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern TABINDEX_PATTERN = Pattern.compile(
        "tabindex\\s*=\\s*[\"']([^\"']+)[\"']",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern INPUT_TYPE_PATTERN = Pattern.compile(
        "type\\s*=\\s*[\"']([^\"']+)[\"']",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern CLASS_PATTERN = Pattern.compile(
        "class\\s*=\\s*[\"']([^\"']+)[\"']",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern ONCLICK_PATTERN = Pattern.compile(
        "onclick\\s*=\\s*[\"']([^\"']+)[\"']",
        Pattern.CASE_INSENSITIVE
    );
    
    @Override
    public List<ValidationResult> validate(PsiFile file, String content) {
        List<ValidationResult> results = new ArrayList<>();
        Matcher matcher = ELEMENT_WITH_ROLE_PATTERN.matcher(content);
        
        while (matcher.find()) {
            String elementTag = matcher.group(1);
            String role = matcher.group(2).trim();
            String attributes = elementTag + " " + matcher.group(3);
            
            // Extract tag name
            Matcher tagMatcher = TAG_NAME_PATTERN.matcher(elementTag);
            String tagName = tagMatcher.find() ? tagMatcher.group(1).toLowerCase() : "";
            
            int start = matcher.start();
            int end = matcher.end();
            
            // Check for invalid role
            if (!VALID_ROLES.contains(role.toLowerCase())) {
                if (ABSTRACT_ROLES.contains(role.toLowerCase())) {
                    results.add(new ValidationResult(start, end,
                        String.format("Abstract ARIA role '%s' should not be used directly", role)));
                } else if (role.contains(" ")) {
                    results.add(new ValidationResult(start, end,
                        "Multiple ARIA roles are not allowed. Use a single, specific role"));
                } else {
                    results.add(new ValidationResult(start, end,
                        String.format("Invalid ARIA role '%s'", role)));
                }
                continue;
            }
            
            // Check for redundant role
            String implicitRole = IMPLICIT_ROLES.get(tagName);
            if (implicitRole != null && implicitRole.equals(role.toLowerCase())) {
                if (tagName.equals("a") && attributes.contains("href")) {
                    results.add(new ValidationResult(start, end,
                        String.format("Redundant role='%s' on <%s> element. This role is implicit", role, tagName)));
                } else if (!tagName.equals("a")) {
                    results.add(new ValidationResult(start, end,
                        String.format("Redundant role='%s' on <%s> element", role, tagName)));
                }
            }
            
            // Check for conflicting implicit role
            if (implicitRole != null && !implicitRole.equals(role.toLowerCase()) && 
                !role.equalsIgnoreCase("presentation") && !role.equalsIgnoreCase("none")) {
                results.add(new ValidationResult(start, end,
                    String.format("Role='%s' conflicts with implicit role '%s' of <%s> element", 
                        role, implicitRole, tagName)));
            }
            
            // Check for required properties
            List<String> required = REQUIRED_PROPERTIES.get(role.toLowerCase());
            if (required != null) {
                for (String prop : required) {
                    if (!attributes.contains(prop)) {
                        results.add(new ValidationResult(start, end,
                            String.format("Role '%s' requires '%s' property", role, prop)));
                    }
                }
            }
            
            // Check for required labeling
            if (ROLES_REQUIRING_LABEL.contains(role.toLowerCase())) {
                if (!attributes.contains("aria-label") && !attributes.contains("aria-labelledby")) {
                    results.add(new ValidationResult(start, end,
                        String.format("Role '%s' requires aria-labelledby or aria-label", role)));
                }
            }
            
            // Check for aria-hidden on interactive elements
            if (ARIA_HIDDEN_PATTERN.matcher(attributes).find()) {
                if (isInteractiveElement(tagName) || isInteractiveRole(role)) {
                    results.add(new ValidationResult(start, end,
                        "Element with aria-hidden='true' should not be interactive"));
                }
                
                // Check for tabindex with aria-hidden
                Matcher tabindexMatcher = TABINDEX_PATTERN.matcher(attributes);
                if (tabindexMatcher.find()) {
                    String tabindex = tabindexMatcher.group(1);
                    try {
                        int tabindexValue = Integer.parseInt(tabindex);
                        if (tabindexValue >= 0) {
                            results.add(new ValidationResult(start, end,
                                "Element with aria-hidden='true' should not be interactive"));
                        }
                    } catch (NumberFormatException e) {
                        // Ignore invalid tabindex values
                    }
                }
            }
            
            // Check input type consistency
            if (tagName.equals("input")) {
                Matcher typeMatcher = INPUT_TYPE_PATTERN.matcher(attributes);
                if (typeMatcher.find()) {
                    String type = typeMatcher.group(1);
                    checkInputTypeRoleConsistency(type, role, start, end, results);
                }
            }
            
            // Check semantic consistency
            checkSemanticConsistency(tagName, role, attributes, start, end, results);
            
            // Check context appropriateness
            if (tagName.equals("form") && role.equalsIgnoreCase("navigation")) {
                results.add(new ValidationResult(start, end,
                    "Role 'navigation' may be inappropriate within <form> context"));
            }
            
            // Check presentation role on semantic elements
            if ((role.equalsIgnoreCase("presentation") || role.equalsIgnoreCase("none")) &&
                isSemanticElement(tagName)) {
                results.add(new ValidationResult(start, end,
                    String.format("Using role='%s' on semantic element <%s> removes accessibility information", 
                        role, tagName)));
            }
            
            // Check interactive element with non-interactive role
            if (isInteractiveElement(tagName) && !isInteractiveRole(role)) {
                results.add(new ValidationResult(start, end,
                    String.format("Interactive element <%s> with role='%s' may not be accessible", 
                        tagName, role)));
            }
        }
        
        return results;
    }
    
    private void checkInputTypeRoleConsistency(String type, String role, int start, int end, 
                                               List<ValidationResult> results) {
        Map<String, String> expectedRoles = new HashMap<>();
        expectedRoles.put("checkbox", "checkbox");
        expectedRoles.put("radio", "radio");
        expectedRoles.put("button", "button");
        expectedRoles.put("search", "searchbox");
        expectedRoles.put("email", "textbox");
        expectedRoles.put("tel", "textbox");
        expectedRoles.put("text", "textbox");
        expectedRoles.put("url", "textbox");
        expectedRoles.put("number", "spinbutton");
        expectedRoles.put("range", "slider");
        
        String expected = expectedRoles.get(type);
        if (expected != null && !expected.equals(role.toLowerCase()) && 
            !role.equalsIgnoreCase("presentation") && !role.equalsIgnoreCase("none")) {
            results.add(new ValidationResult(start, end,
                String.format("Role='%s' is inappropriate for input type='%s'", role, type)));
        }
    }
    
    private void checkSemanticConsistency(String tagName, String role, String attributes, 
                                         int start, int end, List<ValidationResult> results) {
        // Check button-styled elements
        Matcher classMatcher = CLASS_PATTERN.matcher(attributes);
        if (classMatcher.find()) {
            String classes = classMatcher.group(1).toLowerCase();
            boolean hasButtonClass = classes.contains("btn") || classes.contains("button");
            boolean hasLinkClass = classes.contains("link");
            
            if (hasButtonClass && role.equalsIgnoreCase("link")) {
                results.add(new ValidationResult(start, end,
                    "Element appears to be styled as a button but has role='link'"));
            }
            
            if (tagName.equals("a") && hasButtonClass && !role.equalsIgnoreCase("button")) {
                results.add(new ValidationResult(start, end,
                    "Link styled as button should have role='button'"));
            }
        }
        
        // Check onclick handlers
        if (ONCLICK_PATTERN.matcher(attributes).find() && role.equalsIgnoreCase("link")) {
            results.add(new ValidationResult(start, end,
                "Element appears to be styled as a button but has role='link'"));
        }
    }
    
    private boolean isInteractiveElement(String tagName) {
        return Arrays.asList("button", "a", "input", "select", "textarea", "details", "summary")
            .contains(tagName);
    }
    
    private boolean isInteractiveRole(String role) {
        return Arrays.asList("button", "link", "checkbox", "radio", "textbox", "combobox",
            "listbox", "menu", "menuitem", "option", "progressbar", "scrollbar", "searchbox",
            "slider", "spinbutton", "switch", "tab", "treeitem")
            .contains(role.toLowerCase());
    }
    
    private boolean isSemanticElement(String tagName) {
        return Arrays.asList("h1", "h2", "h3", "h4", "h5", "h6", "nav", "main", "aside",
            "header", "footer", "article", "section", "button", "a")
            .contains(tagName);
    }
    
    @Override
    public int getPriority() {
        return 90; // High priority for ARIA validation
    }
    
    @Override
    public boolean shouldApply(PsiFile file) {
        return file.getText().contains("role=");
    }
}