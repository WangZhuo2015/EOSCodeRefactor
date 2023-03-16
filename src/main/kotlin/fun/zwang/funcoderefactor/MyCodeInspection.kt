package `fun`.zwang.funcoderefactor

import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiLocalVariable
import com.intellij.psi.PsiMethodCallExpression
import `fun`.zwang.funcoderefactor.ExpressionUtils.Companion.isDataObjectCreation
import `fun`.zwang.funcoderefactor.ExpressionUtils.Companion.isDataObjectGetterOrSetter


// This class represents a custom code inspection tool for refactoring DataObject instances
// and their getter/setter methods.
class MyCodeInspection : AbstractBaseJavaLocalInspectionTool() {
    // This method creates a visitor that processes Java elements in the inspected code.
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        // An anonymous JavaElementVisitor object is returned to process the Java elements.
        return object : JavaElementVisitor() {
            // This method is called when a method call expression is encountered in the inspected code.
            override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
                // Check if the current method call expression is a DataObject getter/setter or a DataObject creation.
                when {
                    // If it is a DataObject getter/setter, register a problem with a quick fix for refactoring the getter/setter method.
                    isDataObjectGetterOrSetter(expression) -> {
                        holder.registerProblem(
                            expression, "Refactor to typed method", MyCodeRefactoringQuickFix()
                        )
                    }

                    // If it is a DataObject creation, register a problem with a quick fix for refactoring the object creation.
                    isDataObjectCreation(expression) -> {
                        val parent = expression.parent
                        // If the parent of the expression is a local variable, register problems with quick fixes for refactoring.
                        if (parent is PsiLocalVariable) {
                            // Register a problem with a quick fix for refactoring only the object creation.
                            holder.registerProblem(
                                parent, "Refactor to typed object creation", DataObjectCreationOnlyQuickFix()
                            )
                            // Register a problem with a quick fix for refactoring both the object creation and getter/setter methods.
                            holder.registerProblem(
                                parent, "Refactor to typed object creation", DataObjectCreationAndGetSetQuickFix()
                            )
                        }
                    }
                }
            }
        }
    }
}
