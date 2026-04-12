package com.example.urbanfix.data

import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object IssuesApi {

    fun getIssue(baseUrl: String, issueId: Int, viewerEmail: String): JSONObject {
        val normalized = baseUrl.trimEnd('/')
        val enc = URLEncoder.encode(viewerEmail, Charsets.UTF_8.name())
        val url = URL("$normalized/issues/$issueId?viewer_email=$enc")
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

    fun postVote(baseUrl: String, issueId: Int, userId: Int, value: Int): JSONObject {
        val normalized = baseUrl.trimEnd('/')
        val url = URL("$normalized/issues/$issueId/vote")
        val c = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 15_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
        }
        val payload = JSONObject().put("user_id", userId).put("value", value)
        return try {
            OutputStreamWriter(c.outputStream, Charsets.UTF_8).use { it.write(payload.toString()) }
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

    fun patchIssueStatus(
        baseUrl: String,
        issueId: Int,
        status: String,
        actorEmail: String,
    ): Pair<Int, String> {
        val normalized = baseUrl.trimEnd('/')
        val url = URL("$normalized/issues/$issueId")
        val c = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "PATCH"
            connectTimeout = 15_000
            readTimeout = 15_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
        }
        val payload = JSONObject()
            .put("status", status)
            .put("actor_email", actorEmail)
        return try {
            OutputStreamWriter(c.outputStream, Charsets.UTF_8).use { it.write(payload.toString()) }
            val code = c.responseCode
            val body = (if (code in 200..299) c.inputStream else c.errorStream)
                ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            code to body
        } finally {
            c.disconnect()
        }
    }
}
