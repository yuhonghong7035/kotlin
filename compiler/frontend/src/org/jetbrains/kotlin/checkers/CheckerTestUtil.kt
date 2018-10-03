/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.checkers

import com.google.common.collect.LinkedListMultimap
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.SmartList
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.containers.Stack
import org.jetbrains.kotlin.cfg.LeakingThisDescriptor
import org.jetbrains.kotlin.checkers.CheckerTestUtil.INDIVIDUAL_DIAGNOSTIC_PATTERN
import org.jetbrains.kotlin.checkers.CheckerTestUtil.INDIVIDUAL_PARAMETER_PATTERN
import org.jetbrains.kotlin.checkers.CheckerTestUtil.NEW_INFERENCE_PREFIX
import org.jetbrains.kotlin.checkers.CheckerTestUtil.OLD_INFERENCE_PREFIX
import org.jetbrains.kotlin.checkers.CheckerTestUtil.SHOULD_BE_ESCAPED
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.diagnostics.rendering.AbstractDiagnosticWithParametersRenderer
import org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages
import org.jetbrains.kotlin.diagnostics.rendering.Renderers
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.psi.KtWhenExpression
import org.jetbrains.kotlin.resolve.AnalyzingUtils
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.MultiTargetPlatform
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactory
import org.jetbrains.kotlin.resolve.calls.smartcasts.ExplicitSmartCasts
import org.jetbrains.kotlin.resolve.calls.smartcasts.ImplicitSmartCasts
import org.jetbrains.kotlin.types.expressions.KotlinTypeInfo
import org.jetbrains.kotlin.util.slicedMap.WritableSlice
import java.util.*
import java.util.regex.Pattern

private abstract class AbstractDiagnosticDescriptor internal constructor(val start: Int, val end: Int) {
    val textRange: TextRange
        get() = TextRange(start, end)
}

private class ActualDiagnosticDescriptor internal constructor(start: Int, end: Int, val diagnostics: List<AbstractTestDiagnostic>) :
    AbstractDiagnosticDescriptor(start, end) {

    val textDiagnosticsMap: MutableMap<AbstractTestDiagnostic, TextDiagnostic>
        get() {
            val diagnosticMap = TreeMap<AbstractTestDiagnostic, TextDiagnostic>()
            for (diagnostic in diagnostics) {
                diagnosticMap[diagnostic] = TextDiagnostic.asTextDiagnostic(diagnostic)
            }

            return diagnosticMap
        }
}

private class TextDiagnosticDescriptor internal constructor(private val positionalTextDiagnostic: PositionalTextDiagnostic) :
    AbstractDiagnosticDescriptor(positionalTextDiagnostic.start, positionalTextDiagnostic.end) {

    val textDiagnostic: TextDiagnostic
        get() = positionalTextDiagnostic.diagnostic
}

interface AbstractTestDiagnostic : Comparable<AbstractTestDiagnostic> {
    val name: String

    val platform: String?

    val inferenceCompatibility: TextDiagnostic.InferenceCompatibility

    fun enhanceInferenceCompatibility(inferenceCompatibility: TextDiagnostic.InferenceCompatibility)

    override fun compareTo(diagnostic: AbstractTestDiagnostic): Int
}

class ActualDiagnostic constructor(val diagnostic: Diagnostic, override val platform: String?, withNewInference: Boolean) :
    AbstractTestDiagnostic {
    override var inferenceCompatibility = if (withNewInference)
        TextDiagnostic.InferenceCompatibility.NEW
    else
        TextDiagnostic.InferenceCompatibility.OLD

    override val name: String
        get() = diagnostic.factory.name

    val file: PsiFile
        get() = diagnostic.psiFile

    override fun compareTo(diagnostic: AbstractTestDiagnostic): Int {
        return if (this.diagnostic is DiagnosticWithParameters1<*, *> && diagnostic is ActualDiagnostic && diagnostic.diagnostic is DiagnosticWithParameters1<*, *>) {
            (name + this.diagnostic.a).compareTo(diagnostic.name + diagnostic.diagnostic.a)
        } else if (this.diagnostic is DiagnosticWithParameters1<*, *>) {
            (name + this.diagnostic.a).compareTo(diagnostic.name)
        } else if (diagnostic is ActualDiagnostic && diagnostic.diagnostic is DiagnosticWithParameters1<*, *>) {
            name.compareTo(diagnostic.name + diagnostic.diagnostic.a)
        } else {
            name.compareTo(diagnostic.name)
        }
    }

    override fun enhanceInferenceCompatibility(inferenceCompatibility: TextDiagnostic.InferenceCompatibility) {
        this.inferenceCompatibility = inferenceCompatibility
    }

    override fun equals(obj: Any?): Boolean {
        if (obj !is ActualDiagnostic) return false

        val other = obj as ActualDiagnostic?
        // '==' on diagnostics is intentional here
        return other!!.diagnostic === diagnostic &&
                (if (other!!.platform == null) platform == null else other.platform == platform) &&
                other.inferenceCompatibility == inferenceCompatibility
    }

    override fun hashCode(): Int {
        var result = System.identityHashCode(diagnostic)
        result = 31 * result + (platform?.hashCode() ?: 0)
        result = 31 * result + inferenceCompatibility.hashCode()
        return result
    }

    override fun toString(): String {
        val inferenceAbbreviation = inferenceCompatibility.abbreviation
        return (if (inferenceAbbreviation != null) inferenceAbbreviation + ";" else "") +
                (if (platform != null) "$platform:" else "") +
                diagnostic.toString()
    }
}


