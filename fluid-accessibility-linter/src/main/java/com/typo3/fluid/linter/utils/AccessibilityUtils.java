package com.typo3.fluid.linter.utils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Utility class for common accessibility validation functions.
 */
public class AccessibilityUtils {
    
    // Valid ARIA roles (non-abstract)
    private static final Set<String> VALID_ARIA_ROLES = new HashSet<>(Arrays.asList(
        // Widget roles
        "button", "checkbox", "gridcell", "link", "menuitem", "menuitemcheckbox",
        "menuitemradio", "option", "progressbar", "radio", "scrollbar", "searchbox",
        "separator", "slider", "spinbutton", "switch", "tab", "tabpanel", "textbox",
        "treeitem", "combobox", "listbox", "menu", "menubar", "radiogroup", "tablist",
        "tree", "treegrid", "grid",
        
        // Document structure roles
        "application", "article", "cell", "columnheader", "definition", "directory",
        "document", "feed", "figure", "group", "heading", "img", "list", "listitem",
        "math", "none", "note", "presentation", "region", "row", "rowgroup", "rowheader",
        "separator", "table", "term", "toolbar", "tooltip",
        
        // Landmark roles
        "banner", "complementary", "contentinfo", "form", "main", "navigation", "search",
        
        // Live region roles
        "alert", "log", "marquee", "status", "timer",
        
        // Window roles
        "alertdialog", "dialog"
    ));
    
    // Abstract roles that should not be used
    private static final Set<String> ABSTRACT_ARIA_ROLES = new HashSet<>(Arrays.asList(
        "command", "composite", "input", "landmark", "range", "roletype",
        "section", "sectionhead", "select", "structure", "widget", "window"
    ));
    
    // Common language codes (ISO 639-1 and some regional variants)
    private static final Pattern VALID_LANG_PATTERN = Pattern.compile(
        "^[a-z]{2}(-[A-Z]{2})?$"
    );
    
    // Common valid language codes for quick validation
    private static final Set<String> COMMON_LANG_CODES = new HashSet<>(Arrays.asList(
        "en", "en-US", "en-GB", "en-CA", "en-AU",
        "de", "de-DE", "de-AT", "de-CH",
        "fr", "fr-FR", "fr-CA", "fr-BE", "fr-CH",
        "es", "es-ES", "es-MX", "es-AR",
        "it", "it-IT", "it-CH",
        "pt", "pt-PT", "pt-BR",
        "nl", "nl-NL", "nl-BE",
        "pl", "pl-PL",
        "ru", "ru-RU",
        "ja", "ja-JP",
        "zh", "zh-CN", "zh-TW", "zh-HK",
        "ko", "ko-KR",
        "ar", "ar-SA", "ar-AE",
        "he", "he-IL",
        "tr", "tr-TR",
        "sv", "sv-SE",
        "no", "no-NO", "nb-NO", "nn-NO",
        "da", "da-DK",
        "fi", "fi-FI",
        "cs", "cs-CZ",
        "hu", "hu-HU",
        "el", "el-GR",
        "ro", "ro-RO",
        "uk", "uk-UA",
        "bg", "bg-BG",
        "hr", "hr-HR",
        "sr", "sr-RS",
        "sk", "sk-SK",
        "sl", "sl-SI",
        "et", "et-EE",
        "lv", "lv-LV",
        "lt", "lt-LT",
        "hi", "hi-IN",
        "th", "th-TH",
        "vi", "vi-VN",
        "id", "id-ID",
        "ms", "ms-MY"
    ));
    
    /**
     * Check if an ARIA role is valid (non-abstract)
     */
    public static boolean isValidARIARole(String role) {
        if (role == null || role.trim().isEmpty()) {
            return false;
        }
        return VALID_ARIA_ROLES.contains(role.toLowerCase().trim());
    }
    
    /**
     * Check if an ARIA role is abstract (should not be used)
     */
    public static boolean isAbstractARIARole(String role) {
        if (role == null || role.trim().isEmpty()) {
            return false;
        }
        return ABSTRACT_ARIA_ROLES.contains(role.toLowerCase().trim());
    }
    
    /**
     * Check if a language code is valid according to BCP 47
     */
    public static boolean isValidLanguageCode(String lang) {
        if (lang == null || lang.trim().isEmpty()) {
            return false;
        }
        
        String trimmedLang = lang.trim();
        
        // Quick check against common codes
        if (COMMON_LANG_CODES.contains(trimmedLang)) {
            return true;
        }
        
        // Check against pattern for basic validation
        return VALID_LANG_PATTERN.matcher(trimmedLang).matches();
    }
    
