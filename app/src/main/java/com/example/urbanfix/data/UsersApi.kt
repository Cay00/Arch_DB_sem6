package com.example.urbanfix.data

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object UsersApi {

    fun getUserByEmail(baseUrl: String, email: String): JSONObject {
        val normalized = baseUrl.trimEnd('/')
        val enc = URLEncoder.encode(email, Charsets.UTF_8.name())
        val url = URL("$normalized/users/by-email?email=$enc")
        val c = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 15_000
        }
        return try {
            val code = c.responseCode
            val body = (if (code in 200..299) c.inputStream else c.errorStream)
                ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (code != HttpURLConnection.HTTP_OK) {
                error("HTTP $code: ${body.take(400)}")
            }
            JSONObject(body)
        } finally {
            c.disconnect()
        }
    }
}