class TextDiagnostic(
    override val name: String,
    override val platform: String?,
    val parameters: List<String>?,
    inference: InferenceCompatibility?
) : AbstractTestDiagnostic {
    override var inferenceCompatibility = inference ?: InferenceCompatibility.ALL

    val description: String
        get() = (if (platform != null) "$platform:" else "") + name

    enum class InferenceCompatibility private constructor(internal var abbreviation: String?) {
        NEW(NEW_INFERENCE_PREFIX), OLD(OLD_INFERENCE_PREFIX), ALL(null);

        fun isCompatible(other: InferenceCompatibility): Boolean {
            return this == other || this == ALL || other == ALL
        }
    }

    override fun compareTo(diagnostic: AbstractTestDiagnostic): Int {
        return name.compareTo(diagnostic.name)
    }

    override fun enhanceInferenceCompatibility(inferenceCompatibility: InferenceCompatibility) {
        this.inferenceCompatibility = inferenceCompatibility
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false

        val that = o as TextDiagnostic?

        if (name != that!!.name) return false
        if (if (platform != null) platform != that.platform else that.platform != null) return false
        if (if (parameters != null) parameters != that.parameters else that.parameters != null) return false
        return if (inferenceCompatibility != that.inferenceCompatibility) false else true

    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + (platform?.hashCode() ?: 0)
        result = 31 * result + (parameters?.hashCode() ?: 0)
        result = 31 * result + inferenceCompatibility.hashCode()
        return result
    }

    fun asString(): String {
        val result = StringBuilder()
        if (inferenceCompatibility.abbreviation != null) {
            result.append(inferenceCompatibility.abbreviation)
            result.append(";")
        }
        if (platform != null) {
            result.append(platform)
            result.append(":")
        }
        result.append(name)
        if (parameters != null) {
            result.append("(")
            result.append(StringUtil.join(parameters, { escape(it) }, "; "))
            result.append(")")
        }
        return result.toString()
    }

    override fun toString(): String {
        return asString()
    }

    companion object {
        fun parseDiagnostic(text: String): TextDiagnostic {
            val matcher = INDIVIDUAL_DIAGNOSTIC_PATTERN.matcher(text)
            if (!matcher.find())
                throw IllegalArgumentException("Could not parse diagnostic: $text")

            val inference = computeInferenceCompatibility(extractDataBefore(matcher.group(1), ";"))
            val platform = extractDataBefore(matcher.group(2), ":")

            val name = matcher.group(3)
            val parameters = matcher.group(4) ?: return TextDiagnostic(name, platform, null, inference)

            val parsedParameters = SmartList<String>()
            val parametersMatcher = INDIVIDUAL_PARAMETER_PATTERN.matcher(parameters)
            while (parametersMatcher.find())
                parsedParameters.add(unescape(parametersMatcher.group().trim({ it <= ' ' })))
            return TextDiagnostic(name, platform, parsedParameters, inference)
        }

        private fun computeInferenceCompatibility(abbreviation: String?): InferenceCompatibility {
            return if (abbreviation == null) InferenceCompatibility.ALL else InferenceCompatibility.values().single { inference -> abbreviation == inference.abbreviation }
        }

        private fun extractDataBefore(prefix: String?, anchor: String): String? {
            assert(prefix == null || prefix.endsWith(anchor)) { prefix ?: "" }
            return prefix?.substringBeforeLast(anchor, prefix)
        }

        private fun escape(s: String): String {
            return s.replace("([$SHOULD_BE_ESCAPED])".toRegex(), "\\\\$1")
        }

        private fun unescape(s: String): String {
            return s.replace("\\\\([$SHOULD_BE_ESCAPED])".toRegex(), "$1")
        }

        fun asTextDiagnostic(abstractTestDiagnostic: AbstractTestDiagnostic): TextDiagnostic {
            return if (abstractTestDiagnostic is ActualDiagnostic) {
                asTextDiagnostic(abstractTestDiagnostic)
            } else abstractTestDiagnostic as TextDiagnostic

        }

        fun asTextDiagnostic(actualDiagnostic: ActualDiagnostic): TextDiagnostic {
            val diagnostic = actualDiagnostic.diagnostic

            val renderer = DefaultErrorMessages.getRendererForDiagnostic(diagnostic)
            val diagnosticName = actualDiagnostic.name
            if (renderer is AbstractDiagnosticWithParametersRenderer) {
                val renderParameters = renderer.renderParameters(diagnostic)
                val parameters = ContainerUtil.map(renderParameters, { it.toString() })
                return TextDiagnostic(diagnosticName, actualDiagnostic.platform, parameters, actualDiagnostic.inferenceCompatibility)
            }
            return TextDiagnostic(diagnosticName, actualDiagnostic.platform, null, actualDiagnostic.inferenceCompatibility)
        }
    }
}

