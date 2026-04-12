package com.example.urbanfix.ui.issues

import android.content.Context

fun issueTileBodyAfterTitle(
    context: Context,
    category: String,
    location: String,
    description: String,
): String = buildString {
    append("Kategoria: $category")
    append("\n")
    append("Lokalizacja: $location")
    append("\n\n")
    append(description)
}
