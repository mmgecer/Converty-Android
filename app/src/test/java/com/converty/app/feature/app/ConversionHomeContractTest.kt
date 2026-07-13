package com.converty.app.feature.app

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversionHomeContractTest {
    @Test
    fun `home is the two sided format picker without legacy clutter`() {
        val root = sourceRoot()
        val home = Files.readString(root.resolve("com/converty/app/feature/app/ConversionHomeScreen.kt"))
        val app = Files.readString(root.resolve("com/converty/app/feature/app/ConvertyApp.kt"))
        val legacyContainer = Files.readString(
            root.resolve("com/converty/app/feature/app/HomeQueueHistoryScreens.kt"),
        )

        assertTrue(home.contains("R.string.app_name"))
        assertTrue(home.contains("labelRes = R.string.source_format"))
        assertTrue(home.contains("labelRes = R.string.target_format"))
        assertTrue(home.contains("direction.reversedOrNull() != null"))
        assertTrue(home.contains("R.string.preserve_appearance_summary"))
        assertFalse(home.contains("R.string.home_title"))
        assertFalse(home.contains("R.string.home_subtitle"))
        assertFalse(home.contains("R.string.coming_soon"))

        assertTrue(app.contains("ActivityResultContracts.OpenMultipleDocuments()"))
        assertFalse(app.contains("ActivityResultContracts.OpenDocument()"))
        assertFalse(legacyContainer.contains("LegacyHomeScreen"))
        assertFalse(legacyContainer.contains("ConverterTileSpec"))
        assertFalse(legacyContainer.contains("R.string.coming_soon"))
        assertTrue(legacyContainer.contains("job.direction.targetFormat.icon()"))
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
