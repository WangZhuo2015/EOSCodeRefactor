package `fun`.zwang.funcoderefactor

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiMethodCallExpression

class RefactorDataObjectAction : AnAction() {

    private val dataObjectCreationAndGetSetQuickFix = DataObjectCreationAndGetSetQuickFix()

    override fun actionPerformed(e: AnActionEvent) {
        // 获取当前项目和编辑器
        val project = e.project
        val editor = e.getRequiredData(CommonDataKeys.EDITOR)

        // 获取当前文件
        val psiFile = PsiDocumentManager.getInstance(project!!).getPsiFile(editor.document)
        if (psiFile is PsiJavaFile) {
            // 在这里添加寻找所有DataObject对象并重构的逻辑
            refactorDataObjects(project, psiFile)
        }
    }

    private fun refactorDataObjects(project: Project, element: PsiElement) {
        val dataObjectCreationExpressions = mutableListOf<PsiMethodCallExpression>()
        // 寻找所有的 DataObject 创建表达式
        fun findAllDataObjects(element: PsiElement) {
            if (element is PsiMethodCallExpression && ExpressionUtils.isDataObjectCreation(element)) {
                dataObjectCreationExpressions.add(element)
            }
            element.children.forEach { findAllDataObjects(it) }
        }
        findAllDataObjects(element)
        // 应用 DataObject 创建和 get/set 方法的重构逻辑
        dataObjectCreationExpressions.forEach {
            dataObjectCreationAndGetSetQuickFix.applyFix(project, it.parent)
        }
    }
}