class DiagnosedRange constructor(val start: Int) {
    var end: Int = 0
    private val diagnostics = ContainerUtil.newSmartList<TextDiagnostic>()

    fun getDiagnostics(): List<TextDiagnostic> {
        return diagnostics
    }

    fun addDiagnostic(diagnostic: String) {
        diagnostics.add(TextDiagnostic.parseDiagnostic(diagnostic))
    }
}

class DebugInfoDiagnosticFactory1 : DiagnosticFactory1<PsiElement, String>, DebugInfoDiagnosticFactory {
    private val name: String

    override fun getName(): String {
        return "DEBUG_INFO_$name"
    }

    override val withExplicitDefinitionOnly: Boolean

    override fun createDiagnostic(
        expression: KtExpression,
        bindingContext: BindingContext,
        dataFlowValueFactory: DataFlowValueFactory,
        languageVersionSettings: LanguageVersionSettings,
        moduleDescriptor: ModuleDescriptorImpl
    ): Diagnostic {
        return this.on(
            expression,
            "\"" + Renderers.renderExpressionType(
                expression,
                bindingContext,
                dataFlowValueFactory,
                languageVersionSettings,
                moduleDescriptor
            ) + "\""
        )
    }

    protected constructor(name: String, severity: Severity) : super(severity, PositioningStrategies.DEFAULT) {
        this.name = name
        this.withExplicitDefinitionOnly = false
    }

    protected constructor(name: String, severity: Severity, withExplicitDefinitionOnly: Boolean) : super(
        severity,
        PositioningStrategies.DEFAULT
    ) {
        this.name = name
        this.withExplicitDefinitionOnly = withExplicitDefinitionOnly
    }

    companion object {
        val EXPRESSION_TYPE = create("EXPRESSION_TYPE", Severity.INFO, true)

        fun create(name: String, severity: Severity): DebugInfoDiagnosticFactory1 {
            return DebugInfoDiagnosticFactory1(name, severity)
        }

        fun create(name: String, severity: Severity, withExplicitDefinitionOnly: Boolean): DebugInfoDiagnosticFactory1 {
            return DebugInfoDiagnosticFactory1(name, severity, withExplicitDefinitionOnly)
        }
    }
}

class DebugInfoDiagnostic(element: KtElement, factory: DiagnosticFactory<*>) : AbstractDiagnosticForTests(element, factory)

class DebugInfoDiagnosticFactory0 : DiagnosticFactory0<PsiElement>, DebugInfoDiagnosticFactory {
    private val name: String
    override val withExplicitDefinitionOnly: Boolean

    override fun createDiagnostic(
        expression: KtExpression,
        bindingContext: BindingContext,
        dataFlowValueFactory: DataFlowValueFactory,
        languageVersionSettings: LanguageVersionSettings,
        moduleDescriptor: ModuleDescriptorImpl
    ): Diagnostic {
        return DebugInfoDiagnostic(expression, this)
    }

    private constructor(name: String, severity: Severity = Severity.ERROR) : super(severity, PositioningStrategies.DEFAULT) {
        this.name = name
        this.withExplicitDefinitionOnly = false
    }

    private constructor(name: String, severity: Severity, withExplicitDefinitionOnly: Boolean) : super(
        severity,
        PositioningStrategies.DEFAULT
    ) {
        this.name = name
        this.withExplicitDefinitionOnly = withExplicitDefinitionOnly
    }

    override fun getName(): String {
        return "DEBUG_INFO_$name"
    }

    companion object {
        val SMARTCAST = DebugInfoDiagnosticFactory0("SMARTCAST", Severity.INFO)
        val IMPLICIT_RECEIVER_SMARTCAST = DebugInfoDiagnosticFactory0("IMPLICIT_RECEIVER_SMARTCAST", Severity.INFO)
        val CONSTANT = DebugInfoDiagnosticFactory0("CONSTANT", Severity.INFO)
        val LEAKING_THIS = DebugInfoDiagnosticFactory0("LEAKING_THIS")
        val IMPLICIT_EXHAUSTIVE = DebugInfoDiagnosticFactory0("IMPLICIT_EXHAUSTIVE", Severity.INFO)
        val ELEMENT_WITH_ERROR_TYPE = DebugInfoDiagnosticFactory0("ELEMENT_WITH_ERROR_TYPE")
        val UNRESOLVED_WITH_TARGET = DebugInfoDiagnosticFactory0("UNRESOLVED_WITH_TARGET")
        val MISSING_UNRESOLVED = DebugInfoDiagnosticFactory0("MISSING_UNRESOLVED")
        val DYNAMIC = DebugInfoDiagnosticFactory0("DYNAMIC", Severity.INFO)
    }
}

open class AbstractDiagnosticForTests(private val element: PsiElement, private val factory: DiagnosticFactory<*>) : Diagnostic {

    override fun getFactory(): DiagnosticFactory<*> {
        return factory
    }

    override fun getSeverity(): Severity {
        return Severity.ERROR
    }

