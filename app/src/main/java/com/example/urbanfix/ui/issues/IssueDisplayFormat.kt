package com.example.urbanfix.ui.issues

import android.content.Context
import com.example.urbanfix.R

fun issueTileBodyAfterTitle(
    context: Context,
    category: String,
    location: String,
    description: String,
    voteCount: Int,
): String = buildString {
    append("Kategoria: $category")
    append("\n")
    append("Lokalizacja: $location")
    append("\n")
    append(communityVotesSummary(context, voteCount))
    append("\n\n")
    append(description)
}

fun communityVotesSummary(context: Context, voteCount: Int): String =
    context.resources.getQuantityString(
        R.plurals.issue_community_votes_suffix,
        voteCount,
        voteCount,
    )
