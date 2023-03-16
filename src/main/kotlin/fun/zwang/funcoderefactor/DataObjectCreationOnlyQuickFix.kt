package `fun`.zwang.funcoderefactor

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiLocalVariable
import com.intellij.psi.PsiMethodCallExpression
import `fun`.zwang.funcoderefactor.ExpressionUtils.Companion.extractClassNameFromDataObjectCreationStatement

class DataObjectCreationOnlyQuickFix : LocalQuickFix {
    override fun getFamilyName(): String {
        return "重构DataObject,使用实际的实体类"
    }

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val variable = descriptor.psiElement as? PsiLocalVariable ?: return
        val initializer = variable.initializer as? PsiMethodCallExpression ?: return
        val className = extractClassNameFromDataObjectCreationStatement(initializer) ?: return

        // 用新创建的实例替换旧的 DataObjectUtil.createDataObject() 调用
        val elementFactory = JavaPsiFacade.getElementFactory(project)
        val newExpression = elementFactory.createExpressionFromText("new $className()", null)
        initializer.replace(newExpression)

        // 更新变量类型
        val newType = elementFactory.createTypeByFQClassName(className, variable.resolveScope)
        variable.typeElement.replace(elementFactory.createTypeElement(newType))
    }
}