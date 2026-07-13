package com.converty.app.i18n

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocaleSwitchContractTest {
    @Test
    fun `language changes use supported api without activity recreation`() {
        val sourceRoot = sourceRoot()
        val localeProvider = Files.readString(
            sourceRoot.resolve("com/converty/app/ui/theme/AppLocaleController.kt"),
        )
        val app = Files.readString(
            sourceRoot.resolve("com/converty/app/feature/app/ConvertyApp.kt"),
        )
        val manifest = Files.readString(
            sourceRoot.parent.resolve("AndroidManifest.xml"),
        )

        assertTrue(localeProvider.contains("AppCompatDelegate.setApplicationLocales"))
        assertTrue(localeProvider.contains("AppCompatDelegate.getApplicationLocales"))
        assertFalse(localeProvider.contains("createConfigurationContext"))
        assertTrue(manifest.contains("android:configChanges=\"locale|layoutDirection\""))
        assertTrue(app.contains("if (state.settingsReady)"))
    }

    private fun sourceRoot(): Path {
        var current: Path? = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize()
        while (current != null) {
            val candidate = current.resolve("app/src/main/java")
            if (Files.isDirectory(candidate)) return candidate
            current = current.parent
        }
        error("Could not locate app/src/main/java above ${System.getProperty("user.dir")}")
    }
}
