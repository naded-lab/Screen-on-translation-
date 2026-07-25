package com.tarjaman.offline.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * يبني Typography كاملة بناءً على عائلة الخط المختارة ومعامل تكبير الحجم المحفوظ في الإعدادات.
 * هذا ما يجعل تغيير الخط ينعكس فوراً على كل شاشات التطبيق دون استثناء.
 */
fun buildAppTypography(fontFamily: FontFamily, scale: Float): Typography {
    fun sp(value: Int) = (value * scale).sp
    return Typography(
        displayLarge = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = sp(57), lineHeight = sp(64)),
        displayMedium = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = sp(45), lineHeight = sp(52)),
        headlineLarge = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.SemiBold, fontSize = sp(32), lineHeight = sp(40)),
        headlineMedium = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.SemiBold, fontSize = sp(28), lineHeight = sp(36)),
        titleLarge = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.SemiBold, fontSize = sp(22), lineHeight = sp(28)),
        titleMedium = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Medium, fontSize = sp(16), lineHeight = sp(24)),
        bodyLarge = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = sp(16), lineHeight = sp(24)),
        bodyMedium = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = sp(14), lineHeight = sp(20)),
        bodySmall = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = sp(12), lineHeight = sp(16)),
        labelLarge = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Medium, fontSize = sp(14), lineHeight = sp(20)),
        labelMedium = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Medium, fontSize = sp(12), lineHeight = sp(16)),
        labelSmall = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Medium, fontSize = sp(11), lineHeight = sp(16))
    )
}
