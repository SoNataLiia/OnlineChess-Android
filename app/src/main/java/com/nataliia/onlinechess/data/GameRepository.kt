package com.nataliia.onlinechess.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.nataliia.onlinechess.model.Game
import com.nataliia.onlinechess.model.MatchRecord
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class GameRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    val uid: String get() = auth.currentUser?.uid.orEmpty()

    suspend fun signIn(): String = suspendCoroutine { c ->
        if (auth.currentUser != null) c.resume(auth.currentUser!!.uid)
        else auth.signInAnonymously().addOnSuccessListener { c.resume(it.user!!.uid) }
            .addOnFailureListener { c.resumeWithException(it) }
    }

    suspend fun createGame(playerName: String): Game {
        signIn()
        repeat(10) {
            val code = (100000..999999).random().toString()
            val query = db.collection("games").whereEqualTo("roomCode", code).limit(1).get().await()
            if (query.isEmpty) {
                val ref = db.collection("games").document()
                val game = Game(id = ref.id, roomCode = code, whiteId = uid, whiteName = playerName.trim())
                ref.set(game).awaitUnit()
                return game
            }
        }
        error("Не удалось создать уникальный код. Попробуйте снова")
    }

    suspend fun joinGame(code: String, playerName: String): Game {
        signIn()
        val snap = db.collection("games").whereEqualTo("roomCode", code.trim()).limit(1).get().await()
        val doc = snap.documents.firstOrNull() ?: error("Комната не найдена")
        val game = doc.toObject(Game::class.java) ?: error("Некорректная партия")
        check(game.blackId.isBlank() || game.blackId == uid) { "В комнате уже два игрока" }
        doc.reference.update(mapOf("blackId" to uid, "blackName" to playerName.trim(), "status" to "PLAYING")).awaitUnit()
        return game.copy(blackId = uid, blackName = playerName.trim(), status = "PLAYING")
    }

    fun observeGame(id: String, onUpdate: (Game?) -> Unit, onError: (Throwable) -> Unit): ListenerRegistration =
        db.collection("games").document(id).addSnapshotListener { snapshot, error ->
            if (error != null) onError(error) else onUpdate(snapshot?.toObject(Game::class.java))
        }

    suspend fun submitMove(game: Game, expectedFen: String, newFen: String, move: String, status: String, winnerId: String) {
        val ref = db.collection("games").document(game.id)
        db.runTransaction { tx ->
            val current = tx.get(ref).toObject(Game::class.java) ?: error("Партия удалена")
            check(current.fen == expectedFen) { "Позиция уже изменилась" }
            check(current.status == "PLAYING") { "Партия не активна" }
            val myTurn = (current.turn == "WHITE" && current.whiteId == uid) || (current.turn == "BLACK" && current.blackId == uid)
            check(myTurn) { "Сейчас ход соперника" }
            val nextTurn = if (current.turn == "WHITE") "BLACK" else "WHITE"
            tx.update(ref, mapOf(
                "fen" to newFen, "lastMove" to move, "turn" to nextTurn,
                "status" to status, "winnerId" to winnerId,
                "finishedAt" to if (status == "FINISHED") System.currentTimeMillis() else null
            ))
        }.awaitUnit()
    }

    fun observeHistory(onUpdate: (List<MatchRecord>) -> Unit, onError: (Throwable) -> Unit): ListenerRegistration =
        db.collection("games").whereIn("status", listOf("FINISHED", "DRAW")).limit(50)
            .addSnapshotListener { snap, error ->
                if (error != null) onError(error) else onUpdate(snap?.documents.orEmpty().mapNotNull { d ->
                    val g = d.toObject(Game::class.java) ?: return@mapNotNull null
                    MatchRecord(g.id, g.whiteName, g.blackName,
                        when { g.status == "DRAW" -> "Ничья"; g.winnerId == g.whiteId -> "1–0"; else -> "0–1" },
                        g.finishedAt ?: 0L)
                }.sortedByDescending { it.finishedAt })
            }
}

private suspend fun <T> com.google.android.gms.tasks.Task<T>.await(): T = suspendCoroutine { c ->
    addOnSuccessListener { c.resume(it) }.addOnFailureListener { c.resumeWithException(it) }
}
private suspend fun <T> com.google.android.gms.tasks.Task<T>.awaitUnit(): Unit =
    suspendCoroutine { continuation ->
        addOnSuccessListener {
            continuation.resume(Unit)
        }

        addOnFailureListener { exception ->
            continuation.resumeWithException(exception)
        }
    }
