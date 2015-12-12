package org.jetbrains.prettyPrinter.core.util.box

import org.jetbrains.format.Format

/**
 * User: anlun
 */

data class Box(
  val width : Int
, val height: Int
) {
    companion object {
        private val everywhereSuitableBox: Box = Box(-1, -1)
        public fun getEverywhereSuitable(): Box = everywhereSuitableBox
    }

    public fun isEverywhereSuitable(): Boolean = width < 0 && height < 0
    public infix fun isSuitable(box: Box): Boolean {
        if (isEverywhereSuitable()) { return true }

        val areOneLiners = box.height == height && box.height <= 1
        if (areOneLiners) { return true }

        val isHoleMultiLiner = height > 1
        return isHoleMultiLiner
    }
}

fun String.toBox(fillConstant: Int): Box {
    if (this.equals("")) { return Box(0, 0) }

    val lines  = split('\n')
    val height = lines.size

    val firstLineLength = lines[0].length
    val width  = lines.drop(1)
                      .fold(firstLineLength) { curMax, line ->
                        Math.max(curMax, line.length  - fillConstant)
                      }

    return Box(width, height)
}

public fun Format.toBox(): Box = Box(totalWidth, height)
