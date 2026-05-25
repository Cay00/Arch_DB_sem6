package com.example.urbanfix.ui

import android.content.Context
import android.view.View
import android.widget.TextView
import com.example.urbanfix.R
import com.google.android.material.card.MaterialCardView

/** Wspólne odstępy — karty i chipy tworzone w kodzie. */
object UiSpacing {

    fun cardContentPaddingHorizontalPx(context: Context): Int =
        context.resources.getDimensionPixelSize(R.dimen.card_content_padding_horizontal)

    fun cardContentPaddingVerticalPx(context: Context): Int =
        context.resources.getDimensionPixelSize(R.dimen.card_content_padding_vertical)

    fun cardMarginBottomPx(context: Context): Int =
        context.resources.getDimensionPixelSize(R.dimen.card_margin_bottom)

    fun applyCardContentPadding(card: MaterialCardView, context: Context) {
        val h = cardContentPaddingHorizontalPx(context)
        val v = cardContentPaddingVerticalPx(context)
        card.setContentPadding(h, v, h, v)
    }

    fun applyChipPadding(textView: TextView, context: Context) {
        val h = context.resources.getDimensionPixelSize(R.dimen.chip_padding_horizontal)
        val v = context.resources.getDimensionPixelSize(R.dimen.chip_padding_vertical)
        textView.setPadding(h, v, h, v)
    }

    fun elementGapPx(context: Context): Int =
        context.resources.getDimensionPixelSize(R.dimen.spacing_element)

    fun blockGapPx(context: Context): Int =
        context.resources.getDimensionPixelSize(R.dimen.spacing_block)

    fun chipGapPx(context: Context): Int =
        context.resources.getDimensionPixelSize(R.dimen.chip_gap)

    fun spacingLabelToValuePx(context: Context): Int =
        context.resources.getDimensionPixelSize(R.dimen.spacing_label_to_value)

    fun verticalSpacer(context: Context, heightPx: Int): View =
        View(context).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                heightPx,
            )
        }
}