    /**
     * Check if text appears to be generic or placeholder text
     */
    public static boolean isGenericText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return true;
        }
        
        String lower = text.toLowerCase().trim();
        
        // Common generic/placeholder text patterns
        return lower.equals("click here") ||
               lower.equals("read more") ||
               lower.equals("more") ||
               lower.equals("link") ||
               lower.equals("button") ||
               lower.equals("image") ||
               lower.equals("icon") ||
               lower.equals("untitled") ||
               lower.equals("title") ||
               lower.equals("heading") ||
               lower.equals("page") ||
               lower.equals("document") ||
               lower.equals("here") ||
               lower.equals("this") ||
               lower.equals("more info") ||
               lower.equals("more information") ||
               lower.equals("learn more") ||
               lower.equals("see more") ||
               lower.equals("view more") ||
               lower.equals("details") ||
               lower.equals("info") ||
               lower.equals("information");
    }
    
    /**
     * Check if alt text contains redundant phrases
     */
    public static boolean hasRedundantAltPhrase(String altText) {
        if (altText == null || altText.trim().isEmpty()) {
            return false;
        }
        
        String lower = altText.toLowerCase().trim();
        
        return lower.startsWith("image of") ||
               lower.startsWith("picture of") ||
               lower.startsWith("photo of") ||
               lower.startsWith("graphic of") ||
               lower.startsWith("icon of") ||
               lower.startsWith("illustration of") ||
               lower.startsWith("diagram of") ||
               lower.startsWith("screenshot of");
    }
    
    /**
     * Check if alt text appears to be a filename
     */
    public static boolean looksLikeFilename(String altText) {
        if (altText == null || altText.trim().isEmpty()) {
            return false;
        }
        
        String text = altText.trim();
        
        // Check for common image file extensions
        return text.matches(".*\\.(jpg|jpeg|png|gif|svg|webp|bmp|ico|tiff?)$") ||
               // Check for typical filename patterns (underscores, hyphens, no spaces)
               (text.matches("[\\w\\-_]+") && !text.contains(" ")) ||
               // Check for dimension patterns like "image_300x200"
               text.matches(".*_\\d+x\\d+.*");
    }
    
    /**
     * Extract text content from HTML, removing tags
     */
    public static String extractTextContent(String html) {
        if (html == null) return "";
        
        // Remove script and style content
        html = html.replaceAll("(?s)<script[^>]*>.*?</script>", "");
        html = html.replaceAll("(?s)<style[^>]*>.*?</style>", "");
        
        // Remove HTML tags
        html = html.replaceAll("<[^>]+>", " ");
        
        // Decode common HTML entities
        html = html.replaceAll("&nbsp;", " ");
        html = html.replaceAll("&amp;", "&");
        html = html.replaceAll("&lt;", "<");
        html = html.replaceAll("&gt;", ">");
        html = html.replaceAll("&quot;", "\"");
        html = html.replaceAll("&#39;", "'");
        
        // Clean up whitespace
        return html.replaceAll("\\s+", " ").trim();
    }
    
    /**
     * Check if an element ID exists in the file content
     */
    public static boolean elementWithIdExists(String fileContent, String id) {
        if (fileContent == null || id == null || id.trim().isEmpty()) {
            return false;
        }
        
        // Look for id="value" or id='value'
        String pattern = "\\bid\\s*=\\s*[\"']" + Pattern.quote(id.trim()) + "[\"']";
        return Pattern.compile(pattern).matcher(fileContent).find();
    }
    
    /**
     * Count occurrences of a pattern in text
     */
    public static int countOccurrences(String text, Pattern pattern) {
        if (text == null || pattern == null) {
            return 0;
        }
        
        Matcher matcher = pattern.matcher(text);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }
    
    /**
     * Check if a string is empty or contains only whitespace
     */
    public static boolean isEmptyOrWhitespace(String text) {
        return text == null || text.trim().isEmpty();
    }
    
    /**
     * Get a readable name for a form input type
     */
    public static String getInputTypeDescription(String type) {
        if (type == null) return "input field";
        
        switch (type.toLowerCase()) {
            case "text": return "text field";
            case "email": return "email field";
            case "password": return "password field";
            case "tel": return "telephone field";
            case "url": return "URL field";
            case "search": return "search field";
            case "number": return "number field";
            case "date": return "date field";
            case "time": return "time field";
            case "datetime-local": return "date and time field";
            case "month": return "month field";
            case "week": return "week field";
            case "color": return "color picker";
            case "range": return "slider";
            case "file": return "file upload";
            case "checkbox": return "checkbox";
            case "radio": return "radio button";
            case "submit": return "submit button";
            case "reset": return "reset button";
            case "button": return "button";
            case "hidden": return "hidden field";
            default: return "input field";
        }
    }
}