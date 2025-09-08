package com.typo3.fluid.linter.ui.fluida11y.vm;

/**
 * Immutable view model for the Diagnostics tab.
 */
public final class DiagnosticsVM {
    public final boolean indexing;
    public final long lastUpdatedMillis;
    public final int rootsExplicit;
    public final int rootsAuto;
    public final int impactedFiles;
    public final int queueDepth;

    public DiagnosticsVM(boolean indexing, long lastUpdatedMillis, int rootsExplicit, int rootsAuto, int impactedFiles, int queueDepth) {
        this.indexing = indexing;
        this.lastUpdatedMillis = lastUpdatedMillis;
        this.rootsExplicit = rootsExplicit;
        this.rootsAuto = rootsAuto;
        this.impactedFiles = impactedFiles;
        this.queueDepth = queueDepth;
    }
}

