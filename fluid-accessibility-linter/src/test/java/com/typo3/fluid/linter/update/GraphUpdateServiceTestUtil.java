package com.typo3.fluid.linter.update;

import com.intellij.openapi.project.Project;

public final class GraphUpdateServiceTestUtil {
    public static void enableImmediateTestMode(Project project) {
        GraphUpdateService svc = project.getService(GraphUpdateService.class);
        if (svc != null) svc.enableImmediateTestMode();
    }
    public static void flush(Project project) {
        GraphUpdateService svc = project.getService(GraphUpdateService.class);
        if (svc != null) svc.flushForTests();
    }
    public static int getRunCount(Project project) {
        GraphUpdateService svc = project.getService(GraphUpdateService.class);
        return svc != null ? svc.getRunCountForTests() : -1;
    }
    public static void resetRunCount(Project project) {
        GraphUpdateService svc = project.getService(GraphUpdateService.class);
        if (svc != null) svc.resetRunCountForTests();
    }
    public static boolean lastRunHadChanges(Project project) {
        GraphUpdateService svc = project.getService(GraphUpdateService.class);
        return svc != null && svc.lastRunHadChanges();
    }
}

