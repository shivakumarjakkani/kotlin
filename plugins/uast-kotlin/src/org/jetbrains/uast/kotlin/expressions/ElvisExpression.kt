package org.jetbrains.uast.kotlin.expressions

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.uast.*
import org.jetbrains.uast.kotlin.*
import org.jetbrains.uast.kotlin.kinds.KotlinSpecialExpressionKinds
import org.jetbrains.uast.kotlin.psi.UastKotlinPsiVariable


private fun createVariableReferenceExpression(variable: UVariable, containingElement: UElement?) =
        object : USimpleNameReferenceExpression {
            override val psi: PsiElement? = null
            override fun resolve(): PsiElement? = variable
            override val containingElement: UElement? = containingElement
            override val resolvedName: String? = variable.name
            override val annotations: List<UAnnotation> = emptyList()
            override val identifier: String = variable.name.orAnonymous()
        }

private fun createNullLiteralExpression(containingElement: UElement?) =
        object : ULiteralExpression {
            override val psi: PsiElement? = null
            override val containingElement: UElement? = containingElement
            override val value: Any? = null
            override val annotations: List<UAnnotation> = emptyList()
        }

private fun createNotEqWithNullExpression(variable: UVariable, containingElement: UElement?) =
        object : UBinaryExpression {
            override val psi: PsiElement? = null
            override val containingElement: UElement? = containingElement
            override val leftOperand: UExpression by lz { createVariableReferenceExpression(variable, this) }
            override val rightOperand: UExpression by lz { createNullLiteralExpression(this) }
            override val operator: UastBinaryOperator = UastBinaryOperator.NOT_EQUALS
            override val operatorIdentifier: UIdentifier? = UIdentifier(null, this)
            override fun resolveOperator(): PsiMethod? = null
            override val annotations: List<UAnnotation> = emptyList()
        }

private fun createElvisExpressions(
        left: KtExpression,
        right: KtExpression,
        containingElement: UElement?,
        psiParent: PsiElement): List<UExpression> {

    val declaration = KotlinUDeclarationsExpression(containingElement)
    val tempVariable = KotlinUVariable.create(UastKotlinPsiVariable.create(left, declaration, psiParent),
            declaration)
    declaration.declarations = listOf(tempVariable)

    val ifExpression = object : UIfExpression {
        override val psi: PsiElement? = null
        override val containingElement: UElement? = containingElement
        override val condition: UExpression by lz { createNotEqWithNullExpression(tempVariable, this) }
        override val thenExpression: UExpression? by lz { createVariableReferenceExpression(tempVariable, this) }
        override val elseExpression: UExpression? by lz { KotlinConverter.convertExpression(right, this) }
        override val isTernary: Boolean = false
        override val annotations: List<UAnnotation> = emptyList()
        override val ifIdentifier: UIdentifier = UIdentifier(null, this)
        override val elseIdentifier: UIdentifier? = UIdentifier(null, this)
    }

    return listOf(declaration, ifExpression)
}

fun createElvisExpression(elvisExpression: KtBinaryExpression, containingElement: UElement?): UExpressionList? {
    val left = elvisExpression.left ?: return null
    val right = elvisExpression.right ?: return null

    return object : UExpressionList, KotlinEvaluatableUElement, KotlinUElementWithType {
        override val psi: PsiElement? = elvisExpression
        override val kind = KotlinSpecialExpressionKinds.ELVIS
        override val containingElement: UElement? = containingElement
        override val annotations: List<UAnnotation> = emptyList()
        override val expressions: List<UExpression> by lz {
            createElvisExpressions(left, right, this, elvisExpression.parent)
        }
        override fun asRenderString(): String = kind.name + " " +
                expressions.joinToString(separator = "\n", prefix = "{\n", postfix = "\n}") {
                    it.asRenderString().withMargin
                }
    }
}