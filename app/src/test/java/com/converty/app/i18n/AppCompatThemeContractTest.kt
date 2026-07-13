package com.converty.app.i18n

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import javax.xml.parsers.DocumentBuilderFactory

class AppCompatThemeContractTest {
    @Test
    fun `activity themes inherit from AppCompat in day and night resources`() {
        val resourceRoot = resourceRoot()

        listOf("values", "values-night").forEach { directory ->
            val file = resourceRoot.resolve(directory).resolve("themes.xml")
            assertTrue("Missing theme resource: $file", Files.isRegularFile(file))

            val root = secureDocumentBuilderFactory()
                .newDocumentBuilder()
                .parse(file.toFile())
                .documentElement
            val theme = (0 until root.childNodes.length)
                .mapNotNull { root.childNodes.item(it) as? Element }
                .single { it.tagName == "style" && it.getAttribute("name") == "Theme.Converty" }

            assertEquals(
                "$directory Theme.Converty must be valid for AppCompatActivity",
                "Theme.AppCompat.DayNight.NoActionBar",
                theme.getAttribute("parent"),
            )
        }
    }

    private fun secureDocumentBuilderFactory(): DocumentBuilderFactory =
        DocumentBuilderFactory.newInstance().apply {
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            setFeature("http://xml.org/sax/features/external-general-entities", false)
            setFeature("http://xml.org/sax/features/external-parameter-entities", false)
            isXIncludeAware = false
            isExpandEntityReferences = false
        }

    private fun resourceRoot(): Path {
        var current: Path? = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize()
        while (current != null) {
            val candidate = current.resolve("app/src/main/res")
            if (Files.isDirectory(candidate)) return candidate
            current = current.parent
        }
        error("Could not locate app/src/main/res above ${System.getProperty("user.dir")}")
    }
}
