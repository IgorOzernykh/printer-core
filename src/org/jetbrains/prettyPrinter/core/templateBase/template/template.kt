package org.jetbrains.prettyPrinter.core.templateBase.template

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.jetbrains.format.FormatSet
import org.jetbrains.prettyPrinter.core.util.base.InsertPlace
import org.jetbrains.prettyPrinter.core.util.base.insertToText
import org.jetbrains.prettyPrinter.core.util.box.Box
import org.jetbrains.prettyPrinter.core.util.string.LineEquation
import org.jetbrains.prettyPrinter.core.util.string.TagPlaceLine
import org.jetbrains.prettyPrinter.core.util.string.replaceMultiple
import java.util.ArrayList

/**
 * User: anlun
 */

open class SmartInsertPlace(
  val range: TextRange

/**
 * If object need to be insert by Beside format concatenation, fillConstant must be -1.
 * If fillConstant is non-negative, it means the fill constant of block.
 */
, val fillConstant : Int
, val boxToSuit    : Box
) {
    fun toInsertPlace(): InsertPlace = InsertPlace(range, fillConstant)

    open fun shiftRight(delta: Int) = SmartInsertPlace(range.shiftRight(delta), fillConstant, boxToSuit)

    protected fun boxToString(): String {
        if (boxToSuit.height == 0) { return "EmptyB"   }
        if (boxToSuit.height == 1) { return "OneLineB" }
        return "MultiB" // Also in case of everywhere suitable box
    }

    override fun toString(): String = "#$fillConstant ${boxToString()}#"
}

open class Template<T: SmartInsertPlace>(
  val text                   : String
, val insertPlaceMap         : Map<String, T>
, val tagPlaceToLineNumberMap: Map<TagPlaceLine, Int>
, val lineEquationMap        : Map<Int, LineEquation>
) {
    override fun toString(): String {
        val replacementSet = insertPlaceMap.entries
                                .map { e -> Pair(e.value.range, e.key + e.value.toString()) }
        val replacementList = replacementSet.toList()
        return text.replaceMultiple(replacementList)
    }
}

open class PsiTemplateGen<T: PsiElement, SIP: SmartInsertPlace>(
  val psi                : T
, insertPlaceMap         : Map<String, SIP>
, tagPlaceToLineNumberMap: Map<TagPlaceLine, Int>
, lineEquationMap        : Map<Int, LineEquation>
): Template<SIP>(psi.text ?: "", insertPlaceMap, tagPlaceToLineNumberMap, lineEquationMap)

open class PsiTemplate<T: SmartInsertPlace>(
  psi                    : PsiElement
, insertPlaceMap         : Map<String, T>
, tagPlaceToLineNumberMap: Map<TagPlaceLine, Int>
, lineEquationMap        : Map<Int, LineEquation>
): PsiTemplateGen<PsiElement, T>(psi, insertPlaceMap, tagPlaceToLineNumberMap, lineEquationMap)

open class ListInsertTemplate(
  val text           : String
, val insertPlaceList: List<InsertPlace>
)

class ListTemplate<ET: PsiElement, IPT: SmartInsertPlace>(
        p: ET
        , insertPlaceMap: Map<String, IPT>
        , tagPlaceToLineNumberMap: Map<TagPlaceLine, Int>
        , lineEquationMap        : Map<Int, LineEquation>
): PsiTemplateGen<ET, IPT>(p, insertPlaceMap, tagPlaceToLineNumberMap, lineEquationMap)

fun insertPlaceListTemplateToFormatList(
         width: Int
,        tmplt: ListInsertTemplate
, elemVariants: List<FormatSet>
): FormatSet {
    val insertPlaceList = tmplt.insertPlaceList
    if (insertPlaceList.size != elemVariants.size) { return FormatSet.empty(width) }

    val fmtListsWithRanges = ArrayList<Pair<InsertPlace, FormatSet>>()
    for (i in 0..insertPlaceList.lastIndex) {
        fmtListsWithRanges.add(Pair(insertPlaceList[i], elemVariants[i]))
    }

    return insertToText(width, tmplt.text, fmtListsWithRanges)
}