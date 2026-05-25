package com.example.urbanfix.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.core.widget.TextViewCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.urbanfix.R
import com.example.urbanfix.databinding.FragmentHomeBinding
import com.example.urbanfix.ui.issues.IssueCategoryStyle
import com.example.urbanfix.ui.issues.IssueStatusStyle
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
    private var activeStatusFilter: String = ""

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
        setupStatusFilter()
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

    private fun setupStatusFilter() {
        val allLabel = getString(R.string.home_status_filter_all)
        val statuses = resources.getStringArray(R.array.issue_status_values).toList()
        val options = listOf(allLabel) + statuses
        activeStatusFilter = allLabel
        val adapter = NoFilterAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line)
        adapter.updateData(options)
        binding.dropdownHomeStatusFilter.setAdapter(adapter)
        binding.dropdownHomeStatusFilter.setText(allLabel, false)
        binding.dropdownHomeStatusFilter.setOnItemClickListener { _, _, position, _ ->
            activeStatusFilter = options[position]
            renderFilteredIssues()
        }
    }

    private fun loadIssues() {
        val email = auth.currentUser?.email?.trim().orEmpty()
        if (email.isEmpty()) {
            binding.textHomeIssuesEmpty.visibility = View.VISIBLE
            binding.textHomeIssuesEmpty.text = getString(R.string.my_issues_load_error)
            return
        }
        setLoading(true)
        val baseUrl = backendBaseUrl()
        Thread {
            runCatching {
                val enc = URLEncoder.encode(email, Charsets.UTF_8.name())
                val userUrl = "$baseUrl/users/by-email?email=$enc"
                val userConn = (URL(userUrl).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 10_000
                    readTimeout = 10_000
                }
                val accountType = try {
                    if (userConn.responseCode != HttpURLConnection.HTTP_OK) {
                        error("HTTP ${userConn.responseCode}")
                    }
                    val body = userConn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                    JSONObject(body).optString("account_type", "").trim().lowercase()
                } finally {
                    userConn.disconnect()
                }
                val url = if (accountType == "official") {
                    "$baseUrl/issues?official_email=$enc"
                } else {
                    "$baseUrl/issues?community_viewer_email=$enc"
                }
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
                    renderStats()
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

    private fun renderStats() {
        val ctx = requireContext()
        binding.containerHomeStats.removeAllViews()
        if (allIssues.isEmpty()) {
            binding.textHomeIssuesSection.visibility = View.GONE
            return
        }
        val categoryCounts = linkedMapOf<String, Int>()
        val statusCounts = linkedMapOf<String, Int>()
        allIssues.forEach { issue ->
            val category = issue.optString("category").ifBlank { "Inne" }
            categoryCounts[category] = (categoryCounts[category] ?: 0) + 1
            val status = issue.optString("status").ifBlank { "Brak statusu" }
            statusCounts[status] = (statusCounts[status] ?: 0) + 1
        }
        val total = allIssues.size
        val subtitle = getString(R.string.home_stats_total, total)
        val emptyMsg = getString(R.string.home_stats_empty)
        val categoryEntries = categoryCounts.entries
            .sortedByDescending { it.value }
            .map { (label, count) ->
                HomeBarChart.Entry(
                    label = label,
                    count = count,
                    color = IssueCategoryStyle.accentColor(ctx, label),
                )
            }
        val statusOrder = resources.getStringArray(R.array.issue_status_values).toList()
        val statusEntries = statusCounts.entries
            .sortedWith(
                compareBy<Map.Entry<String, Int>> { (status, _) ->
                    val idx = statusOrder.indexOfFirst { it.equals(status, ignoreCase = true) }
                    if (idx < 0) Int.MAX_VALUE else idx
                }.thenByDescending { it.value },
            )
            .map { (label, count) ->
                HomeBarChart.Entry(
                    label = label,
                    count = count,
                    color = IssueStatusStyle.accentColor(ctx, label),
                )
            }
        binding.containerHomeStats.addView(
            HomeBarChart.createChartCard(
                ctx,
                getString(R.string.home_stats_categories_title),
                subtitle,
                categoryEntries,
                emptyMsg,
            ),
        )
        binding.containerHomeStats.addView(
            HomeBarChart.createChartCard(
                ctx,
                getString(R.string.home_stats_status_title),
                subtitle,
                statusEntries,
                emptyMsg,
            ),
        )
        binding.textHomeIssuesSection.visibility = View.VISIBLE
    }

    private fun renderFilteredIssues() {
        val filtered = allIssues.filter { issue ->
            val category = issue.optString("category").lowercase()
            val categoryMatches = when (activeFilter) {
                IssueFilter.ALL -> true
                IssueFilter.ROADS -> category.contains("drog")
                IssueFilter.GREENERY -> category.contains("ziel")
                IssueFilter.VANDALISM -> category.contains("wandal")
            }
            val statusMatches = issueMatchesStatusFilter(issue)
            categoryMatches && statusMatches
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

    private fun issueMatchesStatusFilter(issue: JSONObject): Boolean {
        val selected = activeStatusFilter
        val allLabel = getString(R.string.home_status_filter_all)
        if (selected.isBlank() || selected == allLabel) return true
        val status = issue.optString("status").trim()
        return status.equals(selected, ignoreCase = true)
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
                val status = issue.optString("status").ifBlank { "—" }
                text = status
                TextViewCompat.setTextAppearance(this, R.style.TextAppearance_Urbanfix_Label)
                setPadding(12, 6, 12, 6)
                IssueStatusStyle.applyStatusChip(this, status)
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
                    R.id.action_navigation_dashboard_to_navigation_issue_detail,
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
        findNavController().navigate(R.id.action_navigation_dashboard_to_road_damage_report, bundle)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private class NoFilterAdapter(context: android.content.Context, layout: Int) :
        ArrayAdapter<String>(context, layout) {
        private val items = mutableListOf<String>()

        fun updateData(newData: List<String>) {
            items.clear()
            items.addAll(newData)
            clear()
            addAll(newData)
            notifyDataSetChanged()
        }

        override fun getFilter(): Filter {
            return object : Filter() {
                override fun performFiltering(constraint: CharSequence?): FilterResults {
                    val results = FilterResults()
                    results.values = items
                    results.count = items.size
                    return results
                }

                override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                    notifyDataSetChanged()
                }
            }
        }
    }
}
