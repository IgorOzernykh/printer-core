package org.jetbrains.prettyPrinter.core.util.psiElement

import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import org.jetbrains.format.FormatSet
import org.jetbrains.prettyPrinter.core.printer.CommentConnectionUtils.VariantConstructionContext
import org.jetbrains.prettyPrinter.core.printer.Printer
import org.jetbrains.prettyPrinter.core.templateBase.template.SmartInsertPlace
import org.jetbrains.prettyPrinter.core.util.box.Box
import org.jetbrains.prettyPrinter.core.util.box.toBox
import org.jetbrains.prettyPrinter.core.util.string.*
import java.util.LinkedList

/**
 * User: anlun
 */

fun hasElement(p: PsiElement?): Boolean = (p?.textRange?.length ?: -1) > 0

fun List<PsiElement>.sortByOffset(): List<PsiElement> = sortedBy { el -> el.getCorrectTextOffset() }

fun Iterable<PsiElement>.getTextRange(): TextRange? =
     fold(null as TextRange?) { range, elem ->
        val elemRange = elem.textRange
        if (range != null && elemRange != null)
            range.union(elemRange)
        else elemRange
     }
// Separate realization for arrays, because they don't implement Iterable interface
fun Array<out PsiElement>.getTextRange(): TextRange? =
     fold(null as TextRange?) { range, elem ->
        val elemRange = elem.textRange
        if (range != null && elemRange != null)
            range.union(elemRange)
        else elemRange
     }

fun PsiElement?.getVariants(printer: Printer, context: VariantConstructionContext): FormatSet =
        if (this != null)
            printer.getVariants(this, context)
        else printer.getEmptySet()

fun PsiElement?.getNotNullTextRange(): TextRange = this?.textRange ?: TextRange(0, 0)

/* ----------- */
fun PsiElement.getCorrectTextOffset(): Int = getCorrectTextOffset(false)
fun PsiElement.getCorrectTextOffset(withoutLeadingComments: Boolean): Int {
    val children = getAllChildren()
    if (children.isEmpty()) { return textOffset }

    if (!withoutLeadingComments) { return children[0].getCorrectTextOffset() }

    for (ch in getChildren()) {
        if (ch is PsiWhiteSpace || ch is PsiComment) { continue }
        return ch.getCorrectTextOffset(withoutLeadingComments)
    }

    return textOffset
}

fun PsiElement.getOffsetInStartLine(): Int = getOffsetInStartLine(false)
fun PsiElement.getOffsetInStartLine(withoutLeadingComments: Boolean): Int {
    val offset = getCorrectTextOffset(withoutLeadingComments)
    val text = containingFile?.text ?: ""
    return text.getOffsetInLine(offset)
}

fun PsiElement.getFillConstant(): Int {
    val range = getNotNullTextRange()
    val text  = containingFile?.text ?: ""
    return text.getFillConstant(range)
}

/* ----------- */

fun PsiElement.maxDropSpaceNumber(): Int = maxDropSpaceNumber(false)
fun PsiElement.maxDropSpaceNumber(withoutLeadingComments: Boolean): Int {
    val startLineOffset = getOffsetInStartLine(withoutLeadingComments)
    return text?.maxDropSpaceNumber(startLineOffset) ?: 0
}

fun PsiElement.doesStartWithNewLine(): Boolean {
    val offset = getCorrectTextOffset()
    val text   = containingFile?.text ?: ""
    return text.isLineStart(offset)
}

// Deleting unneeded spaces. DON'T work with tabs
fun PsiElement.deleteSpaces(): String = deleteSpaces(false)
fun PsiElement.deleteSpaces(withoutLeadingComments: Boolean): String {
    val lenToDrop = maxDropSpaceNumber(withoutLeadingComments)
    val text = getText(withoutLeadingComments)
    return text.deleteSpacesAfterFirstLine(lenToDrop)
}

fun PsiElement.getText(withoutLeadingComments: Boolean): String {
    if (withoutLeadingComments) { return getTextWithoutLeadingComments() }
    return text ?: ""
}

fun PsiElement.getAllChildren() : List<PsiElement> {
    val result = LinkedList<PsiElement>()
    var child = firstChild
    while (child != null) {
        result.add(child)
        child = child.nextSibling
    }
    return result
}

fun PsiElement.getTextWithoutLeadingComments(): String {
    val children = getAllChildren()
    for (ch in children) {
        if (ch is PsiWhiteSpace || ch is PsiComment) { continue }

        //ch isn't whitespace or comment
        val startOffset = ch.textOffset
        return containingFile?.text
            ?.substring(startOffset, textRange?.endOffset ?: startOffset)
        ?: ""
    }
    return text ?: ""
}

fun PsiElement.toSmartInsertPlace(): SmartInsertPlace {
    return SmartInsertPlace(getNotNullTextRange(), getFillConstant(), toBox())
}

fun Iterable<PsiElement>.toSmartInsertPlace(text: String, delta: Int = 0): SmartInsertPlace? {
    val resultRange = getTextRange()?.shiftRight(delta) ?: return null
    val statementsFillConstant = text.getFillConstant(resultRange)
    val blockBox = text.substring(resultRange).toBox(statementsFillConstant)
    return SmartInsertPlace(resultRange, statementsFillConstant, blockBox)
}
// Separate realization for arrays, because they don't implement Iterable interface
fun Array<out PsiElement>.toSmartInsertPlace(text: String, delta: Int = 0): SmartInsertPlace? {
    val resultRange = getTextRange()?.shiftRight(delta) ?: return null
    val statementsFillConstant = text.getFillConstant(resultRange)
    val blockBox = text.substring(resultRange).toBox(statementsFillConstant)
    return SmartInsertPlace(resultRange, statementsFillConstant, blockBox)
}

fun PsiElement.toBox(): Box {
    val text = text ?: ""
    return text.toBox(getFillConstant())
}