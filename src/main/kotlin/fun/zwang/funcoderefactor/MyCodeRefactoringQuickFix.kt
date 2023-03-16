package `fun`.zwang.funcoderefactor

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiReferenceExpression
import java.util.*

class MyCodeRefactoringQuickFix : LocalQuickFix {
    override fun getName(): String = "重构DataObject的get/set函数"

    override fun getFamilyName(): String = name

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val expression = descriptor.psiElement as? PsiMethodCallExpression ?: return
        val methodName = expression.methodExpression.referenceName
        val qualifier = expression.methodExpression.qualifierExpression as? PsiReferenceExpression ?: return

        val factory = JavaPsiFacade.getInstance(project).elementFactory
        when (methodName) {
            "set" -> {
                val propertyName = extractPropertyName(expression) ?: return
                val newExpression = factory.createExpressionFromText(
                    "${qualifier.text}.set${propertyName.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }}(${expression.argumentList.expressions[1].text})",
                    expression
                )
                expression.replace(newExpression)
            }

            "get" -> {
                val propertyName = extractPropertyName(expression) ?: return
                val newExpression = factory.createExpressionFromText(
                    "${qualifier.text}.get${propertyName.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }}()",
                    expression
                )
                expression.replace(newExpression)
            }
        }
    }

    private fun extractPropertyName(expression: PsiMethodCallExpression): String? {
        val argumentList = expression.argumentList
        val keyArgument = argumentList.expressions.getOrNull(0) as? PsiLiteralExpression ?: return null
        return keyArgument.value as? String
    }

}