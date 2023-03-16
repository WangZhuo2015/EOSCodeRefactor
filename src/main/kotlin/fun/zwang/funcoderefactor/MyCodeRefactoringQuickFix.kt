package `fun`.zwang.funcoderefactor

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import java.util.*

// This class implements a quick fix for refactoring DataObject getter and setter methods.
class MyCodeRefactoringQuickFix : LocalQuickFix {

    // Returns the name of this quick fix.
    override fun getName(): String = "重构DataObject的get/set函数"

    // Returns the family name of this quick fix.
    override fun getFamilyName(): String = name

    // Applies the fix to the given problem descriptor.
    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        applyFix(project, descriptor.psiElement)
    }

    private fun applyFix(project: Project, element: PsiElement) {
        // Get the method call expression for the getter or setter.
        val expression = element as? PsiMethodCallExpression ?: return
        // Get the method name (get or set).
        val methodName = expression.methodExpression.referenceName
        // Get the qualifier expression.
        val qualifier = expression.methodExpression.qualifierExpression as? PsiReferenceExpression ?: return

        // Create a new expression based on the method name (get or set).
        val factory = JavaPsiFacade.getInstance(project).elementFactory
        when (methodName) {
            "set" -> {
                // Extract the property name and create a new setter expression.
                val propertyName = extractPropertyName(expression) ?: return
                val newExpression = factory.createExpressionFromText(
                    "${qualifier.text}.set${propertyName.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }}(${expression.argumentList.expressions[1].text})",
                    expression
                )
                // Replace the old expression with the new expression.
                expression.replace(newExpression)
            }

            "get" -> {
                // Extract the property name and create a new getter expression.
                val propertyName = extractPropertyName(expression) ?: return
                val newExpression = factory.createExpressionFromText(
                    "${qualifier.text}.get${propertyName.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }}()",
                    expression
                )
                // Replace the old expression with the new expression.
                expression.replace(newExpression)
            }
        }
    }

    // Extracts the property name from the given method call expression.
    private fun extractPropertyName(expression: PsiMethodCallExpression): String? {
        val argumentList = expression.argumentList
        val keyArgument = argumentList.expressions.getOrNull(0) as? PsiLiteralExpression ?: return null
        return keyArgument.value as? String
    }
}
