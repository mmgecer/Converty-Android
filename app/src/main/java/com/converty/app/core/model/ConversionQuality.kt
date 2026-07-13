package com.converty.app.core.model

enum class ConversionQuality(val renderDpi: Int) {
    COMPACT(96),
    BALANCED(150),
    HIGH(240),
    MAXIMUM(300),
}
