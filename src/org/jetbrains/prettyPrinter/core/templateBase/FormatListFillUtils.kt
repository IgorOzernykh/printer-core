package org.jetbrains.prettyPrinter.core.templateBase

import org.jetbrains.format.Format
import org.jetbrains.format.FormatSet
import org.jetbrains.prettyPrinter.core.templateBase.template.SmartInsertPlace
import org.jetbrains.prettyPrinter.core.util.base.InsertPlace
import org.jetbrains.prettyPrinter.core.util.box.toBox
import java.util.ArrayList

/**
 * User: anlun
 */
interface FormatListFillUtils {
    fun <IPT: SmartInsertPlace> fillVariantsToInsertPlaceList(
              insertPlaceMap: Map<String, IPT>
            , variants      : Map<String, FormatSet>
    ): List<Pair<InsertPlace, FormatSet>>? {
        if (insertPlaceMap.size != variants.size) { return null }

        val list = ArrayList<Pair<InsertPlace, FormatSet>>()
        for (place in insertPlaceMap) {
            val placeTag  = place.key
            val placeInfo = place.value

            val placeVariants = variants.get(placeTag)
            if (placeVariants == null) { return null }

            val placeBox = placeInfo.boxToSuit
            val suitableVariants = placeVariants.filter { v -> placeBox isSuitable v.toBox() }
            if (suitableVariants.isEmpty()) { return null }

            list.add(Pair(placeInfo.toInsertPlace(), suitableVariants))
        }

        return list
    }

    fun <IPT: SmartInsertPlace> fillVariantsToInsertPlaceList_SingleFormat(
              insertPlaceMap: Map<String, IPT>
            , variants      : Map<String, Format>
    ): List<Pair<InsertPlace, Format>>? {
        if (insertPlaceMap.size != variants.size) { return null }

        val list = ArrayList<Pair<InsertPlace, Format>>()
        for (place in insertPlaceMap) {
            val placeTag  = place.key
            val placeInfo = place.value

            val placeVariant = variants.get(placeTag) ?: return null
            list.add(Pair<InsertPlace, Format>(placeInfo.toInsertPlace(), placeVariant))
        }

        return list
    }
}