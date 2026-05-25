package com.example.urbanfix.ui.issues

import android.content.Context
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import com.example.urbanfix.R

object IssueStatusStyle {

    private enum class Kind(
        @DrawableRes val chipBackgroundRes: Int,
        val textColorRes: Int,
        val accentColorRes: Int,
    ) {
        ZGLOSZONE(
            R.drawable.bg_issue_status_zgloszone,
            R.color.issue_status_zgloszone_text,
            R.color.issue_status_zgloszone,
        ),
        ROZPATRYWANE(
            R.drawable.bg_issue_status_rozpatrywane,
            R.color.issue_status_rozpatrywane_text,
            R.color.issue_status_rozpatrywane,
        ),
        ZAAKCEPTOWANE(
            R.drawable.bg_issue_status_zaakceptowane,
            R.color.issue_status_zaakceptowane_text,
            R.color.issue_status_zaakceptowane,
        ),
        ODRZUCONE(
            R.drawable.bg_issue_status_odrzucone,
            R.color.issue_status_odrzucone_text,
            R.color.issue_status_odrzucone,
        ),
        UNKNOWN(
            R.drawable.bg_issue_tag_status,
            R.color.issue_status_unknown_text,
            R.color.issue_status_timeline_inactive,
        ),
    }

    private fun kindFor(status: String): Kind {
        val key = status.trim().lowercase()
        return when {
            key == "zgłoszone" || key == "zgloszone" || key == "new" -> Kind.ZGLOSZONE
            key == "rozpatrywane" -> Kind.ROZPATRYWANE
            key == "zaakceptowane" -> Kind.ZAAKCEPTOWANE
            key == "odrzucone" -> Kind.ODRZUCONE
            else -> Kind.UNKNOWN
        }
    }

    fun applyStatusChip(textView: TextView, status: String) {
        val kind = kindFor(status)
        val ctx = textView.context
        textView.setBackgroundResource(kind.chipBackgroundRes)
        textView.setTextColor(ContextCompat.getColor(ctx, kind.textColorRes))
    }

    fun applyStatusText(textView: TextView, status: String) {
        textView.setTextColor(accentColor(textView.context, status))
    }

    @ColorInt
    fun accentColor(context: Context, status: String): Int =
        ContextCompat.getColor(context, kindFor(status).accentColorRes)

    @ColorInt
    fun timelineColorForStep(context: Context, stepIndex: Int, reached: Boolean): Int {
        if (!reached) {
            return ContextCompat.getColor(context, R.color.issue_status_timeline_inactive)
        }
        val kind = when (stepIndex) {
            0 -> Kind.ZGLOSZONE
            1 -> Kind.ROZPATRYWANE
            2 -> Kind.ZAAKCEPTOWANE
            3 -> Kind.ODRZUCONE
            else -> Kind.UNKNOWN
        }
        return ContextCompat.getColor(context, kind.accentColorRes)
    }
}
