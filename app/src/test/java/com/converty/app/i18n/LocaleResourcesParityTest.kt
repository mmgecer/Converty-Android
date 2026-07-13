package com.converty.app.i18n

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import javax.xml.parsers.DocumentBuilderFactory

class LocaleResourcesParityTest {
    @Test
    fun `all eight locales keep resource placeholder and plural parity`() {
        val resourceRoot = resourceRoot()
        val snapshots = LOCALE_DIRECTORIES.associateWith { directory ->
            parse(resourceRoot.resolve(directory).resolve("strings.xml"))
        }
        val reference = snapshots.getValue("values")

        snapshots.forEach { (locale, localized) ->
            assertEquals("$locale string keys", reference.strings.keys, localized.strings.keys)
            assertEquals("$locale plural keys", reference.plurals.keys, localized.plurals.keys)

            reference.strings.forEach { (name, placeholders) ->
                assertEquals("$locale string/$name placeholders", placeholders, localized.strings.getValue(name))
            }

            reference.plurals.forEach { (name, referenceQuantities) ->
                val localizedQuantities = localized.plurals.getValue(name)
                assertTrue("$locale plurals/$name must define other", "other" in localizedQuantities)
                assertTrue(
                    "$locale plurals/$name contains an invalid quantity",
                    localizedQuantities.keys.all(VALID_QUANTITIES::contains),
                )

                val expectedPlaceholders = referenceQuantities.getValue("other")
                assertEquals(
                    "$locale plurals/$name[other] placeholders",
                    expectedPlaceholders,
                    localizedQuantities.getValue("other"),
                )
                localizedQuantities.forEach { (quantity, placeholders) ->
                    assertTrue(
                        "$locale plurals/$name[$quantity] must keep the count placeholder or omit it",
                        placeholders.isEmpty() || placeholders == expectedPlaceholders,
                    )
                }
            }
        }
    }

    private fun parse(file: Path): ResourceSnapshot {
        assertTrue("Missing locale file: $file", Files.isRegularFile(file))
        val factory = DocumentBuilderFactory.newInstance().apply {
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            setFeature("http://xml.org/sax/features/external-general-entities", false)
            setFeature("http://xml.org/sax/features/external-parameter-entities", false)
            isXIncludeAware = false
            isExpandEntityReferences = false
        }
        val root = factory.newDocumentBuilder().parse(file.toFile()).documentElement
        val strings = linkedMapOf<String, List<String>>()
        val plurals = linkedMapOf<String, Map<String, List<String>>>()

        for (index in 0 until root.childNodes.length) {
            val element = root.childNodes.item(index) as? Element ?: continue
            val name = element.getAttribute("name")
            when (element.tagName) {
                "string" -> {
                    check(strings.put(name, placeholders(element.textContent)) == null) {
                        "Duplicate string resource $name in $file"
                    }
                }
                "plurals" -> {
                    val quantities = linkedMapOf<String, List<String>>()
                    for (itemIndex in 0 until element.childNodes.length) {
                        val item = element.childNodes.item(itemIndex) as? Element ?: continue
                        if (item.tagName != "item") continue
                        val quantity = item.getAttribute("quantity")
                        check(quantities.put(quantity, placeholders(item.textContent)) == null) {
                            "Duplicate plurals quantity $name[$quantity] in $file"
                        }
                    }
                    check(plurals.put(name, quantities) == null) {
                        "Duplicate plurals resource $name in $file"
                    }
                }
            }
        }
        return ResourceSnapshot(strings, plurals)
    }

    private fun placeholders(value: String): List<String> = PLACEHOLDER
        .findAll(value)
        .map { it.value }
        .sorted()
        .toList()

    private fun resourceRoot(): Path {
        var current: Path? = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize()
        while (current != null) {
            val candidate = current.resolve("app/src/main/res")
            if (Files.isDirectory(candidate)) return candidate
            current = current.parent
        }
        error("Could not locate app/src/main/res above ${System.getProperty("user.dir")}")
    }

    private data class ResourceSnapshot(
        val strings: Map<String, List<String>>,
        val plurals: Map<String, Map<String, List<String>>>,
    )

    private companion object {
        val LOCALE_DIRECTORIES = listOf(
            "values",
            "values-tr",
            "values-de",
            "values-b+zh+Hans",
            "values-ar",
            "values-pt",
            "values-fr",
            "values-ru",
        )
        val VALID_QUANTITIES = setOf("zero", "one", "two", "few", "many", "other")
        val PLACEHOLDER = Regex("%(?:\\d+\\$)?[a-zA-Z]")
    }
}
