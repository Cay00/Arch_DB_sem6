package com.example.urbanfix.ui.issues

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.example.urbanfix.R
import com.example.urbanfix.data.BackendUserJson
import com.example.urbanfix.databinding.FragmentRoadDamageReportBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
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
import java.util.Locale
import java.util.UUID

class ReportFragment : Fragment() {
    private var _binding: FragmentRoadDamageReportBinding? = null
    private val binding get() = _binding!!

    private val categoryArg: String by lazy { arguments?.getString("category") ?: "Drogi" }
    private val availableCategories = listOf("Drogi", "Zieleń", "Inwestycje", "Oświetlenie", "Porządek")

    private fun backendBaseUrl(): String = requireContext().getString(R.string.backend_base_url)
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    private var photoUri: Uri? = null
    private var photoFile: File? = null

    private val geocoder by lazy { Geocoder(requireContext(), Locale.forLanguageTag("pl-PL")) }
    private lateinit var addressAdapter: NoFilterAdapter

    private val fusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(requireActivity())
    }

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
            photoFile = copyUriToCache(it)
            // Podgląd z pliku w cache (FileProvider) — unikamy błędów w logcat przy setImageURI(content://…).
            photoUri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                photoFile!!,
            )
            showPhotoPreview()
        }
    }

    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) startCamera()
        else showSnackbar("Kamera jest wymagana do zrobienia zdjecia")
    }

    private val requestLocationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                      permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) getCurrentLocation()
        else showSnackbar("Lokalizacja jest wymagana do zczytania adresu")
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
        
        binding.editIssueCategory.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, availableCategories),
        )
        binding.editIssueCategory.setText(categoryArg, false)
        
        setupUserInformation()
        setupPhotoButtons()
        setupLocationAutocomplete()
        setupLocationButton()
        binding.buttonSubmitIssue.setOnClickListener { submitIssue() }
    }

    private fun setupLocationButton() {
        binding.inputLayoutIssueLocation.setEndIconOnClickListener {
            checkLocationPermissionAndFetch()
        }
    }

    private fun checkLocationPermissionAndFetch() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocation()
        } else {
            requestLocationPermissionLauncher.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
        }
    }

    private fun getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return

        binding.progressIssue.visibility = View.VISIBLE
        
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location ->
                if (location != null) {
                    reverseGeocode(location.latitude, location.longitude)
                } else {
                    fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
                        if (lastLoc != null) {
                            reverseGeocode(lastLoc.latitude, lastLoc.longitude)
                        } else {
                            binding.progressIssue.visibility = View.GONE
                            showSnackbar("Nie udalo sie pobrac lokalizacji GPS. Sprobuj ponownie.")
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                binding.progressIssue.visibility = View.GONE
                showSnackbar("Blad GPS: ${e.message}")
            }
    }

    private fun reverseGeocode(lat: Double, lng: Double) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            geocoder.getFromLocation(lat, lng, 1) { addresses ->
                requireActivity().runOnUiThread {
                    updateLocationFromAddress(addresses.firstOrNull())
                }
            }
        } else {
            Thread {
                try {
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(lat, lng, 1)
                    requireActivity().runOnUiThread {
                        updateLocationFromAddress(addresses?.firstOrNull())
                    }
                } catch (e: Exception) {
                    requireActivity().runOnUiThread {
                        binding.progressIssue.visibility = View.GONE
                        showSnackbar("Blad geokodowania")
                    }
                }
            }.start()
        }
    }

    private fun updateLocationFromAddress(address: Address?) {
        binding.progressIssue.visibility = View.GONE
        val addressStr = address?.getAddressLine(0)
        if (addressStr != null) {
            binding.editIssueLocation.setText(addressStr, false)
            binding.editIssueLocation.dismissDropDown()
        }
    }

    private fun setupUserInformation() {
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
    }

    private fun setupPhotoButtons() {
        binding.buttonAddPhoto.setOnClickListener { checkPermissionAndOpenCamera() }
        binding.buttonSelectGallery.setOnClickListener { openGallery() }
    }

    private fun setupLocationAutocomplete() {
        addressAdapter = NoFilterAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line)
        binding.editIssueLocation.setAdapter(addressAdapter)

        binding.editIssueLocation.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString()?.trim().orEmpty()
                if (query.length >= 3) {
                    searchAddresses(query)
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun searchAddresses(query: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            geocoder.getFromLocationName(query, 10) { addresses ->
                requireActivity().runOnUiThread {
                    updateAddressAdapter(addresses)
                }
            }
        } else {
            Thread {
                try {
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocationName(query, 10)
                    requireActivity().runOnUiThread {
                        updateAddressAdapter(addresses ?: emptyList())
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }.start()
        }
    }

    private fun updateAddressAdapter(addresses: List<Address>) {
        if (_binding == null) return
        val addressStrings = addresses.mapNotNull { it.getAddressLine(0) }.distinct()
        addressAdapter.updateData(addressStrings)
        if (addressStrings.isNotEmpty() && binding.editIssueLocation.hasFocus()) {
            binding.editIssueLocation.showDropDown()
        }
    }

    private fun checkPermissionAndOpenCamera() {
        when {
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
                startCamera()
            }
            else -> {
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
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
            showSnackbar("Brak zalogowanego uzytkownika")
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
                        
                        val selectedCategory = binding.editIssueCategory.text?.toString()?.trim().orEmpty()
                            .ifBlank { categoryArg }
                        addFormField(writer, outputStream, boundary, "category", selectedCategory)

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
                        showSnackbar(getString(R.string.issue_submit_success_with_status))
                        clearForm()
                    } else {
                        showSnackbar("Blad API: $code")
                    }
                }
            }.onFailure { error ->
                requireActivity().runOnUiThread {
                    setLoading(false)
                    showSnackbar("Nie udalo sie wyslac zgloszenia: ${error.message}")
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
        binding.editIssueLocation.setText("")
        binding.editIssueCategory.setText(categoryArg, false)
        binding.editIssueTitle.text = null
        binding.editIssueDescription.text = null
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

    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private class NoFilterAdapter(context: Context, layout: Int) : ArrayAdapter<String>(context, layout) {
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
