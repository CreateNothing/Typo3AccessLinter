package com.typo3.fluid.linter.inspections;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.typo3.fluid.linter.utils.AccessibilityUtils;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Enhanced table accessibility inspection with context-aware intelligence.
 * Features:
 * - Layout vs data table detection and validation
 * - Complex table structure validation (rowspan/colspan)
 * - Comprehensive scope attribute checking
 * - Smart caption and summary usage validation
 * - Table relationship analysis
 */
public class TableAccessibilityInspection extends FluidAccessibilityInspection {
    
    private static final Pattern TABLE_PATTERN = Pattern.compile(
        "<table\\s*([^>]*)>(.*?)</table>",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    
    private static final Pattern CAPTION_PATTERN = Pattern.compile(
        "<caption[^>]*>(.*?)</caption>",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    
    private static final Pattern TH_PATTERN = Pattern.compile(
        "<th\\s*([^>]*)>(.*?)</th>",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    
    private static final Pattern TD_PATTERN = Pattern.compile(
        "<td\\s*([^>]*)>(.*?)</td>",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    
    private static final Pattern TR_PATTERN = Pattern.compile(
        "<tr[^>]*>(.*?)</tr>",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    
    private static final Pattern SCOPE_PATTERN = Pattern.compile(
        "scope\\s*=\\s*[\"'](row|col|rowgroup|colgroup)[\"']",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern HEADERS_PATTERN = Pattern.compile(
        "headers\\s*=\\s*[\"']([^\"']+)[\"']",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern ID_PATTERN = Pattern.compile(
        "id\\s*=\\s*[\"']([^\"']+)[\"']",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern ROWSPAN_PATTERN = Pattern.compile(
        "rowspan\\s*=\\s*[\"']?(\\d+)[\"']?",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern COLSPAN_PATTERN = Pattern.compile(
        "colspan\\s*=\\s*[\"']?(\\d+)[\"']?",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern THEAD_PATTERN = Pattern.compile(
        "<thead[^>]*>(.*?)</thead>",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    
    private static final Pattern TBODY_PATTERN = Pattern.compile(
        "<tbody[^>]*>(.*?)</tbody>",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    
    private static final Pattern TFOOT_PATTERN = Pattern.compile(
        "<tfoot[^>]*>(.*?)</tfoot>",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    
    private static final Pattern SUMMARY_PATTERN = Pattern.compile(
        "summary\\s*=\\s*[\"']([^\"']+)[\"']",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern ROLE_PRESENTATION_PATTERN = Pattern.compile(
        "role\\s*=\\s*[\"']presentation[\"']",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern ARIA_LABEL_PATTERN = Pattern.compile(
        "aria-label\\s*=\\s*[\"']([^\"']+)[\"']",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern ARIA_LABELLEDBY_PATTERN = Pattern.compile(
        "aria-labelledby\\s*=\\s*[\"']([^\"']+)[\"']",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern ARIA_DESCRIBEDBY_PATTERN = Pattern.compile(
        "aria-describedby\\s*=\\s*[\"']([^\"']+)[\"']",
        Pattern.CASE_INSENSITIVE
    );
    
    // Patterns to detect layout tables (common CSS classes and styling patterns)
    private static final Pattern LAYOUT_TABLE_INDICATORS = Pattern.compile(
        "class\\s*=\\s*[\"'][^\"']*(?:layout|grid|container|wrapper|structure)[^\"']*[\"']|" +
        "style\\s*=\\s*[\"'][^\"']*(?:border\\s*:\\s*0|border\\s*:\\s*none)[^\"']*[\"']",
        Pattern.CASE_INSENSITIVE
    );
    
    // Patterns that suggest data tables
    private static final Pattern DATA_TABLE_INDICATORS = Pattern.compile(
        "class\\s*=\\s*[\"'][^\"']*(?:data|result|report|list|record)[^\"']*[\"']",
        Pattern.CASE_INSENSITIVE
    );
    
    @NotNull
    @Override
    public String getDisplayName() {
        return "Table accessibility and structure issues";
    }
    
    @NotNull
    @Override
    public String getShortName() {
        return "TableAccessibility";
    }
    
    @Override
    protected void inspectFile(@NotNull PsiFile file, @NotNull ProblemsHolder holder) {
        String content = file.getText();
        
        Matcher tableMatcher = TABLE_PATTERN.matcher(content);
        
        while (tableMatcher.find()) {
            String tableAttributes = tableMatcher.group(1);
            String tableContent = tableMatcher.group(2);
            int tableStart = tableMatcher.start();
            int tableEnd = tableMatcher.end();
            
            // Analyze table characteristics
            TableAnalysis analysis = analyzeTable(tableAttributes, tableContent);
            
            // Enhanced table checking with context awareness
            checkTablePurposeEnhanced(analysis, file, holder, tableStart, tableEnd);
            checkTableHeadersEnhanced(analysis, file, holder, tableStart);
            checkTableCaptionEnhanced(analysis, file, holder, tableStart, tableEnd);
            checkTableStructureEnhanced(analysis, file, holder, tableStart);
            checkHeaderScopeEnhanced(analysis, file, holder, tableStart);
            checkComplexTableAccessibilityEnhanced(analysis, file, holder, tableStart, tableEnd);
            checkTableRelationships(analysis, content, file, holder, tableStart, tableEnd);
        }
    }
    
    private TableAnalysis analyzeTable(String attributes, String content) {
        TableAnalysis analysis = new TableAnalysis();
        
        // Basic structure analysis
        analysis.attributes = attributes;
        analysis.content = content;
        analysis.hasHeaders = TH_PATTERN.matcher(content).find();
        analysis.hasCaption = CAPTION_PATTERN.matcher(content).find();
        analysis.rowCount = countOccurrences(content, "<tr");
        analysis.estimatedColumnCount = estimateColumnCount(content);
        
        // Table sections
        analysis.hasThead = THEAD_PATTERN.matcher(content).find();
        analysis.hasTbody = TBODY_PATTERN.matcher(content).find();
        analysis.hasTfoot = TFOOT_PATTERN.matcher(content).find();
        
        // Determine table purpose
        analysis.isLayoutTable = determineIfLayoutTable(attributes, content);
        analysis.isDataTable = determineIfDataTable(attributes, content, analysis);
        analysis.isComplexTable = determineIfComplexTable(content, analysis);
        
        // Analyze cells and headers
        analysis.cellAnalysis = analyzeCells(content);
        analysis.headerAnalysis = analyzeHeaders(content);
        
        // Caption analysis
        if (analysis.hasCaption) {
            Matcher captionMatcher = CAPTION_PATTERN.matcher(content);
            if (captionMatcher.find()) {
                String captionText = captionMatcher.group(1).trim();
                int captionStart = captionMatcher.start();
                int captionEnd = captionMatcher.end();
                boolean isEmpty = captionText.isEmpty();
                boolean isFirstChild = content.indexOf("<caption") < content.indexOf("<tr") && 
                                     (content.indexOf("<thead") == -1 || content.indexOf("<caption") < content.indexOf("<thead"));
                analysis.captionInfo = new CaptionInfo(captionText, captionStart, captionEnd, isEmpty, isFirstChild);
            }
        }
        
        // Accessibility features
        analysis.hasAriaLabel = ARIA_LABEL_PATTERN.matcher(attributes).find();
        analysis.hasAriaLabelledBy = ARIA_LABELLEDBY_PATTERN.matcher(attributes).find();
        analysis.hasAriaDescribedBy = ARIA_DESCRIBEDBY_PATTERN.matcher(attributes).find();
        analysis.hasSummary = SUMMARY_PATTERN.matcher(attributes).find();
        
        return analysis;
    }
    
    private boolean determineIfLayoutTable(String attributes, String content) {
        // Explicit layout table markers
        if (ROLE_PRESENTATION_PATTERN.matcher(attributes).find()) {
            return true;
        }
        
        // CSS classes or styles that suggest layout
        if (LAYOUT_TABLE_INDICATORS.matcher(attributes).find()) {
            return true;
        }
        
        // Heuristics: tables with no headers and simple structure might be layout
        boolean hasHeaders = TH_PATTERN.matcher(content).find();
        boolean hasCaption = CAPTION_PATTERN.matcher(content).find();
        int rowCount = countOccurrences(content, "<tr");
        int colCount = estimateColumnCount(content);
        
        // Simple 2x2 or similar tables with no semantic markers are likely layout
        if (!hasHeaders && !hasCaption && rowCount <= 3 && colCount <= 3) {
            // Additional check: if content looks like form fields or navigation, likely layout
            if (content.contains("<input") || content.contains("<select") || 
                content.contains("<button") || containsNavigationElements(content)) {
                return true;
            }
        }
        
        return false;
    }
    
    private boolean determineIfDataTable(String attributes, String content, TableAnalysis analysis) {
        // Explicit data table indicators
        if (DATA_TABLE_INDICATORS.matcher(attributes).find()) {
            return true;
        }
        
        // Has proper table semantics
        if (analysis.hasHeaders || analysis.hasCaption) {
            return true;
        }
        
        // Large tables are likely data tables
        if (analysis.rowCount > 5 || analysis.estimatedColumnCount > 4) {
            return true;
        }
        
        // Contains structured data patterns
        if (containsDataPatterns(content)) {
            return true;
        }
        
        return false;
    }
    
    private boolean determineIfComplexTable(String content, TableAnalysis analysis) {
        // Has spanning cells
        boolean hasSpanning = content.contains("rowspan") || content.contains("colspan");
        
        // Large size
        boolean isLarge = analysis.rowCount > 10 || analysis.estimatedColumnCount > 6;
        
        // Multiple header levels
        boolean hasComplexHeaders = hasMultiLevelHeaders(content);
        
        return hasSpanning || isLarge || hasComplexHeaders;
    }
    
    private void checkTablePurposeEnhanced(TableAnalysis analysis, PsiFile file, ProblemsHolder holder, 
                                          int start, int end) {
        if (analysis.isLayoutTable) {
            // Layout table validation
            if (analysis.hasHeaders || analysis.hasCaption) {
                registerProblem(holder, file, start, end,
                    "Layout table (role='presentation') should not have semantic elements like <th> or <caption>",
                    new RemoveSemanticElementsFix());
            }
            
            // Suggest CSS alternatives for layout
            registerProblem(holder, file, start, end,
                "Consider using CSS Grid or Flexbox instead of tables for layout",
                new SuggestCSSAlternativeFix());
                
        } else if (analysis.isDataTable) {
            // Data table validation
            if (!analysis.hasHeaders && analysis.rowCount > 1) {
                registerProblem(holder, file, start, end,
                    "Data table should have header cells (<th>) to describe the data",
                    new AddTableHeadersFix());
            }
            
            // Check for proper labeling
            if (!analysis.hasCaption && !analysis.hasAriaLabel && !analysis.hasAriaLabelledBy && 
                (analysis.rowCount > 5 || analysis.estimatedColumnCount > 4)) {
                registerProblem(holder, file, start, end,
                    "Complex data table should have a <caption> or aria-label to describe its content",
                    new AddTableCaptionFix());
            }
            
        } else {
            // Ambiguous table - provide guidance
            registerProblem(holder, file, start, end,
                "Table purpose unclear. Add role='presentation' for layout or proper headers for data tables",
                new ClarifyTablePurposeFix());
        }
    }
    
    // Enhanced table checking methods
    private void checkTableHeadersEnhanced(TableAnalysis analysis, PsiFile file, ProblemsHolder holder, int baseOffset) {
        if (!analysis.isDataTable) return;
        
        for (HeaderCell header : analysis.headerAnalysis.headers) {
            // Check empty headers
            if (header.textContent.trim().isEmpty()) {
                registerProblem(holder, file, baseOffset + header.startOffset, baseOffset + header.endOffset,
                    "Table header <th> should not be empty", null);
            }
            
            // Check generic headers
            if (header.textContent.trim().matches("^(col|column|row)\\s*\\d*$")) {
                registerProblem(holder, file, baseOffset + header.startOffset, baseOffset + header.endOffset,
                    "Table header '" + header.textContent.trim() + "' is not descriptive", null);
            }
        }
        
        // Check for proper header structure
        if (analysis.headerAnalysis.hasFirstRowTdCells && analysis.rowCount > 2) {
            registerProblem(holder, file, baseOffset, baseOffset + 100,
                "First row appears to be headers but uses <td> instead of <th>",
                new ConvertToHeaderCellsFix());
        }
    }
    
    private void checkTableCaptionEnhanced(TableAnalysis analysis, PsiFile file, ProblemsHolder holder, 
                                          int start, int end) {
        if (!analysis.isDataTable) return;
        
        // Check obsolete summary attribute
        if (analysis.hasSummary) {
            registerProblem(holder, file, start, end,
                "The 'summary' attribute is obsolete in HTML5. Use <caption> or aria-describedby instead",
                new ReplaceSummaryWithCaptionFix());
        }
        
        // Validate caption positioning and content
        if (analysis.hasCaption) {
            CaptionInfo caption = analysis.captionInfo;
            if (caption.isEmpty) {
                registerProblem(holder, file, start + caption.startOffset, start + caption.endOffset,
                    "Table caption should not be empty", null);
            }
            
            if (!caption.isFirstChild) {
                registerProblem(holder, file, start + caption.startOffset, start + caption.endOffset,
                    "<caption> must be the first child of <table>", null);
            }
        }
    }
    
    private void checkTableStructureEnhanced(TableAnalysis analysis, PsiFile file, ProblemsHolder holder, int baseOffset) {
        if (!analysis.isDataTable) return;
        
        // Check for proper sectioning in large tables
        if (analysis.rowCount > 10 && !analysis.hasThead && !analysis.hasTbody) {
            registerProblem(holder, file, baseOffset, baseOffset + 100,
                "Large table should use <thead>, <tbody>, and optionally <tfoot> for better structure",
                new AddTableSectionsFix());
        }
        
        // Check tbody without thead when headers exist
        if (analysis.hasTbody && !analysis.hasThead && analysis.hasHeaders) {
            registerProblem(holder, file, baseOffset, baseOffset + 100,
                "Table with <tbody> and header cells should also have <thead>", null);
        }
    }
    
    private void checkHeaderScopeEnhanced(TableAnalysis analysis, PsiFile file, ProblemsHolder holder, int baseOffset) {
        if (!analysis.isDataTable || !analysis.isComplexTable) return;
        
        for (HeaderCell header : analysis.headerAnalysis.headers) {
            if (!header.hasScope) {
                String suggestedScope = determineSuggestedScope(header, analysis);
                registerProblem(holder, file, baseOffset + header.startOffset, baseOffset + header.endOffset,
                    "Complex table header should have 'scope' attribute",
                    new AddScopeFix(suggestedScope));
            }
        }
    }
    
    private void checkComplexTableAccessibilityEnhanced(TableAnalysis analysis, PsiFile file, 
                                                        ProblemsHolder holder, int start, int end) {
        if (!analysis.isComplexTable) return;
        
        boolean hasSpanning = analysis.cellAnalysis.hasRowspan || analysis.cellAnalysis.hasColspan;
        
        if (hasSpanning) {
            boolean hasProperAssociation = analysis.headerAnalysis.hasHeadersAttribute || 
                                          analysis.headerAnalysis.hasScopeAttributes;
            
            if (!hasProperAssociation) {
                registerProblem(holder, file, start, end,
                    "Table with merged cells needs explicit header associations using 'scope' or 'headers' attributes",
                    new AddHeaderAssociationsFix());
            }
        }
        
        // Check for missing ids on complex headers
        if (analysis.headerAnalysis.needsHeaderIds && !analysis.headerAnalysis.hasHeaderIds) {
            registerProblem(holder, file, start, end,
                "Complex table headers should have 'id' attributes for proper association",
                new AddHeaderIdsFix());
        }
    }
    
    private void checkTableRelationships(TableAnalysis analysis, String fullContent, PsiFile file, 
                                        ProblemsHolder holder, int start, int end) {
        // Check if table references other elements
        if (analysis.hasAriaDescribedBy) {
            String describedByIds = extractAriaDescribedByIds(analysis.attributes);
            for (String id : describedByIds.split("\\s+")) {
                if (!AccessibilityUtils.elementWithIdExists(fullContent, id.trim())) {
                    registerProblem(holder, file, start, end,
                        "aria-describedby references non-existent element with id='" + id.trim() + "'",
                        null);
                }
            }
        }
        
        // Check for multiple similar tables that might need better distinction
        // This would require more complex analysis across the entire document
    }
    
    private void checkTableHeaders(String content, PsiFile file, ProblemsHolder holder, int baseOffset) {
        Matcher thMatcher = TH_PATTERN.matcher(content);
        
        while (thMatcher.find()) {
            String thContent = thMatcher.group(2);
            
            if (thContent.trim().isEmpty()) {
                registerProblem(holder, file, baseOffset + thMatcher.start(), baseOffset + thMatcher.end(),
                    "Table header <th> should not be empty",
                    null);
            }
            
            if (thContent.trim().matches("^(col|column|row)\\s*\\d*$")) {
                registerProblem(holder, file, baseOffset + thMatcher.start(), baseOffset + thMatcher.end(),
                    "Table header '" + thContent.trim() + "' is not descriptive",
                    null);
            }
        }
        
        Matcher firstRowMatcher = TR_PATTERN.matcher(content);
        if (firstRowMatcher.find()) {
            String firstRow = firstRowMatcher.group(1);
            boolean hasThInFirstRow = TH_PATTERN.matcher(firstRow).find();
            boolean hasTdInFirstRow = TD_PATTERN.matcher(firstRow).find();
            
            if (hasTdInFirstRow && !hasThInFirstRow && countOccurrences(content, "<tr") > 2) {
                registerProblem(holder, file, baseOffset + firstRowMatcher.start(), 
                    baseOffset + firstRowMatcher.end(),
                    "First row appears to be headers but uses <td> instead of <th>",
                    new ConvertToHeaderCellsFix());
            }
        }
    }
    
    private void checkTableCaption(String attributes, String content, PsiFile file, ProblemsHolder holder, int start, int end) {
        boolean hasCaption = CAPTION_PATTERN.matcher(content).find();
        boolean hasSummary = SUMMARY_PATTERN.matcher(attributes).find();
        boolean hasAriaLabel = attributes.contains("aria-label");
        boolean hasAriaLabelledby = attributes.contains("aria-labelledby");
        
        if (!hasCaption && !hasSummary && !hasAriaLabel && !hasAriaLabelledby) {
            int rowCount = countOccurrences(content, "<tr");
            int colCount = estimateColumnCount(content);
            
            if (rowCount > 5 || colCount > 4) {
                registerProblem(holder, file, start, end,
                    "Complex table should have a <caption> or aria-label to describe its content",
                    new AddTableCaptionFix());
            }
        }
        
        if (hasSummary) {
            registerProblem(holder, file, start, end,
                "The 'summary' attribute is obsolete in HTML5. Use <caption> or aria-describedby instead",
                new ReplaceSummaryWithCaptionFix());
        }
        
        Matcher captionMatcher = CAPTION_PATTERN.matcher(content);
        if (captionMatcher.find()) {
            String captionText = captionMatcher.group(1).trim();
            if (captionText.isEmpty()) {
                registerProblem(holder, file, start + captionMatcher.start(), 
                    start + captionMatcher.end(),
                    "Table caption should not be empty",
                    null);
            }
            
            int captionPosition = content.indexOf("<caption");
            int firstTrPosition = content.indexOf("<tr");
            int theadPosition = content.indexOf("<thead");
            
            if (captionPosition > firstTrPosition || 
                (theadPosition > 0 && captionPosition > theadPosition)) {
                registerProblem(holder, file, start + captionMatcher.start(),
                    start + captionMatcher.end(),
                    "<caption> must be the first child of <table>",
                    null);
            }
        }
    }
    
    private void checkTableStructure(String content, PsiFile file, ProblemsHolder holder, int baseOffset) {
        boolean hasThead = THEAD_PATTERN.matcher(content).find();
        boolean hasTbody = TBODY_PATTERN.matcher(content).find();
        boolean hasTfoot = TFOOT_PATTERN.matcher(content).find();
        
        int rowCount = countOccurrences(content, "<tr");
        
        if (rowCount > 10 && !hasThead && !hasTbody) {
            registerProblem(holder, file, baseOffset, baseOffset + 100,
                "Large table should use <thead>, <tbody>, and optionally <tfoot> for better structure",
                new AddTableSectionsFix());
        }
        
        if (hasTbody && !hasThead) {
            Matcher firstThMatcher = TH_PATTERN.matcher(content);
            if (firstThMatcher.find()) {
                registerProblem(holder, file, baseOffset, baseOffset + 100,
                    "Table with <tbody> and header cells should also have <thead>",
                    null);
            }
        }
        
        if (hasTfoot) {
            int tfootPosition = content.indexOf("<tfoot");
            int tbodyPosition = content.indexOf("<tbody");
            if (tbodyPosition > 0 && tfootPosition > tbodyPosition) {
                registerProblem(holder, file, baseOffset + tfootPosition, baseOffset + tfootPosition + 50,
                    "<tfoot> should come before <tbody> in the markup for proper table structure",
                    null);
            }
        }
    }
    
    private void checkHeaderScope(String content, PsiFile file, ProblemsHolder holder, int baseOffset) {
        Matcher thMatcher = TH_PATTERN.matcher(content);
        
        while (thMatcher.find()) {
            String thAttributes = thMatcher.group(1);
            boolean hasScope = SCOPE_PATTERN.matcher(thAttributes).find();
            
            boolean isInThead = isWithinTag(content, thMatcher.start(), "thead");
            boolean isFirstInRow = isFirstThInRow(content, thMatcher.start());
            
            if (!hasScope) {
                if (isComplexTable(content)) {
                    registerProblem(holder, file, baseOffset + thMatcher.start(), 
                        baseOffset + thMatcher.end(),
                        "Table header in complex table should have 'scope' attribute",
                        new AddScopeFix(isInThead ? "col" : (isFirstInRow ? "row" : "col")));
                }
            }
        }
    }
    
    private void checkComplexTableAccessibility(String content, PsiFile file, ProblemsHolder holder, int start, int end) {
        boolean hasColspan = content.contains("colspan");
        boolean hasRowspan = content.contains("rowspan");
        
        if (hasColspan || hasRowspan) {
            boolean hasHeaders = content.contains("headers=");
            boolean hasScope = SCOPE_PATTERN.matcher(content).find();
            
            if (!hasHeaders && !hasScope) {
                registerProblem(holder, file, start, end,
                    "Table with merged cells (colspan/rowspan) needs explicit header associations using 'scope' or 'headers' attributes",
                    null);
            }
        }
    }
    
    private int estimateColumnCount(String content) {
        Matcher trMatcher = TR_PATTERN.matcher(content);
        int maxCols = 0;
        
        while (trMatcher.find()) {
            String row = trMatcher.group(1);
            int cols = countOccurrences(row, "<td") + countOccurrences(row, "<th");
            maxCols = Math.max(maxCols, cols);
        }
        
        return maxCols;
    }
    
    private boolean isComplexTable(String content) {
        return countOccurrences(content, "<tr") > 5 || 
               estimateColumnCount(content) > 4 ||
               content.contains("colspan") || 
               content.contains("rowspan");
    }
    
    private boolean isWithinTag(String content, int position, String tagName) {
        Pattern tagPattern = Pattern.compile(
            "<" + tagName + "[^>]*>(.*?)</" + tagName + ">",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );
        
        Matcher matcher = tagPattern.matcher(content);
        while (matcher.find()) {
            if (position >= matcher.start() && position <= matcher.end()) {
                return true;
            }
        }
        return false;
    }
    
    private boolean isFirstThInRow(String content, int thPosition) {
        int rowStart = content.lastIndexOf("<tr", thPosition);
        if (rowStart == -1) return false;
        
        String beforeTh = content.substring(rowStart, thPosition);
        return !beforeTh.contains("<th") && !beforeTh.contains("<td");
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
    
    
    private static class AddTableHeadersFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Convert first row to header cells";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would convert td to th in first row
        }
    }
    
    private static class AddTableCaptionFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add table caption";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add caption element
        }
    }
    
    private static class AddTableSectionsFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add table sections (thead/tbody)";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would wrap rows in thead/tbody
        }
    }
    
    private static class AddScopeFix implements LocalQuickFix {
        private final String scopeValue;
        
        AddScopeFix(String scopeValue) {
            this.scopeValue = scopeValue;
        }
        
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add scope='" + scopeValue + "' to header";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add scope attribute
        }
    }
    
    private static class ConvertToHeaderCellsFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Convert <td> to <th> for header cells";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would convert td to th
        }
    }
    
    private static class ReplaceSummaryWithCaptionFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Replace summary attribute with caption";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would convert summary to caption
        }
    }
    
    // Essential helper methods and data structures for enhanced analysis
    private CellAnalysis analyzeCells(String content) {
        CellAnalysis analysis = new CellAnalysis();
        analysis.hasRowspan = content.contains("rowspan");
        analysis.hasColspan = content.contains("colspan");
        return analysis;
    }
    
    private HeaderAnalysis analyzeHeaders(String content) {
        HeaderAnalysis analysis = new HeaderAnalysis();
        analysis.hasScopeAttributes = SCOPE_PATTERN.matcher(content).find();
        analysis.hasHeadersAttribute = HEADERS_PATTERN.matcher(content).find();
        analysis.hasHeaderIds = ID_PATTERN.matcher(content).find();
        analysis.needsHeaderIds = content.contains("rowspan") || content.contains("colspan");
        
        // Check if first row has TD cells instead of TH
        Matcher firstRowMatcher = TR_PATTERN.matcher(content);
        if (firstRowMatcher.find()) {
            String firstRow = firstRowMatcher.group(1);
            boolean hasThInFirstRow = TH_PATTERN.matcher(firstRow).find();
            boolean hasTdInFirstRow = TD_PATTERN.matcher(firstRow).find();
            analysis.hasFirstRowTdCells = hasTdInFirstRow && !hasThInFirstRow;
        }
        
        Matcher thMatcher = TH_PATTERN.matcher(content);
        while (thMatcher.find()) {
            String attributes = thMatcher.group(1);
            String textContent = AccessibilityUtils.extractTextContent(thMatcher.group(2));
            analysis.headers.add(new HeaderCell(
                attributes, textContent, thMatcher.start(), thMatcher.end(),
                SCOPE_PATTERN.matcher(attributes).find(),
                ID_PATTERN.matcher(attributes).find()
            ));
        }
        return analysis;
    }
    
    private boolean containsNavigationElements(String content) {
        return content.contains("<nav") || content.contains("menu") || content.contains("<a ");
    }
    
    private boolean containsDataPatterns(String content) {
        return content.matches(".*\\d+[.,]\\d+.*") || content.contains("$") || content.contains("%");
    }
    
    private boolean hasMultiLevelHeaders(String content) {
        return THEAD_PATTERN.matcher(content).find() && TH_PATTERN.matcher(content).find();
    }
    
    private String determineSuggestedScope(HeaderCell header, TableAnalysis analysis) {
        return "col"; // Simplified - real implementation would analyze position
    }
    
    private String extractAriaDescribedByIds(String attributes) {
        Matcher matcher = ARIA_DESCRIBEDBY_PATTERN.matcher(attributes);
        return matcher.find() ? matcher.group(1) : "";
    }
    
    // Enhanced data structures for comprehensive table analysis
    private static class TableAnalysis {
        String attributes, content;
        boolean hasHeaders, hasCaption, hasSummary, hasAriaLabel, hasAriaLabelledBy, hasAriaDescribedBy;
        boolean hasThead, hasTbody, hasTfoot;
        int rowCount, estimatedColumnCount;
        boolean isLayoutTable, isDataTable, isComplexTable;
        CellAnalysis cellAnalysis;
        HeaderAnalysis headerAnalysis;
        CaptionInfo captionInfo;
    }
    
    private static class CellAnalysis {
        List<TableCell> cells = new ArrayList<>();
        boolean hasRowspan, hasColspan;
    }
    
    private static class HeaderAnalysis {
        List<HeaderCell> headers = new ArrayList<>();
        boolean hasFirstRowTdCells, hasScopeAttributes, hasHeadersAttribute, hasHeaderIds, needsHeaderIds;
    }
    
    private static class HeaderCell {
        String attributes, textContent;
        int startOffset, endOffset;
        boolean hasScope, hasId;
        
        HeaderCell(String attributes, String textContent, int startOffset, int endOffset, 
                  boolean hasScope, boolean hasId) {
            this.attributes = attributes; this.textContent = textContent;
            this.startOffset = startOffset; this.endOffset = endOffset;
            this.hasScope = hasScope; this.hasId = hasId;
        }
    }
    
    private static class TableCell {
        String attributes, textContent;
        int startOffset, endOffset;
        boolean isHeader;
        
        TableCell(String attributes, String textContent, int startOffset, int endOffset, boolean isHeader) {
            this.attributes = attributes; this.textContent = textContent;
            this.startOffset = startOffset; this.endOffset = endOffset; this.isHeader = isHeader;
        }
    }
    
    private static class CaptionInfo {
        String textContent;
        int startOffset, endOffset;
        boolean isEmpty, isFirstChild;
        
        CaptionInfo(String textContent, int startOffset, int endOffset, boolean isEmpty, boolean isFirstChild) {
            this.textContent = textContent; this.startOffset = startOffset; this.endOffset = endOffset;
            this.isEmpty = isEmpty; this.isFirstChild = isFirstChild;
        }
    }
    
    // Additional Quick Fix classes for enhanced functionality
    private static class RemoveSemanticElementsFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Remove semantic elements from layout table";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would remove th and caption elements
        }
    }
    
    private static class SuggestCSSAlternativeFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Suggest CSS Grid/Flexbox alternative";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would provide CSS alternatives
        }
    }
    
    private static class ClarifyTablePurposeFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add role='presentation' or proper headers";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add role attribute or headers
        }
    }
    
    private static class AddHeaderAssociationsFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add header associations (scope/headers attributes)";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add scope or headers attributes
        }
    }
    
    private static class AddHeaderIdsFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add id attributes to table headers";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add id attributes to headers
        }
    }
}