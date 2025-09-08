package com.typo3.fluid.linter.context;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileVisitor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.typo3.fluid.linter.update.FileChangeFilter.classify;
import static com.typo3.fluid.linter.update.FileChangeFilter.Reason;

@Service(Service.Level.PROJECT)
public final class FluidContextManager implements DumbAware {
    private static final Logger LOG = Logger.getInstance(FluidContextManager.class);

    private final Project project;

    private final Map<ContextId, RootPathSet> contexts = new HashMap<>();
    // Contributions per config file
    private final Map<String, RootPathSet> contributions = new HashMap<>();

    private RootPathSet autoDetectedRoots;

    public FluidContextManager(@NotNull Project project) {
        this.project = project;
        // Initial scan (cheap heuristic): gather from known folders
        rescanAll();
    }

    public synchronized @NotNull Map<ContextId, RootPathSet> getAll() {
        return new HashMap<>(contexts);
    }

    public synchronized @Nullable RootPathSet get(@NotNull ContextId id) { return contexts.get(id); }

    public synchronized void rescanAll() {
        contributions.clear();
        VirtualFile base = project.getBaseDir();
        if (base == null) return;
        VfsUtilCore.visitChildrenRecursively(base, new VirtualFileVisitor<>() {
            @Override
            public boolean visitFile(@NotNull VirtualFile file) {
                if (!file.isDirectory()) {
                    Reason r = classify(file);
                    if (r == Reason.TYPOSCRIPT || r == Reason.SITE_SETTINGS) {
                        parseContribution(file);
                    }
                }
                return true;
            }
        });
        rebuildEffectiveAndPublish();
    }

    public synchronized void onConfigFilesChanged(@NotNull Collection<VirtualFile> files) {
        boolean changed = false;
        for (VirtualFile f : files) {
            Reason r = classify(f);
            if (r == Reason.TYPOSCRIPT || r == Reason.SITE_SETTINGS) {
                changed |= parseContribution(f);
            }
        }
        if (changed) rebuildEffectiveAndPublish();
    }

    private boolean parseContribution(@NotNull VirtualFile file) {
        String path = file.getPath();
        RootPathSet prev = contributions.get(path);
        RootPathSet next = extractPaths(file);
        if (!Objects.equals(prev, next)) {
            if (next == null) contributions.remove(path); else contributions.put(path, next);
            LOG.debug("Context contribution updated from " + path + ": " + next);
            return true;
        }
        return false;
    }

    private void rebuildEffectiveAndPublish() {
        RootPathSet effective = mergeContributions(contributions.values());
        if ((effective.templates().isEmpty() && effective.layouts().isEmpty() && effective.partials().isEmpty())
                && autoDetectedRoots != null) {
            effective = autoDetectedRoots;
        }
        Map<ContextId, RootPathSet> changed = new HashMap<>();
        RootPathSet prev = contexts.get(ContextId.DEFAULT);
        if (!Objects.equals(prev, effective)) {
            contexts.put(ContextId.DEFAULT, effective);
            changed.put(ContextId.DEFAULT, effective);
        }
        if (!changed.isEmpty()) {
            project.getMessageBus().syncPublisher(ContextChangeTopic.TOPIC).contextsChanged(new ContextDiff(changed));
            LOG.info("Fluid contexts updated: " + changed);
        }
    }

    public synchronized void applyAutoDetectedRoots(@NotNull RootPathSet roots) {
        this.autoDetectedRoots = roots;
        rebuildEffectiveAndPublish();
    }

    private static RootPathSet mergeContributions(@NotNull Collection<RootPathSet> sets) {
        // Merge preserving relative order of sources; duplicates removed while keeping last occurrence
        List<String> t = new ArrayList<>();
        List<String> l = new ArrayList<>();
        List<String> p = new ArrayList<>();
        for (RootPathSet s : sets) {
            appendAllUnique(t, s.templates());
            appendAllUnique(l, s.layouts());
            appendAllUnique(p, s.partials());
        }
        return new RootPathSet(t, l, p);
    }

    private static void appendAllUnique(List<String> target, List<String> src) {
        for (String v : src) if (!target.contains(v)) target.add(v);
    }

    private @Nullable RootPathSet extractPaths(@NotNull VirtualFile file) {
        try (InputStream in = file.getInputStream();
             BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append('\n');
            String text = sb.toString();
            Reason r = classify(file);
            if (r == Reason.TYPOSCRIPT) return parseTypoScript(text);
            if (r == Reason.SITE_SETTINGS) return parseYaml(text);
            return null;
        } catch (IOException e) {
            LOG.warn("Failed reading config file: " + file.getPath(), e);
            return null;
        }
    }

