package com.example

import Room
import com.example.utils.ServerUtils
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import java.util.concurrent.ConcurrentHashMap

val rooms = ConcurrentHashMap<Int, Room>()

fun Application.module() {
    configureSerialization()
    configureSockets()
    configureRoutes(rooms)

    routing {

        get("/create-room") {
            var createRoomId = createNewRoom(rooms)
            call.respond(mapOf("roomId" to createRoomId))
        }

        webSocket("/play/{roomId}") {

        }
    }
}
fun createNewRoom(rooms : ConcurrentHashMap<Int, Room>) : Int{
    var createRoomId : Int
    do {
        createRoomId = ServerUtils.generateUUID()
    } while(rooms.contains(createRoomId))
    val createdRoom = Room(
        roomId = createRoomId,
        player1 = null,
        player2 = null
    )
    rooms[createRoomId] = createdRoom
    return createRoomId
}