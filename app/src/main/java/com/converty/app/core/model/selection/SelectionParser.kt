package com.converty.app.core.model.selection

object SelectionParser {
    private val singleNumber = Regex("^[+-]?\\d+$")
    private val numberRange = Regex("^([+-]?\\d+)\\s*[-–—]\\s*([+-]?\\d+)$")
    private val command = Regex("^(all|first|last|keep|remove)(?:\\s+(.+))?$", RegexOption.IGNORE_CASE)

    /**
     * Resolves 1-based user input to canonical zero-based ranges.
     * Adjacent and overlapping ranges are merged.
     */
    fun resolve(request: SelectionRequest, totalItems: Int): SelectionParseResult {
        if (totalItems <= 0) {
            return failure(SelectionErrorCode.INVALID_TOTAL_ITEMS, value = totalItems.toLong())
        }

        return when (request.mode) {
            SelectionMode.ALL -> success(totalItems, listOf(SelectionRange(0, totalItems - 1)))
            SelectionMode.FIRST -> resolveCount(request.count, totalItems, fromEnd = false)
            SelectionMode.LAST -> resolveCount(request.count, totalItems, fromEnd = true)
            SelectionMode.KEEP -> resolveExpression(request.expression, totalItems, remove = false)
            SelectionMode.REMOVE -> resolveExpression(request.expression, totalItems, remove = true)
        }
    }

    /** Convenience parser for persisted/debug commands such as `last 3` or `keep 1,3-5`. */
    fun resolveCommand(value: String, totalItems: Int): SelectionParseResult {
        val match = command.matchEntire(value.trim())
            ?: return failure(SelectionErrorCode.MALFORMED_TOKEN, token = value)
        val argument = match.groupValues.getOrNull(2)?.takeIf(String::isNotBlank)
        val request = when (match.groupValues[1].lowercase()) {
            "all" -> if (argument == null) {
                SelectionRequest.all()
            } else {
                return failure(SelectionErrorCode.MALFORMED_TOKEN, token = value)
            }
            "first" -> countRequest(SelectionMode.FIRST, argument)
                ?: return invalidCommandCount(argument)
            "last" -> countRequest(SelectionMode.LAST, argument)
                ?: return invalidCommandCount(argument)
            "keep" -> SelectionRequest.keep(argument.orEmpty())
            "remove" -> SelectionRequest.remove(argument.orEmpty())
            else -> return failure(SelectionErrorCode.MALFORMED_TOKEN, token = value)
        }
        return resolve(request, totalItems)
    }

    private fun countRequest(mode: SelectionMode, argument: String?): SelectionRequest? {
        if (argument == null || !singleNumber.matches(argument)) return null
        val parsed = argument.toLongOrNull() ?: return null
        if (parsed !in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong()) return null
        return SelectionRequest(mode, count = parsed.toInt())
    }

    private fun invalidCommandCount(argument: String?): SelectionParseResult = when {
        argument == null -> failure(SelectionErrorCode.MISSING_COUNT)
        singleNumber.matches(argument) -> {
            val value = argument.toLongOrNull()
            if (value == null || value !in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong()) {
                failure(SelectionErrorCode.NUMBER_TOO_LARGE, token = argument)
            } else {
                failure(SelectionErrorCode.MALFORMED_TOKEN, token = argument)
            }
        }
        else -> failure(SelectionErrorCode.MALFORMED_TOKEN, token = argument)
    }

    private fun resolveCount(count: Int?, totalItems: Int, fromEnd: Boolean): SelectionParseResult {
        if (count == null) return failure(SelectionErrorCode.MISSING_COUNT)
        if (count <= 0) {
            return failure(SelectionErrorCode.COUNT_MUST_BE_POSITIVE, value = count.toLong())
        }
        if (count > totalItems) {
            return failure(
                SelectionErrorCode.COUNT_OUT_OF_BOUNDS,
                value = count.toLong(),
                maximum = totalItems,
            )
        }
        val start = if (fromEnd) totalItems - count else 0
        val end = if (fromEnd) totalItems - 1 else count - 1
        return success(totalItems, listOf(SelectionRange(start, end)))
    }

