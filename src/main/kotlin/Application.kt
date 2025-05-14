package com.example

import Room
import io.ktor.server.application.*
import java.util.concurrent.ConcurrentHashMap

val rooms = ConcurrentHashMap<Int, Room>()

fun Application.module() {
    configureSerialization()
    configureSockets()
    configureRoutes(rooms)

}