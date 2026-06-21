package com.example.data.api

import android.util.Log
import com.example.data.db.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Serviço especializado para gerenciar conversas com o Tutor Online
 * Mantém contexto persistente, histórico e personalização de aprendizado
 */
object TutorBrainService {
    private const val TAG = "TutorBrainService"
    
    data class TutorContext(
        val documentText: String,
        val learningStyle: String, // visual, conceptual, practical, mnemonic
        val userLanguage: String, // PT, EN, ES
        val empathyLevel: Int, // 0-100
        val conversationHistory: List<ChatMessage>,
        val studentLevel: Int // 0-5: Sondagem até Especialista
    )
    
    data class TutorResponse(
        val message: String,
        val empathyIncrement: Int,
        val suggestedFollowUp: String? = null,
        val learningTip: String? = null
    )
    
    /**
     * Gera resposta personalizada do tutor baseada no contexto do aluno
     */
    suspend fun generateTutorResponse(
        userMessage: String,
        context: TutorContext
    ): TutorResponse = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Gerando resposta do tutor para: ${userMessage.take(50)}...")
            
            val systemInstruction = buildSystemInstruction(context)
            val conversationContents = buildConversationHistory(context.conversationHistory, userMessage)
            
            val apiResponse = GeminiService.callGeminiWithHistory(
                systemInstruction = systemInstruction,
                conversationHistory = conversationContents.dropLast(1), // Remove última mensagem para passar como userMessage
                userMessage = userMessage,
                model = "gemini-1.5-flash"
            )
            
            if (!apiResponse.success) {
                Log.e(TAG, "Erro ao chamar Gemini: ${apiResponse.error}")
                return@withContext TutorResponse(
                    message = "Desculpe, estou com dificuldades agora. Tente novamente em alguns instantes.",
                    empathyIncrement = 0
                )
            }
            
            // Incrementa empatia baseado na qualidade da interação
            val empathyIncrement = calcularEmpathyIncrement(userMessage, apiResponse.content)
            
