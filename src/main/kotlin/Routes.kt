package com.example

import Room
import com.example.utils.ServerUtils
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import java.util.concurrent.ConcurrentHashMap

fun Application.configureRoutes(rooms: ConcurrentHashMap<Int, Room>) {
    routing {
        get("/create-room") {
            val roomId = generateUniqueRoomId(rooms)
            rooms[roomId] = Room(roomId, null, null)
            call.respond(mapOf("roomId" to roomId))
        }

        webSocket("/play/{roomId}") {
            handleWebSocketSession(this, rooms)
        }
    }
}

private fun generateUniqueRoomId(rooms: ConcurrentHashMap<Int, Room>): Int {
    var roomId: Int
    do {
        roomId = ServerUtils.generateUUID()
    } while (rooms.contains(roomId))
    return roomId
}
