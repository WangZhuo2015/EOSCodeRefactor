package `fun`.zwang.funcoderefactor

import com.intellij.codeInspection.*
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.codeStyle.CodeStyleManager
import org.jetbrains.annotations.NotNull


class MyCodeInspection : AbstractBaseJavaLocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : JavaElementVisitor() {
            override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
                when {
                    isDataObjectGetterOrSetter(expression) -> {
                        holder.registerProblem(
                            expression, "Refactor to typed method", MyCodeRefactoringQuickFix()
                        )
                    }
                    isDataObjectCreation(expression) -> {
                        val parent = expression.parent
                        if (parent is PsiLocalVariable) {
                            println(parent.text)
                            holder.registerProblem(
                                parent, "Refactor to typed object creation", DataObjectCreationQuickFix()
                            )
                        }
                    }
                }
            }
        }
    }

    private fun isDataObjectGetterOrSetter(expression: PsiMethodCallExpression): Boolean {
        val method = expression.resolveMethod() ?: return false
        val methodName = method.name

        if (!methodName.startsWith("get") && !methodName.startsWith("set")) return false

        val qualifier = expression.methodExpression.qualifierExpression as? PsiReferenceExpression ?: return false
        val variable = qualifier.resolve() as? PsiVariable ?: return false
        val variableTypeName = variable.type.presentableText

        // 如果类型为DataObject或新实体类名称
        return variableTypeName == "DataObject" || variableTypeName == extractClassNameFromDataObjectQualifiedName(variableTypeName)
    }

    private fun extractClassNameFromDataObjectQualifiedName(qualifiedName: String): String? {
        val className = qualifiedName.split(".").last()
        return className.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }

    private fun hasStringLiteralArgument(expression: PsiMethodCallExpression, referenceName: String): Boolean {
        val arguments = expression.argumentList.expressions
        val firstArgument = arguments.getOrNull(0)

        return when (referenceName) {
            "set" -> arguments.size == 2 && firstArgument is PsiLiteralExpression
            "get" -> arguments.size == 1 && firstArgument is PsiLiteralExpression
            else -> false
        }
    }

    private fun isDataObjectCreation(expression: PsiMethodCallExpression): Boolean {
        val methodExpression = expression.methodExpression
        val referenceName = methodExpression.referenceName
        return referenceName == "createDataObject"
    }


    class MyCodeRefactoringQuickFix : LocalQuickFix {
        override fun getName(): String = "Refactor code"

        override fun getFamilyName(): String = name

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val expression = descriptor.psiElement as? PsiMethodCallExpression ?: return
            val methodName = expression.methodExpression.referenceName
            val qualifier = expression.methodExpression.qualifierExpression as? PsiReferenceExpression ?: return

            val factory = JavaPsiFacade.getInstance(project).elementFactory
            when (methodName) {
                "set" -> {
                    val propertyName = extractPropertyName(expression) ?: return
                    val newExpression = factory.createExpressionFromText("${qualifier.text}.set${propertyName.capitalize()}(${expression.argumentList.expressions[1].text})", expression)
                    expression.replace(newExpression)
                }
                "get" -> {
                    val propertyName = extractPropertyName(expression) ?: return
                    val newExpression = factory.createExpressionFromText("${qualifier.text}.get${propertyName.capitalize()}()", expression)
                    expression.replace(newExpression)
                }
                "createDataObject" -> {
                    val className = extractClassName(expression) ?: return
                    val shortClassName = className.substring(className.lastIndexOf('.') + 1)
                    val variableName = shortClassName.decapitalize()
                    val newExpression = factory.createExpressionFromText("$shortClassName $variableName = new $shortClassName()", expression)
                    expression.replace(newExpression)
                }
            }
        }

        private fun extractPropertyName(expression: PsiMethodCallExpression): String? {
            val argumentList = expression.argumentList
            val keyArgument = argumentList.expressions.getOrNull(0) as? PsiLiteralExpression ?: return null
            return keyArgument.value as? String
        }

        private fun extractClassName(expression: PsiMethodCallExpression): String? {
            val argumentList = expression.argumentList
            val classNameArgument = argumentList.expressions.getOrNull(0) as? PsiLiteralExpression ?: return null
            return classNameArgument.value as? String
        }
    }
    class DataObjectCreationQuickFix : LocalQuickFix {
        override fun getFamilyName(): String {
            return "Refactor DataObject creation"
        }

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val variable = descriptor.psiElement as? PsiLocalVariable ?: return
            val initializer = variable.initializer as? PsiMethodCallExpression ?: return
            val className = extractClassName(initializer) ?: return

            // 用新创建的实例替换旧的 DataObjectUtil.createDataObject() 调用
            val elementFactory = JavaPsiFacade.getElementFactory(project)
            val newExpression = elementFactory.createExpressionFromText("new $className()", null)
            initializer.replace(newExpression)

            // 更新变量类型
            val newType = elementFactory.createTypeByFQClassName(className, variable.resolveScope)
            variable.typeElement?.replace(elementFactory.createTypeElement(newType))
        }
        private fun extractClassName(expression: PsiMethodCallExpression): String? {
            val argumentList = expression.argumentList
            if (argumentList.expressions.size != 1) return null

            val classNameArg = argumentList.expressions[0] as? PsiLiteralExpression ?: return null
            val fullClassName = classNameArg.value as? String ?: return null

            val className = fullClassName.split(".").last()
            return className.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
    }
}