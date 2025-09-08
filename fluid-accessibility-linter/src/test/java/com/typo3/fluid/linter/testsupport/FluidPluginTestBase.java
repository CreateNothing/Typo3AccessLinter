package com.typo3.fluid.linter.testsupport;

import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Callable;

public abstract class FluidPluginTestBase extends BasePlatformTestCase {
    @Override
    protected String getTestDataPath() {
        String p = System.getProperty("fluid.testDataPath");
        if (p != null && !p.isBlank() && new java.io.File(p).exists()) return p;
        java.io.File f = new java.io.File("../FluidA11yFixtures/FluidA11yFixtures");
        if (f.exists()) return f.getAbsolutePath();
        f = new java.io.File("FluidA11yFixtures/FluidA11yFixtures");
        if (f.exists()) return f.getAbsolutePath();
        return "src/test/resources/testData";
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // Copy fixtures into the in-memory project
        myFixture.copyDirectoryToProject("", "");
        // Enable pass-through for deterministic tests
        try { com.typo3.fluid.linter.update.GraphUpdateServiceTestUtil.enableImmediateTestMode(getProject()); } catch (Throwable ignored) {}
    }

    protected @NotNull VirtualFile findVFile(@NotNull String rel) {
        VirtualFile root = getProject().getBaseDir();
        assertNotNull(root);
        VirtualFile f = root.findFileByRelativePath(rel);
        assertNotNull("Missing file: " + rel, f);
        return f;
    }

    protected VirtualFile findVFileOrNull(@NotNull String rel) {
        VirtualFile root = getProject().getBaseDir();
        return root != null ? root.findFileByRelativePath(rel) : null;
    }

    protected VirtualFile createFile(@NotNull String rel, @NotNull String text) {
        return myFixture.addFileToProject(rel, text).getVirtualFile();
    }

    protected <T> T write(Callable<T> block) {
        return WriteAction.compute((com.intellij.openapi.util.ThrowableComputable<T, RuntimeException>) () -> {
            try {
                return block.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }
}
