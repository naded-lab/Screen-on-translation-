package com.tarjaman.offline.ui.font

import androidx.compose.ui.text.font.FontFamily
import com.tarjaman.offline.R

/**
 * مدير الخطوط.
 *
 * قرار هندسي مهم: نعتمد على "عائلات خطوط النظام" (System Generic Font Families) المضمونة
 * الوجود على كل جهاز أندرويد (بما فيها Redmi/MIUI)، وتدعم رسم العربية بشكل صحيح تماماً
 * عبر نظام Font Fallback المدمج في أندرويد (عادة Noto Sans/Naskh Arabic مثبّتة كخط احتياطي
 * على مستوى النظام). هذا يضمن:
 *   - عدم وجود أي اعتماد على الإنترنت حتى لأول استخدام (بخلاف مزوّدات الخطوط السحابية).
 *   - عدم زيادة حجم الـ APK بملفات خطوط مضمَّنة.
 *   - ثبات 100% دون أي فشل تنزيل محتمل.
 *
 * لإضافة خط عربي مخصص لاحقاً (مثل ملفات .ttf الخاصة بك):
 *   1) ضع الملف في: app/src/main/res/font/my_font.ttf (اسم الملف بحروف صغيرة وأرقام و _ فقط)
 *   2) أضف عنصراً جديداً هنا: CUSTOM("custom", R.string.font_custom, FontFamily(Font(R.font.my_font)))
 *   3) سيظهر تلقائياً في قائمة الإعدادات ويُطبَّق فوراً على كامل التطبيق.
 */
enum class AppFont(val key: String, val displayNameRes: Int, val fontFamily: FontFamily) {
    SYSTEM_DEFAULT("system_default", R.string.font_system_default, FontFamily.Default),
    SANS_SERIF("sans_serif", R.string.font_sans_serif, FontFamily.SansSerif),
    SERIF("serif", R.string.font_serif, FontFamily.Serif),
    CONDENSED("condensed", R.string.font_condensed, FontFamily(genericFontFamily = "sans-serif-condensed"));

    companion object {
        fun fromKey(key: String): AppFont = entries.find { it.key == key } ?: SYSTEM_DEFAULT
    }
}
