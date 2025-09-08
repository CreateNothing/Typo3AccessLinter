package com.typo3.fluid.linter.ui.fluida11y.tabs;

import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBLabel;
import com.typo3.fluid.linter.ui.fluida11y.FluidA11yAggregatorTopic;
import com.typo3.fluid.linter.ui.fluida11y.vm.AggregatedSnapshot;
import com.typo3.fluid.linter.ui.fluida11y.vm.DiagnosticsVM;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

/**
 * Initial scaffold for Diagnostics tab. Shows counters and indexing state.
 */
public final class DiagnosticsTab extends JPanel {
    private final JBLabel label = new JBLabel("Initializing…");

    public DiagnosticsTab(@NotNull Project project) {
        super(new BorderLayout());
        add(label, BorderLayout.NORTH);
        project.getMessageBus().connect().subscribe(FluidA11yAggregatorTopic.TOPIC, this::onUpdate);
    }

    private void onUpdate(@NotNull AggregatedSnapshot snap) {
        DiagnosticsVM vm = snap.diagnostics;
        String idx = vm.indexing ? "Indexing… " : "";
        label.setText(idx + "Roots(explicit=" + vm.rootsExplicit + ", auto=" + vm.rootsAuto + ") · impacted=" + vm.impactedFiles + " · queue=" + vm.queueDepth);
    }
}