    override fun getPsiElement(): PsiElement {
        return element
    }

    override fun getTextRanges(): List<TextRange> {
        return listOf(element.textRange)
    }

    override fun getPsiFile(): PsiFile {
        return element.containingFile
    }

    override fun isValid(): Boolean {
        return true
    }
}

class SyntaxErrorDiagnosticFactory private constructor() : DiagnosticFactory<SyntaxErrorDiagnostic>(Severity.ERROR) {

    override fun getName(): String {
        return "SYNTAX"
    }

    companion object {
        val INSTANCE = SyntaxErrorDiagnosticFactory()
    }
}

class SyntaxErrorDiagnostic(errorElement: PsiErrorElement) :
    AbstractDiagnosticForTests(errorElement, SyntaxErrorDiagnosticFactory.INSTANCE)

internal interface DebugInfoDiagnosticFactory {
    val withExplicitDefinitionOnly: Boolean

    fun createDiagnostic(
        expression: KtExpression,
        bindingContext: BindingContext,
        dataFlowValueFactory: DataFlowValueFactory,
        languageVersionSettings: LanguageVersionSettings,
        moduleDescriptor: ModuleDescriptorImpl
    ): Diagnostic
}


interface DiagnosticDiffCallbacks {
    fun missingDiagnostic(diagnostic: TextDiagnostic, expectedStart: Int, expectedEnd: Int)

    fun wrongParametersDiagnostic(expectedDiagnostic: TextDiagnostic, actualDiagnostic: TextDiagnostic, start: Int, end: Int)

    fun unexpectedDiagnostic(diagnostic: TextDiagnostic, actualStart: Int, actualEnd: Int)
}

object CheckerTestUtil {
    val IGNORE_DIAGNOSTIC_PARAMETER = "IGNORE"
    val SHOULD_BE_ESCAPED = "\\)\\(;"
    val DIAGNOSTIC_PARAMETER = "(?:(?:\\\\[$SHOULD_BE_ESCAPED])|[^$SHOULD_BE_ESCAPED])+"
    val INDIVIDUAL_DIAGNOSTIC = "(\\w+;)?(\\w+:)?(\\w+)(\\($DIAGNOSTIC_PARAMETER(;\\s*$DIAGNOSTIC_PARAMETER)*\\))?"
    val RANGE_START_OR_END_PATTERN = Pattern.compile("(<!" + INDIVIDUAL_DIAGNOSTIC + "(,\\s*" + INDIVIDUAL_DIAGNOSTIC + ")*!>)|(<!>)")
    val INDIVIDUAL_DIAGNOSTIC_PATTERN = Pattern.compile(INDIVIDUAL_DIAGNOSTIC)
    val INDIVIDUAL_PARAMETER_PATTERN = Pattern.compile(DIAGNOSTIC_PARAMETER)
    val NEW_INFERENCE_PREFIX = "NI"
    val OLD_INFERENCE_PREFIX = "OI"

    fun getDiagnosticsIncludingSyntaxErrors(
        bindingContext: BindingContext,
        implementingModulesBindings: List<Pair<MultiTargetPlatform, BindingContext>>,
        root: PsiElement,
        markDynamicCalls: Boolean,
        dynamicCallDescriptors: MutableList<DeclarationDescriptor>,
        withNewInference: Boolean,
        languageVersionSettings: LanguageVersionSettings,
        dataFlowValueFactory: DataFlowValueFactory,
        moduleDescriptor: ModuleDescriptorImpl
    ): List<ActualDiagnostic> {
        val result = getDiagnosticsIncludingSyntaxErrors(
            bindingContext,
            root,
            markDynamicCalls,
            dynamicCallDescriptors,
            null,
            withNewInference,
            languageVersionSettings,
            dataFlowValueFactory,
            moduleDescriptor
        )

        val sortedBindings = implementingModulesBindings.sortedWith(
            { (first), (second) -> first.compareTo(second) }
        )

        for ((platform, second) in sortedBindings) {
            assert(platform is MultiTargetPlatform.Specific) { "Implementing module must have a specific platform: $platform" }

            result.addAll(
                getDiagnosticsIncludingSyntaxErrors(
                    second,
                    root,
                    markDynamicCalls,
                    dynamicCallDescriptors,
                    (platform as MultiTargetPlatform.Specific).platform,
                    withNewInference,
                    languageVersionSettings,
                    dataFlowValueFactory,
                    moduleDescriptor
                )
            )
        }

        return result
    }

