package `fun`.zwang.funcoderefactor

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemDescriptorBase
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiLocalVariable
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.PsiTreeUtil
import `fun`.zwang.funcoderefactor.ExpressionUtils.Companion.extractClassNameFromDataObjectCreationStatement
import `fun`.zwang.funcoderefactor.ExpressionUtils.Companion.isDataObjectGetterOrSetter

class DataObjectCreationAndGetSetQuickFix : LocalQuickFix {
    override fun getFamilyName(): String {
        return "重构DataObject,使用实际的实体类,同时修改所有get、set方法"
    }

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val variable = descriptor.psiElement as? PsiLocalVariable ?: return
        val initializer = variable.initializer as? PsiMethodCallExpression ?: return
        val className = extractClassNameFromDataObjectCreationStatement(initializer) ?: return

        val refactoringQuickFix = MyCodeRefactoringQuickFix()
        val creationQuickFix = DataObjectCreationOnlyQuickFix()
        val references = ReferencesSearch.search(variable).findAll()
        for (reference in references) {
            val methodCallExpression =
                PsiTreeUtil.getParentOfType(reference.element, PsiMethodCallExpression::class.java)
            if (methodCallExpression != null && isDataObjectGetterOrSetter(methodCallExpression, allowedTypes = listOf("DataObject", className))) {
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

            }
        }
        creationQuickFix.applyFix(project,descriptor)
    }
}