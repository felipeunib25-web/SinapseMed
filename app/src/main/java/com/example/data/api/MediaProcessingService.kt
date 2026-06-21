package com.example.data.api

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import com.example.utils.PdfHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

/**
 * Serviço centralizado para processamento de diferentes tipos de mídia
 * Suporta: PDF, Imagens, Áudio, Vídeo
 */
object MediaProcessingService {
    private const val TAG = "MediaProcessingService"
    
    data class ProcessedMedia(
        val text: String,
        val mimeType: String,
        val base64Data: String? = null,
        val duration: Long? = null,
        val metadata: Map<String, String> = emptyMap()
    )
    
    /**
     * Processa qualquer tipo de mídia suportado
     */
    suspend fun processMedia(
        context: Context,
        uri: Uri,
        fileType: String
    ): ProcessedMedia = withContext(Dispatchers.IO) {
        try {
            return@withContext when (fileType.uppercase()) {
                "PDF" -> processPdf(context, uri)
                "PHOTO", "IMAGE" -> processImage(context, uri)
                "AUDIO" -> processAudio(context, uri)
                "VIDEO" -> processVideo(context, uri)
                else -> throw IllegalArgumentException("Tipo de arquivo não suportado: $fileType")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao processar mídia: ${e.message}", e)
            throw e
        }
    }
    
    /**
     * Processa PDF e extrai texto
     */
    private suspend fun processPdf(context: Context, uri: Uri): ProcessedMedia {
        return withContext(Dispatchers.IO) {
            Log.d(TAG, "Processando PDF...")
            val text = PdfHelper.extractText(context, uri)
            
            if (text.isBlank()) {
                throw Exception("PDF vazio ou não legível")
            }
            
            ProcessedMedia(
                text = text,
                mimeType = "application/pdf",
                metadata = mapOf(
                    "pages" to PdfHelper.getPageCount(context, uri).toString()
                )
            )
        }
    }
    
    /**
     * Processa imagens (fotos de cadernos, slides, etc)
     */
    private suspend fun processImage(context: Context, uri: Uri): ProcessedMedia {
        return withContext(Dispatchers.IO) {
            Log.d(TAG, "Processando imagem...")
            
            val pair = PdfHelper.getScaledImageBytes(context, uri)
                ?: throw Exception("Não foi possível processar a imagem")
            
            val base64 = android.util.Base64.encodeToString(pair.first, android.util.Base64.NO_WRAP)
            
            ProcessedMedia(
                text = "", // Será extraído pela IA
                mimeType = pair.second,
                base64Data = base64,
                metadata = mapOf(
                    "size" to pair.first.size.toString(),
                    "format" to pair.second
                )
            )
        }
    }
    
    /**
     * Processa áudio (gravações de aulas, palestras)
     */
    private suspend fun processAudio(context: Context, uri: Uri): ProcessedMedia {
        return withContext(Dispatchers.IO) {
            Log.d(TAG, "Processando áudio...")
            
            val pair = PdfHelper.getUriBytesAndMime(context, uri)
                ?: throw Exception("Não foi possível carregar o arquivo de áudio")
            
            val base64 = android.util.Base64.encodeToString(pair.first, android.util.Base64.NO_WRAP)
            
            // Extrai metadados de duração
            val duration = try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(context, uri)
                val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                retriever.release()
                durationStr?.toLongOrNull()
            } catch (e: Exception) {
                Log.w(TAG, "Não foi possível extrair duração do áudio: ${e.message}")
                null
            }
            
            ProcessedMedia(
                text = "", // Será transcrito pela IA
                mimeType = pair.second,
                base64Data = base64,
                duration = duration,
                metadata = mapOf(
                    "size" to pair.first.size.toString(),
                    "format" to pair.second,
                    "duration_ms" to (duration?.toString() ?: "desconhecido")
                )
            )
        }
    }
    
    /**
     * Processa vídeo (teleaulas gravadas, simulações)
     */
    private suspend fun processVideo(context: Context, uri: Uri): ProcessedMedia {
        return withContext(Dispatchers.IO) {
            Log.d(TAG, "Processando vídeo...")
            
            var pair = PdfHelper.getUriBytesAndMime(context, uri)
                ?: throw Exception("Não foi possível carregar o arquivo de vídeo")
            
            // Limita tamanho para upload (máximo 14MB para Gemini)
            val maxSize = 14 * 1024 * 1024
            if (pair.first.size > maxSize) {
                Log.w(TAG, "Vídeo truncado de ${pair.first.size} para $maxSize bytes")
                pair = Pair(pair.first.copyOfRange(0, maxSize), pair.second)
            }
            
            val base64 = android.util.Base64.encodeToString(pair.first, android.util.Base64.NO_WRAP)
            
            // Extrai metadados de duração
            val duration = try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(context, uri)
                val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                retriever.release()
                durationStr?.toLongOrNull()
            } catch (e: Exception) {
                Log.w(TAG, "Não foi possível extrair duração do vídeo: ${e.message}")
                null
            }
            
            ProcessedMedia(
                text = "", // Será analisado pela IA
                mimeType = pair.second,
                base64Data = base64,
                duration = duration,
                metadata = mapOf(
                    "size" to pair.first.size.toString(),
                    "format" to pair.second,
                    "duration_ms" to (duration?.toString() ?: "desconhecido"),
                    "truncated" to (pair.first.size > maxSize).toString()
                )
            )
        }
    }
    
    /**
     * Valida se o arquivo é compatível com processamento
     */
    suspend fun validateFile(context: Context, uri: Uri, fileType: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                val isValid = inputStream != null && inputStream.available() > 0
                inputStream?.close()
                isValid
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao validar arquivo: ${e.message}", e)
                false
            }
        }
    }
}
