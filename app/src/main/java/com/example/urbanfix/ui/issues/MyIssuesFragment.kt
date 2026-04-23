package com.example.urbanfix.ui.issues

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
import com.example.urbanfix.databinding.FragmentMyIssuesBinding
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class MyIssuesFragment : Fragment() {
    private var _binding: FragmentMyIssuesBinding? = null
    private val binding get() = _binding!!
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    private fun backendBaseUrl(): String = requireContext().getString(R.string.backend_base_url)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentMyIssuesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadMyIssues()
    }

    private fun loadMyIssues() {
        val email = auth.currentUser?.email?.trim().orEmpty()
        if (email.isEmpty()) {
            binding.textMyIssuesEmpty.visibility = View.VISIBLE
            binding.textMyIssuesEmpty.text = getString(R.string.my_issues_load_error)
            return
        }
        setLoading(true)
        Thread {
            runCatching {
                val encoded = URLEncoder.encode(email, Charsets.UTF_8.name())
                val userConn =
                    (URL("${backendBaseUrl()}/users/by-email?email=$encoded").openConnection() as HttpURLConnection).apply {
                        requestMethod = "GET"
                        connectTimeout = 10_000
                        readTimeout = 10_000
                    }
                val userId = try {
                    if (userConn.responseCode != HttpURLConnection.HTTP_OK) error("HTTP ${userConn.responseCode}")
                    val body = userConn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                    JSONObject(body).getInt("id")
                } finally {
                    userConn.disconnect()
                }
                val issuesConn = (URL("${backendBaseUrl()}/issues?user_id=$userId").openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 10_000
                    readTimeout = 10_000
                }
                try {
                    if (issuesConn.responseCode != HttpURLConnection.HTTP_OK) error("HTTP ${issuesConn.responseCode}")
                    val body = issuesConn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                    JSONArray(body)
                } finally {
                    issuesConn.disconnect()
                }
            }.onSuccess { array ->
                activity?.runOnUiThread {
                    if (!isAdded) return@runOnUiThread
                    setLoading(false)
                    renderIssues(array)
                }
            }.onFailure {
                activity?.runOnUiThread {
                    if (!isAdded) return@runOnUiThread
                    setLoading(false)
                    binding.textMyIssuesEmpty.visibility = View.VISIBLE
                    binding.textMyIssuesEmpty.text = getString(R.string.my_issues_load_error)
                }
            }
        }.start()
    }

    private fun renderIssues(array: JSONArray) {
        binding.containerMyIssues.removeAllViews()
        if (array.length() == 0) {
            binding.textMyIssuesEmpty.visibility = View.VISIBLE
            binding.textMyIssuesEmpty.text = getString(R.string.my_issues_empty)
            return
        }
        binding.textMyIssuesEmpty.visibility = View.GONE
        for (i in 0 until array.length()) {
            binding.containerMyIssues.addView(createIssueCard(array.getJSONObject(i)))
        }
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
        col.addView(
            TextView(context).apply {
                text = issue.optString("title").trim().ifEmpty { "—" }
                TextViewCompat.setTextAppearance(this, R.style.TextAppearance_Urbanfix_Subtitle)
            },
        )
        col.addView(
            TextView(context).apply {
                text = getString(R.string.issue_card_status, issue.optString("status", "—"))
                TextViewCompat.setTextAppearance(this, R.style.TextAppearance_Urbanfix_BodySecondary)
            },
        )
        card.addView(col)
        val issueId = issue.optInt("id", -1)
        if (issueId >= 0) {
            card.isClickable = true
            card.isFocusable = true
            card.setOnClickListener {
                findNavController().navigate(
                    R.id.action_navigation_my_issues_to_navigation_issue_detail,
                    bundleOf("issueId" to issueId),
                )
            }
        }
        return card
    }

    private fun setLoading(loading: Boolean) {
        binding.progressMyIssues.visibility = if (loading) View.VISIBLE else View.GONE
        binding.scrollMyIssues.visibility = if (loading) View.GONE else View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
