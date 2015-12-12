package org.jetbrains.prettyPrinter.core.printer

import java.util.HashMap
import com.intellij.psi.PsiElement
import org.jetbrains.format.FormatSet
import java.util.ArrayList
import org.jetbrains.prettyPrinter.core.printer.CommentConnectionUtils.VariantConstructionContext

/**
 * User: anlun
 */
open public class Memoization {
    public var memorizedVariantUseCount: Int = 0
    public var           cacheMissCount: Int = 0

    private val delta: Int = 10

    public val log: ArrayList<String> = ArrayList()

    public val psiElementVariantCache: HashMap<PsiElement, FormatSet> = HashMap()

    public fun clearCache() {
        clearLog()
        psiElementVariantCache.clear()
    }

    public var replaceTime: Long = 0

    public fun clearLog() {
        log.clear()
        memorizedVariantUseCount = 0
        cacheMissCount = 0
    }

    protected fun getMemoizedVariants(p: PsiElement): FormatSet? {
        val value = psiElementVariantCache.get(p)
        if (value == null) { return null }
        memorizedVariantUseCount++
        return value
    }

    protected fun addToCache(p: PsiElement, value: FormatSet) {
        cacheMissCount++
        psiElementVariantCache.put(p, value)
    }

    protected fun renewCache(old: PsiElement, new: PsiElement) {
        val value = psiElementVariantCache.get(old)
        if (value == null) { return }
//        psiElementVariantCache.remove(old)
        psiElementVariantCache.put(new, value)
    }
}