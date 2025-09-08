package com.typo3.fluid.linter.update;

import org.junit.Test;

import static org.junit.Assert.*;

public class FileChangeFilterTest {

    @Test
    public void relevantNameInParent_detectsFluidTypoScriptYaml() {
        assertTrue(FileChangeFilter.isRelevantNameInParent("/project/src", "foo.html"));
        assertTrue(FileChangeFilter.isRelevantNameInParent("/project/Configuration/TypoScript", "setup.ts"));
        assertTrue(FileChangeFilter.isRelevantNameInParent("/project/config/sites/site1", "config.yaml"));

        assertFalse(FileChangeFilter.isRelevantNameInParent("/project/assets", "app.ts")); // TypeScript asset
        assertFalse(FileChangeFilter.isRelevantNameInParent("/project/src", "readme.md"));
    }
}

