// This class provides utility methods for working with DataObject-related expressions
// in the codebase.
package `fun`.zwang.funcoderefactor

import com.intellij.psi.*

/**
 * This class contains utility methods for working with DataObject-related expressions,
 * including determining if a given method call expression is a getter or setter method for a DataObject instance,
 * checking if a method call expression is a DataObject creation statement,
 * extracting the class name from a DataObject creation statement, and converting an input string to CamelCase format.
 */
class ExpressionUtils {
    companion object {
        // Checks if the given method call expression is a getter or setter method
        // for a DataObject instance.
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

        // Checks if the given method call expression is a DataObject creation statement.
        @JvmStatic
        fun isDataObjectCreation(expression: PsiMethodCallExpression): Boolean {
            val methodExpression = expression.methodExpression
            val referenceName = methodExpression.referenceName
            return referenceName == "createDataObject"
        }

        fun isDataObjectAssignment(expression: PsiAssignmentExpression): Boolean {
            val leftExpression = expression.lExpression
            val rightExpression = expression.rExpression
            if (leftExpression is PsiReferenceExpression && rightExpression is PsiMethodCallExpression) {
                val rightValueType = leftExpression.type?.presentableText
                return rightValueType == "DataObject"
            }
            return false
        }

        // Extracts the class name from a DataObject creation statement.
        @JvmStatic
        fun extractClassNameFromDataObjectCreationStatement(expression: PsiMethodCallExpression): String? {
            val argumentList = expression.argumentList
            if (argumentList.expressions.size != 1) return null

            val fullClassName = when (val classNameArg = argumentList.expressions[0]) {
                is PsiLiteralExpression -> classNameArg.value as? String
                is PsiReferenceExpression -> convertToCamelCase(classNameArg.referenceName ?: return null)
                else -> return null
            } ?: return null

            val className = fullClassName.split(".").last()
            return className.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }

        // Converts an input string to CamelCase format.
        fun convertToCamelCase(input: String): String {
            return input.split("_").joinToString("") { it.replaceFirstChar { ch -> ch.uppercase() } }
        }
    }
}
