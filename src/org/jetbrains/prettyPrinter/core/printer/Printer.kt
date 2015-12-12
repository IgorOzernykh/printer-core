package org.jetbrains.prettyPrinter.core.printer

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.format.*
import org.jetbrains.format.FormatSet.FormatSetType
import org.jetbrains.prettyPrinter.core.printer.CommentConnectionUtils.VariantConstructionContext
import org.jetbrains.prettyPrinter.core.templateBase.template.SmartInsertPlace
import org.jetbrains.prettyPrinter.core.templateBase.template.Template
import org.jetbrains.prettyPrinter.core.util.base.walker
import org.jetbrains.prettyPrinter.core.util.base.walker2
import org.jetbrains.prettyPrinter.core.util.psiElement.getFillConstant
import org.jetbrains.prettyPrinter.core.util.psiElement.getOffsetInStartLine

/**
 * User: anlun
 */

abstract class Printer(
        private val settings: PrinterSettings
): Memoization(), CommentConnectionUtils {
    public fun hasToUseMultipleListElemVariants(): Boolean = settings.multipleListElemVariants
    public fun hasToUseMultipleExprStmtVariants(): Boolean = settings.multipleExprStmtVariants

    public fun setMultipleListElemVariantNeeds(f: Boolean) { settings.multipleListElemVariants = f }
    public fun setMultipleExprStmtVariantNeeds(f: Boolean) { settings.multipleExprStmtVariants = f }
    public fun setFormatSetType(f: FormatSetType) { settings.formatSetType = f }
    public fun setMaxWidth(f: Int) { settings.width = f }

    override public fun getMaxWidth(): Int = settings.width
    public fun    getProject(): Project   = settings.project
    public fun   getEmptySet(): FormatSet =
            when (settings.formatSetType) {
                FormatSetType.D1   -> FormatMap1D   (getMaxWidth())
                FormatSetType.D2   -> FormatMap2D_LL(getMaxWidth())
                FormatSetType.D3   -> FormatMap3D   (getMaxWidth())
                FormatSetType.D3AF -> FormatMap3D_AF(getMaxWidth())
                FormatSetType.List -> FormatList    (getMaxWidth())
                FormatSetType.SteppedD3AF -> SteppedFormatMap(FormatSet.stepInMap, getMaxWidth())

                else -> FormatMap3D(getMaxWidth())
            }
    override public fun getInitialSet(f: Format): FormatSet {
        val fs = getEmptySet()
        fs.add(f)
        return fs
    }

    abstract public fun fillTemplateLists(templateFile: PsiFile)
    abstract public fun createElementFromText(p: PsiElement, text: String): PsiElement?
    abstract public fun countTemplates(): Int
    abstract public fun addTemplate(p: PsiElement)
    abstract public fun getTemplate(p: PsiElement): Template<SmartInsertPlace>?
    abstract protected fun applyTmplt(p: PsiElement)
    abstract protected fun reprintElementWithChildren_AllMeaningful(psiElement: PsiElement)
    abstract protected fun getTemplateVariants(p: PsiElement, context: VariantConstructionContext): FormatSet

    public fun reprint(javaFile: PsiFile) { reprintElementWithChildren(javaFile) }

    /// public only for testing purposes!!!
    public fun reprintElementWithChildren(psiElement: PsiElement) {
        reprintElementWithChildren_AllMeaningful(psiElement) // variant for partial template
        //        reprintElementWithChildren_OnlyPsiFile(psiElement) // variant for situations with full template
    }

    private fun reprintElementWithChildren_OnlyPsiFile(psiElement: PsiElement) {
        walker(psiElement) { p -> if (p is PsiFile) applyTmplt(p) }
    }

    public fun getVariants(p: PsiElement, context: VariantConstructionContext = defaultContext()): FormatSet {
        val pCommentContext = getCommentContext(p)
        val widthToSuit = context.widthToSuit
        val variantConstructionContext = VariantConstructionContext(pCommentContext, widthToSuit)

        val mv = getMemoizedVariants(p)
        if (mv != null) { return surroundVariantsByAttachedComments(p, mv, context) }

        val resultWithoutOuterContextComments: FormatSet
        val templateVariant = getTemplateVariants(p, variantConstructionContext)
        if (templateVariant.isNotEmpty()) {
            resultWithoutOuterContextComments = surroundVariantsByAttachedComments(
                    p, templateVariant, variantConstructionContext
            )

            addToCache(p, resultWithoutOuterContextComments)
        } else {
            val s = p.getText() ?: ""
            if (s.contains(" ")) { log.add(s) }
            resultWithoutOuterContextComments = getVariantsByText(p)

            //TODO: For test purposes!!!
            addToCache(p, resultWithoutOuterContextComments)
        }

        val variants = surroundVariantsByAttachedComments(p, resultWithoutOuterContextComments, context)
        return variants
    }

    override public fun getVariantsByText(p: PsiElement): FormatSet {
        val offsetInStartLine = p.getOffsetInStartLine()
        val normalizedFillConstant = Math.max(p.getFillConstant(), 0)
        return getInitialSet(Format.text(p.getText(), offsetInStartLine + normalizedFillConstant))
    }

    public var constructions : Int = 0
    public fun areTemplatesFilled(): Boolean = areTemplatesFilled
    protected var areTemplatesFilled  : Boolean = false

    public fun reprintCurrentPsi(currPsi: PsiElement, oldP: PsiElement) {
        val list = walker2(currPsi, oldP)
        for (el in list) {
            applyNewTmplt(el, oldP)
        }
    }

    public fun applyNewTmplt(currPsi: PsiElement, oldPsi: PsiElement) {
        val currPsiTmpltStr = getTemplate(currPsi).toString()
        val oldPsiTmpltStr = getTemplate(oldPsi).toString()
        if (currPsiTmpltStr == oldPsiTmpltStr) {
            applyTmplt(currPsi)
        }
    }
}
