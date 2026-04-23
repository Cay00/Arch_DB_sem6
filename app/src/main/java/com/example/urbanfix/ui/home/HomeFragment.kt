package com.example.urbanfix.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.core.widget.TextViewCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.urbanfix.R
import com.example.urbanfix.databinding.FragmentHomeBinding
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.Duration
import java.time.OffsetDateTime

class HomeFragment : Fragment() {

    private enum class IssueFilter { ALL, ROADS, GREENERY, VANDALISM }

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private var allIssues: List<JSONObject> = emptyList()
    private var activeFilter: IssueFilter = IssueFilter.ALL

    private fun backendBaseUrl(): String = requireContext().getString(R.string.backend_base_url)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.fabCreateIssue.setOnClickListener { navigateToReport() }
        binding.toggleIssueFilters.check(R.id.button_filter_all)
        binding.toggleIssueFilters.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            activeFilter = when (checkedId) {
                R.id.button_filter_roads -> IssueFilter.ROADS
                R.id.button_filter_greenery -> IssueFilter.GREENERY
                R.id.button_filter_investments -> IssueFilter.VANDALISM
                else -> IssueFilter.ALL
            }
            renderFilteredIssues()
        }
        loadIssues()
    }

    private fun loadIssues() {
        val email = auth.currentUser?.email?.trim().orEmpty()
        if (email.isEmpty()) {
            binding.textHomeIssuesEmpty.visibility = View.VISIBLE
            binding.textHomeIssuesEmpty.text = getString(R.string.my_issues_load_error)
            return
        }
        setLoading(true)
        Thread {
            runCatching {
                val enc = URLEncoder.encode(email, Charsets.UTF_8.name())
                val url = "${backendBaseUrl()}/issues?community_viewer_email=$enc"
                val c = (URL(url).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 10_000
                    readTimeout = 10_000
                }
                try {
                    if (c.responseCode != HttpURLConnection.HTTP_OK) error("HTTP ${c.responseCode}")
                    val body = c.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                    JSONArray(body)
                } finally {
                    c.disconnect()
                }
            }.onSuccess { arr ->
                activity?.runOnUiThread {
                    if (!isAdded) return@runOnUiThread
                    val list = mutableListOf<JSONObject>()
                    for (i in 0 until arr.length()) list += arr.getJSONObject(i)
                    allIssues = list
                    setLoading(false)
                    renderFilteredIssues()
                }
            }.onFailure {
                activity?.runOnUiThread {
                    if (!isAdded) return@runOnUiThread
                    setLoading(false)
                    binding.textHomeIssuesEmpty.visibility = View.VISIBLE
                    binding.textHomeIssuesEmpty.text = getString(R.string.my_issues_load_error)
                }
            }
        }.start()
    }

    private fun renderFilteredIssues() {
        val filtered = allIssues.filter { issue ->
            val category = issue.optString("category").lowercase()
            when (activeFilter) {
                IssueFilter.ALL -> true
                IssueFilter.ROADS -> category.contains("drog")
                IssueFilter.GREENERY -> category.contains("ziel")
                IssueFilter.VANDALISM -> category.contains("wandal")
            }
        }
        binding.containerHomeIssues.removeAllViews()
        if (filtered.isEmpty()) {
            binding.textHomeIssuesEmpty.visibility = View.VISIBLE
            binding.textHomeIssuesEmpty.text = getString(R.string.home_issues_empty)
            return
        }
        binding.textHomeIssuesEmpty.visibility = View.GONE
        filtered.forEach { binding.containerHomeIssues.addView(createIssueCard(it)) }
    }

    private fun createIssueCard(issue: JSONObject): View {
        val context = requireContext()
        val res = resources
        val card = MaterialCardView(context).apply {
            radius = res.getDimension(R.dimen.home_issue_tile_corner_radius)
            cardElevation = res.getDimension(R.dimen.home_issue_tile_elevation)
            strokeWidth = res.getDimensionPixelSize(R.dimen.home_issue_tile_stroke_width)
            strokeColor = context.getColor(R.color.home_tile_stroke)
            layoutParams = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply { bottomMargin = res.getDimensionPixelSize(R.dimen.issue_list_card_margin_bottom) }
            setContentPadding(18, 18, 18, 18)
        }
        val col = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        val titleRow = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
        titleRow.addView(
            TextView(context).apply {
                text = issue.optString("title").trim().ifEmpty { "—" }
                TextViewCompat.setTextAppearance(this, R.style.TextAppearance_Urbanfix_Subtitle)
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            },
        )
        titleRow.addView(
            TextView(context).apply {
                text = issue.optInt("vote_count", 0).toString()
                TextViewCompat.setTextAppearance(this, R.style.TextAppearance_Urbanfix_Subtitle)
            },
        )
        col.addView(titleRow)
        col.addView(
            TextView(context).apply {
                val location = issue.optString("location").trim()
                val city = location.substringAfterLast(",").trim().ifEmpty { "Wrocław" }
                val whenReported = relativeTime(issue.optString("created_at"))
                text = "$city • $whenReported"
                TextViewCompat.setTextAppearance(this, R.style.TextAppearance_Urbanfix_BodySecondary)
                setPadding(0, 8, 0, 0)
            },
        )
        val tags = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 10, 0, 0)
        }
        tags.addView(
            TextView(context).apply {
                text = issue.optString("category").ifBlank { "—" }
                TextViewCompat.setTextAppearance(this, R.style.TextAppearance_Urbanfix_Label)
                setBackgroundResource(R.drawable.bg_issue_tag_category)
                setPadding(12, 6, 12, 6)
            },
        )
        tags.addView(
            TextView(context).apply {
                text = issue.optString("status").ifBlank { "—" }
                TextViewCompat.setTextAppearance(this, R.style.TextAppearance_Urbanfix_Label)
                setBackgroundResource(R.drawable.bg_issue_tag_status)
                setPadding(12, 6, 12, 6)
            },
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply { marginStart = 8 },
        )
        col.addView(tags)
        card.addView(col)
        val issueId = issue.optInt("id", -1)
        if (issueId >= 0) {
            card.isClickable = true
            card.isFocusable = true
            card.setOnClickListener {
                findNavController().navigate(
                    R.id.action_navigation_home_to_navigation_issue_detail,
                    bundleOf("issueId" to issueId),
                )
            }
        }
        return card
    }

    private fun relativeTime(rawCreatedAt: String): String {
        return runCatching {
            val created = OffsetDateTime.parse(rawCreatedAt).toInstant()
            val now = java.time.Instant.now()
            val days = Duration.between(created, now).toDays()
            when {
                days <= 0L -> "dzisiaj"
                days == 1L -> "1 dzień temu"
                days < 7L -> "$days dni temu"
                days < 14L -> "tydzień temu"
                days < 30L -> "${days / 7} tygodnie temu"
                else -> "${days / 30} mies. temu"
            }
        }.getOrDefault("niedawno")
    }

    private fun setLoading(loading: Boolean) {
        binding.progressHomeIssues.visibility = if (loading) View.VISIBLE else View.GONE
        binding.scrollHomeIssues.visibility = if (loading) View.GONE else View.VISIBLE
    }

    private fun navigateToReport() {
        val bundle = bundleOf("category" to "Drogi")
        findNavController().navigate(R.id.action_navigation_home_to_road_damage_report, bundle)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
