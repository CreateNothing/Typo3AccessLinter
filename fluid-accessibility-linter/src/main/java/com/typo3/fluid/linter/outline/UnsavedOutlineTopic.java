package com.typo3.fluid.linter.outline;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.messages.Topic;
import org.jetbrains.annotations.NotNull;

public interface UnsavedOutlineTopic {
    Topic<UnsavedOutlineTopic> TOPIC = Topic.create("Fluid Unsaved Outline", UnsavedOutlineTopic.class);

    void outlineUpdated(@NotNull VirtualFile file, @NotNull UnsavedOutlineService.OutlineResult result);
}