    fun getDiagnosticsIncludingSyntaxErrors(
        bindingContext: BindingContext,
        root: PsiElement,
        markDynamicCalls: Boolean,
        dynamicCallDescriptors: MutableList<DeclarationDescriptor>,
        platform: String?,
        withNewInference: Boolean,
        languageVersionSettings: LanguageVersionSettings,
        dataFlowValueFactory: DataFlowValueFactory,
        moduleDescriptor: ModuleDescriptorImpl
    ): MutableList<ActualDiagnostic> {
        val diagnostics: MutableList<ActualDiagnostic> = mutableListOf()

        bindingContext.getDiagnostics().all().forEach { diagnostic ->
            if (PsiTreeUtil.isAncestor(root, diagnostic.getPsiElement(), false)) {
                diagnostics.add(ActualDiagnostic(diagnostic, platform, withNewInference))
            }
        }

        for (errorElement in AnalyzingUtils.getSyntaxErrorRanges(root)) {
            diagnostics.add(ActualDiagnostic(SyntaxErrorDiagnostic(errorElement), platform, withNewInference))
        }

        diagnostics.addAll(
            getDebugInfoDiagnostics(
                root,
                bindingContext,
                markDynamicCalls,
                dynamicCallDescriptors,
                platform,
                withNewInference,
                languageVersionSettings,
                dataFlowValueFactory,
                moduleDescriptor
            )
        )
        return diagnostics
    }

    @SuppressWarnings("TestOnlyProblems")
    private fun getDebugInfoDiagnostics(
        root: PsiElement,
        bindingContext: BindingContext,
        markDynamicCalls: Boolean,
        dynamicCallDescriptors: MutableList<DeclarationDescriptor>,
        platform: String?,
        withNewInference: Boolean,
        languageVersionSettings: LanguageVersionSettings,
        dataFlowValueFactory: DataFlowValueFactory,
        moduleDescriptor: ModuleDescriptorImpl
    ): List<ActualDiagnostic> {
        val debugAnnotations = LinkedList<ActualDiagnostic>()

        DebugInfoUtil.markDebugAnnotations(root, bindingContext, object : DebugInfoUtil.DebugInfoReporter() {
            override fun reportElementWithErrorType(expression: KtReferenceExpression) {
                newDiagnostic(expression, DebugInfoDiagnosticFactory0.ELEMENT_WITH_ERROR_TYPE)
            }

            override fun reportMissingUnresolved(expression: KtReferenceExpression) {
                newDiagnostic(expression, DebugInfoDiagnosticFactory0.MISSING_UNRESOLVED)
            }

            override fun reportUnresolvedWithTarget(expression: KtReferenceExpression, target: String) {
                newDiagnostic(expression, DebugInfoDiagnosticFactory0.UNRESOLVED_WITH_TARGET)
            }

            override fun reportDynamicCall(element: KtElement, declarationDescriptor: DeclarationDescriptor) {
                dynamicCallDescriptors.add(declarationDescriptor)

                if (markDynamicCalls) {
                    newDiagnostic(element, DebugInfoDiagnosticFactory0.DYNAMIC)
                }
            }

            private fun newDiagnostic(element: KtElement, factory: DebugInfoDiagnosticFactory0) {
                debugAnnotations.add(ActualDiagnostic(DebugInfoDiagnostic(element, factory), platform, withNewInference))
            }
        })


        // this code is used in tests and in internal action 'copy current file as diagnostic test'
        //noinspection unchecked

        val factoryList = Arrays.asList(
            BindingContext.EXPRESSION_TYPE_INFO.to<WritableSlice<KtExpression, KotlinTypeInfo>, DebugInfoDiagnosticFactory>(
                DebugInfoDiagnosticFactory1.EXPRESSION_TYPE
            ),
            BindingContext.SMARTCAST.to<WritableSlice<KtExpression, ExplicitSmartCasts>, DebugInfoDiagnosticFactory>(
                DebugInfoDiagnosticFactory0.SMARTCAST
            ),
            BindingContext.IMPLICIT_RECEIVER_SMARTCAST.to<WritableSlice<KtExpression, ImplicitSmartCasts>, DebugInfoDiagnosticFactory>(
                DebugInfoDiagnosticFactory0.IMPLICIT_RECEIVER_SMARTCAST
            ),
            BindingContext.SMARTCAST_NULL.to(DebugInfoDiagnosticFactory0.CONSTANT),
            BindingContext.LEAKING_THIS.to<WritableSlice<KtExpression, LeakingThisDescriptor>, DebugInfoDiagnosticFactory>(
                DebugInfoDiagnosticFactory0.LEAKING_THIS
            ),
            BindingContext.IMPLICIT_EXHAUSTIVE_WHEN.to<WritableSlice<KtWhenExpression, Boolean>, DebugInfoDiagnosticFactory>(
                DebugInfoDiagnosticFactory0.IMPLICIT_EXHAUSTIVE
            )
        )

        for (factory in factoryList) {
            val test = bindingContext.getSliceContents(factory.first).keys.asList()

            for (expression in test) {
                if (PsiTreeUtil.isAncestor(root, expression, false)) {
                    val diagnostic = factory.second.createDiagnostic(
                        expression,
                        bindingContext,
                        dataFlowValueFactory,
                        languageVersionSettings,
                        moduleDescriptor
                    )
                    val t = ActualDiagnostic(diagnostic, platform, withNewInference)

                    debugAnnotations.add(t)
                }
            }
        }

        return debugAnnotations
    }


