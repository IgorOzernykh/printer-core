package org.jetbrains.prettyPrinter.core.util.string

import com.intellij.openapi.util.TextRange
import org.jetbrains.prettyPrinter.core.util.base.InsertPlace
import java.util.HashMap
import org.apache.commons.lang.StringUtils
import java.util.Arrays
import org.jetbrains.format.util.toLines
import org.jetbrains.format.util.startWhitespaceLength
import org.jetbrains.format.util.getIndentation
import org.jetbrains.format.util.countLeadingTabs
import org.jetbrains.prettyPrinter.core.util.base.insertFormatsToText
import org.jetbrains.format.Format

/**
 * User: anlun
 */

fun String.substring(range: TextRange): String {
    if (this.length >= range.endOffset)
        return substring(range.startOffset, range.endOffset)
    else
        return ""
}

fun String.replace(range: TextRange, newSubstring: String): String =
        substring(0, range.startOffset) + newSubstring + substring(range.endOffset)

/**
 * Returns a copy of the string, with trailing whitespace and \r
 * omitted for each line of string (before each \n).
 */
fun String.lineEndTrim(): String {
    val trimCarriageReturn = { text: String -> StringUtils.stripEnd(text, "\r") }
    val trimTrailingWhitespace = { text: String -> StringUtils.stripEnd(text, " ") }
    val lines = (toLines().map(trimCarriageReturn)).map(trimTrailingWhitespace)

    if (lines.isEmpty()) { return this }

    val builder = StringBuilder()
    builder.append(lines.firstOrNull())
    lines.drop(1).map { line -> builder.append("\n"); builder.append(line) }
    return builder.toString()
}

fun String.maxDropSpaceNumber(): Int = maxDropSpaceNumber(0)

/**
 * Calculates how much first symbols from lines can be dropped.
 * Example (1):
 *  |[   ]a
 *  |  aa
 *  |   aaaa
 *  |     aaa
 *  | aa
 *  For "a"-string it's 1 because of last line.
 *
 * Example (2):
 *  |b
 *  |  bb
 *  |   b
 *  |     b
 *  | b
 *  For "b"-string it's 0 because of first line.
 */
fun String.maxDropSpaceNumber(startLineOffset: Int): Int {
    val lines = toLines()
    if (lines.isEmpty()) { return 0 }

    val normalizedStartLineOffset = Math.max(startLineOffset, 0)
    val tail = lines.drop(1)
    return Math.min(normalizedStartLineOffset, startWhitespaceLength(tail))
}

fun String.deleteSpaces(startLineOffset: Int): String {
    val dropSpaceNumber = maxDropSpaceNumber(startLineOffset)
    return deleteSpacesAfterFirstLine(dropSpaceNumber)
}

fun String.deleteSpacesAfterFirstLine(lenToDrop: Int): String {
    val text  = toString()
    val lines = text.split('\n')

    val builder = StringBuilder(lines[0])
    lines.drop(1).forEach { line -> builder.append("\n" + line.drop(lenToDrop)) }
    return builder.toString()
}

/** Checks if the symbol at offset position is the start of the line (Indentation isn't counts). */
fun String.isLineStart(offset: Int): Boolean {
    if (length < offset || offset < 0) { return false }

    var curPos = offset - 1
    while (curPos > 0) {
        when (get(curPos)) {
            '\n' -> return true
            ' '  -> curPos--
            else -> return false
        }
    }
    return true
}

/**
 * Returns the fill constant for range in string.
 * Example (2):
 * |______ a
 * |   aa
 * |   aa
 * |   aa
 * For "a"-range function returns 3
 *
 * Example (2):
 * |______ a
 * |   aa
 * |   aa
 * |  aa
 * For "a"-range function returns 2 because of last line
 */
fun String.getFillConstant(range: TextRange): Int {
    if (isLineStart(range.startOffset)) { return InsertPlace.STARTS_WITH_NEW_LINE }

    val elementText = substring(range)
    val lines = elementText.toLines()
    if (lines.size < 2) { return InsertPlace.DOESNT_START_WITH_NEW_LINE }

    val firstLine            = lines[0]
    val firstLineIndentation = firstLine.getIndentation()

    val secondLine            = lines[1]
    val secondLineIndentation = secondLine.getIndentation()
    val minTailIndentation = lines.drop(1).fold(secondLineIndentation) { v, n -> Math.min(v, n.getIndentation()) }

    val offsetInLine = getOffsetInLine(range.startOffset)
    if (minTailIndentation >= offsetInLine) { return InsertPlace.STARTS_WITH_NEW_LINE }

    return Math.max(0, minTailIndentation - firstLineIndentation)
}

/**
 * Returns an insertPlace for a given range
 */
fun String.toInsertPlace(range : TextRange): InsertPlace {
    return InsertPlace(range, getFillConstant(range))
}

/**
 * Returns a list of all insertPlaces for @substring starting from @fromIndex
 */
fun String.substringToInsertPlaces(substring : String, fromIndex : Int): List<InsertPlace>? {
    val index = indexOf(substring, fromIndex)
    if (index == -1) { return null }

    val range = TextRange(index, index + substring.length)
    val nextInsertPlaces = substringToInsertPlaces(substring, index + substring.length + 1)

    when (nextInsertPlaces) {
        null    ->  return listOf(toInsertPlace(range))
        else    ->  return nextInsertPlaces.plus(toInsertPlace(range))
    }
}

