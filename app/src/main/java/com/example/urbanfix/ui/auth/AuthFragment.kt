package com.example.urbanfix.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.urbanfix.R
import com.example.urbanfix.data.BackendApi
import com.example.urbanfix.databinding.FragmentAuthBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth

class AuthFragment : Fragment() {

    private var _binding: FragmentAuthBinding? = null
    private val binding get() = _binding!!

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    private var isRegisterMode: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAuthBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (auth.currentUser != null) {
            goToHome()
            return
        }
        applyModeUi()
        binding.buttonSubmit.setOnClickListener { submit() }
        binding.textSwitchMode.setOnClickListener {
            isRegisterMode = !isRegisterMode
            applyModeUi()
        }
    }

    private fun applyModeUi() {
        if (isRegisterMode) {
            binding.textAuthHeading.setText(R.string.auth_heading_register)
            binding.textAuthSubtitle.setText(R.string.auth_subtitle_register)
            binding.buttonSubmit.setText(R.string.auth_action_register)
            binding.textSwitchMode.setText(R.string.auth_switch_to_login)
        } else {
            binding.textAuthHeading.setText(R.string.auth_heading_login)
            binding.textAuthSubtitle.setText(R.string.auth_subtitle_login)
            binding.buttonSubmit.setText(R.string.auth_action_login)
            binding.textSwitchMode.setText(R.string.auth_switch_to_register)
        }
        binding.inputLayoutEmail.error = null
        binding.inputLayoutPassword.error = null
    }

    private fun submit() {
        val email = binding.editEmail.text?.toString()?.trim().orEmpty()
        val password = binding.editPassword.text?.toString().orEmpty()
        var valid = true
        if (email.isEmpty()) {
            binding.inputLayoutEmail.error = getString(R.string.auth_error_email_required)
            valid = false
        }
        if (password.isEmpty()) {
            binding.inputLayoutPassword.error = getString(R.string.auth_error_password_required)
            valid = false
        } else if (password.length < 6) {
            binding.inputLayoutPassword.error = getString(R.string.auth_error_password_short)
            valid = false
        }
        if (!valid) return

        setLoading(true)
        val task =
            if (isRegisterMode) {
                auth.createUserWithEmailAndPassword(email, password)
            } else {
                auth.signInWithEmailAndPassword(email, password)
            }
        task.addOnCompleteListener(requireActivity()) { result ->
            if (!result.isSuccessful) {
                setLoading(false)
                val message = result.exception?.localizedMessage
                    ?: getString(R.string.auth_error_generic)
                snackbar(message)
                return@addOnCompleteListener
            }
            // Zawsze zsynchronizuj z backendem: po rejestracji i po logowaniu.
            // POST /users → 201 (nowy) lub 409 (już w bazie) — naprawia brak rekordu gdy wcześniej backend nie działał.
            pushUserToBackendThenNavigate(email, password)
        }
    }

    private fun goToHome() {
        findNavController().navigate(R.id.action_navigation_auth_to_navigation_home)
    }

    private fun setLoading(loading: Boolean) {
        if (_binding == null) return
        binding.progressAuth.visibility = if (loading) View.VISIBLE else View.GONE
        binding.buttonSubmit.isEnabled = !loading
        binding.textSwitchMode.isClickable = !loading
        binding.editEmail.isEnabled = !loading
        binding.editPassword.isEnabled = !loading
    }

    private fun pushUserToBackendThenNavigate(email: String, password: String) {
        val baseUrl = requireContext().getString(R.string.backend_base_url)
        val firebaseUid = auth.currentUser?.uid
        Thread {
            val syncResult = runCatching {
                BackendApi.registerUser(baseUrl, email, password, firebaseUid)
            }
            requireActivity().runOnUiThread {
                setLoading(false)
                if (!isAdded) return@runOnUiThread
                syncResult.fold(
                    onSuccess = { goToHome() },
                    onFailure = { error ->
                        snackbar(
                            getString(
                                R.string.auth_backend_sync_failed,
                                error.message ?: error.toString()
                            )
                        )
                    },
                )
            }
        }.start()
    }

    private fun snackbar(message: String) {
        val anchor = view ?: activity?.findViewById(android.R.id.content) ?: return
        Snackbar.make(anchor, message, Snackbar.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
