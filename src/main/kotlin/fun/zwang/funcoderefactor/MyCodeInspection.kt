package `fun`.zwang.funcoderefactor

import com.intellij.codeInspection.*
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.PsiTreeUtil


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

    private fun isDataObjectCreation(expression: PsiMethodCallExpression): Boolean {
        val methodExpression = expression.methodExpression
        val referenceName = methodExpression.referenceName
        return referenceName == "createDataObject"
    }


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
                        "${qualifier.text}.set${propertyName.capitalize()}(${expression.argumentList.expressions[1].text})",
                        expression
                    )
                    expression.replace(newExpression)
                }

                "get" -> {
                    val propertyName = extractPropertyName(expression) ?: return
                    val newExpression = factory.createExpressionFromText(
                        "${qualifier.text}.get${propertyName.capitalize()}()",
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

    class DataObjectCreationQuickFix : LocalQuickFix {
        override fun getFamilyName(): String {
            return "重构DataObject,使用实际的实体类"
        }

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val variable = descriptor.psiElement as? PsiLocalVariable ?: return
            val initializer = variable.initializer as? PsiMethodCallExpression ?: return
            val className = extractClassName(initializer) ?: return

            val refactoringQuickFix = MyCodeRefactoringQuickFix()
            val references = ReferencesSearch.search(variable).findAll()
            for (reference in references) {
                val methodCallExpression =
                    PsiTreeUtil.getParentOfType(reference.element, PsiMethodCallExpression::class.java)
                if (methodCallExpression != null) {
//                    if (isDataObjectGetterOrSetter(methodCallExpression, allowedTypes = listOf("DataObject", className))) {
                    val newDescriptor = ProblemDescriptorBase(
                        methodCallExpression,
                        methodCallExpression,
                        descriptor.descriptionTemplate,
                        arrayOf(refactoringQuickFix),
                        ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                        false,
                        null,
                        true,
                        false
                    )
                    refactoringQuickFix.applyFix(project, newDescriptor)
//                    }
                }
                // 用新创建的实例替换旧的 DataObjectUtil.createDataObject() 调用
                val elementFactory = JavaPsiFacade.getElementFactory(project)
                val newExpression = elementFactory.createExpressionFromText("new $className()", null)
                initializer.replace(newExpression)

                // 更新变量类型
                val newType = elementFactory.createTypeByFQClassName(className, variable.resolveScope)
                variable.typeElement.replace(elementFactory.createTypeElement(newType))
            }
        }

        private fun extractClassName(expression: PsiMethodCallExpression): String? {
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