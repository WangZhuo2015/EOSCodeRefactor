package `fun`.zwang.funcoderefactor

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLocalVariable
import com.intellij.psi.PsiMethodCallExpression
import `fun`.zwang.funcoderefactor.ExpressionUtils.Companion.extractClassNameFromDataObjectCreationStatement

// This class implements a quick fix for replacing DataObject creation statements
// with actual entity class instances, without modifying getter and setter methods.
class DataObjectCreationOnlyQuickFix : LocalQuickFix {

    // Returns the family name of this quick fix.
    override fun getFamilyName(): String {
        return "重构DataObject,使用实际的实体类"
    }

    // Applies the fix to the given problem descriptor.
    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        applyFix(project, descriptor.psiElement)
    }

    fun applyFix(project: Project, element: PsiElement){
        // Get the variable containing the DataObject creation statement.
        val variable = element as? PsiLocalVariable ?: return
        // Get the initializer expression for the variable.
        val initializer = variable.initializer as? PsiMethodCallExpression ?: return
        // Extract the class name from the DataObject creation statement.
        val className = extractClassNameFromDataObjectCreationStatement(initializer) ?: return

        // Replace the old DataObjectUtil.createDataObject() call with a new instance of the actual entity class.
        val elementFactory = JavaPsiFacade.getElementFactory(project)
        val newExpression = elementFactory.createExpressionFromText("new $className()", null)
        val newType = elementFactory.createTypeByFQClassName(className, variable.resolveScope)
        WriteCommandAction.runWriteCommandAction(project) {
            initializer.replace(newExpression)
            // Update the variable type to match the actual entity class.
            variable.typeElement.replace(elementFactory.createTypeElement(newType))
        }
    }
}
