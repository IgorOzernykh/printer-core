package org.jetbrains.prettyPrinter.core.printer

import com.intellij.psi.*
import org.jetbrains.format.Format
import org.jetbrains.format.FormatSet
import org.jetbrains.prettyPrinter.core.util.base.SimpleWidthToSuit
import org.jetbrains.prettyPrinter.core.util.base.WidthToSuit
import org.jetbrains.prettyPrinter.core.util.psiElement.hasElement
import java.util.HashMap

/**
 * User: anlun
 */
interface CommentConnectionUtils {
    fun getMaxWidth(): Int
    fun getInitialSet(f: Format = Format.empty): FormatSet

    class CommentConnection(
      val contentBefore: FormatSet?
    , val contentAfter : FormatSet?
    )

    class VariantConstructionContext(
      val commentContext: MutableMap<PsiElement, CommentConnection>
    , val widthToSuit   : WidthToSuit
    )

    fun defaultContext(): VariantConstructionContext =
            VariantConstructionContext(HashMap(), SimpleWidthToSuit(getMaxWidth()))

    fun updateCommentConnection(context: CommentConnection?, update: CommentConnection?): CommentConnection? {
        if (context == null) { return  update }
        if ( update == null) { return context }

        val contextBefore = context.contentBefore
        val  updateBefore =  update.contentBefore
        val newBefore: FormatSet? = if (contextBefore != null)
                                         contextBefore - (updateBefore ?: getInitialSet())
                                    else updateBefore

        val contextAfter = context.contentAfter
        val  updateAfter =  update.contentAfter
        val newAfter: FormatSet? = if (contextAfter != null)
                                        contextAfter - (updateAfter ?: getInitialSet())
                                   else updateAfter

        if (newBefore == null && newAfter == null) { return null }
        return CommentConnection(newBefore, newAfter)
    }

    fun updateElementCommentConnection(
              p: PsiElement
            , newCommentConnection: CommentConnection?
            , context: MutableMap<PsiElement, CommentConnection>
    ) {
        val updatedConnection = updateCommentConnection(context[p], newCommentConnection) ?: return
        context.put(p, updatedConnection)
    }

    fun surroundVariantsByAttachedComments(
                     p: PsiElement
            , variants: FormatSet
            ,  context: VariantConstructionContext
    ): FormatSet {
        val curCommentConnection = context.commentContext[p] ?: return variants

        var result = variants
        val contentBefore = curCommentConnection.contentBefore
        if (contentBefore != null) { result = contentBefore - result }
        val contentAfter = curCommentConnection.contentAfter
        if (contentAfter != null) { result -= contentAfter }

        return result
    }

    fun getCommentContext(p: PsiElement): MutableMap<PsiElement, CommentConnection> {
        val context = HashMap<PsiElement, CommentConnection>()

        val children = p.children
        val meaningChildren = children.filterNot { child ->
            child is PsiWhiteSpace || child is PsiJavaToken || !(hasElement(child))
        } //TODO: may be need another filtering
        if (meaningChildren.isEmpty()) { return context }

        val firstCommentChildren = meaningChildren.takeWhile { c -> c is PsiComment }
        firstCommentChildren.map { c ->
            val variants = getVariantsByText(c)
            updateElementCommentConnection(p, CommentConnection(variants, null), context)
        }
        val  lastCommentChildren = meaningChildren
                .drop(firstCommentChildren.size).reversed().takeWhile { c -> c is PsiComment }
        lastCommentChildren.map { c ->
            val variants = getVariantsByText(c)
            updateElementCommentConnection(p, CommentConnection(null, variants), context)
        }

        val indices = meaningChildren.indices
                .drop(firstCommentChildren.size)
                .reversed()
                .drop(lastCommentChildren.size)
                .reversed()
        for (i in indices) {
            val child = meaningChildren[i]
            if (child !is PsiComment) { continue }

            val childVariants = getVariantsByText(child)

            if (i != meaningChildren.lastIndex) {
                val nextSibling = blockStatementToCodeBlock(meaningChildren[i + 1])
                val beforeContext = context.get(child)?.contentBefore
                val contentForNextSibling = if (beforeContext != null)
                                                beforeContext - childVariants
                                            else childVariants
                context.put(nextSibling, CommentConnection(contentForNextSibling, null))
                continue
            }

            if (i != 0) {
                val prevSibling = blockStatementToCodeBlock(meaningChildren[i - 1])
                val prevSiblingCommentConnection = context.get(prevSibling)
                context.put(prevSibling, CommentConnection(prevSiblingCommentConnection?.contentBefore, childVariants))
                continue
            }

            val connection = CommentConnection(childVariants, null)
            updateElementCommentConnection(p, connection, context)
        }

        return context
    }

    private fun blockStatementToCodeBlock(p: PsiElement): PsiElement =
            if (p is PsiBlockStatement) p.codeBlock else p

    fun getVariantsByText(p: PsiElement): FormatSet
}