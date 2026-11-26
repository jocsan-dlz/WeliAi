package com.npcg.practica1.data.util

import java.text.Normalizer
import java.util.Locale

internal fun normalizedLabel(value: String): String {
    val collapsed = value.trim().replace(Regex("\\s+"), " ")
    return Normalizer.normalize(collapsed, Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .lowercase(Locale.ROOT)
}

internal fun displayLabel(value: String): String = value.trim().replace(Regex("\\s+"), " ")
