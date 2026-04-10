package com.example.urbanfix.data

import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Publiczne endpointy FastAPI: `/users`, `/issues`.
 */
object BackendApi {

    private const val CONNECT_TIMEOUT_MS = 15_000
    private const val READ_TIMEOUT_MS = 15_000

    /**
     * Synchronizacja konta po Firebase: e-mail, UID Firebase, hasło (pole JSON `password_hash` — serwer robi bcrypt).
     * Sukces: 200 (aktualizacja) lub 201 (nowy); 409 przy konflikcie unikalności.
     */
    fun registerUser(baseUrl: String, email: String, password: String, firebaseUid: String?) {
        val normalized = baseUrl.trimEnd('/')
        val connection = (URL("$normalized/users").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
        }
        try {
            val body = JSONObject().put("email", email).put("password_hash", password)
            if (firebaseUid != null) {
                body.put("firebase_uid", firebaseUid)
            }
            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { it.write(body.toString()) }
            val code = connection.responseCode
            val text = readResponsePayload(connection)
            if (code !in 200..299) {
                throw IllegalStateException(
                    buildString {
                        append("HTTP ")
                        append(code)
                        if (text.isNotBlank()) {
                            append(": ")
                            append(text.take(800))
                        }
                    }
                )
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun readResponsePayload(c: HttpURLConnection): String {
        val stream = if (c.responseCode in 200..299) c.inputStream else c.errorStream
        return stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
    }
}
