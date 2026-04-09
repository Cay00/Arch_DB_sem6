package com.example.urbanfix.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.urbanfix.R
import com.example.urbanfix.databinding.FragmentAuthBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class AuthFragment : Fragment() {

    private var _binding: FragmentAuthBinding? = null
    private val binding get() = _binding!!

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val backendBaseUrl = "http://10.0.2.2:8000"

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
            setLoading(false)
            if (result.isSuccessful) {
                if (isRegisterMode) {
                    syncUserWithBackend(email, password)
                }
                goToHome()
            } else {
                val message = result.exception?.localizedMessage
                    ?: getString(R.string.auth_error_generic)
                Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun goToHome() {
        findNavController().navigate(R.id.action_navigation_auth_to_navigation_home)
    }

    private fun setLoading(loading: Boolean) {
        binding.progressAuth.visibility = if (loading) View.VISIBLE else View.GONE
        binding.buttonSubmit.isEnabled = !loading
        binding.textSwitchMode.isClickable = !loading
        binding.editEmail.isEnabled = !loading
        binding.editPassword.isEnabled = !loading
    }

    private fun syncUserWithBackend(email: String, password: String) {
        Thread {
            runCatching {
                val url = URL("$backendBaseUrl/users")
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 5000
                    readTimeout = 5000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                }
                val body = JSONObject()
                    .put("email", email)
                    .put("password_hash", password)
                    .toString()
                OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                    writer.write(body)
                }
                val code = connection.responseCode
                connection.disconnect()
                if (code != HttpURLConnection.HTTP_CREATED && code != HttpURLConnection.HTTP_CONFLICT) {
                    throw IllegalStateException("Backend registration failed with status: $code")
                }
            }.onFailure { error ->
                requireActivity().runOnUiThread {
                    Snackbar.make(
                        binding.root,
                        "Firebase OK, ale zapis do backendu nieudany: ${error.message}",
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
        }.start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
