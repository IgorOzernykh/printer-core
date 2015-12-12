package org.jetbrains.prettyPrinter.core.templateBase

import com.intellij.openapi.util.TextRange
import org.jetbrains.prettyPrinter.core.templateBase.template.SmartInsertPlace
import org.jetbrains.prettyPrinter.core.util.base.InsertPlace
import org.jetbrains.prettyPrinter.core.util.box.Box
import org.jetbrains.prettyPrinter.core.util.string.LineEquation
import org.jetbrains.prettyPrinter.core.util.string.TagPlaceLine

/**
 * User: anlun
 */
public interface FullConstructionUtils {
    val FULL_CONSTRUCTION_TAG: String
        get() = "full construction"
    val JUNK_TEXT: String
        get() = "A"

    public fun getFullConstructionInsertPlaceMap(): Map<String, SmartInsertPlace> =
            mapOf(Pair(FULL_CONSTRUCTION_TAG
                    , SmartInsertPlace(TextRange(0, JUNK_TEXT.length)
                                     , InsertPlace.STARTS_WITH_NEW_LINE
                                     , Box.getEverywhereSuitable()
                      )
                  )
            )

    public fun getFullConstructionTagPlaceToLineNumberMap(): Map<TagPlaceLine, Int> {
        val contentRelation = getContentRelation(JUNK_TEXT, getFullConstructionInsertPlaceMap())
        return contentRelation.first
    }

    public fun getFullConstructionLineEquationMap(): Map<Int, LineEquation> {
        val contentRelation = getContentRelation(JUNK_TEXT, getFullConstructionInsertPlaceMap())
        return contentRelation.second
    }

    public fun getContentRelation(
            text: String, insertPlaceMap: Map<String, SmartInsertPlace>
    ): Pair< Map<TagPlaceLine, Int>
             , Map<Int, LineEquation>
           >
}