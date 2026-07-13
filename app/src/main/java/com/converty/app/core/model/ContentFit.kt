package com.converty.app.core.model

/** How source content is placed in the destination page/slide bounds. */
enum class ContentFit {
    /** Preserve aspect ratio and show all content; unused area may remain. */
    CONTAIN,

    /** Preserve aspect ratio and fill the bounds; edges may be cropped. */
    COVER,

    /** Fill both axes even when this changes the aspect ratio. */
    STRETCH,
}
