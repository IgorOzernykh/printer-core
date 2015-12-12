package org.jetbrains.prettyPrinter.core.printer

import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileVisitor
import com.intellij.psi.PsiFileFactory
import org.jetbrains.prettyPrinter.core.util.string.replaceIndentTabs
import java.io.BufferedReader
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStreamReader

/**
 * User: anlun
 */
public fun readFile(vf: VirtualFile): String {
    try {
        val inputStream = vf.getInputStream()
        if (inputStream == null) { return "" }
        val reader = BufferedReader(InputStreamReader(inputStream))
        val firstLine = reader.readLine()
        if (firstLine == null) { return "" }
        val builder = StringBuilder(firstLine)

        var line = reader.readLine()
        while (line != null) {
            builder.append("\n")
            builder.append(line)
            line = reader.readLine()
        }
        reader.close()
        return builder.toString()
    }
    catch (f1 : FileNotFoundException) {}
    catch (f2 : IOException) {}

    return ""
}

public fun fillPrinterTemplatesByFile(file: VirtualFile, printer: Printer, factory: PsiFileFactory, tabSize: Int) {
    VfsUtilCore.visitChildrenRecursively(file, object: VirtualFileVisitor<Int>() {
        public override fun visitFile(file: VirtualFile) : Boolean {
            if (file.isDirectory()) { return true }
            val fileSpaceIndentRepresentation = readFile(file).replaceIndentTabs(tabSize)
            val psiFile = factory.createFileFromText(file.getName(), file.getFileType(), fileSpaceIndentRepresentation)
            printer.fillTemplateLists(psiFile)
            return true
        }
    })
}
//
//public fun createPrinterByFile(
//     file: VirtualFile, settings: PrinterSettings, tabSize: Int
//): JavaPrinter {
//    val printer = JavaPrinter(null, settings)
//    val psiFileFactory = PsiFileFactory.getInstance(settings.project)
//    if (psiFileFactory == null) { return printer }
//    fillPrinterTemplatesByFile(file, printer, psiFileFactory, tabSize)
//    return printer
//}

