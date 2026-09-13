package com.nataliia.onlinechess.model

data class Game(
    val id: String = "",
    val roomCode: String = "",
    val whiteId: String = "",
    val whiteName: String = "",
    val blackId: String = "",
    val blackName: String = "",
    val fen: String = START_FEN,
    val turn: String = "WHITE",
    val status: String = "WAITING",
    val winnerId: String = "",
    val lastMove: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val finishedAt: Long? = null
) {
    companion object { const val START_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1" }
}

data class MatchRecord(
    val gameId: String = "",
    val whiteName: String = "",
    val blackName: String = "",
    val result: String = "",
    val finishedAt: Long = 0L
)
