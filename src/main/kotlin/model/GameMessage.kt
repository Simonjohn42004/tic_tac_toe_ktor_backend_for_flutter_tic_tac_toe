package model

import kotlinx.serialization.Serializable

@Serializable
data class GameMessage(
    val index : Int
)