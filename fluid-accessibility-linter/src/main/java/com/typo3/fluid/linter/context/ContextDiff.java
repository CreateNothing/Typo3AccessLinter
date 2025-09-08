package com.typo3.fluid.linter.context;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Map;

public final class ContextDiff {
    public final Map<ContextId, RootPathSet> changed; // new effective sets per context

    public ContextDiff(@NotNull Map<ContextId, RootPathSet> changed) {
        this.changed = Collections.unmodifiableMap(changed);
    }
}

