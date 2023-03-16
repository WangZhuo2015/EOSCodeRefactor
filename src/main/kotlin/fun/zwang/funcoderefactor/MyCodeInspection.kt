package `fun`.zwang.funcoderefactor

import com.intellij.codeInspection.*
import com.intellij.psi.*
import `fun`.zwang.funcoderefactor.ExpressionUtils.Companion.isDataObjectCreation
import `fun`.zwang.funcoderefactor.ExpressionUtils.Companion.isDataObjectGetterOrSetter


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
                            holder.registerProblem(
                                parent, "Refactor to typed object creation", DataObjectCreationOnlyQuickFix()
                            )
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

