package com.typo3.fluid.linter.resolution;

import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ResolutionChange {
    public final @NotNull ResolutionKey key;
    public final @Nullable VirtualFile oldFile;
    public final @Nullable VirtualFile newFile;

    public ResolutionChange(@NotNull ResolutionKey key, @Nullable VirtualFile oldFile, @Nullable VirtualFile newFile) {
        this.key = key; this.oldFile = oldFile; this.newFile = newFile;
    }
}

