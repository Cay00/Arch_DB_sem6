package com.example.urbanfix.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.example.urbanfix.R
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
    }

    private fun loadBackendSummary() {
        val email = auth.currentUser?.email
        if (email.isNullOrBlank()) {
            binding.textProfileBackendId.text = getString(R.string.profile_dash)
            binding.textProfileIssuesSummary.text = getString(R.string.profile_issues_need_login)
            return
        }

        binding.progressProfileBackend.visibility = View.VISIBLE
        Thread {
            val result = runCatching {
                val userId = fetchUserIdByEmail(email)
                val issues = fetchIssues(userId)
                userId to issues.length()
            }
            requireActivity().runOnUiThread {
                binding.progressProfileBackend.visibility = View.GONE
                result.fold(
                    onSuccess = { (userId, count) ->
                        binding.textProfileBackendId.text = userId.toString()
                        binding.textProfileIssuesSummary.text =
                            resources.getQuantityString(
                                R.plurals.profile_issues_count,
                                count,
                                count
                            )
                    },
                    onFailure = {
                        binding.textProfileBackendId.text = getString(R.string.profile_backend_error)
                        binding.textProfileIssuesSummary.text = getString(R.string.profile_issues_load_error)
                    },
                )
            }
        }.start()
    }

    private fun fetchUserIdByEmail(email: String): Int {
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
            JSONObject(body).getInt("id")
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