    fun diagnosticsDiff(
        expected: List<DiagnosedRange>,
        actual: Collection<ActualDiagnostic>,
        callbacks: DiagnosticDiffCallbacks
    ): Map<AbstractTestDiagnostic, TextDiagnostic> {
        val diagnosticToExpectedDiagnostic = mutableMapOf<AbstractTestDiagnostic, TextDiagnostic>()

        assertSameFile(actual)

        val expectedDiagnostics = expected.iterator()
        val sortedDiagnosticDescriptors = getActualSortedDiagnosticDescriptors(actual)
        val actualDiagnostics = sortedDiagnosticDescriptors.iterator()

        var currentExpected = safeAdvance(expectedDiagnostics)
        var currentActual = safeAdvance(actualDiagnostics)
        while (currentExpected != null || currentActual != null) {
            if (currentExpected != null) {
                if (currentActual == null) {
                    missingDiagnostics(callbacks, currentExpected)
                    currentExpected = safeAdvance(expectedDiagnostics)
                } else {
                    val expectedStart = currentExpected.start
                    val actualStart = currentActual.start
                    val expectedEnd = currentExpected.end
                    val actualEnd = currentActual.end
                    if (expectedStart < actualStart) {
                        missingDiagnostics(callbacks, currentExpected)
                        currentExpected = safeAdvance(expectedDiagnostics)
                    } else if (expectedStart > actualStart) {
                        unexpectedDiagnostics(currentActual, callbacks)
                        currentActual = safeAdvance(actualDiagnostics)
                    } else if (expectedEnd > actualEnd) {
                        assert(expectedStart == actualStart)
                        missingDiagnostics(callbacks, currentExpected)
                        currentExpected = safeAdvance(expectedDiagnostics)
                    } else if (expectedEnd < actualEnd) {
                        assert(expectedStart == actualStart)
                        unexpectedDiagnostics(currentActual, callbacks)
                        currentActual = safeAdvance(actualDiagnostics)
                    } else {
                        compareDiagnostics(callbacks, currentExpected, currentActual, diagnosticToExpectedDiagnostic)
                        currentExpected = safeAdvance(expectedDiagnostics)
                        currentActual = safeAdvance(actualDiagnostics)
                    }
                }
            } else {

                assert(currentActual != null)

                unexpectedDiagnostics(currentActual!!, callbacks)
                currentActual = safeAdvance(actualDiagnostics)
            }
        }

        return diagnosticToExpectedDiagnostic
    }

    private fun compareDiagnostics(
        callbacks: DiagnosticDiffCallbacks,
        currentExpected: DiagnosedRange,
        currentActual: ActualDiagnosticDescriptor,
        diagnosticToInput: MutableMap<AbstractTestDiagnostic, TextDiagnostic>
    ) {
        val expectedStart = currentExpected.start
        val expectedEnd = currentExpected.end

        val actualStart = currentActual.start
        val actualEnd = currentActual.end
        assert(expectedStart == actualStart && expectedEnd == actualEnd)

        val actualDiagnostics = currentActual.textDiagnosticsMap
        val expectedDiagnostics = currentExpected.getDiagnostics()
        val diagnosticNames = HashSet<String>()

        for (expectedDiagnostic in expectedDiagnostics) {
            val actualDiagnosticEntry = actualDiagnostics.entries.firstOrNull({ entry ->
                                                                                  val actualDiagnostic = entry.value
                                                                                  expectedDiagnostic.description == actualDiagnostic.description && expectedDiagnostic.inferenceCompatibility.isCompatible(
                                                                                      actualDiagnostic.inferenceCompatibility
                                                                                  )
                                                                              }
            )

            if (actualDiagnosticEntry != null) {
                val actualDiagnostic = actualDiagnosticEntry.key
                val actualTextDiagnostic = actualDiagnosticEntry.value

                if (!compareTextDiagnostic(expectedDiagnostic, actualTextDiagnostic)) {
                    callbacks.wrongParametersDiagnostic(expectedDiagnostic, actualTextDiagnostic, expectedStart, expectedEnd)
                }

                actualDiagnostics.remove(actualDiagnostic)
                diagnosticNames.add(actualDiagnostic.name)
                actualDiagnostic.enhanceInferenceCompatibility(expectedDiagnostic.inferenceCompatibility)

                diagnosticToInput[actualDiagnostic] = expectedDiagnostic
            } else {
                callbacks.missingDiagnostic(expectedDiagnostic, expectedStart, expectedEnd)
            }
        }

        for (unexpectedDiagnostic in actualDiagnostics.keys) {
            val textDiagnostic = actualDiagnostics[unexpectedDiagnostic]

            if (hasExplicitDefinitionOnlyOption(unexpectedDiagnostic) && !diagnosticNames.contains(unexpectedDiagnostic.name))
                continue

            callbacks.unexpectedDiagnostic(textDiagnostic!!, actualStart, actualEnd)
        }
    }

    private fun compareTextDiagnostic(expected: TextDiagnostic, actual: TextDiagnostic): Boolean {
        if (expected.description != actual.description) return false

        if (expected.parameters == null) return true
        if (actual.parameters == null || expected.parameters.size != actual.parameters.size) return false

        for (index in 0 until expected.parameters.size) {
            val expectedParameter = expected.parameters[index]
            val actualParameter = actual.parameters[index]
            if (expectedParameter != IGNORE_DIAGNOSTIC_PARAMETER && expectedParameter != actualParameter) {
                return false
            }
        }
        return true
    }


