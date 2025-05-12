package com.example

import io.ktor.websocket.close
import kotlin.text.toIntOrNull
import Room
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

    if(roomId == null){
        session.close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Invalid room ID"))
        return
    }

    val existingRoom = rooms[roomId]


    if (existingRoom != null && existingRoom.isFull() &&
        existingRoom.player1 != session && existingRoom.player2 != session) {
        session.send(Frame.Text("Room is full"))
        session.close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Room is full"))
        return
    }


    val room = rooms.compute(roomId) { _, existingRoom ->
        if (existingRoom == null) {
            Room(roomId, session, null)
        } else {
            if (existingRoom.player1 == null) {
                existingRoom.player1 = session
            } else if (existingRoom.player2 == null) {
                existingRoom.player2 = session
            }
            existingRoom
        }
    }


    val isPlayer1 = room?.player1 == session
    val opponent = if (isPlayer1) room.player2 else room?.player1

    if (room?.isFull() == true) {
        room.sendToBoth("Both players connected. Game start!")
    }

    try {
        for (frame in session.incoming) {
            if (frame is Frame.Text) {
                val text = frame.readText()
                room?.broadcast(session, text)
            }
        }
    } catch (e: Exception) {
        println("Error: ${e.message}")
    } finally {
        // Clean up on disconnect
        if (isPlayer1) room.player1 = null else room?.player2 = null
        opponent?.send(Frame.Text("Opponent disconnected"))

        if (room?.player1 == null && room?.player2 == null) {
            rooms.remove(roomId)
        }
    }
}
