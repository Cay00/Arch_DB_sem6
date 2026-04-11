package com.example.urbanfix.data

import org.json.JSONObject

object BackendUserJson {

    /** Pełna nazwa z pól `first_name` + `last_name`, w razie pustki z `display_name`. */
    fun displayNameFromUser(obj: JSONObject): String {
        val fn = obj.optString("first_name", "").trim()
        val ln = obj.optString("last_name", "").trim()
        val parts = listOf(fn, ln).filter { it.isNotEmpty() }
        if (parts.isNotEmpty()) return parts.joinToString(" ")
        val dn = obj.optString("display_name", "").trim()
        return dn
    }
}