    private fun assertSameFile(actual: Collection<ActualDiagnostic>) {
        if (actual.isEmpty()) return
        val file = actual.first().file
        for (actualDiagnostic in actual) {
            assert(actualDiagnostic.file == file) { "All diagnostics should come from the same file: " + actualDiagnostic.file + ", " + file }
        }
    }

    private fun unexpectedDiagnostics(descriptor: ActualDiagnosticDescriptor, callbacks: DiagnosticDiffCallbacks) {
        for (diagnostic in descriptor.diagnostics) {
            if (hasExplicitDefinitionOnlyOption(diagnostic))
                continue

            callbacks.unexpectedDiagnostic(TextDiagnostic.asTextDiagnostic(diagnostic), descriptor.start, descriptor.end)
        }
    }

    private fun missingDiagnostics(callbacks: DiagnosticDiffCallbacks, currentExpected: DiagnosedRange) {
        for (diagnostic in currentExpected.getDiagnostics()) {
            callbacks.missingDiagnostic(diagnostic, currentExpected.start, currentExpected.end)
        }
    }

    private fun <T> safeAdvance(iterator: Iterator<T>): T? {
        return if (iterator.hasNext()) iterator.next() else null
    }


    fun parseDiagnosedRanges(text: String, result: MutableList<DiagnosedRange>): String {
        val matcher = RANGE_START_OR_END_PATTERN.matcher(text)

        val opened = Stack<DiagnosedRange>()

        var offsetCompensation = 0

        while (matcher.find()) {
            val effectiveOffset = matcher.start() - offsetCompensation
            val matchedText = matcher.group()
            if ("<!>" == matchedText) {
                opened.pop().end = effectiveOffset
            } else {
                val diagnosticTypeMatcher = INDIVIDUAL_DIAGNOSTIC_PATTERN.matcher(matchedText)
                val range = DiagnosedRange(effectiveOffset)
                while (diagnosticTypeMatcher.find()) {
                    range.addDiagnostic(diagnosticTypeMatcher.group())
                }
                opened.push(range)
                result.add(range)
            }
            offsetCompensation += matchedText.length
        }

        assert(opened.isEmpty()) { "Stack is not empty" }

        matcher.reset()
        return matcher.replaceAll("")
    }

    private fun hasExplicitDefinitionOnlyOption(diagnostic: AbstractTestDiagnostic): Boolean {
        if (diagnostic !is ActualDiagnostic)
            return false
        val factory = diagnostic.diagnostic.factory
        return factory is DebugInfoDiagnosticFactory && (factory as DebugInfoDiagnosticFactory).withExplicitDefinitionOnly
    }

    fun addDiagnosticMarkersToText(psiFile: PsiFile, diagnostics: Collection<ActualDiagnostic>): StringBuffer {
        return addDiagnosticMarkersToText(
            psiFile,
            diagnostics,
            emptyMap(),
            { it.text },
            emptyList(),
            false
        )
    }


    fun addDiagnosticMarkersToText(
        psiFile: PsiFile,
        diagnostics: Collection<ActualDiagnostic>,
        diagnosticToExpectedDiagnostic: Map<AbstractTestDiagnostic, TextDiagnostic>,
        getFileText: com.intellij.util.Function<PsiFile, String>,
        uncheckedDiagnostics: Collection<PositionalTextDiagnostic>,
        withNewInferenceDirective: Boolean
    ): StringBuffer {
        var diagnostics = diagnostics
        val text = getFileText.`fun`(psiFile)
        val result = StringBuffer()
        diagnostics = diagnostics.filter { actualDiagnostic -> psiFile == actualDiagnostic.file }
        if (diagnostics.isEmpty() && uncheckedDiagnostics.isEmpty()) {
            result.append(text)
            return result
        }

        val diagnosticDescriptors = getSortedDiagnosticDescriptors(diagnostics, uncheckedDiagnostics)

        val opened = Stack<AbstractDiagnosticDescriptor>()
        val iterator = diagnosticDescriptors.listIterator()
        var currentDescriptor: AbstractDiagnosticDescriptor? = iterator.next()

        for (i in 0 until text.length) {
            val c = text[i]
            while (!opened.isEmpty() && i == opened.peek().end) {
                closeDiagnosticString(result)
                opened.pop()
            }
            while (currentDescriptor != null && i == currentDescriptor.start) {
                val isSkip = openDiagnosticsString(result, currentDescriptor, diagnosticToExpectedDiagnostic, withNewInferenceDirective)

                if (currentDescriptor.end == i && !isSkip) {
                    closeDiagnosticString(result)
                } else if (!isSkip) {
                    opened.push(currentDescriptor)
                }
                if (iterator.hasNext()) {
                    currentDescriptor = iterator.next()
                } else {
                    currentDescriptor = null
                }
            }
            result.append(c)
        }

        if (currentDescriptor != null) {
            assert(currentDescriptor.start == text.length)
            assert(currentDescriptor.end == text.length)
            val isSkip = openDiagnosticsString(result, currentDescriptor, diagnosticToExpectedDiagnostic, withNewInferenceDirective)

            if (!isSkip)
                opened.push(currentDescriptor)
        }

        while (!opened.isEmpty() && text.length == opened.peek().end) {
            closeDiagnosticString(result)
            opened.pop()
        }

        assert(opened.isEmpty()) { "Stack is not empty: $opened" }

        return result
    }

