package com.typo3.fluid.linter.ui.fluida11y.tabs;

import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import com.typo3.fluid.linter.ui.fluida11y.FluidA11yAggregatorTopic;
import com.typo3.fluid.linter.ui.fluida11y.vm.AggregatedSnapshot;
import com.typo3.fluid.linter.ui.fluida11y.vm.OverridesVM;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

/**
 * Initial scaffold for Parents & Overrides tab.
 */
public final class OverridesTab extends JPanel {
    private final OverridesTableModel model = new OverridesTableModel();

    public OverridesTab(@NotNull Project project) {
        super(new BorderLayout());
        JBTable table = new JBTable(model);
        add(new JBScrollPane(table), BorderLayout.CENTER);
        project.getMessageBus().connect().subscribe(FluidA11yAggregatorTopic.TOPIC, this::onUpdate);
    }

    private void onUpdate(@NotNull AggregatedSnapshot snap) {
        model.setRows(snap.overrides.rows);
    }

    private static final class OverridesTableModel extends javax.swing.table.AbstractTableModel {
        private java.util.List<OverridesVM.Row> rows = java.util.List.of();
        private static final String[] COLS = {"Context", "Kind", "Name", "State", "Effective Path"};

        void setRows(java.util.List<OverridesVM.Row> rows) {
            this.rows = rows; fireTableDataChanged();
        }

        @Override public int getRowCount() { return rows.size(); }
        @Override public int getColumnCount() { return COLS.length; }
        @Override public String getColumnName(int column) { return COLS[column]; }

        @Override public Object getValueAt(int rowIndex, int columnIndex) {
            OverridesVM.Row r = rows.get(rowIndex);
            switch (columnIndex) {
                case 0: return r.ctx.id();
                case 1: return r.kind.name().toLowerCase();
                case 2: return r.logicalName;
                case 3: return r.badge;
                case 4: return r.effectivePath;
            }
            return "";
        }
    }
}

