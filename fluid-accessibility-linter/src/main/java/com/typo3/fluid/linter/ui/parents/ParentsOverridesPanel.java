package com.typo3.fluid.linter.ui.parents;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import com.intellij.util.messages.MessageBusConnection;
import com.typo3.fluid.linter.catalog.ImplementationKind;
import com.typo3.fluid.linter.context.ContextChangeTopic;
import com.typo3.fluid.linter.context.ContextId;
import com.typo3.fluid.linter.context.AutoRootDetector;
import com.typo3.fluid.linter.resolution.ResolutionChange;
import com.typo3.fluid.linter.resolution.ResolutionChangeTopic;
import com.typo3.fluid.linter.resolution.ResolutionDiff;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.*;

class ParentsOverridesPanel extends JPanel {
    private final Project project;
    private final ParentsTableModel model;
    private final MessageBusConnection connection;

    ParentsOverridesPanel(@NotNull Project project) {
        super(new BorderLayout());
        this.project = project;
        this.model = new ParentsTableModel();

        JBTable table = new JBTable(model);
        table.setAutoCreateRowSorter(true);
        JPanel center = new JPanel(new BorderLayout());
        center.add(new JBScrollPane(table), BorderLayout.CENTER);

        JPanel emptyBanner = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel msg = new JLabel("No roots configured. Detect default Fluid roots?");
        JButton detect = new JButton("Detect Default Roots");
        detect.addActionListener(e -> {
            project.getService(AutoRootDetector.class).detectAndApplyIfEmpty();
            project.getService(com.typo3.fluid.linter.resolution.EffectiveResolutionService.class).refreshDefault();
        });
        emptyBanner.add(msg);
        emptyBanner.add(detect);
        center.add(emptyBanner, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);

        JPanel top = new JPanel(new BorderLayout());
        JLabel hint = new JLabel("Parents & Overrides (auto-refresh)");
        hint.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        JLabel indexing = new JLabel("");
        indexing.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        top.add(hint, BorderLayout.WEST);
        top.add(indexing, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        connection = project.getMessageBus().connect();
        connection.subscribe(ResolutionChangeTopic.TOPIC, this::onResolutionDiff);
        connection.subscribe(ContextChangeTopic.TOPIC, diff -> clear());
        connection.subscribe(DumbService.DUMB_MODE, new DumbService.DumbModeListener() {
            @Override public void enteredDumbMode() { indexing.setText("Indexing… results may be stale"); }
            @Override public void exitDumbMode() { indexing.setText(""); }
        });
    }

    private void onResolutionDiff(@NotNull ResolutionDiff diff) {
        ApplicationManager.getApplication().invokeLater(() -> {
            for (ResolutionChange ch : diff.changes) {
                Badge b = badgeFor(ch.oldFile, ch.newFile);
                String path = displayPath(ch.newFile != null ? ch.newFile : ch.oldFile);
                model.upsert(new Row(ch.key.ctx, ch.key.kind, ch.key.logicalName, b, path));
            }
            revalidate();
            repaint();
        });
    }

    private void clear() {
        ApplicationManager.getApplication().invokeLater(model::clear);
    }

    private enum Badge { INTRODUCED, CHANGED, REMOVED }

    private static Badge badgeFor(VirtualFile oldF, VirtualFile newF) {
        if (oldF == null && newF != null) return Badge.INTRODUCED;
        if (oldF != null && newF == null) return Badge.REMOVED;
        if (oldF != null && newF != null && !Objects.equals(oldF.getUrl(), newF.getUrl())) return Badge.CHANGED;
        return Badge.CHANGED; // default fall-back
    }

    private String displayPath(VirtualFile file) {
        if (file == null) return "(none)";
        String base = project.getBasePath();
        String full = file.getPath();
        if (base != null && full.startsWith(base)) return full.substring(base.length() + 1);
        return full;
    }

    static final class RowKey {
        final ContextId ctx; final ImplementationKind kind; final String name;
        RowKey(ContextId ctx, ImplementationKind kind, String name) { this.ctx = ctx; this.kind = kind; this.name = name; }
        @Override public boolean equals(Object o) { if (this == o) return true; if (!(o instanceof RowKey)) return false; RowKey that = (RowKey) o; return Objects.equals(ctx, that.ctx) && kind == that.kind && Objects.equals(name, that.name); }
        @Override public int hashCode() { return Objects.hash(ctx, kind, name); }
    }

    static final class Row {
        final RowKey key; Badge badge; String path;
        Row(ContextId ctx, ImplementationKind kind, String name, Badge badge, String path) {
            this.key = new RowKey(ctx, kind, name); this.badge = badge; this.path = path;
        }
    }

    static final class ParentsTableModel extends javax.swing.table.AbstractTableModel {
        private final java.util.List<Row> rows = new ArrayList<>();
        private final java.util.Map<RowKey, Row> byKey = new HashMap<>();

        private static final String[] COLS = {"Context", "Kind", "Logical Name", "State", "Effective Path"};

        void clear() { rows.clear(); byKey.clear(); fireTableDataChanged(); }

        void upsert(Row r) {
            Row existing = byKey.get(r.key);
            if (existing == null) {
                rows.add(r);
                byKey.put(r.key, r);
                int idx = rows.size() - 1;
                fireTableRowsInserted(idx, idx);
            } else {
                existing.badge = r.badge;
                existing.path = r.path;
                int idx = rows.indexOf(existing);
                fireTableRowsUpdated(idx, idx);
            }
        }

        @Override public int getRowCount() { return rows.size(); }
        @Override public int getColumnCount() { return COLS.length; }
        @Override public String getColumnName(int column) { return COLS[column]; }

        @Override public Object getValueAt(int rowIndex, int columnIndex) {
            Row r = rows.get(rowIndex);
            switch (columnIndex) {
                case 0: return r.key.ctx.id();
                case 1: return r.key.kind.name().toLowerCase();
                case 2: return r.key.name;
                case 3: return badgeLabel(r.badge);
                case 4: return r.path;
            }
            return "";
        }

        private static String badgeLabel(Badge b) {
            switch (b) {
                case INTRODUCED: return "override introduced";
                case CHANGED: return "override changed";
                case REMOVED: return "override removed";
            }
            return "";
        }
    }
}