/**
 * Returns a copy of the string, with all instances of parameter replaced (parameter = Pair<Parameter, Value>)
 */
fun String.replaceInsertPlace(parameter : Pair<String, String>) : String {
    val insertPlaces = substringToInsertPlaces(parameter.first, 0) ?: return this

    val zipWithFormat = { element : InsertPlace -> Pair(element, Format.text(parameter.second)) }
    val fmtsWithRanges = insertPlaces.map(zipWithFormat)

    return insertFormatsToText(this, fmtsWithRanges).toString()
}

/**
 * Returns a copy of the string, with all parameters replaced (parameters = List<Pair<Parameter, Value>>)
 */
fun String.replaceAllInsertPlace(parameters : List<Pair<String, String>>) : String {
    when (parameters.size) {
        0       ->  return this
        else    ->  return replaceInsertPlace(parameters.first()).replaceAllInsertPlace(parameters.drop(1))
    }
}

/** Calculates position in line the offset symbol. */
fun String.getOffsetInLine(offset: Int): Int {
    if (length <= offset) { return 0 }

    var curPos = offset - 1
    while (curPos >= 0) {
        if (get(curPos) == '\n') { break }
        curPos--
    }
    return offset - (curPos + 1)
}

fun List<String>.toRanges(): List<TextRange> {
    var currentOffset = 0
    return map { line ->
        val lineEnd = currentOffset + line.length
        val lineRange = TextRange(currentOffset, lineEnd)
        currentOffset = lineEnd + 1
        lineRange
    }
}

//------------//

enum class LinePosition {
    FIRST,
    LAST
}

data class TagPlaceLine(
  val tag         : String
, val linePosition: LinePosition
)

data class LineEquation(
  /// Number of positions to subtract
  val penalty          : Int
  /// Elements in line
, val containedElements: Set<TagPlaceLine>
)

fun List<String>.getTagPlaceToLineNumberMap(tagToRangeMap: Map<String, TextRange>): Map<TagPlaceLine, Int>  {
    val lineRanges = toRanges()

    val tagPlaceToLineNumber = HashMap<TagPlaceLine, Int>()
    for (tagWithRange in tagToRangeMap.entries) {
        var count = 0
        val tag   = tagWithRange.key
        val range = tagWithRange.value
        val startOffset = range.startOffset
        val lastOffset = if (range.length > 0) { range.endOffset - 1 } else { startOffset }

        lineRanges.map { lineRange ->
            val intersection = range.intersection(lineRange)
            val linePosition = if (intersection?.contains(startOffset) ?: false) { LinePosition.FIRST }
                          else if (intersection?.contains( lastOffset) ?: false) { LinePosition.LAST  }
                          else { null }
            if (linePosition != null) { tagPlaceToLineNumber.put(TagPlaceLine(tag, linePosition), count) }
            count++
        }
    }

    return tagPlaceToLineNumber
}

/** Transforms line number to line equation. */
fun List<String>.getLineEquations(
  tagToRangeMap       : Map<String, TextRange>
, tagPlaceToLineNumber: Map<TagPlaceLine, Int>
): Map<Int, LineEquation> {
    val lineRanges = toRanges()

    val lineNumberToEquation = HashMap<Int, LineEquation>()
    var count = -1
    for (lineRange in lineRanges) {
        count++
        val list = tagPlaceToLineNumber.map { e ->
            if (e.value == count) { listOf(e.key) } else { listOf<TagPlaceLine>() }
        }
        val tagPlacesInLine = list.flatMap { t -> t }
        if (tagPlacesInLine.isEmpty()) { continue }

        val penalty = lineRange.getLength() - tagPlacesInLine.fold(0) { r, tgp ->
            val range = tagToRangeMap.get(tgp.tag)
            val intersection = range?.intersection(lineRange)
            r + (intersection?.getLength() ?: 0)
        }
        lineNumberToEquation.put(count, LineEquation(penalty, tagPlacesInLine.toSet()))
    }

    return lineNumberToEquation
}

fun String.replaceMultiple(replacement: Iterable<Pair<TextRange, String>>): String {
    val sortedReplacement = replacement.sortedBy { p -> p.first.startOffset }
    val builder = StringBuilder()
    var curPos = 0
    sortedReplacement.forEach { p ->
        val range = p.first
        val start = range.startOffset
        val end = range.endOffset
        val replacementString = p.second

        if (curPos < start) { builder.append(substring(curPos, start)) }
        builder.append(replacementString)
        curPos = end
    }
    if (curPos < this.length) { builder.append(substring(curPos, this.length)) }

    return builder.toString()
}

fun Char.repeat(count: Int): String {
    val buffer = CharArray(count)
    Arrays.fill(buffer, this)
    return String(buffer)
}

fun String.replaceIndentTabs(tabSize: Int): String {
    val tabSpaces = ' '.repeat(tabSize)

    val lines = toLines()
    val builder = StringBuilder()
    var isFirstLine = true
    for (line in lines) {
        val leadingTabs = line.countLeadingTabs()
        if (!isFirstLine) { builder.append("\n") }
        for (i in 0..leadingTabs - 1) {
            builder.append(tabSpaces)
        }
        val tail = line.substring(leadingTabs)
        builder.append(tail)
        isFirstLine = false
    }

    return builder.toString()
}