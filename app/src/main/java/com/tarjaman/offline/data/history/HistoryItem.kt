package com.tarjaman.offline.data.history

/** عنصر واحد في سجل الترجمات الأخيرة */
data class HistoryItem(
    val id: Long,
    val sourceLang: String,
    val targetLang: String,
    val sourceText: String,
    val translatedText: String,
    val timestamp: Long
) {
    /** تسلسل بسيط بفاصل آمن (نتجنب Room/Gson لتقليل حجم APG وتبعيات البناء) */
    fun serialize(): String {
        fun esc(s: String) = s.replace("\\", "\\\\").replace("|", "\\p").replace("\n", "\\n")
        return listOf(id.toString(), sourceLang, targetLang, esc(sourceText), esc(translatedText), timestamp.toString())
            .joinToString("|")
    }

    companion object {
        fun deserialize(raw: String): HistoryItem? {
            return try {
                val parts = splitEscaped(raw)
                if (parts.size != 6) return null
                fun unesc(s: String) = s.replace("\\n", "\n").replace("\\p", "|").replace("\\\\", "\\")
                HistoryItem(
                    id = parts[0].toLong(),
                    sourceLang = parts[1],
                    targetLang = parts[2],
                    sourceText = unesc(parts[3]),
                    translatedText = unesc(parts[4]),
                    timestamp = parts[5].toLong()
                )
            } catch (e: Exception) {
                null
            }
        }

        private fun splitEscaped(raw: String): List<String> {
            val result = mutableListOf<String>()
            val current = StringBuilder()
            var i = 0
            while (i < raw.length) {
                val c = raw[i]
                if (c == '\\' && i + 1 < raw.length) {
                    current.append(c).append(raw[i + 1])
                    i += 2
                } else if (c == '|') {
                    result.add(current.toString())
                    current.clear()
                    i++
                } else {
                    current.append(c)
                    i++
                }
            }
            result.add(current.toString())
            return result
        }
    }
}
