package com.tarjaman.offline.data.ocr

import android.graphics.Bitmap
import android.graphics.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await

/** كتلة نص واحدة مكتشَفة في الصورة مع موضعها بالضبط لعرض الترجمة في نفس المكان */
data class RecognizedBlock(val text: String, val boundingBox: Rect)

/**
 * يستخرج النصوص من لقطة شاشة محلياً بالكامل (بدون إنترنت).
 * نستخدم نموذج اللاتينية (Latin) الافتراضي من ML Kit لأن معظم فصول المانهوا المترجمة على
 * تطبيقات القراءة تعرض النص بالأحرف اللاتينية (إنجليزي).
 */
class TextRecognitionRepository {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun recognize(bitmap: Bitmap): List<RecognizedBlock> {
        val image = InputImage.fromBitmap(bitmap, 0)
        val result = recognizer.process(image).await()
        return result.textBlocks.mapNotNull { block ->
            val box = block.boundingBox ?: return@mapNotNull null
            RecognizedBlock(text = block.text, boundingBox = box)
        }
    }

    fun close() {
        recognizer.close()
    }
}
