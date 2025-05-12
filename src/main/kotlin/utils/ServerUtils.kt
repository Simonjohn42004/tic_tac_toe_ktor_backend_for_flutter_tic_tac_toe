package com.example.utils

import java.util.UUID
import kotlin.math.abs

object ServerUtils {
    fun generateUUID(): Int {
        val uuid = UUID.randomUUID()
        val hash = uuid.hashCode()
        return abs(hash)
    }
}