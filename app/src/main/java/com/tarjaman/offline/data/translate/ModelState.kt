package com.tarjaman.offline.data.translate

/** يمثّل حالة نموذج الترجمة على الجهاز، ليعرف المستخدم دائماً هل الترجمة جاهزة Offline أم لا */
sealed interface ModelState {
    data object NotDownloaded : ModelState
    data object Downloading : ModelState
    data object Ready : ModelState
    data class Error(val message: String) : ModelState
}
