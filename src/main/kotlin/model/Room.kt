import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.websocket.Frame

data class Room(
    val roomId: Int,
    var player1: DefaultWebSocketServerSession?,
    var player2: DefaultWebSocketServerSession?
) {
    fun isFull(): Boolean = player1 != null && player2 != null

    suspend fun broadcast(sender: DefaultWebSocketServerSession, message: String) {
        if (sender == player1 && player2 != null) {
            player2!!.send(Frame.Text(message))
        } else if (sender == player2 && player1 != null) {
            player1!!.send(Frame.Text(message))
        } else {
            println(player1)
            println(player2)
            sendToBoth("Network Connection Error! Please Create a New Room")
        }
    }


    suspend fun sendToBoth(message: String) {
        player1?.send(Frame.Text(message))
        player2?.send(Frame.Text(message))
    }
}
