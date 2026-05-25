package com.example.urbanfix.ui.home

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.widget.TextViewCompat
import com.example.urbanfix.R
import com.google.android.material.card.MaterialCardView

/**
 * Poziomy wykres słupkowy: etykieta | pasek | liczba.
 */
object HomeBarChart {

    data class Entry(
        val label: String,
        val count: Int,
        val color: Int,
    )

    fun createChartCard(
        context: Context,
        title: String,
        subtitle: String,
        entries: List<Entry>,
        emptyMessage: String,
    ): View {
        val card = MaterialCardView(context).apply {
            radius = context.resources.getDimension(R.dimen.home_issue_tile_corner_radius)
            cardElevation = context.resources.getDimension(R.dimen.home_issue_tile_elevation)
            strokeWidth = context.resources.getDimensionPixelSize(R.dimen.home_issue_tile_stroke_width)
            strokeColor = context.getColor(R.color.home_tile_stroke)
            layoutParams = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                bottomMargin = context.resources.getDimensionPixelSize(R.dimen.home_stats_card_margin_bottom)
            }
            setContentPadding(18, 18, 18, 18)
        }
        val col = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        col.addView(
            TextView(context).apply {
                text = title
                TextViewCompat.setTextAppearance(this, R.style.TextAppearance_Urbanfix_Title)
            },
        )
        col.addView(
            TextView(context).apply {
                text = subtitle
                TextViewCompat.setTextAppearance(this, R.style.TextAppearance_Urbanfix_BodySecondary)
                setPadding(0, 4, 0, 0)
            },
        )
        if (entries.isEmpty()) {
            col.addView(
                TextView(context).apply {
                    text = emptyMessage
                    TextViewCompat.setTextAppearance(this, R.style.TextAppearance_Urbanfix_BodySecondary)
                    setPadding(0, 14, 0, 0)
                },
            )
        } else {
            val rowGap = context.resources.getDimensionPixelSize(R.dimen.home_chart_row_gap)
            entries.forEachIndexed { index, entry ->
                if (index > 0) {
                    col.addView(View(context).apply { layoutParams = LinearLayout.LayoutParams(MATCH, rowGap) })
                }
                col.addView(createBarRow(context, entry, entries.maxOf { it.count }.coerceAtLeast(1)))
            }
        }
        card.addView(col)
        return card
    }

    private fun createBarRow(context: Context, entry: Entry, maxCount: Int): View {
        val labelWidth = context.resources.getDimensionPixelSize(R.dimen.home_chart_label_width)
        val barHeight = context.resources.getDimensionPixelSize(R.dimen.home_chart_bar_height)
        val density = context.resources.displayMetrics.density
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        row.addView(
            TextView(context).apply {
                text = entry.label
                TextViewCompat.setTextAppearance(this, R.style.TextAppearance_Urbanfix_Label)
                setTextColor(entry.color)
                setTypeface(null, Typeface.BOLD)
                maxLines = 2
                layoutParams = LinearLayout.LayoutParams(labelWidth, ViewGroup.LayoutParams.WRAP_CONTENT)
            },
        )
        val fillWeight = if (entry.count <= 0) {
            0.01f
        } else {
            (entry.count.toFloat() / maxCount.toFloat()).coerceIn(0.08f, 1f)
        }
        val track = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            background = context.getDrawable(R.drawable.bg_chart_bar_track)
            layoutParams = LinearLayout.LayoutParams(0, barHeight, 1f).apply {
                marginStart = (8 * density).toInt()
                marginEnd = (8 * density).toInt()
            }
            setPadding((4 * density).toInt(), (4 * density).toInt(), (4 * density).toInt(), (4 * density).toInt())
        }
        if (entry.count > 0) {
            track.addView(
                View(context).apply {
                    background = GradientDrawable().apply {
                        cornerRadius = 6f * density
                        setColor(entry.color)
                    }
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, fillWeight)
                },
            )
        }
        track.addView(
            View(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    (1f - fillWeight).coerceAtLeast(0.01f),
                )
            },
        )
        row.addView(track)
        row.addView(
            TextView(context).apply {
                text = entry.count.toString()
                TextViewCompat.setTextAppearance(this, R.style.TextAppearance_Urbanfix_Label)
                setTypeface(null, Typeface.BOLD)
                gravity = Gravity.END
                minWidth = (40 * density).toInt()
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                )
            },
        )
        return row
    }

    private val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
}
