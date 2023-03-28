package `fun`.zwang.funcoderefactor.quickfixes

import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaRecursiveElementVisitor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMethodCallExpression
import `fun`.zwang.funcoderefactor.ExpressionUtils.Companion.isLoggerStatement

class LogStringInsertionBatchQuickFix : LogStringInsertionQuickFix() {
    private val singleFix = LogStringInsertionQuickFix()

    override fun getFamilyName(): String = "Fix all problems like this in this file"
    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val file = descriptor.psiElement?.containingFile ?: return

        val matchingProblems = findMatchingProblems(file)

        for (problem in matchingProblems) {
            singleFix.applyFix(project, problem)
        }
    }

    private fun findMatchingProblems(file: PsiFile): List<PsiElement> {
        val matchingProblems = mutableListOf<PsiElement>()
        file.accept(object : JavaRecursiveElementVisitor() {
            override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
                if (isLoggerStatement(expression)) {
                    matchingProblems.add(expression)
                }
                super.visitMethodCallExpression(expression)
            }
        })
        return matchingProblems
    }
}
