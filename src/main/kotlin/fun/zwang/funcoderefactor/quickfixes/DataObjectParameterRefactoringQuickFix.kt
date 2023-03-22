package `fun`.zwang.funcoderefactor.quickfixes

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemDescriptorBase
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import `fun`.zwang.funcoderefactor.ExpressionUtils
import `fun`.zwang.funcoderefactor.ExpressionUtils.Companion.isDataObjectGetterOrSetter

class DataObjectParameterRefactoringQuickFix : LocalQuickFix {
    override fun getFamilyName(): String {
        return "重构DataObject参数为具体类型"
    }

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        applyFix(project, descriptor.psiElement)

    }

    fun applyFix(project: Project, element: PsiElement) {
        val parameter = element as? PsiParameter ?: return
        val className = ExpressionUtils.convertToCamelCase(parameter.name)

        // Update the getter/setter method calls
        val method = PsiTreeUtil.getParentOfType(parameter, PsiMethod::class.java)
        if (method != null) {
            processMethodReferences(method, project, parameter)
        }

        // Update the parameter type
        val elementFactory = JavaPsiFacade.getElementFactory(project)
        val newType = elementFactory.createTypeByFQClassName(className, parameter.resolveScope)
        WriteCommandAction.runWriteCommandAction(project) {
            parameter.typeElement?.replace(elementFactory.createTypeElement(newType))
        }
    }

    private fun processMethodReferences(method: PsiMethod, project: Project, parameter: PsiParameter) {
        val refactoringQuickFix = MyCodeRefactoringQuickFix()

        method.accept(object : JavaRecursiveElementVisitor() {
            override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
                super.visitMethodCallExpression(expression)
                val qualifier = expression.methodExpression.qualifierExpression as? PsiReferenceExpression
                if (isDataObjectGetterOrSetter(expression) && qualifier?.referenceName == parameter.name) {
                    refactoringQuickFix.applyFix(
                        project, ProblemDescriptorBase(
                            expression,
                            expression,
                            "Refactor get/set method call",
                            arrayOf(refactoringQuickFix),
                            ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                            false,
                            null,
                            true,
                            false
                        )
                    )
                }
            }
        })
    }
}
