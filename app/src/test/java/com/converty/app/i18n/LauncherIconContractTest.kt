package com.converty.app.i18n

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherIconContractTest {
    @Test
    fun `launcher icon keeps legacy adaptive and themed variants`() {
        val projectRoot = projectRoot()
        val resourceRoot = projectRoot.resolve("app/src/main/res")
        val manifest = Files.readString(projectRoot.resolve("app/src/main/AndroidManifest.xml"))

        assertTrue(manifest.contains("android:icon=\"@mipmap/ic_launcher\""))
        assertTrue(manifest.contains("android:roundIcon=\"@mipmap/ic_launcher_round\""))

        listOf(
            "drawable/ic_launcher_background.xml",
            "drawable/ic_launcher_foreground.xml",
            "drawable/ic_launcher_monochrome.xml",
            "mipmap-anydpi/ic_launcher.xml",
            "mipmap-anydpi/ic_launcher_round.xml",
            "mipmap-anydpi-v26/ic_launcher.xml",
            "mipmap-anydpi-v26/ic_launcher_round.xml",
            "mipmap-anydpi-v33/ic_launcher.xml",
            "mipmap-anydpi-v33/ic_launcher_round.xml",
        ).forEach { relativePath ->
            assertTrue("Missing launcher resource: $relativePath", Files.isRegularFile(resourceRoot.resolve(relativePath)))
        }

        val adaptiveIcon = Files.readString(resourceRoot.resolve("mipmap-anydpi-v26/ic_launcher.xml"))
        val themedIcon = Files.readString(resourceRoot.resolve("mipmap-anydpi-v33/ic_launcher.xml"))
        assertTrue(adaptiveIcon.contains("@drawable/ic_launcher_background"))
        assertTrue(adaptiveIcon.contains("@drawable/ic_launcher_foreground"))
        assertFalse(adaptiveIcon.contains("<monochrome"))
        assertTrue(themedIcon.contains("@drawable/ic_launcher_monochrome"))
    }

    private fun projectRoot(): Path {
        var current: Path? = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize()
        while (current != null) {
            if (Files.isDirectory(current.resolve("app/src/main/res"))) return current
            current = current.parent
        }
        error("Could not locate project root above ${System.getProperty("user.dir")}")
    }
}
