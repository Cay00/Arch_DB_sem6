package com.example.urbanfix.ui.issues

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.urbanfix.R
import com.example.urbanfix.data.IssuesApi
import com.example.urbanfix.data.UsersApi
import com.example.urbanfix.databinding.FragmentIssueDetailBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import org.json.JSONObject
import java.util.concurrent.Executors

class IssueDetailFragment : Fragment() {

    private var _binding: FragmentIssueDetailBinding? = null
    private val binding get() = _binding!!

    private val io = Executors.newSingleThreadExecutor()

    private fun backendBaseUrl(): String = requireContext().getString(R.string.backend_base_url)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentIssueDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val issueId = arguments?.getInt("issueId") ?: -1
        if (issueId < 0) {
            Snackbar.make(binding.root, R.string.issue_detail_load_error, Snackbar.LENGTH_LONG).show()
            findNavController().navigateUp()
            return
        }

        val email = FirebaseAuth.getInstance().currentUser?.email?.trim().orEmpty()
        if (email.isEmpty()) {
            Snackbar.make(binding.root, R.string.issue_detail_load_error, Snackbar.LENGTH_LONG).show()
            findNavController().navigateUp()
            return
        }

        val statuses = resources.getStringArray(R.array.issue_status_values).toList()
        binding.dropdownIssueStatus.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, statuses),
        )

        loadIssue(issueId, email, statuses)
    }

    private fun loadIssue(issueId: Int, viewerEmail: String, statuses: List<String>) {
        val base = backendBaseUrl()
        io.execute {
            var official = false
            var issueJson: JSONObject? = null
            var err: String? = null
            try {
                val userJson = UsersApi.getUserByEmail(base, viewerEmail)
                val at = userJson.optString("account_type", "").lowercase()
                official = at == "official"
                issueJson = IssuesApi.getIssue(base, issueId, viewerEmail)
            } catch (e: Exception) {
                err = e.message ?: e.toString()
            }
            val finalIssue = issueJson
            val finalErr = err
            activity?.runOnUiThread {
                if (finalIssue == null) {
                    Snackbar.make(
                        binding.root,
                        getString(R.string.issue_detail_load_error) + (finalErr?.let { "\n$it" } ?: ""),
                        Snackbar.LENGTH_LONG,
                    ).show()
                    findNavController().navigateUp()
                    return@runOnUiThread
                }
                bindIssue(finalIssue)
                if (official) {
                    binding.groupOfficialStatusEdit.visibility = View.VISIBLE
                    val current = finalIssue.optString("status", statuses.firstOrNull().orEmpty())
                    val idx = statuses.indexOf(current).coerceAtLeast(0)
                    binding.dropdownIssueStatus.setText(statuses.getOrNull(idx).orEmpty(), false)
                    binding.buttonSaveStatus.setOnClickListener {
                        saveStatus(issueId, viewerEmail, statuses)
                    }
                } else {
                    binding.groupOfficialStatusEdit.visibility = View.GONE
                }
            }
        }
    }

    private fun bindIssue(j: JSONObject) {
        val id = j.optInt("id", -1)
        binding.textIssueId.text = getString(R.string.issue_detail_label_id) + ": $id"
        binding.textIssueTitle.text = j.optString("title")
        binding.textIssueDescription.text = j.optString("description").ifEmpty { "—" }
        binding.textIssueCategory.text = j.optString("category").ifEmpty { "—" }
        binding.textIssueStatus.text = j.optString("status").ifEmpty { "—" }
        binding.textIssueLocation.text = j.optString("location").ifEmpty { "—" }
        val created = j.optString("created_at", "")
        binding.textIssueCreated.text = if (created.contains("T")) {
            created.substringBefore("T") + " " + created.substringAfter("T").take(8)
        } else {
            created.ifEmpty { "—" }
        }
        binding.textIssueUserId.text = j.optInt("user_id", -1).takeIf { it >= 0 }?.toString() ?: "—"
    }

    private fun saveStatus(issueId: Int, actorEmail: String, statuses: List<String>) {
        val selected = binding.dropdownIssueStatus.text?.toString()?.trim().orEmpty()
        if (selected !in statuses) {
            Snackbar.make(binding.root, R.string.issue_detail_status_save_error, Snackbar.LENGTH_SHORT).show()
            return
        }
        val base = backendBaseUrl()
        binding.buttonSaveStatus.isEnabled = false
        io.execute {
            val (code, body) = try {
                IssuesApi.patchIssueStatus(base, issueId, selected, actorEmail)
            } catch (e: Exception) {
                -1 to (e.message ?: e.toString())
            }
            activity?.runOnUiThread {
                binding.buttonSaveStatus.isEnabled = true
                if (code in 200..299) {
                    try {
                        val j = JSONObject(body)
                        bindIssue(j)
                    } catch (_: Exception) {
                        binding.textIssueStatus.text = selected
                    }
                    Snackbar.make(binding.root, R.string.issue_detail_status_saved, Snackbar.LENGTH_SHORT).show()
                } else {
                    Snackbar.make(
                        binding.root,
                        getString(R.string.issue_detail_status_save_error) + "\nHTTP $code: ${body.take(200)}",
                        Snackbar.LENGTH_LONG,
                    ).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
