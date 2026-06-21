package com.example.data.api

import android.util.Log
import com.example.BuildConfig
import com.example.data.db.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Extensão do PdfReaderViewModel com integração dos novos serviços
 * Centraliza chamadas à API e gerenciamento de contexto
 */
object GeminiIntegrationHelper {
    private const val TAG = "GeminiIntegrationHelper"
    
    /**
     * Processa uma mensagem de usuário com integração completa de serviços
     */
    suspend fun processUserMessageWithServices(
        userMessage: String,
        documentText: String,
        conversationHistory: List<ChatMessage>,
        userLanguage: String,
        studentLevel: Int,
        learningStyle: String
    ): Pair<String, TutorBrainService.TutorResponse> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Processando mensagem com integração de serviços...")
            
            // 1. Cria contexto do tutor
            val tutorContext = TutorBrainService.TutorContext(
                documentText = documentText,
                learningStyle = learningStyle,
                userLanguage = userLanguage,
                empathyLevel = minOf(40 + (conversationHistory.size * 3), 100),
                conversationHistory = conversationHistory,
                studentLevel = studentLevel
            )
            
            // 2. Gera resposta personalizada do tutor
            val tutorResponse = TutorBrainService.generateTutorResponse(
                userMessage = userMessage,
                context = tutorContext
            )
            
            // 3. Processa mensagem com gerenciamento de conversação
            val conversationState = ConversationManagementService.processUserMessage(
                userMessage = userMessage,
                documentContext = documentText,
                conversationHistory = conversationHistory,
                userLanguage = userLanguage,
                studentLevel = studentLevel,
                learningStyle = learningStyle
            )
            
            Pair(conversationState.first, tutorResponse)
            
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao processar mensagem: ${e.message}", e)
            throw e
        }
    }
    
    /**
     * Processa arquivo com suporte multi-formato
     */
    suspend fun processMediaFileWithServices(
        context: android.content.Context,
        uri: android.net.Uri,
        fileType: String,
        fileName: String
    ): Pair<String, Map<String, String>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Processando arquivo: $fileName ($fileType)...")
            
            // 1. Valida arquivo
            val isValid = MediaProcessingService.validateFile(context, uri, fileType)
            if (!isValid) {
                throw Exception("Arquivo inválido ou vazio")
            }
            
            // 2. Processa mídia
            val processedMedia = MediaProcessingService.processMedia(context, uri, fileType)
            
            var extractedText = processedMedia.text
            
            // 3. Se arquivo é multimodal (imagem, áudio, vídeo), extrai texto via Gemini
            if (extractedText.isEmpty() && processedMedia.base64Data != null) {
                extractedText = extractTextFromMultimodal(
                    prompt = buildPromptForFileType(fileType),
                    mimeType = processedMedia.mimeType,
                    base64Data = processedMedia.base64Data,
                    fileType = fileType
                )
            }
            
            if (extractedText.isBlank()) {
                throw Exception("Não foi possível extrair texto do arquivo")
            }
            
            Pair(extractedText, processedMedia.metadata)
            
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao processar arquivo: ${e.message}", e)
            throw e
        }
    }
    
    /**
     * Extrai texto de arquivo multimodal usando Gemini
     */
    private suspend fun extractTextFromMultimodal(
        prompt: String,
        mimeType: String,
        base64Data: String,
        fileType: String
    ): String = withContext(Dispatchers.IO) {
        try {
            val systemInstruction = buildSystemInstructionForFileType(fileType)
            
            val response = GeminiService.callGeminiMultimodal(
                systemInstruction = systemInstruction,
                prompt = prompt,
                mimeType = mimeType,
                base64Data = base64Data,
                model = "gemini-1.5-flash"
            )
            
            if (!response.success) {
                Log.e(TAG, "Erro ao extrair texto multimodal: ${response.error}")
                return@withContext ""
            }
            
            response.content
            
        } catch (e: Exception) {
            Log.e(TAG, "Erro em extractTextFromMultimodal: ${e.message}", e)
            ""
        }
    }
    
    /**
     * Gera resumo enriquecido do conteúdo
     */
    suspend fun generateEnrichedSummaryWithService(
        fileName: String,
        extractedText: String,
        fileType: String
    ): String = withContext(Dispatchers.IO) {
        try {
            val systemInstruction = """
Você é a IA de análise médica inteligente do SinapseMed. Sua missão é analisar materiais acadêmicos e educacionais e extrair insights estratégicos.
            """.trimIndent()
            
            val prompt = """
Analise o seguinte material acadêmico e estruture as informações conforme instruído abaixo.

**Nome do Material**: $fileName
**Tipo**: $fileType

**CONTEÚDO**:
---
${extractedText.take(40000)}
---

Forneça EXATAMENTE neste formato (use as tags como delimitadores):

[SUBJECT]
Identifique a disciplina/matéria principal (ex: Cardiologia, Neurologia, etc.)
[/SUBJECT]

[TOPIC]
Qual é o tópico ou assunto específico coberto?
[/TOPIC]

[ENRICHED_SOURCES]
Recomende 3-4 livros ou fontes acadêmicas tradicionais relacionadas ao tema.
[/ENRICHED_SOURCES]

[SUMMARY]
Forneça um resumo clínico e acadêmico estruturado em 150-200 palavras, destacando conceitos-chave, mecanismos fisiopatológicos e aplicações práticas.
[/SUMMARY]
            """.trimIndent()
            
            val response = GeminiService.callGemini(
                systemInstruction = systemInstruction,
                userPrompt = prompt,
                model = "gemini-1.5-flash",
                maxTokens = 2048
            )
            
            if (!response.success) {
                Log.e(TAG, "Erro ao gerar resumo: ${response.error}")
                return@withContext ""
            }
            
            response.content
            
        } catch (e: Exception) {
            Log.e(TAG, "Erro em generateEnrichedSummaryWithService: ${e.message}", e)
            ""
        }
    }
    
    /**
     * Constrói o system instruction baseado no tipo de arquivo
     */
    private fun buildSystemInstructionForFileType(fileType: String): String {
        return when (fileType.uppercase()) {
            "PHOTO" -> "Você é um especialista em OCR e digitalização médica. Transcreva com precisão todo conteúdo legível, incluindo textos manuscritos, diagramas e anotações."
            "AUDIO" -> "Você é um especialista em transcrição e sumarização de palestras médicas. Transcreva e resuma todas as explicações verbais com foco em conceitos-chave."
            "VIDEO" -> "Você é um analista avançado de videoaulas. Analise todo o conteúdo visual e verbal, transcrevendo e resumindo explicações, casos e demonstrações."
            else -> "Você é um assistente de análise de documentos médicos. Extraia e organize as informações de forma clara e estruturada."
        }
    }
    
    /**
     * Constrói o prompt baseado no tipo de arquivo
     */
    private fun buildPromptForFileType(fileType: String): String {
        return when (fileType.uppercase()) {
            "PHOTO" -> "Transcreva de forma idêntica e complete com as devidas explicações todos os apontamentos, textos manuscritos, desenhos anatômicos ou resumos contidos nesta imagem."
            "AUDIO" -> "Transcreva e resuma de forma exaustiva todas as explicações verbais, raciocínios fisiopatológicos e conceitos técnicos presentes neste áudio de aula."
            "VIDEO" -> "Analise este vídeo didático. Redija uma nota acadêmica completa contendo tudo o que é falado, material visual exibido, casos estudados e conclusões."
            else -> "Extraia o conteúdo textual principal deste arquivo de forma clara e estruturada."
        }
    }
}
