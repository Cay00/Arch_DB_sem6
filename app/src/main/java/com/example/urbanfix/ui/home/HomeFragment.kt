package com.example.urbanfix.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.urbanfix.R
import com.example.urbanfix.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Podpinamy Twoje oryginalne kafelki do wspolnego formularza
        binding.tileRoadDamage.setOnClickListener {
            navigateToReport("Drogi")
        }

        binding.tileStreetLighting.setOnClickListener {
            navigateToReport("Awaria oswietlenia")
        }

        binding.tileIllegalDumping.setOnClickListener {
            navigateToReport("Nielegalne wysypisko")
        }

        binding.tileVandalism.setOnClickListener {
            navigateToReport("Akt wandalizmu")
        }
    }

    private fun navigateToReport(category: String) {
        // Przekazujemy kategorie w Bundle, co jest bezpieczniejsze niz SafeArgs przy refaktoryzacji
        val bundle = bundleOf("category" to category)
        findNavController().navigate(R.id.action_navigation_home_to_road_damage_report, bundle)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