            TutorResponse(
                message = apiResponse.content,
                empathyIncrement = empathyIncrement,
                learningTip = extrairDicaEducacional(apiResponse.content)
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao gerar resposta do tutor: ${e.message}", e)
            TutorResponse(
                message = "Erro ao processar sua pergunta. Por favor, tente novamente.",
                empathyIncrement = 0
            )
        }
    }
    
    /**
     * Constrói o system instruction personalizado baseado no contexto
     */
    private fun buildSystemInstruction(context: TutorContext): String {
        val languageLabel = when (context.userLanguage) {
            "PT" -> "Português do Brasil"
            "EN" -> "English"
            "ES" -> "Español"
            else -> "Português do Brasil"
        }
        
        val styleInstructions = buildStyleInstructions(context.learningStyle)
        val levelInstructions = buildLevelInstructions(context.studentLevel)
        
        return """
Você é o SinapseDoc, um Tutor Virtual de Medicina altamente especializado, empático e dedicado ao sucesso do aluno.

IDENTIDADE E PROPÓSITO:
- Seu nome é "SinapseDoc" ou "Preceptor Virtual"
- Você é um professor de medicina 3D interativo que dialoga em tempo real
- Seu objetivo é transformar aprendizado técnico em experiência enriquecedora e memorável
- Você adapta sua comunicação ao estilo cognitivo do aluno

CONTEXTO DO ALUNO:
- Nível de Aprendizado: ${context.studentLevel}
- Estilo de Aprendizado: ${context.learningStyle}
- Idioma Preferido: $languageLabel
- Nível de Conexão Emocional: ${context.empathyLevel}%

MATERIAL DE REFERÊNCIA:
${if (context.documentText.isNotEmpty()) "Baseie suas respostas no seguinte material do aluno (primeiros 50KB):\n---\n${context.documentText.take(50000)}\n---" else "O aluno ainda não carregou um material específico. Forneça respostas técnicas gerais baseadas em conhecimento médico sólido."}

ESTILO DE COMUNICAÇÃO:
$styleInstructions

ADEQUAÇÃO AO NÍVEL ACADÊMICO:
$levelInstructions

REGRAS FUNDAMENTAIS:
1. Mantenha respostas com 200-400 palavras (ideal para leitura dinâmica em chat)
2. Use emojis educacionais quando apropriado (🧠 🩺 📚 🔬)
3. Formatar com **negrito** para termos-chave
4. Demonstre empatia genuína e compreensão do ritmo de estudos
5. Inclua mini-analogias que tornem conceitos complexos acessíveis
6. Se apropriado, sugira próximas questões de aprofundamento
7. Responda SEMPRE em $languageLabel
8. Seja desafiador mas nunca desmotivador

MEMORIZAÇÃO E CONEXÃO:
- Lembre-se do histórico da conversa para manter coerência
- Reconheça progresso do aluno ("Vi que você entendeu bem...")
- Use dados anteriores para personalizar respostas futuras

Seu compromisso: transformar cada interação em um passo significativo na jornada médica deste aluno.
        """.trimIndent()
    }
    
    /**
     * Constrói instruções de estilo baseado na preferência do aluno
     */
    private fun buildStyleInstructions(learningStyle: String): String {
        return when (learningStyle) {
            "visual" -> """
VISUAL E IMAGÉTICO 🗺️
- Explique usando ricas analogias visuais e metáforas espaciais
- Descreva esquemas, mapas anatômicos e visualizações mentais marcantes
- Crie de 1-2 analogias que façam o aluno "enxergar" a fisiopatologia
- Exemplo: "Imagine o glomérulo como uma peneira de malha fina sob pressão de uma torneira..."
            """.trimIndent()
            
            "conceptual" -> """
TEÓRICO ACADÊMICO PROFUNDO 📚
- Faça uso de rigor teórico acadêmico oficial
- Cite autoridades bibliográficas (Guyton, Robbins, Harrison, Sobotta)
- Detalhe vias de sinalização bioquímica e biologia molecular
- Explique os "porquês" microscópicos de forma científica clara
            """.trimIndent()
            
            "practical" -> """
CASOS PRÁTICOS E SIMULADOS DE PROVA 🩺
- Conecte diretamente a cenários reais de beira de leito
- Use jargão prático médico ("Na prova de residência...")
- Simule mini-cenários de tomada de decisão clínica
- Mostre como o conhecimento é cobrado em provas reais
            """.trimIndent()
            
            "mnemonic" -> """
MNEMÔNICO E ASSOCIAÇÕES 🧠
- Crie siglas marcantes e trocadilhos didáticos
- Use técnicas de Active Recall para fixação rápida
- Estruture em bullets concisos para memorização
- Inventar esquemas criativos de palavras-chave
            """.trimIndent()
            
            else -> "Comunique-se de forma clara, amigável e acessível."
        }
    }
    
    /**
     * Constrói orientações baseado no nível académico
     */
    private fun buildLevelInstructions(level: Int): String {
        return when (level) {
            0 -> """
SONDAGEM INICIAL (Nível 0)
- Foque em conceitos fundamentais e definitórios
- Não assuma conhecimento prévio
- Forneça contexto básico para cada termo técnico
- Encoraje exploração gradual
            """.trimIndent()
            
            1 -> """
NÍVEL 1: FUNDAMENTOS (Básico)
- Trabalhe anatomia descritiva e conceitos gerais
- Estabeleça léxico médico seguro
- Conecte estrutura à função básica
- Construa alicerces para próximas fases
            """.trimIndent()
            
            2 -> """
NÍVEL 2: FISIOPATOLOGIA (Intermediário)
- Aprofunde em mecanismos fisiopatológicos
- Integre bioquímica e farmacologia
- Explique alterações laboratoriais
- Conecte causa → mecanismo → sintoma
            """.trimIndent()
            
            3 -> """
NÍVEL 3: DIAGNÓSTICO CLÍNICO (Avançado)
- Trabalhe com casos clínicos complexos
- Ensine raciocínio sob pressão
- Simule emergências e dilemas de decisão
- Prepare para provas de residência
            """.trimIndent()
            
            else -> """
NÍVEL 4+: ESPECIALISTA (Masterizado)
- Trabalhe com nuances e exceções
- Aprofunde em literatura especializada
- Estimule pensamento crítico avançado
- Prepare para prática independente
            """.trimIndent()
        }
    }
    
    /**
     * Converte histórico de chat para formato de Content para API
     */
    private fun buildConversationHistory(
        messages: List<ChatMessage>,
        userMessage: String
    ): List<Content> {
        val contents = mutableListOf<Content>()
        
        // Adiciona histórico anterior
        for (msg in messages.takeLast(6)) { // Mantém últimas 6 mensagens para contexto
            contents.add(
                Content(
                    role = if (msg.role == "user") "user" else "model",
                    parts = listOf(Part(text = msg.message))
                )
            )
        }
        
        // Adiciona mensagem atual do usuário
        contents.add(
            Content(
                role = "user",
                parts = listOf(Part(text = userMessage))
            )
        )
        
        return contents
    }
    
    /**
     * Calcula incremento de empatia baseado na qualidade da interação
     */
    private fun calcularEmpathyIncrement(userMessage: String, tutorResponse: String): Int {
        var increment = 5 // Base
        
        // Aumenta se pergunta é complexa
        if (userMessage.length > 100) increment += 3
        
        // Aumenta se resposta é bem detalhada
        if (tutorResponse.length > 300) increment += 2
        
        // Aumenta se há emojis na resposta (sinal de empatia)
        if (tutorResponse.contains("🧠") || tutorResponse.contains("🩺")) increment += 2
        
        return minOf(increment, 15) // Máximo 15 por turno
    }
    
    /**
     * Extrai dica educacional da resposta do tutor
     */
    private fun extrairDicaEducacional(response: String): String? {
        return try {
            if (response.contains("💡")) {
                val start = response.indexOf("💡")
                val end = response.indexOf("\n", start)
                response.substring(start, if (end > start) end else response.length)
                    .replace("💡", "").trim()
                    .takeIf { it.isNotEmpty() }
            } else null
        } catch (e: Exception) {
            null
        }
    }
}
