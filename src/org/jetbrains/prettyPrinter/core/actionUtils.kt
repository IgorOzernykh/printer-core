package org.jetbrains.prettyPrinter.core

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.prettyPrinter.core.printer.Printer

/**
 * User: anlun
 */

public interface PrinterOwner {
  public fun getPrinter(): Printer?
  public fun setPrinter(printer: Printer)
}

public fun Project.performUndoWrite(task: () -> Unit) {
  WriteCommandAction.runWriteCommandAction(this) {
    CommandProcessor.getInstance().runUndoTransparentAction { task() }
  }
}

public fun AnActionEvent.getPsiFileFromContext(): PsiFile? {
  val psiFile = getData(CommonDataKeys.PSI_FILE)
  if (psiFile == null) { return null }
  val elementAt = psiFile.findElementAt(0)
  return PsiTreeUtil.getParentOfType(elementAt, PsiFile::class.java)
}

/*
private fun getPsiElementFromContext(e: AnActionEvent): PsiElement? {
  val psiFile = e.getData(CommonDataKeys.PSI_FILE)
  val editor  = e.getData(CommonDataKeys.EDITOR)
  if (psiFile == null || editor == null) { return null }
  val selectionModel = editor.getSelectionModel()
  val hasSelection = selectionModel.hasSelection()
  if (!hasSelection) {
    return psiFile.findElementAt(editor.getCaretModel().getOffset())
  }
  val selectionStart = selectionModel.getSelectionStart()
  val selectionEnd = selectionModel.getSelectionEnd()

  var elementAt: PsiElement = psiFile.findElementAt(selectionStart)
  while (!elementAt.getTextRange().containsRange(selectionStart, selectionEnd)) {
    elementAt = elementAt.getParent()
  }
  return elementAt
}
*/