package com.typo3.fluid.linter.ui.fluida11y;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

@Service(Service.Level.PROJECT)
public final class A11yUiState implements DumbAware {
    private final Project project;
    private volatile boolean unsavedPreviewEnabled = true;
    private volatile boolean liveUpdatesEnabled = true;

    public A11yUiState(@NotNull Project project) { this.project = project; }

    public boolean isUnsavedPreviewEnabled() { return unsavedPreviewEnabled; }
    public boolean isLiveUpdatesEnabled() { return liveUpdatesEnabled; }

    public void toggleUnsavedPreview() {
        this.unsavedPreviewEnabled = !this.unsavedPreviewEnabled;
        project.getMessageBus().syncPublisher(UiStateTopic.TOPIC).stateChanged(this);
    }

    public void toggleLiveUpdates() {
        this.liveUpdatesEnabled = !this.liveUpdatesEnabled;
        project.getMessageBus().syncPublisher(UiStateTopic.TOPIC).stateChanged(this);
    }
}
