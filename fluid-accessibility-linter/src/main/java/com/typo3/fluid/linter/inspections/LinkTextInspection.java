package com.typo3.fluid.linter.inspections;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiFile;
import com.typo3.fluid.linter.utils.AccessibilityUtils;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Enhanced inspection for link text accessibility with context-aware intelligence.
 * Features:
 * - Context analysis for "read more" validation
 * - Link clustering detection
 * - Icon-only link validation
 * - Duplicate link detection
 * - Page title matching suggestions
 */
public class LinkTextInspection extends FluidAccessibilityInspection {
    
    private static final Set<String> NON_DESCRIPTIVE_PHRASES = new HashSet<>(Arrays.asList(
        "click here",
        "here",
        "read more",
        "learn more",
        "more",
        "click",
        "download",
        "link",
        "this link",
        "this page",
        "more info",
        "info",
        "details",
        "view",
        "see more",
        "continue",
        "go",
        "start",
        "submit",
        "follow this link",
        "click this link",
        "visit",
        "check out",
        "click to",
        "go to",
        "tap here",
        "press here",
        "follow",
        "open",
        "view details",
        "more details",
        "further information",
        "additional information",
        "click for more",
        "find out",
        "discover",
        "explore"
    ));
    
    private static final Set<String> CONTEXTUAL_PHRASES = new HashSet<>(Arrays.asList(
        "read more",
        "learn more",
        "see more",
        "view more",
        "details",
        "more info",
        "continue reading",
        "find out more"
    ));
    
