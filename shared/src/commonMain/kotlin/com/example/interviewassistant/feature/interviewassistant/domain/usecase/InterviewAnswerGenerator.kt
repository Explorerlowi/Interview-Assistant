package com.example.interviewassistant.feature.interviewassistant.domain.usecase

import com.example.interviewassistant.core.error.AppError
import com.example.interviewassistant.feature.interviewassistant.data.remote.llm.LlmChunk
import com.example.interviewassistant.feature.interviewassistant.data.remote.llm.LlmMessage
import com.example.interviewassistant.feature.interviewassistant.data.remote.llm.OpenAiStreamGateway
import com.example.interviewassistant.feature.interviewassistant.domain.model.LlmConfiguration
import com.example.interviewassistant.feature.interviewassistant.domain.repository.ProviderConfigurationRepository
import kotlinx.coroutines.flow.Flow

/**
 * Context used to ground one interview answer.
 */
data class InterviewAnswerContext(
    val resumeText: String,
    val question: String,
    val recentTranscript: String = "",
    val recentAnswers: List<String> = emptyList(),
)

/**
 * Builds bounded, resume-grounded prompts for interview assistance.
 */
class InterviewPromptBuilder {
    /**
     * Produces system and user messages while respecting [maxCharacters].
     *
     * @param context Resume, question, and recent interview context.
     * @param maxCharacters Character budget for the user message payload.
     * @param systemPrompt Custom system instruction; blank values fall back to the default.
     */
    fun build(
        context: InterviewAnswerContext,
        maxCharacters: Int,
        systemPrompt: String = LlmConfiguration.DEFAULT_SYSTEM_PROMPT,
    ): List<LlmMessage> {
        val budget = maxCharacters.coerceAtLeast(MINIMUM_CONTEXT_CHARACTERS)
        val question = context.question.trim().take(MAX_QUESTION_CHARACTERS)
        require(question.isNotEmpty()) { "Interview question cannot be empty" }

        val resumeBudget = (budget * RESUME_BUDGET_RATIO).toInt()
        val historyBudget = budget - resumeBudget - question.length
        val resume = context.resumeText.trim().take(resumeBudget)
        val recentTranscript = context.recentTranscript.trim().takeLast(historyBudget / 2)
        val recentAnswers = context.recentAnswers
            .joinToString(separator = "\n\n")
            .takeLast(historyBudget / 2)

        val userContent = buildString {
            appendLine("候选人简历：")
            appendLine(resume.ifBlank { "未提供" })
            if (recentTranscript.isNotBlank()) {
                appendLine()
                appendLine("近期面试上下文：")
                appendLine(recentTranscript)
            }
            if (recentAnswers.isNotBlank()) {
                appendLine()
                appendLine("此前回答摘要：")
                appendLine(recentAnswers)
            }
            appendLine()
            appendLine("当前问题：")
            append(question)
        }
        return listOf(
            LlmMessage(
                role = "system",
                content = systemPrompt.trim().ifBlank { LlmConfiguration.DEFAULT_SYSTEM_PROMPT },
            ),
            LlmMessage(
                role = "user",
                content = userContent,
            ),
        )
    }

    private companion object {
        const val MINIMUM_CONTEXT_CHARACTERS = 4_000
        const val MAX_QUESTION_CHARACTERS = 4_000
        const val RESUME_BUDGET_RATIO = 0.60
    }
}

/**
 * Resolves provider configuration and starts one streamed answer.
 */
class InterviewAnswerGenerator(
    private val providers: ProviderConfigurationRepository,
    private val gateway: OpenAiStreamGateway,
    private val promptBuilder: InterviewPromptBuilder,
) {
    /** Model identifier currently selected in provider settings. */
    val model: String
        get() = providers.configuration.value.llm.model

    /**
     * Streams answer deltas for [context].
     *
     * @throws AppError.Configuration when no language-model API key is configured.
     */
    fun generate(context: InterviewAnswerContext): Flow<LlmChunk> {
        val apiKey = providers.llmApiKey()
            ?: throw AppError.Configuration("Language-model API key is not configured")
        val configuration = providers.configuration.value.llm
        val messages = promptBuilder.build(
            context = context,
            maxCharacters = configuration.maxContextCharacters,
            systemPrompt = configuration.systemPrompt,
        )
        return gateway.stream(configuration, apiKey, messages)
    }
}
