package com.example.urbanfix.ui.issues

import android.content.Context
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import com.example.urbanfix.R

object IssueCategoryStyle {

    @ColorInt
    fun accentColor(context: Context, category: String): Int {
        val key = category.trim().lowercase()
        val colorRes = when {
            key.contains("drog") -> R.color.issue_category_drogi
            key.contains("ziel") -> R.color.issue_category_zielen
            key.contains("wandal") -> R.color.issue_category_wandalizm
            key.contains("oświet") || key.contains("oswiet") -> R.color.issue_category_oswietlenie
            key.contains("inwest") -> R.color.issue_category_inwestycje
            key.contains("porząd") || key.contains("porzad") -> R.color.issue_category_porzadek
            else -> R.color.issue_category_unknown
        }
        return ContextCompat.getColor(context, colorRes)
    }
}
