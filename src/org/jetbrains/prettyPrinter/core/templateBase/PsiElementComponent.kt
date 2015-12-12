package org.jetbrains.prettyPrinter.core.templateBase

import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import org.jetbrains.format.Format
import org.jetbrains.format.FormatSet
import org.jetbrains.format.util.toLines
import org.jetbrains.prettyPrinter.core.printer.CommentConnectionUtils.VariantConstructionContext
import org.jetbrains.prettyPrinter.core.printer.Printer
import org.jetbrains.prettyPrinter.core.templateBase.template.SmartInsertPlace
import org.jetbrains.prettyPrinter.core.templateBase.template.Template
import org.jetbrains.prettyPrinter.core.util.base.fillListByWidth
import org.jetbrains.prettyPrinter.core.util.base.insertFormatsToText
import org.jetbrains.prettyPrinter.core.util.base.insertToText
import org.jetbrains.prettyPrinter.core.util.psiElement.deleteSpaces
import org.jetbrains.prettyPrinter.core.util.string.LineEquation
import org.jetbrains.prettyPrinter.core.util.string.TagPlaceLine
import org.jetbrains.prettyPrinter.core.util.string.getLineEquations
import org.jetbrains.prettyPrinter.core.util.string.getTagPlaceToLineNumberMap
import java.util.ArrayList
import java.util.HashMap

/**
 * User: anlun
 */
abstract public class PsiElementComponent<ET: PsiElement, IPT: SmartInsertPlace, T: Template<IPT>>(
        open val printer: Printer
): FullConstructionUtils, FormatListFillUtils {
    open public fun getTmplt(p: ET): T? {
        val text = p.deleteSpaces(true) //deleting leading comments

        try {
            val newElement = getNewElement(text)
            if (newElement == null) { return null }
            val containsComment = newElement.getChildren().any() { ch -> ch is PsiComment }
            if (containsComment) { return null }

            return getTemplateFromElement(newElement)
        } catch (e: ClassCastException) { return null }
    }

    abstract protected fun getNewElement         (text: String): ET?
    abstract public    fun getTemplateFromElement(newP: ET): T?

    open public fun getAndSaveTemplate(newP: ET) {
        val template = getTmplt(newP) ?: return
        val templateString = template.toString()
        val value = templateStringSet.get(templateString)
        if (value != null) { templateStringSet.put(templateString, value + 1); return }

        templateStringSet.put(templateString, 1)
        templates_1.add(template)
    }
    protected val templateStringSet: HashMap<String, Int> = HashMap()
    protected val templates_1      : ArrayList<T>         = ArrayList()

    open public fun getVariants(p: ET, context: VariantConstructionContext): FormatSet {
        val subtreeVariants = prepareSubtreeVariants(p, context)
        val pTagSet = getTags(p)

        val resultSet = printer.getEmptySet()
        val templates = getTemplates()
        for (tmplt in templates) {
            if (!isTemplateSuitable(p, tmplt)) { continue }
            val tmpltTagSet = tmplt.insertPlaceMap.keys
            if (!tmpltTagSet.equals(pTagSet)) { continue }

            val tmpltSubtreeVariants = updateSubtreeVariants(p, tmplt, subtreeVariants, context)
            val newFmtSet = getVariants(tmplt.text, tmplt.insertPlaceMap, tmpltSubtreeVariants)
            if (newFmtSet == null) { continue }
            resultSet.addAll(newFmtSet)
        }
        return resultSet
    }

    abstract protected fun getTags(p: ET): Set<String>

    abstract protected fun prepareSubtreeVariants(p: ET, context: VariantConstructionContext): Map<String, FormatSet>
    abstract protected fun updateSubtreeVariants(
              p       : ET
            , tmplt   : T
            , variants: Map<String, FormatSet>
            , context: VariantConstructionContext
    ): Map<String, FormatSet>

    abstract protected fun isTemplateSuitable(p: ET, tmplt: T): Boolean

    public fun <IPT: SmartInsertPlace> getVariants(
              text          : String
            , insertPlaceMap: Map<String, IPT>
            , variants      : Map<String, FormatSet>
    ): FormatSet? {
        val formatListsWithRanges = fillVariantsToInsertPlaceList(insertPlaceMap, variants)
        if (formatListsWithRanges == null) { return null }
        return insertToText(printer.getMaxWidth(), text, formatListsWithRanges)
    }

    public fun <IPT: SmartInsertPlace> getVariant_SingleFormat(
              text          : String
            , insertPlaceMap: Map<String, IPT>
            , variants      : Map<String, Format>
    ): Format? {
        val formatListsWithRanges = fillVariantsToInsertPlaceList_SingleFormat(insertPlaceMap, variants)
        if (formatListsWithRanges == null) { return null }
        return insertFormatsToText(text, formatListsWithRanges)
    }

    open public fun getTemplates(): List<T> = templates_1

    override public fun getContentRelation(
            text: String, insertPlaceMap: Map<String, SmartInsertPlace>
    ): Pair< Map<TagPlaceLine, Int>
           , Map<Int, LineEquation>
           >
    {
        val lines = text.toLines()
        val tagToRangeMap = insertPlaceMap.mapValues { e -> e.value.range }
        val tagPlaceToLineNumber = lines.getTagPlaceToLineNumberMap(tagToRangeMap)
        val lineNumberToEquation = lines.getLineEquations(tagToRangeMap, tagPlaceToLineNumber)

        return Pair(tagPlaceToLineNumber, lineNumberToEquation)
    }

    //TODO: Unfortunately need to be public
    open public fun getElementsVariants(
            elementList: List<PsiElement>
            ,   context: VariantConstructionContext
            , separator: (Int) -> Format
    ): FormatSet = getElementsVariants(elementList, context, { l: Format -> l }, separator)

    open public fun getElementsVariants(
              elementList: List<PsiElement>
            ,     context: VariantConstructionContext
            , elementWrap: (Format) -> Format
            ,   separator: (Int) -> Format
    ): FormatSet {
        if (elementList.isEmpty()) { return printer.getInitialSet() }

        var elemVariantsList = elementList.map { e -> printer.getVariants(e, context) }
        if (!printer.hasToUseMultipleListElemVariants())
            elemVariantsList = elemVariantsList.map { fs -> fs.headSingleton() }

        return elemVariantsList.fillListByWidth(printer, context.widthToSuit, elementWrap, separator)
    }
}