    private fun resolveExpression(
        expression: String?,
        totalItems: Int,
        remove: Boolean,
    ): SelectionParseResult {
        if (expression.isNullOrBlank()) return failure(SelectionErrorCode.EMPTY_EXPRESSION)

        val parsedRanges = mutableListOf<SelectionRange>()
        expression.split(',', '،').forEachIndexed { tokenIndex, rawToken ->
            val token = rawToken.trim()
            if (token.isEmpty()) {
                return failure(
                    SelectionErrorCode.EMPTY_TOKEN,
                    token = rawToken,
                    tokenIndex = tokenIndex,
                )
            }
            when {
                singleNumber.matches(token) -> {
                    val number = parseNumber(token, tokenIndex, totalItems)
                    if (number is NumberResult.Failure) return SelectionParseResult.Failure(number.error)
                    val zeroBased = (number as NumberResult.Value).number - 1
                    parsedRanges += SelectionRange(zeroBased, zeroBased)
                }
                else -> {
                    val match = numberRange.matchEntire(token)
                        ?: return failure(
                            SelectionErrorCode.MALFORMED_TOKEN,
                            token = token,
                            tokenIndex = tokenIndex,
                        )
                    val start = parseNumber(match.groupValues[1], tokenIndex, totalItems)
                    if (start is NumberResult.Failure) return SelectionParseResult.Failure(start.error)
                    val end = parseNumber(match.groupValues[2], tokenIndex, totalItems)
                    if (end is NumberResult.Failure) return SelectionParseResult.Failure(end.error)
                    val startNumber = (start as NumberResult.Value).number
                    val endNumber = (end as NumberResult.Value).number
                    if (startNumber > endNumber) {
                        return failure(
                            SelectionErrorCode.REVERSED_RANGE,
                            token = token,
                            tokenIndex = tokenIndex,
                        )
                    }
                    parsedRanges += SelectionRange(startNumber - 1, endNumber - 1)
                }
            }
        }

        val merged = merge(parsedRanges)
        val resolvedRanges = if (remove) complement(merged, totalItems) else merged
        if (resolvedRanges.isEmpty()) return failure(SelectionErrorCode.EMPTY_SELECTION)
        return success(totalItems, resolvedRanges)
    }

    private fun parseNumber(token: String, tokenIndex: Int, totalItems: Int): NumberResult {
        val number = token.toLongOrNull()
            ?: return NumberResult.Failure(
                SelectionError(
                    code = SelectionErrorCode.NUMBER_TOO_LARGE,
                    token = token,
                    tokenIndex = tokenIndex,
                ),
            )
        if (number <= 0) {
            return NumberResult.Failure(
                SelectionError(
                    code = SelectionErrorCode.INDEX_MUST_BE_POSITIVE,
                    token = token,
                    tokenIndex = tokenIndex,
                    value = number,
                ),
            )
        }
        if (number > totalItems) {
            return NumberResult.Failure(
                SelectionError(
                    code = SelectionErrorCode.INDEX_OUT_OF_BOUNDS,
                    token = token,
                    tokenIndex = tokenIndex,
                    value = number,
                    maximum = totalItems,
                ),
            )
        }
        return NumberResult.Value(number.toInt())
    }

    private fun merge(ranges: List<SelectionRange>): List<SelectionRange> {
        if (ranges.isEmpty()) return emptyList()
        val sorted = ranges.sortedWith(compareBy(SelectionRange::startInclusive, SelectionRange::endInclusive))
        val merged = mutableListOf(sorted.first())
        sorted.drop(1).forEach { next ->
            val previous = merged.last()
            if (next.startInclusive <= previous.endInclusive + 1) {
                merged[merged.lastIndex] = SelectionRange(
                    previous.startInclusive,
                    maxOf(previous.endInclusive, next.endInclusive),
                )
            } else {
                merged += next
            }
        }
        return merged
    }

    private fun complement(ranges: List<SelectionRange>, totalItems: Int): List<SelectionRange> {
        val kept = mutableListOf<SelectionRange>()
        var cursor = 0
        ranges.forEach { removed ->
            if (cursor < removed.startInclusive) {
                kept += SelectionRange(cursor, removed.startInclusive - 1)
            }
            cursor = removed.endInclusive + 1
        }
        if (cursor < totalItems) kept += SelectionRange(cursor, totalItems - 1)
        return kept
    }

    private fun success(totalItems: Int, ranges: List<SelectionRange>) =
        SelectionParseResult.Success(ResolvedSelection(totalItems, ranges))

    private fun failure(
        code: SelectionErrorCode,
        token: String? = null,
        tokenIndex: Int? = null,
        value: Long? = null,
        maximum: Int? = null,
    ) = SelectionParseResult.Failure(SelectionError(code, token, tokenIndex, value, maximum))

    private sealed interface NumberResult {
        data class Value(val number: Int) : NumberResult
        data class Failure(val error: SelectionError) : NumberResult
    }
}
