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
import com.example.urbanfix.ui.issues.IssueStatusStyle
import com.google.android.material.card.MaterialCardView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    private fun backendBaseUrl(): String = requireContext().getString(R.string.backend_base_url)

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
        loadStats()
    }

    private fun loadStats() {
        val email = auth.currentUser?.email?.trim().orEmpty()
        if (email.isEmpty()) {
            Snackbar.make(binding.root, R.string.my_issues_load_error, Snackbar.LENGTH_LONG).show()
            return
        }
        Thread {
            val result = runCatching {
                val encoded = URLEncoder.encode(email, Charsets.UTF_8.name())
                val connection = (URL("${backendBaseUrl()}/issues?community_viewer_email=$encoded").openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 10_000
                    readTimeout = 10_000
                }
                try {
                    if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                        error("HTTP ${connection.responseCode}")
                    }
                    val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                    JSONArray(body)
                } finally {
                    connection.disconnect()
                }
            }
            activity?.runOnUiThread {
                if (!isAdded) return@runOnUiThread
                result.onSuccess { renderStats(it) }
                    .onFailure { Snackbar.make(binding.root, "Nie udalo sie pobrac statystyk", Snackbar.LENGTH_LONG).show() }
            }
        }.start()
    }

    private fun renderStats(issues: JSONArray) {
        binding.containerInvestments.removeAllViews()
        binding.containerSpending.removeAllViews()
        val categoryCounts = linkedMapOf<String, Int>()
        val statusCounts = linkedMapOf<String, Int>()
        var withCoordinates = 0
        for (i in 0 until issues.length()) {
            val item = issues.optJSONObject(i) ?: continue
            val category = item.optString("category").ifBlank { "Nieznana kategoria" }
            categoryCounts[category] = (categoryCounts[category] ?: 0) + 1
            val status = item.optString("status").ifBlank { "Brak statusu" }
            statusCounts[status] = (statusCounts[status] ?: 0) + 1
            val lat = item.optDouble("location_lat", Double.NaN)
            val lng = item.optDouble("location_lng", Double.NaN)
            if (lat.isFinite() && lng.isFinite()) withCoordinates++
        }

        binding.containerInvestments.addView(createInfoCard("Liczba zgłoszeń: ${issues.length()}"))
        categoryCounts.toList()
            .sortedByDescending { it.second }
            .forEach { (category, count) ->
                binding.containerInvestments.addView(createInfoCard("$category: $count"))
            }

        statusCounts.toList()
            .sortedByDescending { it.second }
            .forEach { (status, count) ->
                binding.containerSpending.addView(createStatusStatCard(status, count))
            }
    }

    private fun createStatusStatCard(status: String, count: Int): View {
        val card = createInfoCard("$status: $count")
        val label = ((card as ViewGroup).getChildAt(0) as? LinearLayout)?.getChildAt(0) as? TextView
        label?.let { IssueStatusStyle.applyStatusText(it, status) }
        return card
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