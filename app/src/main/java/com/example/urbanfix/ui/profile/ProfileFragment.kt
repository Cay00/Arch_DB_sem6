package com.example.urbanfix.ui.profile

import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.example.urbanfix.R
import com.example.urbanfix.ui.issues.issueTileBodyAfterTitle
import com.example.urbanfix.data.BackendUserJson
import com.example.urbanfix.databinding.FragmentProfileBinding
import com.google.firebase.auth.FirebaseAuth
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    private data class ProfileLoad(val userId: Int, val issues: JSONArray, val backendName: String)

    private fun backendBaseUrl(): String = requireContext().getString(R.string.backend_base_url)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.buttonProfileOpenIssues.setOnClickListener {
            findNavController().navigate(R.id.navigation_dashboard)
        }
        binding.buttonProfileLogout.setOnClickListener { logout() }
    }

    override fun onResume() {
        super.onResume()
        bindFirebaseUser()
        loadBackendSummary()
    }

    private fun bindFirebaseUser() {
        val user = auth.currentUser
        binding.textProfileEmail.text = user?.email ?: getString(R.string.profile_not_signed_in)
        binding.textProfileFirebaseUid.text =
            user?.uid ?: getString(R.string.profile_dash)
        binding.textProfileName.text =
            user?.displayName?.trim()?.takeIf { it.isNotEmpty() } ?: getString(R.string.profile_dash)
    }

    private fun loadBackendSummary() {
        val email = auth.currentUser?.email
        if (email.isNullOrBlank()) {
            binding.textProfileName.text = getString(R.string.profile_dash)
            binding.textProfileBackendId.text = getString(R.string.profile_dash)
            binding.textProfileIssuesSummary.text = getString(R.string.profile_issues_need_login)
            clearProfileIssuesList()
            return
        }

        binding.progressProfileBackend.visibility = View.VISIBLE
        Thread {
            val result = runCatching {
                val userJson = fetchUserJsonByEmail(email)
                val userId = userJson.getInt("id")
                val backendName = BackendUserJson.displayNameFromUser(userJson)
                val issues = fetchIssues(userId)
                ProfileLoad(userId, issues, backendName)
            }
            requireActivity().runOnUiThread {
                binding.progressProfileBackend.visibility = View.GONE
                result.fold(
                    onSuccess = { load ->
                        binding.textProfileBackendId.text = load.userId.toString()
                        val resolvedName = load.backendName.ifBlank {
                            auth.currentUser?.displayName?.trim().orEmpty()
                        }.ifBlank { getString(R.string.profile_dash) }
                        binding.textProfileName.text = resolvedName
                        val count = load.issues.length()
                        binding.textProfileIssuesSummary.text =
                            resources.getQuantityString(
                                R.plurals.profile_issues_count,
                                count,
                                count,
                            )
                        renderProfileIssuesList(load.issues)
                    },
                    onFailure = {
                        binding.textProfileBackendId.text = getString(R.string.profile_backend_error)
                        binding.textProfileIssuesSummary.text = getString(R.string.profile_issues_load_error)
                        clearProfileIssuesList()
                    },
                )
            }
        }.start()
    }

    private fun clearProfileIssuesList() {
        binding.profileIssuesList.removeAllViews()
        binding.profileIssuesList.visibility = View.GONE
    }

    private fun renderProfileIssuesList(issues: JSONArray) {
        binding.profileIssuesList.removeAllViews()
        if (issues.length() == 0) {
            binding.profileIssuesList.visibility = View.GONE
            return
        }
        binding.profileIssuesList.visibility = View.VISIBLE
        val ctx = requireContext()
        val density = resources.displayMetrics.density
        val padV = (8 * density).toInt()
        val divH = (1 * density).toInt().coerceAtLeast(1)
        for (i in 0 until issues.length()) {
            val issue = issues.getJSONObject(i)
            if (i > 0) {
                binding.profileIssuesList.addView(
                    View(ctx).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            divH,
                        )
                        setBackgroundColor(0x26000000)
                    },
                )
            }
            val block = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                )
                setPadding(0, padV, 0, padV)
            }
            val statusRaw = issue.optString("status").ifBlank { getString(R.string.profile_dash) }
            block.addView(
                TextView(ctx).apply {
                    text = getString(R.string.issue_card_status, statusRaw)
                    textSize = 16f
                    setTypeface(null, Typeface.BOLD)
                },
            )
            block.addView(
                TextView(ctx).apply {
                    text = issue.optString("title").trim().ifEmpty { getString(R.string.profile_dash) }
                    textSize = 17f
                    setTypeface(null, Typeface.BOLD)
                    setPadding(0, (4 * density).toInt(), 0, 0)
                },
            )
            block.addView(
                TextView(ctx).apply {
                    text = issueTileBodyAfterTitle(
                        ctx,
                        issue.optString("category"),
                        issue.optString("location"),
                        issue.optString("description"),
                        issue.optInt("vote_count", 0),
                    )
                    textSize = 14f
                    setPadding(0, (4 * density).toInt(), 0, 0)
                },
            )
            binding.profileIssuesList.addView(block)
        }
    }

    private fun fetchUserJsonByEmail(email: String): JSONObject {
        val encoded = URLEncoder.encode(email, Charsets.UTF_8.name())
        val c =
            (URL("${backendBaseUrl()}/users/by-email?email=$encoded").openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10_000
                readTimeout = 10_000
            }
        return try {
            if (c.responseCode != HttpURLConnection.HTTP_OK) {
                error("HTTP ${c.responseCode}")
            }
            val body = c.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            JSONObject(body)
        } finally {
            c.disconnect()
        }
    }

    private fun fetchIssues(userId: Int): JSONArray {
        val c =
            (URL("${backendBaseUrl()}/issues?user_id=$userId").openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10_000
                readTimeout = 10_000
            }
        return try {
            if (c.responseCode != HttpURLConnection.HTTP_OK) {
                error("HTTP ${c.responseCode}")
            }
            val body = c.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            JSONArray(body)
        } finally {
            c.disconnect()
        }
    }

    private fun logout() {
        auth.signOut()
        findNavController().navigate(
            R.id.navigation_auth,
            null,
            NavOptions.Builder()
                .setPopUpTo(R.id.mobile_navigation, true)
                .build(),
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
