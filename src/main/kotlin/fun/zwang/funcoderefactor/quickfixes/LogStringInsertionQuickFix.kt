package `fun`.zwang.funcoderefactor.quickfixes

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project
import com.intellij.psi.*

open class LogStringInsertionQuickFix : LocalQuickFix {

    override fun getFamilyName(): String {
        return "重构日志字符串插值"
    }

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        applyFix(project, descriptor.psiElement)
    }

fun applyFix(project: Project, element: PsiElement) {
    val methodCallExpression = element as PsiMethodCallExpression
    val methodName = methodCallExpression.methodExpression.referenceName ?: return
    val arguments = methodCallExpression.argumentList.expressions
    if (arguments.size == 1) {
        val argument = arguments[0]
        if (argument is PsiPolyadicExpression && argument.operationTokenType == JavaTokenType.PLUS) {
            // Handle the case with a single argument using '+' for concatenation
            val newExpression = convertLogMessageWithPlus(argument)

            val elementFactory = JavaPsiFacade.getElementFactory(project)
            val newMethodCall = elementFactory.createExpressionFromText("logger.$methodName($newExpression)", null)
            methodCallExpression.replace(newMethodCall)
        }
    }
}


    private fun convertLogMessageWithPlus(argument: PsiPolyadicExpression): String {
        val operands = argument.operands
        val stringBuilder = StringBuilder()
        val nonStringArgs = mutableListOf<String>()

        for (i in operands.indices) {
            val current = operands[i]
            if (current is PsiLiteralExpression && current.type?.canonicalText == "java.lang.String") {
                val stringValue = current.text
                stringBuilder.append(stringValue.substring(1, stringValue.length - 1))
            } else {
                stringBuilder.append("{}")
                nonStringArgs.add(current.text)
            }
        }

        val result = StringBuilder()
        result.append("\"").append(stringBuilder).append("\"")

        // Append the non-String arguments at the end
        if (nonStringArgs.isNotEmpty()) {
            result.append(", ").append(nonStringArgs.joinToString(", "))
        }

        return result.toString()
    }


}