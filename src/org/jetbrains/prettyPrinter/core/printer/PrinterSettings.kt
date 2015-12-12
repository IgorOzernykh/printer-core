package org.jetbrains.prettyPrinter.core.printer

import com.intellij.openapi.project.Project
import org.jetbrains.format.FormatSet.FormatSetType

/**
 * User: anlun
 */
public class PrinterSettings(
  var         width: Int
, val       project: Project
, var formatSetType: FormatSetType = FormatSetType.D3AF
, var multipleListElemVariants: Boolean = true
, var multipleExprStmtVariants: Boolean = true
) {
    companion object {
        public fun createProjectSettings(
                width: Int, project: Project
        ): PrinterSettings = PrinterSettings(width, project)
    }
}