    private fun openDiagnosticsString(
        result: StringBuffer,
        currentDescriptor: AbstractDiagnosticDescriptor,
        diagnosticToExpectedDiagnostic: Map<AbstractTestDiagnostic, TextDiagnostic>,
        withNewInferenceDirective: Boolean
    ): Boolean {
        var isSkip = true
        val diagnosticsAsText = ArrayList<String>()

        if (currentDescriptor is TextDiagnosticDescriptor) {
            diagnosticsAsText.add(currentDescriptor.textDiagnostic.asString())
        } else if (currentDescriptor is ActualDiagnosticDescriptor) {
            val diagnostics = currentDescriptor.diagnostics

            for (diagnostic in diagnostics) {
                val expectedDiagnostic = diagnosticToExpectedDiagnostic[diagnostic]
                if (expectedDiagnostic != null) {
                    val actualTextDiagnostic = TextDiagnostic.asTextDiagnostic(diagnostic)
                    if (compareTextDiagnostic(expectedDiagnostic, actualTextDiagnostic)) {
                        diagnosticsAsText.add(expectedDiagnostic.asString())
                    } else {
                        diagnosticsAsText.add(actualTextDiagnostic.asString())
                    }
                } else if (!hasExplicitDefinitionOnlyOption(diagnostic)) {
                    val diagnosticText = StringBuilder()
                    if (withNewInferenceDirective && diagnostic.inferenceCompatibility.abbreviation != null) {
                        diagnosticText.append(diagnostic.inferenceCompatibility.abbreviation)
                        diagnosticText.append(";")
                    }
                    if (diagnostic.platform != null) {
                        diagnosticText.append(diagnostic.platform)
                        diagnosticText.append(":")
                    }
                    diagnosticsAsText.add(diagnosticText.toString() + diagnostic.name)
                }
            }
        } else {
            throw IllegalStateException("Unknown diagnostic descriptor: $currentDescriptor")
        }

        if (diagnosticsAsText.size != 0) {
            Collections.sort(diagnosticsAsText)
            result.append("<!")
            result.append(diagnosticsAsText.joinToString(", "))
            result.append("!>")
            isSkip = false
        }

        return isSkip
    }


    private fun closeDiagnosticString(result: StringBuffer) {
        result.append("<!>")
    }


    private fun getActualSortedDiagnosticDescriptors(
        diagnostics: Collection<ActualDiagnostic>
    ): List<ActualDiagnosticDescriptor> {
        return getSortedDiagnosticDescriptors(diagnostics, emptyList()).filterIsInstance(
            ActualDiagnosticDescriptor::class.java
        )
    }

    private fun getSortedDiagnosticDescriptors(
        diagnostics: Collection<ActualDiagnostic>,
        uncheckedDiagnostics: Collection<PositionalTextDiagnostic>
    ): List<AbstractDiagnosticDescriptor> {
        val validDiagnostics = diagnostics.filter { actualDiagnostic -> actualDiagnostic.diagnostic.isValid }
        val diagnosticDescriptors = groupDiagnosticsByTextRange(validDiagnostics, uncheckedDiagnostics)
        diagnosticDescriptors.sortWith { d1: AbstractDiagnosticDescriptor, d2: AbstractDiagnosticDescriptor ->
            if (d1.start != d2.start) d1.start - d2.start else d2.end - d1.end
        }
        return diagnosticDescriptors
    }

    private fun groupDiagnosticsByTextRange(
        diagnostics: Collection<ActualDiagnostic>,
        uncheckedDiagnostics: Collection<PositionalTextDiagnostic>
    ): MutableList<AbstractDiagnosticDescriptor> {
        val diagnosticsGroupedByRanges = LinkedListMultimap.create<TextRange, AbstractTestDiagnostic>()
        for (actualDiagnostic in diagnostics) {
            val diagnostic = actualDiagnostic.diagnostic
            for (textRange in diagnostic.textRanges) {
                diagnosticsGroupedByRanges.put(textRange, actualDiagnostic)
            }
        }

        for ((diagnostic, start, end) in uncheckedDiagnostics) {
            val range = TextRange(start, end)
            diagnosticsGroupedByRanges.put(range, diagnostic)
        }

        return diagnosticsGroupedByRanges.keySet().map { range ->
            val abstractDiagnostics = diagnosticsGroupedByRanges.get(range)
            val needSortingByName =
                abstractDiagnostics.any { diagnostic -> diagnostic.inferenceCompatibility != TextDiagnostic.InferenceCompatibility.ALL }

            if (needSortingByName) {
                abstractDiagnostics.sortBy { it.name }
            } else {
                abstractDiagnostics.sortBy { it }
            }

            ActualDiagnosticDescriptor(range.startOffset, range.endOffset, abstractDiagnostics)
        }.toMutableList()
    }

}