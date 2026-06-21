package com.example.data.api

import android.util.Log
import com.example.data.db.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Serviço para gerenciar conversações multi-turno avançadas
 * Mantém contexto, história e sugestões de follow-up inteligentes
 */
object ConversationManagementService {
    private const val TAG = "ConversationManagementService"
    private const val MAX_HISTORY_LENGTH = 10 // Mantém últimas 10 mensagens
    
    data class ConversationState(
        val messages: List<ChatMessage>,
        val contextSummary: String,
        val suggestedTopics: List<String>,
        val engagementLevel: Float // 0.0 a 1.0
    )
    
    data class FollowUpSuggestion(
        val topic: String,
        val question: String,
        val difficulty: String // easy, medium, hard
    )
    
    /**
     * Processa uma mensagem do usuário e gera resposta contextualizada
     */
    suspend fun processUserMessage(
        userMessage: String,
        documentContext: String,
        conversationHistory: List<ChatMessage>,
        userLanguage: String,
        studentLevel: Int,
        learningStyle: String
    ): Pair<String, ConversationState> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Processando mensagem do usuário: ${userMessage.take(50)}...")
            
            // Cria contexto refinado
            val systemPrompt = buildContextualSystemPrompt(
                userLanguage = userLanguage,
                studentLevel = studentLevel,
                learningStyle = learningStyle,
                documentContext = documentContext
            )
            
            // Prepara histórico para envio à API
            val contentHistory = conversationHistory
                .takeLast(MAX_HISTORY_LENGTH - 1)
                .map { msg ->
                    Content(
                        role = if (msg.role == "user") "user" else "model",
                        parts = listOf(Part(text = msg.message))
                    )
                }
                .toMutableList()
            
            // Chama API com histórico
            val apiResponse = GeminiService.callGeminiWithHistory(
                systemInstruction = systemPrompt,
                conversationHistory = contentHistory,
                userMessage = userMessage,
                model = "gemini-1.5-flash"
            )
            
            if (!apiResponse.success) {
                Log.e(TAG, "Erro na resposta da API: ${apiResponse.error}")
                return@withContext Pair(
                    "Desculpe, tive um problema ao processar sua pergunta. Tente novamente.",
                    ConversationState(
                        messages = conversationHistory,
                        contextSummary = "",
                        suggestedTopics = emptyList(),
                        engagementLevel = 0.5f
                    )
                )
            }
            
            // Calcula sugestões de tópicos para follow-up
            val suggestedTopics = gerarSugestoesDeFollowUp(
                userMessage = userMessage,
                tutorResponse = apiResponse.content,
                documentContext = documentContext,
                userLanguage = userLanguage
            )
            
            // Calcula nível de engajamento baseado na interação
            val engagementLevel = calcularEngajamento(
                userMessage = userMessage,
                tutorResponse = apiResponse.content,
                historyLength = conversationHistory.size
            )
            
            val newState = ConversationState(
                messages = conversationHistory + listOf(
                    ChatMessage(
                        documentId = 0,
                        role = "user",
                        message = userMessage
                    ),
                    ChatMessage(
                        documentId = 0,
                        role = "model",
                        message = apiResponse.content
                    )
                ),
                contextSummary = extrairResumoContexto(apiResponse.content),
                suggestedTopics = suggestedTopics.map { it.question },
                engagementLevel = engagementLevel
            )
            
