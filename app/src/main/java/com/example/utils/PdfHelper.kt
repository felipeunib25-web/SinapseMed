package com.example.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Base64
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.ByteArrayOutputStream
import java.io.InputStream

object PdfHelper {

    fun init(context: Context) {
        PDFBoxResourceLoader.init(context.applicationContext)
    }

    /**
     * Extrai todo o texto contido no arquivo PDF.
     */
    fun extractText(context: Context, uri: Uri): String {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val document = PDDocument.load(inputStream)
                val stripper = PDFTextStripper()
                val text = stripper.getText(document)
                document.close()
                text.trim()
            } ?: ""
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    /**
     * Obtém a quantidade total de páginas do arquivo PDF.
     */
    fun getPageCount(context: Context, uri: Uri): Int {
        return try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { fileDescriptor ->
                val renderer = PdfRenderer(fileDescriptor)
                val count = renderer.pageCount
                renderer.close()
                count
            } ?: 0
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }

    /**
     * Renderiza uma página específica do PDF como um Bitmap.
     */
    fun renderPageToBitmap(context: Context, uri: Uri, pageIndex: Int, scale: Float = 1.5f): Bitmap? {
        return try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { fileDescriptor ->
                val renderer = PdfRenderer(fileDescriptor)
                if (pageIndex < 0 || pageIndex >= renderer.pageCount) {
                    renderer.close()
                    return null
                }
                val page = renderer.openPage(pageIndex)

                val width = (page.width * scale).toInt()
                val height = (page.height * scale).toInt()
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

                val canvas = Canvas(bitmap)
                canvas.drawColor(Color.WHITE) // Garante fundo branco

                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                renderer.close()
                bitmap
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Converte um Bitmap para representação Base64 (JPEG, 80% qualidade).
     */
    fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    /**
     * Obtém os bytes brutos e o MimeType de qualquer URI de arquivo.
     */
    fun getUriBytesAndMime(context: Context, uri: Uri): Pair<ByteArray, String>? {
        return try {
            val mimeType = context.contentResolver.getType(uri) ?: when {
                uri.path?.endsWith(".mp3", true) == true -> "audio/mp3"
                uri.path?.endsWith(".wav", true) == true -> "audio/wav"
                uri.path?.endsWith(".m4a", true) == true -> "audio/m4a"
                uri.path?.endsWith(".mp4", true) == true -> "video/mp4"
                uri.path?.endsWith(".jpg", true) == true || uri.path?.endsWith(".jpeg", true) == true -> "image/jpeg"
                uri.path?.endsWith(".png", true) == true -> "image/png"
                else -> "application/octet-stream"
            }
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val bytes = inputStream.readBytes()
                Pair(bytes, mimeType)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Carrega uma imagem de tamanho arbitrário de uma URI de forma eficiente, downscaling para max 1024px e comprimindo.
     */
    fun getScaledImageBytes(context: Context, uri: Uri): Pair<ByteArray, String>? {
        return try {
            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(uri) ?: "image/jpeg"
            
            val options = android.graphics.BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            contentResolver.openInputStream(uri)?.use { inputStream ->
                android.graphics.BitmapFactory.decodeStream(inputStream, null, options)
            }

            var scale = 1
            val maxDim = 1024
            if (options.outWidth > maxDim || options.outHeight > maxDim) {
                scale = Math.max(options.outWidth / maxDim, options.outHeight / maxDim)
            }

            val decodeOptions = android.graphics.BitmapFactory.Options().apply {
                inSampleSize = scale
            }
            
            var bitmap: Bitmap? = null
            contentResolver.openInputStream(uri)?.use { inputStream ->
                bitmap = android.graphics.BitmapFactory.decodeStream(inputStream, null, decodeOptions)
            }

            val finalBitmap = bitmap
            if (finalBitmap != null) {
                val outputStream = ByteArrayOutputStream()
                finalBitmap.compress(Bitmap.CompressFormat.JPEG, 75, outputStream)
                val bytes = outputStream.toByteArray()
                Pair(bytes, "image/jpeg")
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
