package com.example.urbanfix.ui.map

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.urbanfix.R
import com.example.urbanfix.databinding.FragmentMapBinding
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.ItemizedIconOverlay
import org.osmdroid.views.overlay.OverlayItem

class MapFragment : Fragment() {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!
    private var mapView: MapView? = null

    private data class MapPin(
        val location: GeoPoint,
        val title: String,
        val snippet: String,
        val isInvestment: Boolean,
    )

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
            load(
                requireContext(),
                androidx.preference.PreferenceManager.getDefaultSharedPreferences(requireContext()),
            )
        }

        mapView = binding.mapView
        mapView?.let { map ->
            // Set tile source to OpenStreetMap
            map.setTileSource(TileSourceFactory.MAPNIK)
            map.setMultiTouchControls(true)

            // Create pins
            val pins = listOf(
                MapPin(
                    GeoPoint(51.1087, 17.0319),
                    getString(R.string.map_pin_investment_legnicka),
                    getString(R.string.map_pin_investment_type),
                    isInvestment = true,
                ),
                MapPin(
                    GeoPoint(51.1079, 17.0385),
                    getString(R.string.map_pin_issue_sidewalk),
                    getString(R.string.map_pin_issue_type),
                    isInvestment = false,
                ),
                MapPin(
                    GeoPoint(51.1102, 17.0379),
                    getString(R.string.map_pin_investment_tram),
                    getString(R.string.map_pin_investment_type),
                    isInvestment = true,
                ),
                MapPin(
                    GeoPoint(51.1069, 17.0423),
                    getString(R.string.map_pin_issue_lighting),
                    getString(R.string.map_pin_issue_type),
                    isInvestment = false,
                ),
            )

            // Create overlay items
            val overlayItems = mutableListOf<OverlayItem>()
            var minLat = Double.MAX_VALUE
            var maxLat = -Double.MAX_VALUE
            var minLon = Double.MAX_VALUE
            var maxLon = -Double.MAX_VALUE

            pins.forEach { pin ->
                val overlayItem = OverlayItem(
                    pin.title,
                    pin.snippet,
                    pin.location,
                )
                overlayItems.add(overlayItem)

                // Track bounds for zoom
                minLat = minOf(minLat, pin.location.latitude)
                maxLat = maxOf(maxLat, pin.location.latitude)
                minLon = minOf(minLon, pin.location.longitude)
                maxLon = maxOf(maxLon, pin.location.longitude)
            }

            // Add overlay to map
            val overlay = ItemizedIconOverlay(overlayItems, null, requireContext())
            map.overlays.add(overlay)

            // Center and zoom to fit all pins
            if (pins.isNotEmpty()) {
                val centerLat = (minLat + maxLat) / 2
                val centerLon = (minLon + maxLon) / 2
                val centerPoint = GeoPoint(centerLat, centerLon)

                map.controller.setCenter(centerPoint)
                // Calculate appropriate zoom level
                val zoomLevel = calculateZoomLevel(
                    minLat,
                    maxLat,
                    minLon,
                    maxLon,
                    map.width,
                    map.height,
                )
                @Suppress("DEPRECATION")
                map.controller.setZoom(zoomLevel)
            } else {
                // Default center: Wrocław
                map.controller.setCenter(GeoPoint(51.1087, 17.0319))
                @Suppress("DEPRECATION")
                map.controller.setZoom(13)
            }
        }
    }

    private fun calculateZoomLevel(
        minLat: Double,
        maxLat: Double,
        minLon: Double,
        maxLon: Double,
        mapWidth: Int,
        mapHeight: Int,
    ): Int {
        val latRange = maxLat - minLat
        val lonRange = maxLon - minLon

        // Calculate appropriate zoom based on bounds
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
