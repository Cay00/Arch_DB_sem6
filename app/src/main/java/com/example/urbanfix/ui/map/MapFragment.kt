package com.example.urbanfix.ui.map

import android.os.Bundle
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.Fragment
import com.example.urbanfix.R
import com.example.urbanfix.databinding.FragmentMapBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import org.json.JSONArray
import org.json.JSONObject
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class MapFragment : Fragment() {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!
    private var mapView: MapView? = null
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val polandBounds = BoundingBox(54.9, 24.2, 49.0, 14.1)

    private data class MapPin(
        val location: GeoPoint,
        val title: String,
        val snippet: String,
        val category: String,
    )

    private fun backendBaseUrl(): String = requireContext().getString(R.string.backend_base_url)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize OSMDroid configuration
        Configuration.getInstance().apply {
            userAgentValue = requireContext().packageName
            load(
                requireContext(),
                androidx.preference.PreferenceManager.getDefaultSharedPreferences(requireContext()),
            )
        }

        mapView = binding.mapView
        mapView?.let { map ->
            map.setTileSource(TileSourceFactory.MAPNIK)
            map.setMultiTouchControls(true)
            map.setScrollableAreaLimitDouble(polandBounds)
            map.minZoomLevel = 5.5
            map.controller.setCenter(GeoPoint(51.1087, 17.0319))
            @Suppress("DEPRECATION")
            map.controller.setZoom(13)
        }
        loadIssuePins()
    }

    private fun loadIssuePins() {
        val viewerEmail = auth.currentUser?.email?.trim().orEmpty()
        if (viewerEmail.isEmpty()) {
            showMessage("Zaloguj sie, aby zobaczyc zgloszenia na mapie")
            return
        }
        Thread {
            val pins = runCatching {
                val enc = URLEncoder.encode(viewerEmail, Charsets.UTF_8.name())
                val url = "${backendBaseUrl()}/issues?community_viewer_email=$enc"
                val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 10000
                    readTimeout = 10000
                }
                try {
                    if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                        error("HTTP ${connection.responseCode}")
                    }
                    val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                    parsePins(JSONArray(body))
                } finally {
                    connection.disconnect()
                }
            }
            activity?.runOnUiThread {
                if (!isAdded) return@runOnUiThread
                pins.onSuccess { renderPins(it) }
                    .onFailure { showMessage("Nie udalo sie pobrac pinezek: ${it.message}") }
            }
        }.start()
    }

    private fun parsePins(items: JSONArray): List<MapPin> {
        val pins = mutableListOf<MapPin>()
        for (i in 0 until items.length()) {
            val issue = items.optJSONObject(i) ?: continue
            val lat = issue.optDouble("location_lat", Double.NaN)
            val lng = issue.optDouble("location_lng", Double.NaN)
            if (!lat.isFinite() || !lng.isFinite()) continue
            val title = issue.optString("title").ifBlank { "Zgloszenie" }
            val snippet = buildSnippet(issue)
            val category = issue.optString("category").ifBlank { "Nieznana kategoria" }
            pins += MapPin(GeoPoint(lat, lng), title, snippet, category)
        }
        return pins
    }

    private fun buildSnippet(issue: JSONObject): String {
        val category = issue.optString("category").ifBlank { "Brak kategorii" }
        val location = issue.optString("location").ifBlank { "Brak opisu lokalizacji" }
        val status = issue.optString("status").ifBlank { "Brak statusu" }
        return "$category\n$status\n$location"
    }

    private fun renderPins(pins: List<MapPin>) {
        val map = mapView ?: return
        map.overlays.clear()
        pins.forEach { pin ->
            val marker = Marker(map).apply {
                position = pin.location
                title = pin.title
                subDescription = pin.snippet
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                icon = buildCategoryMarkerIcon(pin.category)
            }
            map.overlays.add(marker)
        }

        if (pins.isEmpty()) {
            showMessage("Brak zgloszen ze wspolrzednymi do pokazania")
            map.invalidate()
            return
        }

        var minLat = Double.MAX_VALUE
        var maxLat = -Double.MAX_VALUE
        var minLon = Double.MAX_VALUE
        var maxLon = -Double.MAX_VALUE
        pins.forEach { pin ->
            minLat = minOf(minLat, pin.location.latitude)
            maxLat = maxOf(maxLat, pin.location.latitude)
            minLon = minOf(minLon, pin.location.longitude)
            maxLon = maxOf(maxLon, pin.location.longitude)
        }
        val centerPoint = GeoPoint((minLat + maxLat) / 2, (minLon + maxLon) / 2)
        map.controller.setCenter(centerPoint)
        @Suppress("DEPRECATION")
        map.controller.setZoom(calculateZoomLevel(minLat, maxLat, minLon, maxLon))
        map.invalidate()
    }

    private fun buildCategoryMarkerIcon(category: String): Drawable? {
        val base = resources.getDrawable(org.osmdroid.library.R.drawable.marker_default, null).mutate()
        val wrapped = DrawableCompat.wrap(base)
        DrawableCompat.setTint(wrapped, pinColorForCategory(category))
        return wrapped
    }

    private fun pinColorForCategory(category: String): Int {
        val normalized = category.lowercase()
        return when {
            normalized.contains("ziel") -> 0xFF2E7D32.toInt()
            normalized.contains("drog") -> 0xFFF57C00.toInt()
            normalized.contains("wandal") -> 0xFFC62828.toInt()
            normalized.contains("oswiet") -> 0xFFFDD835.toInt()
            normalized.contains("inwest") -> 0xFF1565C0.toInt()
            normalized.contains("porzad") -> 0xFF6D4C41.toInt()
            else -> 0xFF616161.toInt()
        }
    }

    private fun showMessage(message: String) {
        if (_binding == null) return
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    private fun calculateZoomLevel(
        minLat: Double,
        maxLat: Double,
        minLon: Double,
        maxLon: Double,
    ): Int {
        val latRange = maxLat - minLat
        val lonRange = maxLon - minLon

        var zoom = 15
        if (latRange > 0.01 || lonRange > 0.01) {
            zoom = 14
        }
        if (latRange > 0.05 || lonRange > 0.05) {
            zoom = 13
        }
        if (latRange > 0.1 || lonRange > 0.1) {
            zoom = 12
        }

        return maxOf(1, minOf(zoom, 18))
    }

    override fun onResume() {
        super.onResume()
        mapView?.onResume()
    }

    override fun onPause() {
        mapView?.onPause()
        super.onPause()
    }

    override fun onDestroyView() {
        mapView?.onDetach()
        super.onDestroyView()
        _binding = null
    }
}
