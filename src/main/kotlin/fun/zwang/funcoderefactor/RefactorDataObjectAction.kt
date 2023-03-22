package `fun`.zwang.funcoderefactor

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import `fun`.zwang.funcoderefactor.ExpressionUtils.Companion.isDataObjectCreation
import `fun`.zwang.funcoderefactor.ExpressionUtils.Companion.isDataObjectGetterOrSetter
import `fun`.zwang.funcoderefactor.quickfixes.DataObjectCreationAndGetSetQuickFix
import `fun`.zwang.funcoderefactor.quickfixes.DataObjectParameterRefactoringQuickFix
import `fun`.zwang.funcoderefactor.quickfixes.MyCodeRefactoringQuickFix

class RefactorDataObjectAction : AnAction() {
    private val dataObjectCreationAndGetSetQuickFix = DataObjectCreationAndGetSetQuickFix()
    private val myCodeRefactoringQuickFix = MyCodeRefactoringQuickFix()
    private val dataObjectParameterRefactoringQuickFix = DataObjectParameterRefactoringQuickFix()

    override fun actionPerformed(e: AnActionEvent) {
        // 获取当前项目和编辑器
        val project = e.project ?: return
        val editor = e.getRequiredData(CommonDataKeys.EDITOR)

        // 获取当前文件
        val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.document) as? PsiJavaFile ?: return

        refactorDataObjects(project, psiFile)
    }

    private fun refactorDataObjects(project: Project, psiFile: PsiJavaFile) {
        val methodCallExpressions = PsiTreeUtil.collectElementsOfType(psiFile, PsiMethodCallExpression::class.java)

        println("Problem list:")
        methodCallExpressions.filter(::isDataObjectGetterOrSetter).forEach {
            println(it.text)
            WriteCommandAction.runWriteCommandAction(project) {
                myCodeRefactoringQuickFix.applyFix(project, it)
            }
        }

        val localVariables = PsiTreeUtil.collectElementsOfType(psiFile, PsiLocalVariable::class.java)
        localVariables.filter(::isDataObjectCreationVariable).forEach {
            println(it.text)
            dataObjectCreationAndGetSetQuickFix.applyFix(project, it)
        }

        val methods = PsiTreeUtil.collectElementsOfType(psiFile, PsiMethod::class.java)
        methods.flatMap { it.parameterList.parameters.toList() }
            .filter { it.type.presentableText == "DataObject" }
            .forEach {
                println(it.text)
                dataObjectParameterRefactoringQuickFix.applyFix(project, it)
            }
    }

    private fun isDataObjectCreationVariable(variable: PsiLocalVariable): Boolean {
        val initializer = variable.initializer as? PsiMethodCallExpression ?: return false
        return isDataObjectCreation(initializer)
    }
}
