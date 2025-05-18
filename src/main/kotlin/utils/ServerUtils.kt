package com.example.utils

import kotlinx.serialization.json.Json
import java.util.UUID
import kotlin.math.abs

object ServerUtils {
    fun generateUUID(): Int {
        val uuid = UUID.randomUUID()
        val hash = uuid.hashCode()
        return abs(hash)
    }

    fun jsonMessage(text: String): String {
        return """{"type":"message","text":${escapeJson(text)}}"""
    }

    private fun escapeJson(text: String): String {
        // Basic escaping; could use kotlinx.serialization or Gson if needed
        return "\"" + text.replace("\"", "\\\"") + "\""
    }

}