package com.example.urbanfix.ui.dashboard

import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
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
import com.example.urbanfix.data.IssuesApi
import com.example.urbanfix.databinding.FragmentDashboardBinding
import com.example.urbanfix.ui.issues.issueTileBodyAfterTitle
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class DashboardFragment : Fragment() {

    private enum class IssuesTab { MINE, COMMUNITY }

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private fun backendBaseUrl(): String = requireContext().getString(R.string.backend_base_url)

    private var activeIssuesTab = IssuesTab.MINE
    private var dashboardViewerUserId: Int = -1

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.tabLayoutIssues.addOnTabSelectedListener(
            object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab?) {
                    when (tab?.position) {
                        0 -> loadIssues(IssuesTab.MINE)
                        1 -> loadIssues(IssuesTab.COMMUNITY)
                    }
                }

                override fun onTabUnselected(tab: TabLayout.Tab?) {}

                override fun onTabReselected(tab: TabLayout.Tab?) {}
            },
        )
        loadIssues(IssuesTab.MINE)
    }

    private fun loadIssues(tab: IssuesTab) {
        activeIssuesTab = tab
        val email = auth.currentUser?.email
        if (email.isNullOrBlank()) {
            binding.textIssuesEmpty.visibility = View.VISIBLE
            binding.textIssuesEmpty.text = getString(R.string.my_issues_load_error)
            return
        }

        setLoading(true)
        Thread {
            runCatching {
                val userJson = fetchCurrentUser(email)
                val issues = fetchIssuesList(userJson, email, tab)
                Triple(issues, tab, userJson.getInt("id"))
            }.onSuccess { (issues, loadedTab, viewerId) ->
                requireActivity().runOnUiThread {
                    dashboardViewerUserId = viewerId
                    setLoading(false)
                    renderIssues(issues, loadedTab)
                }
            }.onFailure {
                requireActivity().runOnUiThread {
                    setLoading(false)
                    binding.textIssuesEmpty.visibility = View.VISIBLE
                    binding.textIssuesEmpty.text = getString(R.string.my_issues_load_error)
                }
            }
        }.start()
    }

    private fun fetchCurrentUser(email: String): JSONObject {
        val encodedEmail = URLEncoder.encode(email, Charsets.UTF_8.name())
        val connection = (URL("${backendBaseUrl()}/users/by-email?email=$encodedEmail").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 5000
            readTimeout = 5000
        }
        return try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw IllegalStateException("User lookup failed")
            }
            val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            JSONObject(body)
        } finally {
            connection.disconnect()
        }
    }

    private fun fetchIssuesList(userJson: JSONObject, email: String, tab: IssuesTab): JSONArray {
        val urlString = when (tab) {
            IssuesTab.MINE -> "${backendBaseUrl()}/issues?user_id=${userJson.getInt("id")}"
            IssuesTab.COMMUNITY -> {
                val enc = URLEncoder.encode(email, Charsets.UTF_8.name())
                "${backendBaseUrl()}/issues?community_viewer_email=$enc"
            }
        }
        val connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 5000
            readTimeout = 5000
        }
        return try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw IllegalStateException("Issues fetch failed")
            }
            val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            JSONArray(body)
        } finally {
            connection.disconnect()
        }
    }

    private fun renderIssues(issues: JSONArray, tab: IssuesTab) {
        binding.issuesContainer.removeAllViews()
        if (issues.length() == 0) {
            binding.textIssuesEmpty.visibility = View.VISIBLE
            binding.textIssuesEmpty.text = when (tab) {
                IssuesTab.MINE -> getString(R.string.my_issues_empty)
                IssuesTab.COMMUNITY -> getString(R.string.dashboard_community_empty)
            }
            return
        }

        binding.textIssuesEmpty.visibility = View.GONE
        val viewerId = dashboardViewerUserId
        for (i in 0 until issues.length()) {
            val issue = issues.getJSONObject(i)
            binding.issuesContainer.addView(createIssueCard(issue, viewerId))
        }
    }

    private fun createIssueCard(issue: JSONObject, viewerUserId: Int): View {
        val context = requireContext()
        val res = resources
        val padH = res.getDimensionPixelSize(R.dimen.issue_list_card_content_padding_horizontal)
        val padT = res.getDimensionPixelSize(R.dimen.issue_list_card_content_padding_top)
        val padB = res.getDimensionPixelSize(R.dimen.issue_list_card_content_padding_bottom)
        val gapStatusTitle = res.getDimensionPixelSize(R.dimen.issue_list_card_gap_status_to_title)
        val gapTitleBody = res.getDimensionPixelSize(R.dimen.issue_list_card_gap_title_to_body)
        val cornerRadius = res.getDimension(R.dimen.home_issue_tile_corner_radius)
        val elevation = res.getDimension(R.dimen.home_issue_tile_elevation)
        val strokePx = res.getDimensionPixelSize(R.dimen.home_issue_tile_stroke_width)
        val cardSpacingBottom = res.getDimensionPixelSize(R.dimen.issue_list_card_margin_bottom)

        val card = MaterialCardView(context).apply {
            radius = cornerRadius
            cardElevation = elevation
            strokeWidth = strokePx
            strokeColor = context.getColor(R.color.home_tile_stroke)
            useCompatPadding = true
            setContentPadding(padH, padT, padH, padB)
            layoutParams = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                bottomMargin = cardSpacingBottom
            }
        }

        val inner = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        }

        val lpMatch = ViewGroup.LayoutParams.MATCH_PARENT
        val lpWrap = ViewGroup.LayoutParams.WRAP_CONTENT

        val statusText = issue.optString("status").ifBlank { getString(R.string.profile_dash) }
        inner.addView(
            TextView(context).apply {
                text = getString(R.string.issue_card_status, statusText)
                TextViewCompat.setTextAppearance(this, R.style.TextAppearance_Urbanfix_Label)
                setTypeface(null, Typeface.BOLD)
            },
            LinearLayout.LayoutParams(lpMatch, lpWrap),
        )
        inner.addView(
            TextView(context).apply {
                text = issue.optString("title").trim().ifEmpty { getString(R.string.profile_dash) }
                TextViewCompat.setTextAppearance(this, R.style.TextAppearance_Urbanfix_Subtitle)
                setTypeface(null, Typeface.BOLD)
            },
            LinearLayout.LayoutParams(lpMatch, lpWrap).apply { topMargin = gapStatusTitle },
        )
        inner.addView(
            TextView(context).apply {
                text = issueTileBodyAfterTitle(
                    context,
                    issue.optString("category"),
                    issue.optString("location"),
                    issue.optString("description"),
                )
                TextViewCompat.setTextAppearance(this, R.style.TextAppearance_Urbanfix_BodySecondary)
            },
            LinearLayout.LayoutParams(lpMatch, lpWrap).apply { topMargin = gapTitleBody },
        )
        if (viewerUserId >= 0) {
            val density = res.displayMetrics.density
            val voteTop = (8 * density).toInt()
            val btnW = (56 * density).toInt()
            val hasVoted = issue.has("viewer_vote") && !issue.isNull("viewer_vote")
            val issueId = issue.optInt("id", -1)
            val netTv = TextView(context).apply {
                text = issue.optInt("vote_count", 0).toString()
                TextViewCompat.setTextAppearance(this, R.style.TextAppearance_Urbanfix_Subtitle)
                gravity = Gravity.CENTER
            }
            lateinit var minusBtn: MaterialButton
            lateinit var plusBtn: MaterialButton
            minusBtn = MaterialButton(context, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
                text = "−"
                minHeight = (48 * density).toInt()
                isEnabled = !hasVoted
                setOnClickListener {
                    postVoteFromListCard(issueId, viewerUserId, -1, netTv, minusBtn, plusBtn)
                }
            }
            plusBtn = MaterialButton(context, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
                text = "+"
                minHeight = (48 * density).toInt()
                isEnabled = !hasVoted
                setOnClickListener {
                    postVoteFromListCard(issueId, viewerUserId, 1, netTv, minusBtn, plusBtn)
                }
            }
            val row = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                addView(minusBtn, LinearLayout.LayoutParams(btnW, lpWrap))
                addView(netTv, LinearLayout.LayoutParams(0, lpWrap, 1f))
                addView(plusBtn, LinearLayout.LayoutParams(btnW, lpWrap))
            }
            inner.addView(row, LinearLayout.LayoutParams(lpMatch, lpWrap).apply { topMargin = voteTop })
        }
        card.addView(inner)
        val issueIdNav = issue.optInt("id", -1)
        if (issueIdNav >= 0) {
            card.isClickable = true
            card.isFocusable = true
            card.setOnClickListener {
                findNavController().navigate(
                    R.id.action_navigation_dashboard_to_navigation_issue_detail,
                    bundleOf("issueId" to issueIdNav),
                )
            }
        }
        return card
    }

    private fun postVoteFromListCard(
        issueId: Int,
        userId: Int,
        delta: Int,
        netTv: TextView,
        minus: MaterialButton,
        plus: MaterialButton,
    ) {
        if (issueId < 0) return
        val old = netTv.text.toString().toIntOrNull() ?: 0
        minus.isEnabled = false
        plus.isEnabled = false
        netTv.text = (old + delta).toString()
        val base = backendBaseUrl()
        Thread {
            try {
                IssuesApi.postVote(base, issueId, userId, delta)
                requireActivity().runOnUiThread { loadIssues(activeIssuesTab) }
            } catch (_: Exception) {
                requireActivity().runOnUiThread {
                    netTv.text = old.toString()
                    minus.isEnabled = true
                    plus.isEnabled = true
                    Snackbar.make(binding.root, R.string.issue_vote_error, Snackbar.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun setLoading(loading: Boolean) {
        binding.progressIssues.visibility = if (loading) View.VISIBLE else View.GONE
        binding.issuesScroll.visibility = if (loading) View.GONE else View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}