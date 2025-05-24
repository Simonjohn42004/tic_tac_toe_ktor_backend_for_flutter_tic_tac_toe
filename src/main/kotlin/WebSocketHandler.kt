package com.example

import com.example.utils.ServerUtils
import com.example.model.Room
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
        session.send(Frame.Text(ServerUtils.jsonMessage("room_full")))
        session.close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Room is full"))
        return
    }

    val room = rooms.compute(roomId) { _, existing ->
        when {
            existing == null -> Room(roomId, player1 = session, player2 = null)
            existing.player1 == null -> { existing.player1 = session; existing }
            existing.player2 == null -> { existing.player2 = session; existing }
            else -> existing
        }
    }

    if (room == null) {
        session.send(Frame.Text(ServerUtils.jsonMessage("room_failed")))
        session.close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Room init failed"))
        return
    }

    val joined = (room.player1 == session || room.player2 == session)
    if (!joined) {
        session.send(Frame.Text(ServerUtils.jsonMessage("room_full")))
        session.close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Room is full"))
        return
    }

    // Notify about room state
    if (room.isFull()) {
        room.sendToBoth(ServerUtils.jsonMessage("game_start"))
        room.player1?.send(Frame.Text(ServerUtils.jsonMessage("opponent_joined")))
        room.player2?.send(Frame.Text(ServerUtils.jsonMessage("opponent_joined")))
    } else {
        session.send(Frame.Text(ServerUtils.jsonMessage("waiting_opponent")))
        println("Waiting for opponent...")
    }

    println("Session: ${session.hashCode()}, player1: ${room.player1?.hashCode()}, player2: ${room.player2?.hashCode()}")

    try {
        for (frame in session.incoming) {
            if (frame is Frame.Text) {
                val text = frame.readText()
                println("Received from client: $text")
                room.broadcast(session, text)
            }
        }
    } catch (e: Exception) {
        println("Error: ${e.message}")
    } finally {
        // Clean up
        if (session == room.player1) room.player1 = null else room.player2 = null

        val remaining = if (session == room.player1) room.player2 else room.player1
        remaining?.send(Frame.Text(ServerUtils.jsonMessage("opponent_disconnected")))

        if (room.player1 == null && room.player2 == null) {
            println("Both players disconnected. Removing room $roomId")
            rooms.remove(roomId)
        }
    }
}
