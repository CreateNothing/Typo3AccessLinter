package com.typo3.fluid.linter.resolution;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public final class ResolutionDiff {
    public final List<ResolutionChange> changes;

    public ResolutionDiff(@NotNull List<ResolutionChange> changes) {
        this.changes = Collections.unmodifiableList(changes);
    }
}

