package com.converty.app.core.model.selection

enum class SelectionMode {
    ALL,
    FIRST,
    LAST,
    KEEP,
    REMOVE,
}

/** User-facing selection request. Expression numbers are always 1-based. */
data class SelectionRequest(
    val mode: SelectionMode,
    val count: Int? = null,
    val expression: String? = null,
) {
    companion object {
        fun all() = SelectionRequest(SelectionMode.ALL)
        fun first(count: Int) = SelectionRequest(SelectionMode.FIRST, count = count)
        fun last(count: Int) = SelectionRequest(SelectionMode.LAST, count = count)
        fun keep(expression: String) = SelectionRequest(SelectionMode.KEEP, expression = expression)
        fun remove(expression: String) = SelectionRequest(SelectionMode.REMOVE, expression = expression)
    }
}

/** Inclusive, zero-based range used by conversion engines. */
data class SelectionRange(
    val startInclusive: Int,
    val endInclusive: Int,
) {
    init {
        require(startInclusive >= 0) { "Selection ranges are zero-based and cannot be negative" }
        require(endInclusive >= startInclusive) { "Selection range cannot be reversed" }
    }

    val size: Int get() = endInclusive - startInclusive + 1
}

/** Canonical zero-based selection: sorted, non-overlapping and non-adjacent ranges. */
data class ResolvedSelection(
    val totalItems: Int,
    val ranges: List<SelectionRange>,
) {
    init {
        require(totalItems > 0) { "Total item count must be positive" }
        ranges.forEachIndexed { index, range ->
            require(range.endInclusive < totalItems) { "Selection range exceeds total item count" }
            if (index > 0) {
                require(ranges[index - 1].endInclusive + 1 < range.startInclusive) {
                    "Resolved ranges must already be merged and sorted"
                }
            }
        }
    }

    val selectedCount: Int get() = ranges.sumOf(SelectionRange::size)

    fun contains(index: Int): Boolean = ranges.any { index in it.startInclusive..it.endInclusive }

    fun indices(): Sequence<Int> = ranges.asSequence().flatMap { range ->
        (range.startInclusive..range.endInclusive).asSequence()
    }
}

enum class SelectionErrorCode {
    INVALID_TOTAL_ITEMS,
    MISSING_COUNT,
    COUNT_MUST_BE_POSITIVE,
    COUNT_OUT_OF_BOUNDS,
    EMPTY_EXPRESSION,
    EMPTY_TOKEN,
    MALFORMED_TOKEN,
    NUMBER_TOO_LARGE,
    INDEX_MUST_BE_POSITIVE,
    REVERSED_RANGE,
    INDEX_OUT_OF_BOUNDS,
    EMPTY_SELECTION,
}

/** Structured, localizable error. [tokenIndex] is zero-based. */
data class SelectionError(
    val code: SelectionErrorCode,
    val token: String? = null,
    val tokenIndex: Int? = null,
    val value: Long? = null,
    val maximum: Int? = null,
)

sealed interface SelectionParseResult {
    data class Success(val selection: ResolvedSelection) : SelectionParseResult
    data class Failure(val error: SelectionError) : SelectionParseResult
}
