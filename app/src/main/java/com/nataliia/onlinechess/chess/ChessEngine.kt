package com.nataliia.onlinechess.chess

import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Square
import com.github.bhlangonijr.chesslib.move.Move

data class MoveResult(val fen: String, val notation: String, val status: String, val winnerColor: String?)

object ChessEngine {
    fun pieceAt(fen: String, row: Int, col: Int): String {
        val board = Board().apply { loadFromFen(fen) }
        return symbol(board.getPiece(square(row, col)).name)
    }

    fun legalTargets(fen: String, row: Int, col: Int): Set<Pair<Int, Int>> {
        val board = Board().apply { loadFromFen(fen) }
        val from = square(row, col)
        return board.legalMoves().filter { it.from == from }.map { coordinates(it.to) }.toSet()
    }

    fun move(fen: String, fromRow: Int, fromCol: Int, toRow: Int, toCol: Int): MoveResult {
        val board = Board().apply { loadFromFen(fen) }
        val from = square(fromRow, fromCol)
        val to = square(toRow, toCol)
        val move: Move = board.legalMoves().firstOrNull { it.from == from && it.to == to }
            ?: error("Так ходить нельзя")
        board.doMove(move)
        val draw = board.isStaleMate || board.isInsufficientMaterial || board.isDraw
        val mate = board.isMated
        return MoveResult(board.fen, "${from.name.lowercase()}-${to.name.lowercase()}",
            when { mate -> "FINISHED"; draw -> "DRAW"; else -> "PLAYING" },
            if (mate) if (board.sideToMove.name == "WHITE") "BLACK" else "WHITE" else null)
    }

    private fun square(row: Int, col: Int): Square = Square.valueOf("${('A' + col)}${8 - row}")
    private fun coordinates(square: Square): Pair<Int, Int> {
        val s = square.name
        return (8 - s[1].digitToInt()) to (s[0] - 'A')
    }
    private fun symbol(piece: String): String = mapOf(
        "WHITE_KING" to "♔", "WHITE_QUEEN" to "♕", "WHITE_ROOK" to "♖",
        "WHITE_BISHOP" to "♗", "WHITE_KNIGHT" to "♘", "WHITE_PAWN" to "♙",
        "BLACK_KING" to "♚", "BLACK_QUEEN" to "♛", "BLACK_ROOK" to "♜",
        "BLACK_BISHOP" to "♝", "BLACK_KNIGHT" to "♞", "BLACK_PAWN" to "♟"
    )[piece].orEmpty()
}
