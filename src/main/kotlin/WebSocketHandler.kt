package com.example

import io.ktor.websocket.close
import kotlin.text.toIntOrNull
import Room
import com.example.utils.ServerUtils
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import java.util.concurrent.ConcurrentHashMap

suspend fun handleWebSocketSession(session: DefaultWebSocketServerSession, rooms: ConcurrentHashMap<Int, Room>) {
    val roomIdParam = session.call.parameters["roomId"]

    if (roomIdParam == null) {
        session.close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Missing room ID"))
        return
    }

    val roomId = roomIdParam.toIntOrNull()

    if (roomId == null) {
        session.close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Invalid room ID"))
        return
    }

    val existingRoom = rooms[roomId]

    if (existingRoom != null && existingRoom.isFull() &&
        existingRoom.player1 != session && existingRoom.player2 != session
    ) {
        session.send(Frame.Text(ServerUtils.jsonMessage("Room is full")))
        session.close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Room is full"))
        return
    }

    println(com.example.rooms)

    val room = rooms.compute(roomId) { _, existingRoom ->
        when {
            existingRoom == null -> Room(roomId, player1 = session, player2 = null)
            existingRoom.player1 == null -> {
                existingRoom.player1 = session; existingRoom
            }

            existingRoom.player2 == null -> {
                existingRoom.player2 = session; existingRoom
            }

            else -> existingRoom // No assignment; room already full
        }
    }

    if (room == null) {
        session.send(Frame.Text(ServerUtils.jsonMessage("Room assignment failed.")))
        session.close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Room init failed"))
        return
    }

    val joined = (room.player1 == session || room.player2 == session)

    if (!joined) {
        session.send(Frame.Text(ServerUtils.jsonMessage("Room is full.")))
        session.close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Room is full"))
        return
    }

    if (room.isFull()) {
        room.sendToBoth(ServerUtils.jsonMessage("Both players connected. Game start!"))
    } else {
        session.send(Frame.Text(ServerUtils.jsonMessage("Waiting for opponent...")))
        println("Waiting for opponent...")
    }

    println("Session: ${session.hashCode()}, player1: ${room.player1?.hashCode()}, player2: ${room.player2?.hashCode()}")

    try {
        for (frame in session.incoming) {
            if (frame is Frame.Text) {
                val text = frame.readText()
                println("Received from client: $text")

                // Relay the client-formatted message as-is
                room.broadcast(session, text)
            }
        }
    } catch (e: Exception) {
        println("Error: ${e.message}")
    } finally {
        // Clean up on disconnect
        if (session == room.player1) room.player1 = null else room.player2 = null

        room.player2?.send(Frame.Text(ServerUtils.jsonMessage("Opponent disconnected")))

        if (room.player1 == null && room.player2 == null) {
            println("Connection closed")
            rooms.remove(roomId)
        }
    }
}
