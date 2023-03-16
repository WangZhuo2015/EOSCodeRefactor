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

// This class implements a quick fix for refactoring DataObject creation statements
// and updating all the getter and setter method calls to use the actual entity class.
class DataObjectCreationAndGetSetQuickFix : LocalQuickFix {

    // Returns the family name of this quick fix.
    override fun getFamilyName(): String {
        return "重构DataObject,使用实际的实体类,同时修改所有get、set方法"
    }

    // Applies the fix to the given problem descriptor.
    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        // Get the variable containing the DataObject creation statement.
        val variable = descriptor.psiElement as? PsiLocalVariable ?: return
        // Get the initializer expression for the variable.
        val initializer = variable.initializer as? PsiMethodCallExpression ?: return
        // Extract the class name from the DataObject creation statement.
        val className = extractClassNameFromDataObjectCreationStatement(initializer) ?: return

        // Create instances of quick fixes for refactoring getter/setter methods and
        // replacing DataObject creation statements with actual entity class instances.
        val refactoringQuickFix = MyCodeRefactoringQuickFix()
        val creationQuickFix = DataObjectCreationOnlyQuickFix()

        // Find all references to the DataObject variable.
        val references = ReferencesSearch.search(variable).findAll()

        // Iterate over each reference and apply the appropriate quick fixes.
        for (reference in references) {
            val methodCallExpression =
                PsiTreeUtil.getParentOfType(reference.element, PsiMethodCallExpression::class.java)
            // Check if the method call expression is a getter or setter for the DataObject or the actual entity class.
            if (methodCallExpression != null && isDataObjectGetterOrSetter(
                    methodCallExpression,
                    allowedTypes = listOf("DataObject", className)
                )
            ) {
                // Create a new problem descriptor for the method call expression.
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
                // Apply the refactoring quick fix to the method call expression.
                refactoringQuickFix.applyFix(project, newDescriptor)
            }
        }

        // Apply the creation quick fix to replace the DataObject creation statement
        // with an actual entity class instance.
        creationQuickFix.applyFix(project, descriptor)
    }
}
