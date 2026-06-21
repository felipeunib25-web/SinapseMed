package com.example.data.api

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * Serviço robusto centralizado para comunicação com Gemini API
 * Gerencia requisições, tratamento de erros e cache de respostas
 */
object GeminiService {
    private const val TAG = "GeminiService"
    private const val DEFAULT_MODEL = "gemini-1.5-flash"
    private const val MAX_RETRIES = 3
    private const val TIMEOUT_MS = 30000L
    
    // Cache simples para respostas recentes (evita requisições duplicadas)
    private val responseCache = ConcurrentHashMap<String, CacheEntry>()
    private const val CACHE_DURATION_MS = 5 * 60 * 1000L // 5 minutos
    
    data class CacheEntry(
        val response: String,
        val timestamp: Long
    )
    
    data class ApiResponse(
        val success: Boolean,
        val content: String,
        val error: String? = null,
        val retryable: Boolean = false
    )
    
    /**
     * Realiza uma chamada genérica à API Gemini com retry automático
     */
    suspend fun callGemini(
        systemInstruction: String,
        userPrompt: String,
        model: String = DEFAULT_MODEL,
        temperature: Float = 0.7f,
        maxTokens: Int = 2048
    ): ApiResponse = withContext(Dispatchers.IO) {
        if (!isApiKeyConfigured()) {
            return@withContext ApiResponse(
                success = false,
                content = "",
                error = "Chave da API Gemini não está configurada",
                retryable = false
            )
        }
        
        val cacheKey = generateCacheKey(systemInstruction, userPrompt, model)
        
        // Verifica cache
        responseCache[cacheKey]?.let { cacheEntry ->
            if (System.currentTimeMillis() - cacheEntry.timestamp < CACHE_DURATION_MS) {
                Log.d(TAG, "Retornando resposta do cache")
                return@withContext ApiResponse(
                    success = true,
                    content = cacheEntry.response
                )
            } else {
                responseCache.remove(cacheKey)
            }
        }
        
        var lastError: Exception? = null
        var isRetryable = false
        
        // Tenta com retry
        repeat(MAX_RETRIES) { attempt ->
            try {
                Log.d(TAG, "Tentativa ${attempt + 1}/$MAX_RETRIES para modelo: $model")
                
                val content = Content(
                    parts = listOf(Part(text = userPrompt)),
                    role = "user"
                )
                
                val request = GenerateContentRequest(
                    contents = listOf(content),
                    systemInstruction = Content(parts = listOf(Part(text = systemInstruction))),
                    generationConfig = GenerationConfig(
                        temperature = temperature,
                        maxOutputTokens = maxTokens
                    )
                )
                
                val response = try {
                    RetrofitClient.service.generateContent(
                        model = model,
                        apiKey = BuildConfig.GEMINI_API_KEY,
                        request = request
                    )
                } catch (e: Exception) {
                    throw e
                }
                
                val responseText = response.candidates?.firstOrNull()
                    ?.content?.parts?.firstOrNull()?.text
                    ?: throw Exception("Resposta vazia do Gemini")
                
                // Salva no cache
                responseCache[cacheKey] = CacheEntry(responseText, System.currentTimeMillis())
                
                Log.d(TAG, "Sucesso na chamada à API Gemini")
                return@withContext ApiResponse(
                    success = true,
                    content = responseText
                )
                
            } catch (e: Exception) {
                lastError = e
                isRetryable = e.message?.contains("429") == true || 
                             e.message?.contains("503") == true ||
                             e.message?.contains("timeout") == true
                
                if (!isRetryable || attempt == MAX_RETRIES - 1) {
                    Log.e(TAG, "Erro na chamada à API: ${e.message}", e)
                    if (attempt < MAX_RETRIES - 1) {
                        Thread.sleep(1000 * (attempt + 1).toLong()) // Backoff exponencial
                    }
                }
            }
        }
        
        ApiResponse(
            success = false,
            content = "",
            error = lastError?.message ?: "Erro desconhecido ao chamar Gemini",
            retryable = isRetryable
        )
    }
    