    // Extremely lightweight extraction using regex; aims for common patterns only
    static RootPathSet parseTypoScript(@NotNull String text) {
        List<IndexedValue> templates = new ArrayList<>();
        List<IndexedValue> layouts = new ArrayList<>();
        List<IndexedValue> partials = new ArrayList<>();

        Pattern pLine = Pattern.compile("(?im)\\b(templateRootPaths|layoutRootPaths|partialRootPaths)\\s*\\.\\s*(\\d+)\\s*=\\s*(.+)");
        Matcher m = pLine.matcher(text);
        while (m.find()) {
            String kind = m.group(1).toLowerCase();
            int idx = Integer.parseInt(m.group(2));
            String value = stripQuotes(m.group(3).trim());
            addIndexed(kind, idx, value, templates, layouts, partials);
        }

        // Also support nested block style:
        // templateRootPaths { 10 = ... \n 20 = ... }
        Pattern pNested = Pattern.compile("(?is)\\b(templateRootPaths|layoutRootPaths|partialRootPaths)\\s*\\{(.*?)\\}");
        Matcher n = pNested.matcher(text);
        while (n.find()) {
            String kind = n.group(1).toLowerCase();
            String body = n.group(2);
            Matcher kv = Pattern.compile("(?m)\\b(\\d+)\\s*=\\s*(.+)").matcher(body);
            while (kv.find()) {
                int idx = Integer.parseInt(kv.group(1));
                String value = stripQuotes(kv.group(2).trim());
                addIndexed(kind, idx, value, templates, layouts, partials);
            }
        }

        return new RootPathSet(toOrdered(templates), toOrdered(layouts), toOrdered(partials));
    }

    static RootPathSet parseYaml(@NotNull String text) {
        // Minimal YAML mapping extraction for keys templateRootPaths/layoutRootPaths/partialRootPaths
        List<IndexedValue> templates = new ArrayList<>();
        List<IndexedValue> layouts = new ArrayList<>();
        List<IndexedValue> partials = new ArrayList<>();

        for (String section : new String[]{"templateRootPaths", "layoutRootPaths", "partialRootPaths"}) {
            Pattern sec = Pattern.compile("(?m)^\\s*" + section + "\\s*:\\s*$");
            Matcher sm = sec.matcher(text);
            while (sm.find()) {
                int start = sm.end();
                int indent = leadingSpacesOfLine(text, sm.start());
                int pos = start;
                while (pos < text.length()) {
                    int lineStart = lineStart(text, pos);
                    int lineEnd = lineEnd(text, pos);
                    String line = text.substring(lineStart, lineEnd);
                    int ls = leadingSpaces(line);
                    if (line.trim().isEmpty()) { pos = lineEnd + 1; continue; }
                    if (ls <= indent) break; // out of section
                    Matcher nv = Pattern.compile("^\\s*(\\d+)\\s*:\\s*(.+)$").matcher(line);
                    if (nv.find()) {
                        int idx = Integer.parseInt(nv.group(1));
                        String value = stripQuotes(nv.group(2).trim());
                        addIndexed(section.toLowerCase(), idx, value, templates, layouts, partials);
                    }
                    pos = lineEnd + 1;
                }
            }

            // Inline list form: templateRootPaths: [path1, path2]
            Pattern inline = Pattern.compile("(?m)^\\s*" + section + "\\s*:\\s*\\[(.*?)]\\s*$");
            Matcher im = inline.matcher(text);
            while (im.find()) {
                String body = im.group(1);
                int idx = 0;
                for (String part : body.split(",")) {
                    String value = stripQuotes(part.trim());
                    addIndexed(section.toLowerCase(), idx += 10, value, templates, layouts, partials);
                }
            }
        }

        // Also accept single-path keys often used in site settings: partialRootPath/templateRootPath/layoutRootPath
        collectSinglePathYaml(text, "partialRootPath", partials);
        collectSinglePathYaml(text, "templateRootPath", templates);
        collectSinglePathYaml(text, "layoutRootPath", layouts);

        return new RootPathSet(toOrdered(templates), toOrdered(layouts), toOrdered(partials));
    }

    private static void collectSinglePathYaml(String text, String key, List<IndexedValue> out) {
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("(?m)^\\s*" + key + "\\s*:\\s*(.+)$")
                .matcher(text);
        int idx = 0;
        while (m.find()) {
            String value = stripQuotes(m.group(1).trim());
            out.add(new IndexedValue(idx += 10, value));
        }
    }

    private static void addIndexed(String kind, int idx, String value,
                                   List<IndexedValue> templates,
                                   List<IndexedValue> layouts,
                                   List<IndexedValue> partials) {
        if (value.isEmpty()) return;
        IndexedValue iv = new IndexedValue(idx, value);
        switch (kind) {
            case "templaterootpaths": templates.add(iv); break;
            case "layoutrootpaths": layouts.add(iv); break;
            case "partialrootpaths": partials.add(iv); break;
        }
    }

    private static List<String> toOrdered(List<IndexedValue> list) {
        list.sort(Comparator.comparingInt(iv -> iv.index));
        List<String> out = new ArrayList<>(list.size());
        for (IndexedValue iv : list) out.add(iv.value);
        return out;
    }

    private static int leadingSpacesOfLine(String text, int anyPosInLine) {
        int start = lineStart(text, anyPosInLine);
        int i = start;
        while (i < text.length() && text.charAt(i) == ' ') i++;
        return i - start;
    }

    private static int lineStart(String text, int pos) {
        int i = pos;
        while (i > 0 && text.charAt(i - 1) != '\n') i--;
        return i;
    }

    private static int lineEnd(String text, int pos) {
        int i = pos;
        while (i < text.length() && text.charAt(i) != '\n') i++;
        return i;
    }

    private static int leadingSpaces(String s) { int i=0; while (i < s.length() && s.charAt(i) == ' ') i++; return i; }
    private static String stripQuotes(String s) {
        if ((s.startsWith("\"") && s.endsWith("\"")) || (s.startsWith("'") && s.endsWith("'"))) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }

    private static final class IndexedValue {
        final int index; final String value;
        IndexedValue(int index, String value) { this.index = index; this.value = value; }
    }
}
