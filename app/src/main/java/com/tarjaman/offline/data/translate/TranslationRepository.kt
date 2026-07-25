package com.tarjaman.offline.data.translate

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.concurrent.ConcurrentHashMap

/**
 * مستودع الترجمة: طبقة عزل فوق ML Kit Translate.
 *
 * لماذا ML Kit Translate تحديداً؟
 * - يعمل بالكامل على الجهاز (On-Device) بعد تنزيل نموذج اللغة مرة واحدة فقط.
 * - أخف بكثير من دمج نماذج TFLite/ONNX مخصصة يدوياً (لا حاجة لإدارة Tokenizer أو Runtime يدوي).
 * - مستقر ومختبر على نطاق واسع من Google نفسها ومناسب جداً للأجهزة الضعيفة (استهلاك RAM منخفض،
 *   ومحرك الاستدلال يُدار داخلياً بكفاءة عالية).
 *
 * يدعم التطبيق زوج اللغتين العربية <-> الإنجليزية فقط، تماشياً مع نطاق المنتج المطلوب.
 */
class TranslationRepository {

    private val modelManager = RemoteModelManager.getInstance()

    // تخزين مؤقت للمترجمين حتى لا نعيد إنشاء Translator في كل عملية ترجمة (يوفر ذاكرة ووقت تهيئة)
    private val translatorCache = ConcurrentHashMap<String, com.google.mlkit.nl.translate.Translator>()

    private fun mlkitTag(appLangCode: String): String = when (appLangCode) {
        "ar" -> TranslateLanguage.ARABIC
        "en" -> TranslateLanguage.ENGLISH
        else -> TranslateLanguage.ENGLISH
    }

    private fun translatorFor(source: String, target: String): com.google.mlkit.nl.translate.Translator {
        val key = "$source-$target"
        return translatorCache.getOrPut(key) {
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(mlkitTag(source))
                .setTargetLanguage(mlkitTag(target))
                .build()
            Translation.getClient(options)
        }
    }

    /** يترجم النص، ويتأكد ضمنياً أن النموذج جاهز قبل المتابعة (بدون تنزيل تلقائي عبر بيانات الجوال) */
    suspend fun translate(text: String, source: String, target: String): Result<String> {
        if (text.isBlank()) return Result.success("")
        return try {
            val translator = translatorFor(source, target)
            val result = translator.translate(text).await()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** يفحص هل نموذج لغة معيّنة مثبّت فعلاً على الجهاز (Offline check حقيقي) */
    suspend fun isModelDownloaded(appLangCode: String): Boolean {
        val model = com.google.mlkit.nl.translate.TranslateRemoteModel.Builder(mlkitTag(appLangCode)).build()
        return modelManager.isModelDownloaded(model).await()
    }

    /**
     * يبث تقدّم حالة تنزيل نموذج اللغة كـ Flow، ليعرف المستخدم بالضبط
     * هل النموذج (مثبت / يُنزَّل الآن / جاهز / خطأ).
     */
    fun downloadModel(appLangCode: String, wifiOnly: Boolean): Flow<ModelState> = callbackFlow {
        trySend(ModelState.Downloading)
        val model = com.google.mlkit.nl.translate.TranslateRemoteModel.Builder(mlkitTag(appLangCode)).build()
        val conditions = DownloadConditions.Builder().apply {
            if (wifiOnly) requireWifi()
        }.build()

        modelManager.download(model, conditions)
            .addOnSuccessListener {
                trySend(ModelState.Ready)
                close()
            }
            .addOnFailureListener { e ->
                trySend(ModelState.Error(e.message ?: "download_failed"))
                close()
            }

        awaitClose { }
    }

    suspend fun deleteModel(appLangCode: String): Result<Unit> {
        return try {
            val model = com.google.mlkit.nl.translate.TranslateRemoteModel.Builder(mlkitTag(appLangCode)).build()
            modelManager.deleteDownloadedModel(model).await()
            // نتخلّص من أي Translator مخزَّن مؤقتاً يعتمد على هذا النموذج
            translatorCache.keys.filter { it.contains(appLangCode) }.forEach { translatorCache.remove(it) }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** يجب استدعاؤها عند إغلاق التطبيق لتحرير موارد كل مترجم فوراً (منع تسرّب الذاكرة) */
    fun closeAll() {
        translatorCache.values.forEach { it.close() }
        translatorCache.clear()
    }
}
