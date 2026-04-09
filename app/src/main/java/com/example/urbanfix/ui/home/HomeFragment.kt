package com.example.urbanfix.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.urbanfix.R
import com.example.urbanfix.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel =
            ViewModelProvider(this).get(HomeViewModel::class.java)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textHome
        homeViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }

        binding.root.findViewById<View>(R.id.tile_road_damage)?.setOnClickListener {
            findNavController().navigate(R.id.action_navigation_home_to_road_damage_report)
        }
        binding.root.findViewById<View>(R.id.tile_street_lighting)?.setOnClickListener {
            // TODO: Otwórz ekran zgłoszenia awarii oświetlenia
        }
        binding.root.findViewById<View>(R.id.tile_illegal_dumping)?.setOnClickListener {
            // TODO: Otwórz ekran zgłoszenia nielegalnego wysypiska
        }
        binding.root.findViewById<View>(R.id.tile_vandalism)?.setOnClickListener {
            // TODO: Otwórz ekran zgłoszenia wandalizmu
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}