    /**
     * Chamada com histórico de conversação para chat persistente
     */
    suspend fun callGeminiWithHistory(
        systemInstruction: String,
        conversationHistory: List<Content>,
        userMessage: String,
        model: String = DEFAULT_MODEL
    ): ApiResponse = withContext(Dispatchers.IO) {
        if (!isApiKeyConfigured()) {
            return@withContext ApiResponse(
                success = false,
                content = "",
                error = "Chave da API Gemini não está configurada",
                retryable = false
            )
        }
        
        try {
            val allContents = conversationHistory.toMutableList()
            allContents.add(Content(
                parts = listOf(Part(text = userMessage)),
                role = "user"
            ))
            
            val request = GenerateContentRequest(
                contents = allContents,
                systemInstruction = Content(parts = listOf(Part(text = systemInstruction)))
            )
            
            val response = RetrofitClient.service.generateContent(
                model = model,
                apiKey = BuildConfig.GEMINI_API_KEY,
                request = request
            )
            
            val responseText = response.candidates?.firstOrNull()
                ?.content?.parts?.firstOrNull()?.text
                ?: throw Exception("Resposta vazia do Gemini")
            
            return@withContext ApiResponse(
                success = true,
                content = responseText
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Erro na chamada com histórico: ${e.message}", e)
            return@withContext ApiResponse(
                success = false,
                content = "",
                error = e.message ?: "Erro desconhecido",
                retryable = e.message?.contains("429") == true
            )
        }
    }
    
    /**
     * Chamada com conteúdo multimodal (imagem, áudio, vídeo)
     */
    suspend fun callGeminiMultimodal(
        systemInstruction: String,
        prompt: String,
        mimeType: String,
        base64Data: String,
        model: String = DEFAULT_MODEL
    ): ApiResponse = withContext(Dispatchers.IO) {
        if (!isApiKeyConfigured()) {
            return@withContext ApiResponse(
                success = false,
                content = "",
                error = "Chave da API Gemini não está configurada",
                retryable = false
            )
        }
        
        try {
            val inlineData = InlineData(mimeType = mimeType, data = base64Data)
            val content = Content(
                parts = listOf(
                    Part(text = prompt),
                    Part(inlineData = inlineData)
                ),
                role = "user"
            )
            
            val request = GenerateContentRequest(
                contents = listOf(content),
                systemInstruction = Content(parts = listOf(Part(text = systemInstruction)))
            )
            
            val response = RetrofitClient.service.generateContent(
                model = model,
                apiKey = BuildConfig.GEMINI_API_KEY,
                request = request
            )
            
            val responseText = response.candidates?.firstOrNull()
                ?.content?.parts?.firstOrNull()?.text
                ?: throw Exception("Resposta vazia do Gemini")
            
            return@withContext ApiResponse(
                success = true,
                content = responseText
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Erro na chamada multimodal: ${e.message}", e)
            return@withContext ApiResponse(
                success = false,
                content = "",
                error = e.message ?: "Erro desconhecido",
                retryable = e.message?.contains("429") == true
            )
        }
    }
    
    /**
     * Limpa o cache de respostas (útil para liberar memória)
     */
    fun clearCache() {
        responseCache.clear()
        Log.d(TAG, "Cache de respostas limpo")
    }
    
    /**
     * Verifica se a chave da API está configurada
     */
    private fun isApiKeyConfigured(): Boolean {
        return BuildConfig.GEMINI_API_KEY.isNotEmpty() && 
               BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY"
    }
    
    /**
     * Gera chave de cache baseada nos parâmetros
     */
    private fun generateCacheKey(
        systemInstruction: String,
        userPrompt: String,
        model: String
    ): String {
        return "$model:${systemInstruction.take(50)}:${userPrompt.take(100)}".hashCode().toString()
    }
}
