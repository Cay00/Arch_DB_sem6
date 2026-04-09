package com.example.urbanfix.ui.issues

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.urbanfix.databinding.FragmentRoadDamageReportBinding
import com.google.android.material.snackbar.Snackbar
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class RoadDamageReportFragment : Fragment() {
    private var _binding: FragmentRoadDamageReportBinding? = null
    private val binding get() = _binding!!

    private val backendBaseUrl = "http://10.0.2.2:8000"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRoadDamageReportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.buttonSubmitIssue.setOnClickListener { submitIssue() }
    }

    private fun submitIssue() {
        val title = binding.editIssueTitle.text?.toString()?.trim().orEmpty()
        val description = binding.editIssueDescription.text?.toString()?.trim().orEmpty()
        val location = binding.editIssueLocation.text?.toString()?.trim().orEmpty()
        val userIdText = binding.editIssueUserId.text?.toString()?.trim().orEmpty()

        var valid = true
        if (title.isEmpty()) {
            binding.inputLayoutIssueTitle.error = "Podaj tytuł"
            valid = false
        } else {
            binding.inputLayoutIssueTitle.error = null
        }
        if (description.isEmpty()) {
            binding.inputLayoutIssueDescription.error = "Podaj opis"
            valid = false
        } else {
            binding.inputLayoutIssueDescription.error = null
        }
        if (location.isEmpty()) {
            binding.inputLayoutIssueLocation.error = "Podaj lokalizację"
            valid = false
        } else {
            binding.inputLayoutIssueLocation.error = null
        }
        val userId = userIdText.toIntOrNull()
        if (userId == null) {
            binding.inputLayoutIssueUserId.error = "Podaj poprawne ID użytkownika"
            valid = false
        } else {
            binding.inputLayoutIssueUserId.error = null
        }

        if (!valid || userId == null) return

        setLoading(true)
        Thread {
            runCatching {
                val payload = JSONObject()
                    .put("title", title)
                    .put("description", description)
                    .put("category", "Drogi")
                    .put("status", "NEW")
                    .put("location", location)
                    .put("user_id", userId)
                    .toString()

                val connection = (URL("$backendBaseUrl/issues").openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 5000
                    readTimeout = 5000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                }

                OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                    writer.write(payload)
                }
                val responseCode = connection.responseCode
                connection.disconnect()
                responseCode
            }.onSuccess { code ->
                requireActivity().runOnUiThread {
                    setLoading(false)
                    if (code == HttpURLConnection.HTTP_CREATED) {
                        Snackbar.make(binding.root, "Zgłoszenie zapisane", Snackbar.LENGTH_LONG).show()
                        clearForm()
                    } else {
                        Snackbar.make(binding.root, "Błąd API: $code", Snackbar.LENGTH_LONG).show()
                    }
                }
            }.onFailure { error ->
                requireActivity().runOnUiThread {
                    setLoading(false)
                    Snackbar.make(
                        binding.root,
                        "Nie udało się wysłać zgłoszenia: ${error.message}",
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
        }.start()
    }

    private fun clearForm() {
        binding.editIssueTitle.text = null
        binding.editIssueDescription.text = null
        binding.editIssueLocation.text = null
    }

    private fun setLoading(loading: Boolean) {
        binding.progressIssue.visibility = if (loading) View.VISIBLE else View.GONE
        binding.buttonSubmitIssue.isEnabled = !loading
        binding.editIssueTitle.isEnabled = !loading
        binding.editIssueDescription.isEnabled = !loading
        binding.editIssueLocation.isEnabled = !loading
        binding.editIssueUserId.isEnabled = !loading
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
