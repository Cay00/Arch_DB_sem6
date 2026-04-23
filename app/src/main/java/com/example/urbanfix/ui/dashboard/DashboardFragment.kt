package com.example.urbanfix.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.widget.TextViewCompat
import androidx.fragment.app.Fragment
import com.example.urbanfix.R
import com.example.urbanfix.databinding.FragmentDashboardBinding
import com.google.android.material.card.MaterialCardView

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val investments = listOf(
            "Modernizacja ul. Legnickiej - etap II",
            "Nowa linia tramwajowa na Jagodno",
            "Rewitalizacja skweru przy ul. Świdnickiej",
        )
        val spending = listOf(
            "Budżet dróg miejskich 2026: 128 mln zł",
            "Utrzymanie zieleni miejskiej: 42 mln zł",
            "Oświetlenie i energia: 26 mln zł",
        )
        investments.forEach { binding.containerInvestments.addView(createInfoCard(it)) }
        spending.forEach { binding.containerSpending.addView(createInfoCard(it)) }
    }

    private fun createInfoCard(text: String): View {
        val context = requireContext()
        val card = MaterialCardView(context).apply {
            radius = resources.getDimension(R.dimen.home_issue_tile_corner_radius)
            cardElevation = resources.getDimension(R.dimen.home_issue_tile_elevation)
            strokeWidth = resources.getDimensionPixelSize(R.dimen.home_issue_tile_stroke_width)
            strokeColor = context.getColor(R.color.home_tile_stroke)
            layoutParams = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply { bottomMargin = resources.getDimensionPixelSize(R.dimen.issue_list_card_margin_bottom) }
            setContentPadding(18, 18, 18, 18)
        }
        val label = TextView(context).apply {
            this.text = text
            TextViewCompat.setTextAppearance(this, R.style.TextAppearance_Urbanfix_Body)
        }
        card.addView(
            LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                addView(label)
            },
        )
        return card
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}