    private static final Pattern LINK_PATTERN = Pattern.compile(
        "<(" +
            "a\\s+[^>]*" +
            "|f:link(?:\\.[^\\s>]+)?[^>]*" +
            "|[a-zA-Z0-9_.-]+:link(?:\\.[^\\s>]+)?[^>]*" +
        ")>(.*?)</(" +
            "a" +
            "|f:link(?:\\.[^>]*)?" +
            "|[a-zA-Z0-9_.-]+:link(?:\\.[^>]*)?" +
        ")>",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    
    private static final Pattern ARIA_LABEL_PATTERN = Pattern.compile(
        "aria-label(?:ledby)?\\s*=\\s*[\"']([^\"']+)[\"']",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern TITLE_PATTERN = Pattern.compile(
        "title\\s*=\\s*[\"']([^\"']+)[\"']",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern HREF_PATTERN = Pattern.compile(
        "href\\s*=\\s*[\"']([^\"']+)[\"']",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern ICON_PATTERN = Pattern.compile(
        "<(?:i|span)\\s+[^>]*class\\s*=\\s*[\"'][^\"']*(?:icon|fa-|fas|far|fab|glyphicon|material-icons|bi-|ion-)[^\"']*[\"'][^>]*>|" +
        "<img\\s+[^>]*(?:class\\s*=\\s*[\"'][^\"']*icon[^\"']*[\"']|src\\s*=\\s*[\"'][^\"']*icon[^\"']*[\"'])[^>]*>|" +
        "<svg\\s+[^>]*(?:class\\s*=\\s*[\"'][^\"']*icon[^\"']*[\"'])?[^>]*>.*?</svg>|" +
        "<core:icon\\b[^>]*/?>",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    
    private static final Pattern CONTEXT_EXTRACTION_PATTERN = Pattern.compile(
        "(.{0,200})<(?:a|f:link)\\b[^>]*>.*?</(?:a|f:link[^>]*)>",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    
    @NotNull
    @Override
    public String getDisplayName() {
        return "Non-descriptive link text";
    }
    
    @NotNull
    @Override
    public String getShortName() {
        return "LinkText";
    }
    
    @Override
    protected void inspectFile(@NotNull PsiFile file, @NotNull ProblemsHolder holder) {
        com.typo3.fluid.linter.settings.RuleSettingsState st = com.typo3.fluid.linter.settings.RuleSettingsState.getInstance(file.getProject());
        if (st != null && st.isUniversalEnabled() && st.isSuppressLegacyDuplicates()) {
            return; // suppressed when Universal is enabled and suppression is active
        }
        String content = file.getText();
        
        // Collect all links for context analysis and duplicate detection
        List<LinkInfo> links = collectLinks(content);
        
        // Check each link
        for (LinkInfo link : links) {
            checkEmptyLink(link, file, holder);
            checkNonDescriptiveText(link, content, file, holder);
            checkSingleCharacterText(link, file, holder);
            checkUrlAsText(link, file, holder);
            checkIconOnlyLink(link, file, holder);
            checkLongLinkText(link, file, holder);
            checkWhitespaceOnlyText(link, file, holder);
        }
        
        // Check for duplicate links
        checkDuplicateLinks(links, file, holder);
        
        // Check for link clustering
        checkLinkClustering(links, content, file, holder);
    }
    
    private List<LinkInfo> collectLinks(String content) {
        List<LinkInfo> links = new ArrayList<>();
        Matcher matcher = LINK_PATTERN.matcher(content);
        
        while (matcher.find()) {
            String fullMatch = matcher.group(0);
            String attributes = matcher.group(1);
            String linkContent = matcher.group(2);
            
            if (attributes == null) attributes = "";
            if (linkContent == null) linkContent = "";
            
            String linkText = AccessibilityUtils.extractTextContent(linkContent).trim();
            String href = extractHref(attributes);
            boolean hasAriaLabel = ARIA_LABEL_PATTERN.matcher(attributes).find();
            boolean hasTitle = TITLE_PATTERN.matcher(attributes).find();
            boolean hasIcon = ICON_PATTERN.matcher(linkContent).find();
            
            Matcher ariaLabelMatcher = ARIA_LABEL_PATTERN.matcher(attributes);
            String ariaLabel = ariaLabelMatcher.find() ? ariaLabelMatcher.group(1) : null;
            
            Matcher titleMatcher = TITLE_PATTERN.matcher(attributes);
            String title = titleMatcher.find() ? titleMatcher.group(1) : null;
            
            links.add(new LinkInfo(
                matcher.start(), matcher.end(), 
                linkText, linkContent, attributes, href,
                hasAriaLabel, hasTitle, hasIcon, ariaLabel, title
            ));
        }
        
        return links;
    }
    
    private void checkEmptyLink(LinkInfo link, PsiFile file, ProblemsHolder holder) {
        if (link.linkText.isEmpty()) {
            // Check if it has a valid aria-label
            if (link.hasAriaLabel && link.ariaLabel != null && !link.ariaLabel.trim().isEmpty()) {
                // Validate the quality of the aria-label
                String ariaLabelLower = link.ariaLabel.toLowerCase().trim();
                if (NON_DESCRIPTIVE_PHRASES.contains(ariaLabelLower)) {
                    registerProblem(holder, file, link.start, link.end,
                        String.format("aria-label '%s' is not descriptive", link.ariaLabel), null);
                }
                return; // Has valid aria-label
            }
            
            // No aria-label or empty aria-label
            if (link.hasIcon) {
                registerProblem(holder, file, link.start, link.end,
                    "Icon-only link must have aria-label or meaningful text to be accessible", null);
            } else {
                registerProblem(holder, file, link.start, link.end,
                    "Link has no text content and no accessible label", null);
            }
        }
    }
    
    private void checkNonDescriptiveText(LinkInfo link, String content, PsiFile file, ProblemsHolder holder) {
        if (link.linkText.isEmpty()) return;
        
        String lowerText = link.linkText.toLowerCase().trim();
        
        if (NON_DESCRIPTIVE_PHRASES.contains(lowerText)) {
            // Check if it's a contextual phrase that might be acceptable
            if (CONTEXTUAL_PHRASES.contains(lowerText)) {
                String context = extractContext(content, link.start);
                if (!hasDescriptiveContext(context, lowerText)) {
                    String message = String.format(
                        "Link text '%s' needs context. Either improve the link text or ensure preceding text describes the destination",
                        link.linkText
                    );
                    registerProblem(holder, file, link.start, link.end, message, null);
                }
                // If context is descriptive, allow the generic phrase
            } else {
                String message = String.format(
                    "Link text '%s' is not descriptive. Use words that describe the destination or action",
                    link.linkText
                );
                registerProblem(holder, file, link.start, link.end, message, null);
            }
        }
    }
    
    private void checkSingleCharacterText(LinkInfo link, PsiFile file, ProblemsHolder holder) {
        if (link.linkText.isEmpty()) return;
        
        String text = link.linkText.trim();
        if (text.matches("^[a-zA-Z]$") || text.matches("^\\d+$")) {
            // Check if it has meaningful aria-label
            if (link.hasAriaLabel && link.ariaLabel != null && !link.ariaLabel.trim().isEmpty()) {
                // Single character with good aria-label is acceptable (e.g., "X" with aria-label="Close")
                return;
            }
            
            registerProblem(holder, file, link.start, link.end,
                String.format("Single character '%s' as link text is not descriptive. Add aria-label or use descriptive text", text), null);
        }
    }
    
    private void checkUrlAsText(LinkInfo link, PsiFile file, ProblemsHolder holder) {
        if (link.linkText.isEmpty()) return;
        
        if (isUrlText(link.linkText.toLowerCase())) {
            registerProblem(holder, file, link.start, link.end,
                "URL as link text is not user-friendly. Use descriptive text that explains the link's purpose", null);
        }
    }
    
    private void checkIconOnlyLink(LinkInfo link, PsiFile file, ProblemsHolder holder) {
        if (link.hasIcon && link.linkText.isEmpty()) {
            if (!link.hasAriaLabel && (!link.hasTitle || link.title == null || link.title.trim().isEmpty())) {
                registerProblem(holder, file, link.start, link.end,
                    "Icon-only link must have aria-label to describe its purpose", null);
            } else if (link.hasTitle && (link.ariaLabel == null || link.ariaLabel.trim().isEmpty())) {
                // Has title but no aria-label - suggest adding aria-label for better screen reader support
                registerProblem(holder, file, link.start, link.end,
                    "Icon-only link should use aria-label instead of title for better accessibility", null);
            }
        }
    }
    
    private void checkLongLinkText(LinkInfo link, PsiFile file, ProblemsHolder holder) {
        if (link.linkText.isEmpty()) return;
        
        // Check if link text exceeds recommended length (around 100 characters)
        if (link.linkText.length() > 100) {
            registerProblem(holder, file, link.start, link.end,
                String.format("Link text is too long (%d characters). Consider making it more concise (under 100 characters)", 
                    link.linkText.length()), null);
        }
    }
    
    private void checkWhitespaceOnlyText(LinkInfo link, PsiFile file, ProblemsHolder holder) {
        // Check if link text is only whitespace (spaces, tabs, newlines)
        if (!link.linkText.isEmpty() && link.linkText.trim().isEmpty()) {
            registerProblem(holder, file, link.start, link.end,
                "Link contains only whitespace characters and no meaningful text", null);
        }
    }
    
    private void checkDuplicateLinks(List<LinkInfo> links, PsiFile file, ProblemsHolder holder) {
        Map<String, List<LinkInfo>> linksByText = new HashMap<>();
        
        for (LinkInfo link : links) {
            if (!link.linkText.isEmpty()) {
                String key = link.linkText.toLowerCase().trim();
                linksByText.computeIfAbsent(key, k -> new ArrayList<>()).add(link);
            }
        }
        
        for (Map.Entry<String, List<LinkInfo>> entry : linksByText.entrySet()) {
            List<LinkInfo> duplicateLinks = entry.getValue();
            if (duplicateLinks.size() > 1) {
                // Check if they go to different destinations
                Set<String> destinations = new HashSet<>();
                for (LinkInfo link : duplicateLinks) {
                    destinations.add(link.href != null ? link.href : "");
                }
                
                if (destinations.size() > 1) {
                    // Same text, different destinations - problematic
                    for (LinkInfo link : duplicateLinks) {
                        registerProblem(holder, file, link.start, link.end,
                            String.format("Multiple links with text '%s' point to different destinations. Make link text more specific", entry.getKey()), null);
                    }
                }
            }
        }
    }
    
    private void checkLinkClustering(List<LinkInfo> links, String content, PsiFile file, ProblemsHolder holder) {
        // Look for areas with many links close together
        for (int i = 0; i < links.size() - 2; i++) {
            LinkInfo current = links.get(i);
            LinkInfo next1 = links.get(i + 1);
            LinkInfo next2 = links.get(i + 2);
            
            // Check if 3+ links are within 500 characters of each other
            if (next2.start - current.end < 500) {
                String clusterContent = content.substring(current.start, next2.end);
                String textBetweenLinks = content.substring(current.end, next1.start);
                
                // If there's very little text between links, suggest grouping
                if (textBetweenLinks.trim().length() < 20) {
                    registerProblem(holder, file, current.start, next2.end,
                        "Multiple links clustered together. Consider grouping related links in a list or navigation element for better accessibility", null);
                    break; // Only report once per cluster
                }
            }
        }
    }
    
    private String extractHref(String attributes) {
        Matcher matcher = HREF_PATTERN.matcher(attributes);
        return matcher.find() ? matcher.group(1) : null;
    }
    
    private String extractContext(String content, int linkStart) {
        // First try to find the most recent heading before the link
        String headingContext = extractHeadingContext(content, linkStart);
        if (headingContext != null && !headingContext.isEmpty()) {
            return headingContext;
        }
        
        // Then try to find list item context
        String listContext = extractListItemContext(content, linkStart);
        if (listContext != null && !listContext.isEmpty()) {
            return listContext;
        }
        
        // Finally, extract up to 200 characters before the link
        int contextStart = Math.max(0, linkStart - 200);
        String contextText = content.substring(contextStart, linkStart);
        
        // Clean up and extract meaningful sentences
        contextText = AccessibilityUtils.extractTextContent(contextText);
        
        // Get the last sentence or meaningful phrase
        String[] sentences = contextText.split("[.!?]");
        if (sentences.length > 0) {
            return sentences[sentences.length - 1].trim();
        }
        
        return contextText.trim();
    }
    
    private String extractHeadingContext(String content, int linkStart) {
        // Look for the nearest heading before the link (h1-h6)
        Pattern headingPattern = Pattern.compile(
            "<h[1-6][^>]*>(.*?)</h[1-6]>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );
        
        Matcher matcher = headingPattern.matcher(content.substring(0, linkStart));
        String lastHeading = null;
        int lastHeadingEnd = -1;
        
        while (matcher.find()) {
            // Check if this heading is closer to the link
            if (matcher.end() > lastHeadingEnd) {
                lastHeading = AccessibilityUtils.extractTextContent(matcher.group(1)).trim();
                lastHeadingEnd = matcher.end();
            }
        }
        
        // Only use heading if it's within 500 characters of the link
        if (lastHeading != null && (linkStart - lastHeadingEnd) < 500) {
            return lastHeading;
        }
        
        return null;
    }
    
    private String extractListItemContext(String content, int linkStart) {
        // Check if link is within a list item
        int liStart = content.lastIndexOf("<li", linkStart);
        int liEnd = content.indexOf("</li>", linkStart);
        
        if (liStart != -1 && liEnd != -1 && liEnd > linkStart) {
            // Extract the list item content
            String liContent = content.substring(liStart, liEnd + 5);
            
            // Remove the link itself from the context
            int linkEndInLi = liContent.indexOf("</a>");
            if (linkEndInLi == -1) {
                linkEndInLi = liContent.indexOf("</f:link");
            }
            
            if (linkEndInLi != -1) {
                String beforeLink = liContent.substring(0, liContent.indexOf("<a"));
                if (beforeLink.indexOf("<f:link") != -1) {
                    beforeLink = liContent.substring(0, liContent.indexOf("<f:link"));
                }
                
                return AccessibilityUtils.extractTextContent(beforeLink).trim();
            }
        }
        
        return null;
    }
    
    private boolean hasDescriptiveContext(String context, String linkText) {
        if (context == null || context.trim().length() < 10) {
            return false;
        }
        
        // Check if the context provides meaningful information about what the link does
        String lowerContext = context.toLowerCase();
        
        // Look for descriptive words that indicate what "read more" refers to
        return lowerContext.contains("article") || 
               lowerContext.contains("blog") || 
               lowerContext.contains("story") ||
               lowerContext.contains("about") ||
               lowerContext.contains("guide") ||
               lowerContext.contains("tutorial") ||
               lowerContext.contains("news") ||
               // Check if the context has specific topic keywords
               hasSpecificTopicKeywords(lowerContext);
    }
    
    private boolean hasSpecificTopicKeywords(String context) {
        // Look for specific nouns that would make "read more" contextually clear
        String[] topicKeywords = {
            "project", "product", "service", "company", "research", "study", 
            "report", "analysis", "review", "feature", "update", "release",
            "event", "conference", "workshop", "course", "training"
        };
        
        for (String keyword : topicKeywords) {
            if (context.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
    
    private boolean isUrlText(String text) {
        // Check for various URL patterns
        return text.matches("^(https?://|ftp://|www\\.).*") || 
               text.matches("^[a-zA-Z0-9.-]+\\.(com|org|net|edu|gov|io|co|uk|de|fr|es|it|jp|cn|au|ca|ru|br|in|mx|nl|se|no|fi|dk|pl|ch|at|be|cz|pt|gr|hu|ro|bg|hr|sk|si|lt|lv|ee|ie|lu|mt|cy)(/.*)?$") ||
               text.matches("^[a-zA-Z0-9.-]+\\.(info|biz|name|pro|aero|museum|coop|int|travel|xxx|tech|online|store|app|dev|cloud|digital|website|site|space|live|life|world|earth|today|news|blog|shop|market|trade|business|company|agency|solutions|services|support|systems|network|global|international)(/.*)?$") ||
               text.contains("http://") || text.contains("https://") ||
               text.matches(".*\\.[a-z]{2,4}/.*"); // Any domain-like pattern
    }
    
    
    // Helper class to store link information
    private static class LinkInfo {
        final int start;
        final int end;
        final String linkText;
        final String linkContent;
        final String attributes;
        final String href;
        final boolean hasAriaLabel;
        final boolean hasTitle;
        final boolean hasIcon;
        final String ariaLabel;
        final String title;
        
        LinkInfo(int start, int end, String linkText, String linkContent, String attributes, 
                String href, boolean hasAriaLabel, boolean hasTitle, boolean hasIcon,
                String ariaLabel, String title) {
            this.start = start;
            this.end = end;
            this.linkText = linkText;
            this.linkContent = linkContent;
            this.attributes = attributes;
            this.href = href;
            this.hasAriaLabel = hasAriaLabel;
            this.hasTitle = hasTitle;
            this.hasIcon = hasIcon;
            this.ariaLabel = ariaLabel;
            this.title = title;
        }
    }
    
    // Quick fix classes removed as they are not implemented
    // Future enhancement: implement actual code replacement functionality
}