            Pair(apiResponse.content, newState)
            
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao processar mensagem: ${e.message}", e)
            throw e
        }
    }
    
    /**
     * Constrói o system prompt contextualizado
     */
    private fun buildContextualSystemPrompt(
        userLanguage: String,
        studentLevel: Int,
        learningStyle: String,
        documentContext: String
    ): String {
        val langLabel = when (userLanguage) {
            "PT" -> "Português do Brasil"
            "EN" -> "English"
            "ES" -> "Español"
            else -> "Português do Brasil"
        }
        
        return """
Você é o SinapseDoc, um Tutor de Medicina especializado em ensino personalizado adaptativo.

CONTEXTO DO ALUNO:
- Idioma: $langLabel
- Nível Académico: $studentLevel (0=Iniciante até 5=Especialista)
- Estilo de Aprendizado: $learningStyle

DIRETIVAS DE RESPOSTA:
1. Personalize toda resposta ao nível e estilo do aluno
2. Mantenha continuidade com o histórico da conversa
3. Use analogias e exemplos do contexto médico real
4. Inclua referências ao material do aluno quando relevante
5. Sugira próximos passos de aprendizado naturalmente
6. Valide e celebre o progresso do aluno

REGRAS DE FORMATO:
- Responda em $langLabel
- Use **negrito** para termos-chave
- Inclua emojis educacionais quando apropriado
- Mantenha respostas entre 200-400 palavras
- Se apropriado, termine com uma pergunta de aprofundamento

MATERIAL DE REFERÊNCIA:
${if (documentContext.isNotEmpty()) documentContext.take(30000) else "Nenhum documento carregado. Use conhecimento médico geral."}

Seu objetivo: transformar cada interação em um passo significativo na jornada de aprendizado deste aluno.
        """.trimIndent()
    }
    
    /**
     * Gera sugestões de tópicos para follow-up
     */
    private suspend fun gerarSugestoesDeFollowUp(
        userMessage: String,
        tutorResponse: String,
        documentContext: String,
        userLanguage: String
    ): List<FollowUpSuggestion> = withContext(Dispatchers.IO) {
        try {
            val prompt = when (userLanguage) {
                "PT" -> """
Com base na pergunta do aluno e resposta do tutor, sugira 3 tópicos de aprofundamento.

Pergunta do Aluno: $userMessage
Resposta do Tutor: $tutorResponse

Forneça EXATAMENTE neste formato:
TOPIC_1: [tópico fácil] | [pergunta simples sobre o assunto]
TOPIC_2: [tópico médio] | [pergunta mais aprofundada]
TOPIC_3: [tópico difícil] | [pergunta de nível avançado/caso clínico]
                """
                "EN" -> """
Based on the student's question and tutor response, suggest 3 follow-up learning topics.

Student Question: $userMessage
Tutor Response: $tutorResponse

Provide EXACTLY in this format:
TOPIC_1: [easy topic] | [simple question about the subject]
TOPIC_2: [medium topic] | [deeper learning question]
TOPIC_3: [hard topic] | [advanced question/clinical case]
                """
                else -> """
Con base en la pregunta del alumno y la respuesta del tutor, sugiere 3 temas de seguimiento.

Pregunta del Alumno: $userMessage
Respuesta del Tutor: $tutorResponse

Proporcione EXACTAMENTE en este formato:
TOPIC_1: [tema fácil] | [pregunta simple sobre el tema]
TOPIC_2: [tema medio] | [pregunta de aprendizaje más profundo]
TOPIC_3: [tema difícil] | [pregunta avanzada/caso clínico]
                """
            }
            
            val systemInstruction = "Eres un experto en diseño curricular médico. Sugiere tópicos de follow-up progresivos."
            
            val response = GeminiService.callGemini(
                systemInstruction = systemInstruction,
                userPrompt = prompt,
                model = "gemini-1.5-flash"
            )
            
            if (!response.success) return@withContext emptyList()
            
            // Parse da resposta
            val suggestions = mutableListOf<FollowUpSuggestion>()
            response.content.lines().forEach { line ->
                if (line.startsWith("TOPIC_")) {
                    try {
                        val parts = line.substringAfter(": ").split(" | ")
                        if (parts.size == 2) {
                            val difficulty = when {
                                line.contains("TOPIC_1") -> "easy"
                                line.contains("TOPIC_2") -> "medium"
                                else -> "hard"
                            }
                            suggestions.add(
                                FollowUpSuggestion(
                                    topic = parts[0].trim(),
                                    question = parts[1].trim(),
                                    difficulty = difficulty
                                )
                            )
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Erro ao parsear sugestão: $line")
                    }
                }
            }
            
            suggestions
            
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao gerar sugestões de follow-up: ${e.message}", e)
            emptyList()
        }
    }
    
    /**
     * Extrai resumo do contexto da resposta
     */
    private fun extrairResumoContexto(response: String): String {
        return try {
            // Pega as primeiras 2 linhas ou primeiras 150 caracteres
            val lines = response.split("\n")
            lines.take(2).joinToString(" ").take(150) + "..."
        } catch (e: Exception) {
            response.take(150) + "..."
        }
    }
    
    /**
     * Calcula nível de engajamento baseado na qualidade da interação
     */
    private fun calcularEngajamento(
        userMessage: String,
        tutorResponse: String,
        historyLength: Int
    ): Float {
        var score = 0.5f // Base
        
        // Aumenta com perguntas complexas
        if (userMessage.length > 100) score += 0.1f
        if (userMessage.contains("?")) score += 0.05f
        if (userMessage.contains("por que") || userMessage.contains("como")) score += 0.05f
        
        // Aumenta com respostas detalhadas
        if (tutorResponse.length > 300) score += 0.1f
        if (tutorResponse.contains("**")) score += 0.05f // Tem formatação
        
        // Aumenta com histórico de conversa longa (persistência)
        if (historyLength > 5) score += 0.1f
        if (historyLength > 10) score += 0.05f
        
        return minOf(score, 1.0f)
    }
    
    /**
     * Limpa histórico antigo para manter performance
     */
    fun trimConversationHistory(
        messages: List<ChatMessage>,
        maxMessages: Int = MAX_HISTORY_LENGTH
    ): List<ChatMessage> {
        return if (messages.size > maxMessages) {
            messages.takeLast(maxMessages)
        } else {
            messages
        }
    }
}
