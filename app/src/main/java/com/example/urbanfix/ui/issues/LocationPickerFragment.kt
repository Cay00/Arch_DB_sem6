package com.example.urbanfix.ui.issues

import android.location.Geocoder
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import com.example.urbanfix.databinding.FragmentLocationPickerBinding
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.MapEventsOverlay
import java.util.Locale

class LocationPickerFragment : Fragment() {
    private var _binding: FragmentLocationPickerBinding? = null
    private val binding get() = _binding!!
    private var mapView: MapView? = null
    private var marker: Marker? = null
    private var selectedPoint: GeoPoint? = null
    private val polandBounds = BoundingBox(54.9, 24.2, 49.0, 14.1)
    private val geocoder by lazy { Geocoder(requireContext(), Locale.forLanguageTag("pl-PL")) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentLocationPickerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Configuration.getInstance().apply {
            // Required by OSM tile servers; without User-Agent tiles may be blocked.
            userAgentValue = requireContext().packageName
            load(
                requireContext(),
                PreferenceManager.getDefaultSharedPreferences(requireContext()),
            )
        }
        mapView = binding.mapViewPicker
        mapView?.let { map ->
            map.setTileSource(TileSourceFactory.MAPNIK)
            map.setMultiTouchControls(true)
            map.setScrollableAreaLimitDouble(polandBounds)
            map.minZoomLevel = 5.5
            map.controller.setCenter(GeoPoint(51.1087, 17.0319))
            @Suppress("DEPRECATION")
            map.controller.setZoom(13)
            map.overlays.add(
                MapEventsOverlay(
                    object : MapEventsReceiver {
                        override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                            selectPoint(p)
                            return true
                        }

                        override fun longPressHelper(p: GeoPoint): Boolean = false
                    },
                ),
            )
        }
        binding.buttonConfirmMapLocation.setOnClickListener { confirmSelection() }
    }

    private fun selectPoint(point: GeoPoint) {
        selectedPoint = point
        val map = mapView ?: return
        val mapMarker = marker ?: Marker(map).also {
            it.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            map.overlays.add(it)
            marker = it
        }
        mapMarker.position = point
        map.invalidate()
        binding.buttonConfirmMapLocation.isEnabled = true
    }

    private fun confirmSelection() {
        val point = selectedPoint ?: return
        resolveAddress(point.latitude, point.longitude) { address ->
            parentFragmentManager.setFragmentResult(
                REPORT_MAP_PICKER_RESULT,
                Bundle().apply {
                    putDouble(REPORT_MAP_PICKER_LAT, point.latitude)
                    putDouble(REPORT_MAP_PICKER_LNG, point.longitude)
                    putString(REPORT_MAP_PICKER_ADDRESS, address)
                },
            )
            findNavController().navigateUp()
        }
    }

    private fun resolveAddress(lat: Double, lng: Double, onResult: (String) -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            geocoder.getFromLocation(lat, lng, 1) { addresses ->
                val text = addresses.firstOrNull()?.getAddressLine(0).orEmpty()
                activity?.runOnUiThread { onResult(text) }
            }
            return
        }
        Thread {
            val text = runCatching {
                @Suppress("DEPRECATION")
                geocoder.getFromLocation(lat, lng, 1)?.firstOrNull()?.getAddressLine(0).orEmpty()
            }.getOrDefault("")
            activity?.runOnUiThread { onResult(text) }
        }.start()
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
        mapView = null
        marker = null
        _binding = null
        super.onDestroyView()
    }
}

const val REPORT_MAP_PICKER_RESULT = "report_map_picker_result"
const val REPORT_MAP_PICKER_LAT = "report_map_picker_lat"
const val REPORT_MAP_PICKER_LNG = "report_map_picker_lng"
const val REPORT_MAP_PICKER_ADDRESS = "report_map_picker_address"
