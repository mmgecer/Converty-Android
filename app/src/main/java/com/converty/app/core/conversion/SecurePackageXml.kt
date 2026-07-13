package com.converty.app.core.conversion

import org.w3c.dom.Document
import java.nio.charset.Charset
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException

/**
 * Parses XML package parts without relying on every JAXP security feature being available.
 * Android's Harmony parser omits several desktop-JVM features, so declaration scanning and
 * a rejecting entity resolver remain mandatory even when the feature setters are unsupported.
 */
internal fun parseSecurePackageXml(
    bytes: ByteArray,
    securityViolation: (String) -> Nothing,
): Document {
    if (PROHIBITED_DECLARATIONS.any(bytes::containsAsciiCaseInsensitive)) {
        securityViolation("DOCTYPE and ENTITY declarations are not allowed")
    }

    val factory = DocumentBuilderFactory.newInstance().apply {
        isNamespaceAware = true
        trySetXIncludeAware(false)
        isExpandEntityReferences = false
        trySetFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
        trySetFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        trySetFeature("http://xml.org/sax/features/external-general-entities", false)
        trySetFeature("http://xml.org/sax/features/external-parameter-entities", false)
        trySetFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
        trySetAttribute(ACCESS_EXTERNAL_DTD_PROPERTY, "")
        trySetAttribute(ACCESS_EXTERNAL_SCHEMA_PROPERTY, "")
    }
    val builder = factory.newDocumentBuilder().apply {
        setEntityResolver { _, _ -> securityViolation("External XML entities are not allowed") }
    }
    return builder.parse(bytes.inputStream())
}

private fun DocumentBuilderFactory.trySetXIncludeAware(value: Boolean) {
    try {
        isXIncludeAware = value
    } catch (_: UnsupportedOperationException) {
        // XInclude is false by default on Android's parser.
    } catch (_: AbstractMethodError) {
        // Some old vendor parsers predate this JAXP method.
    }
}

private fun DocumentBuilderFactory.trySetFeature(name: String, value: Boolean) {
    try {
        setFeature(name, value)
    } catch (_: ParserConfigurationException) {
        // Mandatory declaration scanning and the entity resolver provide the fallback.
    } catch (_: UnsupportedOperationException) {
        // Android vendor parsers do not expose every desktop-JVM feature.
    }
}

private fun DocumentBuilderFactory.trySetAttribute(name: String, value: String) {
    try {
        setAttribute(name, value)
    } catch (_: IllegalArgumentException) {
        // JAXP 1.5 properties are absent on some Android parsers.
    } catch (_: UnsupportedOperationException) {
        // Mandatory declaration scanning and the entity resolver provide the fallback.
    }
}

private fun ByteArray.containsAsciiCaseInsensitive(pattern: ByteArray): Boolean {
    if (pattern.isEmpty() || pattern.size > size) return false
    val lastStart = size - pattern.size
    startLoop@ for (start in 0..lastStart) {
        for (offset in pattern.indices) {
            if (this[start + offset].asciiUppercase() != pattern[offset].asciiUppercase()) {
                continue@startLoop
            }
        }
        return true
    }
    return false
}

private fun Byte.asciiUppercase(): Byte {
    val unsigned = toInt() and 0xFF
    return if (unsigned in 'a'.code..'z'.code) (unsigned - 32).toByte() else this
}

private fun declarationPatterns(value: String): List<ByteArray> = listOf(
    Charsets.UTF_8,
    Charsets.UTF_16LE,
    Charsets.UTF_16BE,
).map { charset: Charset -> value.toByteArray(charset) }

private val PROHIBITED_DECLARATIONS =
    declarationPatterns("<!DOCTYPE") + declarationPatterns("<!ENTITY")

private const val ACCESS_EXTERNAL_DTD_PROPERTY =
    "http://javax.xml.XMLConstants/property/accessExternalDTD"
private const val ACCESS_EXTERNAL_SCHEMA_PROPERTY =
    "http://javax.xml.XMLConstants/property/accessExternalSchema"
