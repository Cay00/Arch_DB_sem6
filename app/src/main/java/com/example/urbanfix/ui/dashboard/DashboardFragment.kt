package com.example.urbanfix.ui.dashboard

import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.urbanfix.R
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
                val userId = fetchCurrentUserId(email)
                fetchIssues(userId)
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

    private fun fetchCurrentUserId(email: String): Int {
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
            JSONObject(body).getInt("id")
        } finally {
            connection.disconnect()
        }
    }

    private fun fetchIssues(userId: Int): JSONArray {
        val connection = (URL("${backendBaseUrl()}/issues?user_id=$userId").openConnection() as HttpURLConnection).apply {
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
        val density = resources.displayMetrics.density
        val card = MaterialCardView(context).apply {
            radius = 16f
            cardElevation = 2f
            useCompatPadding = true
            setContentPadding(24, 24, 24, 24)
            layoutParams = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 12
            }
        }

        val inner = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        }

        val statusText = issue.optString("status").ifBlank { getString(R.string.profile_dash) }
        inner.addView(
            TextView(context).apply {
                text = getString(R.string.issue_card_status, statusText)
                textSize = 17f
                setTypeface(null, Typeface.BOLD)
            },
        )
        inner.addView(
            TextView(context).apply {
                text = issue.optString("title")
                textSize = 18f
                setTypeface(null, Typeface.BOLD)
                setPadding(0, (6 * density).toInt(), 0, 0)
            },
        )
        inner.addView(
            TextView(context).apply {
                text = buildString {
                    append("Kategoria: ${issue.optString("category")}")
                    append("\n")
                    append("Lokalizacja: ${issue.optString("location")}")
                    append("\n\n")
                    append(issue.optString("description"))
                }
                textSize = 17f
                setPadding(0, (8 * density).toInt(), 0, 0)
            },
        )
        card.addView(inner)
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