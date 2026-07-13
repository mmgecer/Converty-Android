package com.converty.app.core.model.selection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SelectionParserTest {
    @Test
    fun all_resolvesWholeDocument() {
        assertRanges(SelectionRequest.all(), 5, 0..4)
    }

    @Test
    fun first_resolvesFromStart() {
        assertRanges(SelectionRequest.first(3), 10, 0..2)
    }

    @Test
    fun last_resolvesAfterTotalIsKnown() {
        assertRanges(SelectionRequest.last(3), 10, 7..9)
    }

    @Test
    fun keep_supportsSingleItemsAndDisjointRanges() {
        assertRanges(SelectionRequest.keep("1, 3-5, 9"), 10, 0..0, 2..4, 8..8)
    }

    @Test
    fun keep_convertsOneBasedInputToZeroBasedRanges() {
        val result = SelectionParser.resolve(SelectionRequest.keep("2-4"), 8).success()
        assertEquals(listOf(1, 2, 3), result.indices().toList())
        assertFalse(result.contains(0))
        assertTrue(result.contains(3))
    }

    @Test
    fun whitespaceAroundTokensAndRangeDashIsAccepted() {
        assertRanges(SelectionRequest.keep(" 1 , 3 - 5 , 9 "), 9, 0..0, 2..4, 8..8)
    }

    @Test
    fun overlapAndDuplicatesAreMerged() {
        assertRanges(SelectionRequest.keep("1-4,3-7,5,7-8"), 10, 0..7)
    }

    @Test
    fun adjacentRangesAreCanonicalized() {
        assertRanges(SelectionRequest.keep("1-2,3,4-5,8"), 10, 0..4, 7..7)
    }

    @Test
    fun unsortedInputIsSortedAndMerged() {
        assertRanges(SelectionRequest.keep("9,1,5-7,3"), 10, 0..0, 2..2, 4..6, 8..8)
    }

    @Test
    fun remove_returnsComplementOfExpression() {
        assertRanges(SelectionRequest.remove("1,3-5,9"), 10, 1..1, 5..7, 9..9)
    }

    @Test
    fun removeWholeDocument_isRejectedInsteadOfBecomingEngineAllSentinel() {
        assertFailure(SelectionRequest.remove("1-5"), 5, SelectionErrorCode.EMPTY_SELECTION)
    }

    @Test
    fun commandParser_supportsAllModesCaseInsensitively() {
        assertEquals(5, SelectionParser.resolveCommand("ALL", 5).success().selectedCount)
        assertRanges(SelectionParser.resolveCommand("first 2", 5), 0..1)
        assertRanges(SelectionParser.resolveCommand("Last 2", 5), 3..4)
        assertRanges(SelectionParser.resolveCommand("keep 1,4-5", 5), 0..0, 3..4)
        assertRanges(SelectionParser.resolveCommand("remove 2-4", 5), 0..0, 4..4)
    }

    @Test
    fun zeroOrNegativeTotalIsRejected() {
        assertFailure(SelectionRequest.all(), 0, SelectionErrorCode.INVALID_TOTAL_ITEMS)
        assertFailure(SelectionRequest.all(), -2, SelectionErrorCode.INVALID_TOTAL_ITEMS)
    }

    @Test
    fun missingOrNonPositiveCountIsRejected() {
        assertFailure(SelectionRequest(SelectionMode.FIRST), 5, SelectionErrorCode.MISSING_COUNT)
        assertFailure(SelectionRequest.first(0), 5, SelectionErrorCode.COUNT_MUST_BE_POSITIVE)
        assertFailure(SelectionRequest.last(-1), 5, SelectionErrorCode.COUNT_MUST_BE_POSITIVE)
    }

    @Test
    fun countBeyondDocumentBoundaryIsRejected() {
        val error = assertFailure(SelectionRequest.first(6), 5, SelectionErrorCode.COUNT_OUT_OF_BOUNDS)
        assertEquals(6L, error.value)
        assertEquals(5, error.maximum)
    }

    @Test
    fun emptyExpressionsAndTokensAreRejected() {
        assertFailure(SelectionRequest.keep("   "), 5, SelectionErrorCode.EMPTY_EXPRESSION)
        val error = assertFailure(SelectionRequest.keep("1,,3"), 5, SelectionErrorCode.EMPTY_TOKEN)
        assertEquals(1, error.tokenIndex)
    }

    @Test
    fun malformedTokensAreRejected() {
        assertFailure(SelectionRequest.keep("1,a,3"), 5, SelectionErrorCode.MALFORMED_TOKEN)
        assertFailure(SelectionRequest.keep("1-2-3"), 5, SelectionErrorCode.MALFORMED_TOKEN)
        assertFailure(SelectionRequest.keep("1..3"), 5, SelectionErrorCode.MALFORMED_TOKEN)
    }

    @Test
    fun zeroAndNegativeIndexesAreRejected() {
        assertFailure(SelectionRequest.keep("0"), 5, SelectionErrorCode.INDEX_MUST_BE_POSITIVE)
        assertFailure(SelectionRequest.keep("-2"), 5, SelectionErrorCode.INDEX_MUST_BE_POSITIVE)
        assertFailure(SelectionRequest.keep("1--2"), 5, SelectionErrorCode.INDEX_MUST_BE_POSITIVE)
    }

    @Test
    fun reversedRangeIsRejected() {
        val error = assertFailure(SelectionRequest.keep("5-3"), 8, SelectionErrorCode.REVERSED_RANGE)
        assertEquals("5-3", error.token)
        assertEquals(0, error.tokenIndex)
    }

    @Test
    fun indexesBeyondDocumentBoundaryAreRejected() {
        val error = assertFailure(SelectionRequest.keep("1,7"), 6, SelectionErrorCode.INDEX_OUT_OF_BOUNDS)
        assertEquals(7L, error.value)
        assertEquals(6, error.maximum)
        assertEquals(1, error.tokenIndex)
    }

    @Test
    fun numericOverflowHasSpecificError() {
        assertFailure(
            SelectionRequest.keep("999999999999999999999999"),
            5,
            SelectionErrorCode.NUMBER_TOO_LARGE,
        )
        val commandResult = SelectionParser.resolveCommand("first 999999999999999999999999", 5)
        assertEquals(SelectionErrorCode.NUMBER_TOO_LARGE, commandResult.failure().code)
        val intOverflow = SelectionParser.resolveCommand("first 9999999999", 5)
        assertEquals(SelectionErrorCode.NUMBER_TOO_LARGE, intOverflow.failure().code)
    }

    @Test
    fun allCommandRejectsUnexpectedArgument() {
        val result = SelectionParser.resolveCommand("all 3", 5)
        assertEquals(SelectionErrorCode.MALFORMED_TOKEN, result.failure().code)
    }

    @Test
    fun localizedRangePunctuationMatchesDisplayedExamples() {
        assertRanges(SelectionRequest.keep("1, 3–5, 9"), 10, 0..0, 2..4, 8..8)
        assertRanges(SelectionRequest.keep("1, 3—5, 9"), 10, 0..0, 2..4, 8..8)
        assertRanges(SelectionRequest.keep("1، 3–5، 9"), 10, 0..0, 2..4, 8..8)
    }

    private fun assertRanges(request: SelectionRequest, total: Int, vararg expected: IntRange) {
        assertRanges(SelectionParser.resolve(request, total), *expected)
    }

    private fun assertRanges(result: SelectionParseResult, vararg expected: IntRange) {
        val actual = result.success().ranges.map { it.startInclusive..it.endInclusive }
        assertEquals(expected.toList(), actual)
    }

    private fun assertFailure(
        request: SelectionRequest,
        total: Int,
        expectedCode: SelectionErrorCode,
    ): SelectionError {
        val error = SelectionParser.resolve(request, total).failure()
        assertEquals(expectedCode, error.code)
        return error
    }

    private fun SelectionParseResult.success(): ResolvedSelection =
        (this as SelectionParseResult.Success).selection

    private fun SelectionParseResult.failure(): SelectionError =
        (this as SelectionParseResult.Failure).error
}
