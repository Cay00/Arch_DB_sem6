package com.example.urbanfix.ui.home

import android.content.Context
import android.graphics.Typeface
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.widget.TextViewCompat
import com.example.urbanfix.R
import com.example.urbanfix.ui.UiSpacing
import com.example.urbanfix.ui.issues.IssueCategoryStyle
import com.google.android.material.card.MaterialCardView

/** Przykładowe plany inwestycyjne miasta (dane statyczne, orientacyjne). */
object HomeInvestmentPlans {

    data class Plan(
        val titleRes: Int,
        val descriptionRes: Int,
        val categoryRes: Int,
        val statusRes: Int,
        val budgetRes: Int,
        val periodRes: Int,
        val locationRes: Int,
        val statusKind: StatusKind,
    )

    enum class StatusKind { PLANNED, IN_PROGRESS, COMPLETED }

    private val samplePlans: List<Plan> = listOf(
        Plan(
            R.string.home_plan_legnicka_title,
            R.string.home_plan_legnicka_desc,
            R.string.home_plan_legnicka_category,
            R.string.home_plan_status_in_progress,
            R.string.home_plan_legnicka_budget,
            R.string.home_plan_legnicka_period,
            R.string.home_plan_legnicka_location,
            StatusKind.IN_PROGRESS,
        ),
        Plan(
            R.string.home_plan_tram_title,
            R.string.home_plan_tram_desc,
            R.string.home_plan_tram_category,
            R.string.home_plan_status_planned,
            R.string.home_plan_tram_budget,
            R.string.home_plan_tram_period,
            R.string.home_plan_tram_location,
            StatusKind.PLANNED,
        ),
        Plan(
            R.string.home_plan_odra_bridges_title,
            R.string.home_plan_odra_bridges_desc,
            R.string.home_plan_odra_bridges_category,
            R.string.home_plan_status_planned,
            R.string.home_plan_odra_bridges_budget,
            R.string.home_plan_odra_bridges_period,
            R.string.home_plan_odra_bridges_location,
            StatusKind.PLANNED,
        ),
        Plan(
            R.string.home_plan_szczytnicki_title,
            R.string.home_plan_szczytnicki_desc,
            R.string.home_plan_szczytnicki_category,
            R.string.home_plan_status_in_progress,
            R.string.home_plan_szczytnicki_budget,
            R.string.home_plan_szczytnicki_period,
            R.string.home_plan_szczytnicki_location,
            StatusKind.IN_PROGRESS,
        ),
        Plan(
            R.string.home_plan_led_title,
            R.string.home_plan_led_desc,
            R.string.home_plan_led_category,
            R.string.home_plan_status_completed,
            R.string.home_plan_led_budget,
            R.string.home_plan_led_period,
            R.string.home_plan_led_location,
            StatusKind.COMPLETED,
        ),
    )

    fun renderSection(context: Context, container: LinearLayout) {
        container.removeAllViews()
        container.addView(createSectionCard(context))
    }

    private fun createSectionCard(context: Context): View {
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
        }
        UiSpacing.applyCardContentPadding(card, context)
        val col = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        col.addView(
            TextView(context).apply {
                text = context.getString(R.string.home_investment_plans_title)
                TextViewCompat.setTextAppearance(this, R.style.TextAppearance_Urbanfix_Title)
            },
        )
        col.addView(
            TextView(context).apply {
                text = context.getString(R.string.home_investment_plans_subtitle)
                TextViewCompat.setTextAppearance(this, R.style.TextAppearance_Urbanfix_BodySecondary)
                setPadding(0, UiSpacing.elementGapPx(context), 0, 0)
            },
        )
        samplePlans.forEachIndexed { index, plan ->
            if (index > 0) {
                col.addView(UiSpacing.verticalSpacer(context, UiSpacing.blockGapPx(context)))
            }
            col.addView(createPlanCard(context, plan))
        }
        card.addView(col)
        return card
    }

    private fun createPlanCard(context: Context, plan: Plan): View {
        val category = context.getString(plan.categoryRes)
        val status = context.getString(plan.statusRes)
        val blockGap = UiSpacing.elementGapPx(context)
        val labelGap = UiSpacing.spacingLabelToValuePx(context)

        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_chart_bar_track)
            val pad = UiSpacing.blockGapPx(context)
            setPadding(pad, pad, pad, pad)

            addView(
                TextView(context).apply {
                    text = context.getString(plan.titleRes)
                    TextViewCompat.setTextAppearance(this, R.style.TextAppearance_Urbanfix_Subtitle)
                    setTypeface(null, Typeface.BOLD)
                },
            )

            val tags = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, blockGap, 0, 0)
            }
            tags.addView(categoryChip(context, category))
            tags.addView(
                statusChip(context, status, plan.statusKind),
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ).apply { marginStart = UiSpacing.chipGapPx(context) },
            )
            addView(tags)

            addView(
                metaLine(context, context.getString(R.string.home_plan_label_location), context.getString(plan.locationRes)),
                LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = blockGap },
            )

            addView(
                TextView(context).apply {
                    text = context.getString(plan.descriptionRes)
                    TextViewCompat.setTextAppearance(this, R.style.TextAppearance_Urbanfix_Body)
                },
                LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = blockGap },
            )

            addView(
                metaLine(context, context.getString(R.string.home_plan_label_budget), context.getString(plan.budgetRes)),
                LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = blockGap },
            )
            addView(
                metaLine(context, context.getString(R.string.home_plan_label_period), context.getString(plan.periodRes)),
                LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = labelGap },
            )
        }
    }

    private fun categoryChip(context: Context, category: String): TextView =
        TextView(context).apply {
            text = category
            TextViewCompat.setTextAppearance(this, R.style.TextAppearance_Urbanfix_Label)
            setTextColor(IssueCategoryStyle.accentColor(context, category))
            setTypeface(null, Typeface.BOLD)
            setBackgroundResource(R.drawable.bg_issue_tag_category)
            UiSpacing.applyChipPadding(this, context)
        }

    private fun statusChip(context: Context, status: String, kind: StatusKind): TextView =
        TextView(context).apply {
            text = status
            TextViewCompat.setTextAppearance(this, R.style.TextAppearance_Urbanfix_Label)
            setTypeface(null, Typeface.BOLD)
            val colorRes = when (kind) {
                StatusKind.PLANNED -> R.color.investment_plan_status_planned
                StatusKind.IN_PROGRESS -> R.color.investment_plan_status_in_progress
                StatusKind.COMPLETED -> R.color.investment_plan_status_completed
            }
            val bgRes = when (kind) {
                StatusKind.PLANNED -> R.drawable.bg_investment_plan_planned
                StatusKind.IN_PROGRESS -> R.drawable.bg_investment_plan_in_progress
                StatusKind.COMPLETED -> R.drawable.bg_investment_plan_completed
            }
            setTextColor(ContextCompat.getColor(context, colorRes))
            setBackgroundResource(bgRes)
            UiSpacing.applyChipPadding(this, context)
        }

    private fun metaLine(context: Context, label: String, value: String): LinearLayout =
        LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(
                TextView(context).apply {
                    text = label
                    TextViewCompat.setTextAppearance(this, R.style.TextAppearance_Urbanfix_Label)
                },
            )
            addView(
                TextView(context).apply {
                    text = value
                    TextViewCompat.setTextAppearance(this, R.style.TextAppearance_Urbanfix_BodySecondary)
                    setPadding(0, UiSpacing.spacingLabelToValuePx(context), 0, 0)
                },
            )
        }

    private val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
    private val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT
}
