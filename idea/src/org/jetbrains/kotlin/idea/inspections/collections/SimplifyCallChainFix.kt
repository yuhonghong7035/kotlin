/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.inspections.collections

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.PsiChildRange

class SimplifyCallChainFix(
    private val conversion: AbstractCallChainChecker.Conversion,
    private val removeFirstReceiver: Boolean = false,
    private val modifyArguments: KtPsiFactory.(KtCallExpression) -> Unit = {}
) : LocalQuickFix {
    private val shortenedText = conversion.replacement.substringAfterLast(".")

    override fun getName() = "Merge call chain to '$shortenedText'"

    override fun getFamilyName() = name

    fun apply(secondQualifiedExpression: KtQualifiedExpression) {
        val factory = KtPsiFactory(secondQualifiedExpression)
        val firstExpression = secondQualifiedExpression.receiverExpression

        val operationSign = if (removeFirstReceiver) "" else when (firstExpression) {
            is KtSafeQualifiedExpression -> "?."
            is KtQualifiedExpression -> "."
            else -> ""
        }

        val receiverExpressionOrEmpty =
            (firstExpression as? KtQualifiedExpression)?.receiverExpression?.takeIf { !removeFirstReceiver } ?: ""

        val firstCallExpression = AbstractCallChainChecker.getCallExpression(firstExpression) ?: return
        factory.modifyArguments(firstCallExpression)
        val firstCallArgumentList = firstCallExpression.valueArgumentList

        val secondCallExpression = secondQualifiedExpression.selectorExpression as? KtCallExpression ?: return
        val secondCallArgumentList = secondCallExpression.valueArgumentList

        fun KtValueArgumentList.getTextInsideParentheses(): String {
            val range = PsiChildRange(leftParenthesis?.nextSibling ?: firstChild, rightParenthesis?.prevSibling ?: lastChild)
            return range.joinToString(separator = "") { it.text }
        }

        val lambdaExpression = firstCallExpression.lambdaArguments.singleOrNull()?.getLambdaExpression()
        val additionalArgument = conversion.additionalArgument
        val argumentsText = listOfNotNull(
            secondCallArgumentList.takeIf { it?.arguments?.isNotEmpty() == true },
            firstCallArgumentList.takeIf { it?.arguments?.isNotEmpty() == true }
        ).let {
            when {
                it.isNotEmpty() -> it.joinToString(
                    separator = ", ",
                    prefix = "(",
                    postfix = ")"
                ) { callArgumentList ->
                    callArgumentList.getTextInsideParentheses()
                }
                additionalArgument != null -> "($additionalArgument)"
                else -> ""
            }
        }

        val newCallText = conversion.replacement
        val newQualifiedExpression = if (lambdaExpression != null) factory.createExpressionByPattern(
            "$0$1$2 $3 $4",
            receiverExpressionOrEmpty,
            operationSign,
            newCallText,
            argumentsText,
            lambdaExpression.text
        )
        else factory.createExpressionByPattern(
            "$0$1$2 $3",
            receiverExpressionOrEmpty,
            operationSign,
            newCallText,
            argumentsText
        )

        val result = secondQualifiedExpression.replaced(newQualifiedExpression)
        ShortenReferences.DEFAULT.process(result)
    }

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        (descriptor.psiElement as? KtQualifiedExpression)?.let(this::apply)
    }
}