package com.example.urbanfix.ui.issues

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.example.urbanfix.R
import com.example.urbanfix.data.BackendUserJson
import com.example.urbanfix.databinding.FragmentRoadDamageReportBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.UUID

class RoadDamageReportFragment : Fragment() {
    private var _binding: FragmentRoadDamageReportBinding? = null
    private val binding get() = _binding!!

    private fun backendBaseUrl(): String = requireContext().getString(R.string.backend_base_url)
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    private var photoUri: Uri? = null
    private var photoFile: File? = null

    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            showPhotoPreview()
        } else {
            photoUri = null
            photoFile = null
        }
    }

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            photoUri = it
            // Kopiujemy plik z galerii do cache aplikacji, zeby miec do niego dostep jako File
            photoFile = copyUriToCache(it)
            showPhotoPreview()
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startCamera()
        } else {
            Snackbar.make(binding.root, "Kamera jest wymagana do zrobienia zdjecia", Snackbar.LENGTH_LONG).show()
        }
    }

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
        val email = auth.currentUser?.email.orEmpty()
        binding.editIssueUserIdentity.setText(email)
        val firebaseName = auth.currentUser?.displayName?.trim().orEmpty()
        if (firebaseName.isNotEmpty()) {
            binding.editIssueUserDisplayName.setText(firebaseName)
        } else if (email.isNotBlank()) {
            binding.editIssueUserDisplayName.setText(getString(R.string.profile_dash))
            Thread {
                val name = runCatching {
                    val json = fetchUserJsonByEmail(email)
                    BackendUserJson.displayNameFromUser(json)
                }.getOrDefault("")
                requireActivity().runOnUiThread {
                    if (_binding == null) return@runOnUiThread
                    binding.editIssueUserDisplayName.setText(
                        name.ifBlank { getString(R.string.profile_dash) },
                    )
                }
            }.start()
        } else {
            binding.editIssueUserDisplayName.setText(getString(R.string.profile_dash))
        }

        binding.buttonAddPhoto.setOnClickListener { checkPermissionAndOpenCamera() }
        binding.buttonSelectGallery.setOnClickListener { openGallery() }
        binding.buttonSubmitIssue.setOnClickListener { submitIssue() }
    }

    private fun checkPermissionAndOpenCamera() {
        when {
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
                startCamera()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun startCamera() {
        val file = File(requireContext().cacheDir, "temp_photo_${System.currentTimeMillis()}.jpg")
        photoFile = file
        photoUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            file
        )
        takePictureLauncher.launch(photoUri)
    }

    private fun openGallery() {
        pickImageLauncher.launch("image/*")
    }

    private fun showPhotoPreview() {
        binding.imagePreview.visibility = View.VISIBLE
        binding.imagePreview.setImageURI(photoUri)
    }

    private fun copyUriToCache(uri: Uri): File {
        val file = File(requireContext().cacheDir, "gallery_photo_${System.currentTimeMillis()}.jpg")
        requireContext().contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(file).use { output ->
                input.copyTo(output)
            }
        }
        return file
    }

    private fun submitIssue() {
        val title = binding.editIssueTitle.text?.toString()?.trim().orEmpty()
        val description = binding.editIssueDescription.text?.toString()?.trim().orEmpty()
        val location = binding.editIssueLocation.text?.toString()?.trim().orEmpty()

        var valid = true
        if (title.isEmpty()) {
            binding.inputLayoutIssueTitle.error = "Podaj tytul"
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
            binding.inputLayoutIssueLocation.error = "Podaj lokalizacje"
            valid = false
        } else {
            binding.inputLayoutIssueLocation.error = null
        }
        if (!valid) return

        val email = auth.currentUser?.email
        if (email.isNullOrBlank()) {
            Snackbar.make(binding.root, "Brak zalogowanego uzytkownika", Snackbar.LENGTH_LONG).show()
            return
        }

        setLoading(true)
        Thread {
            runCatching {
                val userId = fetchCurrentUserId(email)
                val boundary = "Boundary-${UUID.randomUUID()}"
                val lineEnd = "\r\n"
                val twoHyphens = "--"

                val connection = (URL("${backendBaseUrl()}/issues").openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 10000
                    readTimeout = 10000
                    doOutput = true
                    setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                }

                connection.outputStream.use { outputStream ->
                    PrintWriter(OutputStreamWriter(outputStream, Charsets.UTF_8), true).use { writer ->
                        addFormField(writer, outputStream, boundary, "title", title)
                        addFormField(writer, outputStream, boundary, "description", description)
                        addFormField(writer, outputStream, boundary, "category", "Drogi")
                        addFormField(writer, outputStream, boundary, "location", location)
                        addFormField(writer, outputStream, boundary, "user_id", userId.toString())

                        photoFile?.let { file ->
                            writer.append(twoHyphens).append(boundary).append(lineEnd)
                            writer.append("Content-Disposition: form-data; name=\"image\"; filename=\"${file.name}\"").append(lineEnd)
                            writer.append("Content-Type: image/jpeg").append(lineEnd)
                            writer.append(lineEnd)
                            writer.flush()

                            FileInputStream(file).use { inputStream ->
                                inputStream.copyTo(outputStream)
                            }
                            outputStream.flush()
                            writer.append(lineEnd)
                        }

                        writer.append(twoHyphens).append(boundary).append(twoHyphens).append(lineEnd)
                        writer.flush()
                    }
                }

                val responseCode = connection.responseCode
                connection.disconnect()
                responseCode
            }.onSuccess { code ->
                requireActivity().runOnUiThread {
                    setLoading(false)
                    if (code == HttpURLConnection.HTTP_CREATED || code == HttpURLConnection.HTTP_OK) {
                        Snackbar.make(
                            binding.root,
                            getString(R.string.issue_submit_success_with_status),
                            Snackbar.LENGTH_LONG,
                        ).show()
                        clearForm()
                    } else {
                        Snackbar.make(binding.root, "Blad API: $code", Snackbar.LENGTH_LONG).show()
                    }
                }
            }.onFailure { error ->
                requireActivity().runOnUiThread {
                    setLoading(false)
                    Snackbar.make(
                        binding.root,
                        "Nie udalo sie wyslac zgloszenia: ${error.message}",
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
        }.start()
    }

    private fun addFormField(writer: PrintWriter, out: OutputStream, boundary: String, name: String, value: String) {
        val lineEnd = "\r\n"
        val twoHyphens = "--"
        writer.append(twoHyphens).append(boundary).append(lineEnd)
        writer.append("Content-Disposition: form-data; name=\"$name\"").append(lineEnd)
        writer.append("Content-Type: text/plain; charset=UTF-8").append(lineEnd)
        writer.append(lineEnd)
        writer.append(value).append(lineEnd)
        writer.flush()
    }

    private fun fetchCurrentUserId(email: String): Int =
        fetchUserJsonByEmail(email).getInt("id")

    private fun fetchUserJsonByEmail(email: String): JSONObject {
        val encodedEmail = URLEncoder.encode(email, Charsets.UTF_8.name())
        val connection =
            (URL("${backendBaseUrl()}/users/by-email?email=$encodedEmail").openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 5000
                readTimeout = 5000
            }
        return try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw IllegalStateException("Nie znaleziono uzytkownika w backendzie")
            }
            val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            JSONObject(body)
        } finally {
            connection.disconnect()
        }
    }

    private fun clearForm() {
        binding.editIssueTitle.text = null
        binding.editIssueDescription.text = null
        binding.editIssueLocation.text = null
        binding.imagePreview.visibility = View.GONE
        photoFile = null
        photoUri = null
    }

    private fun setLoading(loading: Boolean) {
        binding.progressIssue.visibility = if (loading) View.VISIBLE else View.GONE
        binding.buttonSubmitIssue.isEnabled = !loading
        binding.buttonAddPhoto.isEnabled = !loading
        binding.buttonSelectGallery.isEnabled = !loading
        binding.editIssueTitle.isEnabled = !loading
        binding.editIssueDescription.isEnabled = !loading
        binding.editIssueLocation.isEnabled = !loading
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
