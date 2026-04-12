package com.example.urbanfix.ui.dashboard

import android.graphics.Typeface
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
import com.example.urbanfix.ui.issues.issueTileBodyAfterTitle
import com.example.urbanfix.databinding.FragmentDashboardBinding
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import org.json.JSONArray
import org.json.JSONObject
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
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadMyIssues()
    }

    private fun loadMyIssues() {
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
                val official = userJson.optString("account_type", "").lowercase() == "official"
                val issues = fetchIssues(userJson, email, official)
                issues
            }.onSuccess { issues ->
                requireActivity().runOnUiThread {
                    setLoading(false)
                    renderIssues(issues)
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

    private fun fetchIssues(userJson: JSONObject, email: String, isOfficial: Boolean): JSONArray {
        val urlString = if (isOfficial) {
            val enc = URLEncoder.encode(email, Charsets.UTF_8.name())
            "${backendBaseUrl()}/issues?official_email=$enc"
        } else {
            "${backendBaseUrl()}/issues?user_id=${userJson.getInt("id")}"
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

    private fun renderIssues(issues: JSONArray) {
        binding.issuesContainer.removeAllViews()
        if (issues.length() == 0) {
            binding.textIssuesEmpty.visibility = View.VISIBLE
            binding.textIssuesEmpty.text = getString(R.string.my_issues_empty)
            return
        }

        binding.textIssuesEmpty.visibility = View.GONE
        for (i in 0 until issues.length()) {
            val issue = issues.getJSONObject(i)
            binding.issuesContainer.addView(createIssueCard(issue))
        }
    }

    private fun createIssueCard(issue: JSONObject): View {
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
                    issue.optInt("vote_count", 0),
                )
                TextViewCompat.setTextAppearance(this, R.style.TextAppearance_Urbanfix_BodySecondary)
            },
            LinearLayout.LayoutParams(lpMatch, lpWrap).apply { topMargin = gapTitleBody },
        )
        card.addView(inner)
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

    private fun setLoading(loading: Boolean) {
        binding.progressIssues.visibility = if (loading) View.VISIBLE else View.GONE
        binding.issuesScroll.visibility = if (loading) View.GONE else View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}