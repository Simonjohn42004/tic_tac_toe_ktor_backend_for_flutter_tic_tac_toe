package com.example

import Room
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import java.util.concurrent.ConcurrentHashMap

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

val rooms = ConcurrentHashMap<Int, Room>()

fun Application.module() {
    configureSerialization()
    configureSockets()
    configureRouting()
    routing {
        webSocket("/play/{roomId}") {
            val roomIdParam = call.parameters["roomId"]
            if (roomIdParam == null) {
                close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Missing room ID"))
                return@webSocket
            }

            val roomId = roomIdParam.toIntOrNull()
            if (roomId == null) {
                close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Invalid room ID"))
                return@webSocket
            }

            val room = rooms.compute(roomId) { _, existingRoom ->
                if (existingRoom == null) {
                    Room(roomId, this, null)
                } else {
                    if (existingRoom.player1 == null) {
                        existingRoom.player1 = this
                    } else if (existingRoom.player2 == null) {
                        existingRoom.player2 = this
                    }
                    existingRoom
                }
            }


            val isPlayer1 = room?.player1 == this
            val opponent = if (isPlayer1) room?.player2 else room?.player1

            if (room?.isFull() == true) {
                room.sendToBoth("Both players connected. Game start!")
            }

            try {
                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        val text = frame.readText()
                        room?.broadcast(this, text)
                    }
                }
            } catch (e: Exception) {
                println("Error: ${e.message}")
            } finally {
                // Clean up on disconnect
                if (isPlayer1) room?.player1 = null else room?.player2 = null
                opponent?.send(Frame.Text("Opponent disconnected"))

                if (room?.player1 == null && room?.player2 == null) {
                    rooms.remove(roomId)
                }
            }
        }
    }
}