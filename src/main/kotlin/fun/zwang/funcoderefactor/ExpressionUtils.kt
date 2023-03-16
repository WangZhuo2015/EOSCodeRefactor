package `fun`.zwang.funcoderefactor

import com.intellij.psi.*

class ExpressionUtils {
    companion object {
        @JvmStatic
        fun isDataObjectGetterOrSetter(
            expression: PsiMethodCallExpression,
            allowedTypes: List<String> = listOf("DataObject")
        ): Boolean {
            val method = (expression.methodExpression.resolve() as? PsiMethod) ?: return false
            val methodName = method.name

            if (!methodName.startsWith("get") && !methodName.startsWith("set")) return false

            val qualifier = expression.methodExpression.qualifierExpression as? PsiReferenceExpression ?: return false
            val variable = qualifier.resolve() as? PsiVariable ?: return false
            val variableTypeName = variable.type.presentableText

            return variableTypeName in allowedTypes
        }

        @JvmStatic
        fun isDataObjectCreation(expression: PsiMethodCallExpression): Boolean {
            val methodExpression = expression.methodExpression
            val referenceName = methodExpression.referenceName
            return referenceName == "createDataObject"
        }

        @JvmStatic
        fun extractClassNameFromDataObjectCreationStatement(expression: PsiMethodCallExpression): String? {
            val argumentList = expression.argumentList
            if (argumentList.expressions.size != 1) return null

            val classNameArg = argumentList.expressions[0]
            val fullClassName = when (classNameArg) {
                is PsiLiteralExpression -> classNameArg.value as? String
                is PsiReferenceExpression -> convertToCamelCase(classNameArg.referenceName ?: return null)
                else -> return null
            } ?: return null

            val className = fullClassName.split(".").last()
            return className.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
        private fun convertToCamelCase(input: String): String {
            return input.split("_").joinToString("") { it.lowercase().replaceFirstChar { ch -> ch.uppercase() } }
        }
    }
}