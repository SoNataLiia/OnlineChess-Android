package com.nataliia.onlinechess

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.FirebaseApp
import com.nataliia.onlinechess.chess.ChessEngine
import com.nataliia.onlinechess.data.GameRepository
import com.nataliia.onlinechess.model.Game
import com.nataliia.onlinechess.model.MatchRecord
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FirebaseApp.initializeApp(this)
        enableEdgeToEdge()

        setContent {
            ChessTheme {
                OnlineChessApp()
            }
        }
    }
}

enum class Screen {
    LOBBY,
    GAME,
    HISTORY
}

class ChessViewModel : ViewModel() {

    private val repository = GameRepository()

    var screen by mutableStateOf(Screen.LOBBY)
        private set

    var game by mutableStateOf<Game?>(null)
        private set

    var history by mutableStateOf(emptyList<MatchRecord>())
        private set

    var busy by mutableStateOf(false)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    private var gameListener:
            com.google.firebase.firestore.ListenerRegistration? = null

    private var historyListener:
            com.google.firebase.firestore.ListenerRegistration? = null

    fun create(name: String) {
        launchAction {
            require(name.isNotBlank()) {
                "Введите имя"
            }

            val newGame = repository.createGame(name)
            openGame(newGame)
        }
    }

    fun join(name: String, code: String) {
        launchAction {
            require(name.isNotBlank()) {
                "Введите имя"
            }

            require(code.length == 6) {
                "Код должен содержать 6 цифр"
            }

            val joinedGame = repository.joinGame(code, name)
            openGame(joinedGame)
        }
    }

    private fun openGame(initialGame: Game) {
        game = initialGame
        screen = Screen.GAME

        gameListener?.remove()

        gameListener = repository.observeGame(
            id = initialGame.id,
            onUpdate = { updatedGame ->
                if (updatedGame != null) {
                    game = updatedGame
                }
            },
            onError = ::showError
        )
    }

    fun makeMove(
        from: Pair<Int, Int>,
        to: Pair<Int, Int>
    ) {
        launchAction {
            val currentGame = requireNotNull(game)

            val result = ChessEngine.move(
                fen = currentGame.fen,
                fromRow = from.first,
                fromCol = from.second,
                toRow = to.first,
                toCol = to.second
            )

            val winnerId = when (result.winnerColor) {
                "WHITE" -> currentGame.whiteId
                "BLACK" -> currentGame.blackId
                else -> ""
            }

            repository.submitMove(
                game = currentGame,
                expectedFen = currentGame.fen,
                newFen = result.fen,
                move = result.notation,
                status = result.status,
                winnerId = winnerId
            )
        }
    }

    fun showHistory() {
        screen = Screen.HISTORY

        historyListener?.remove()

        historyListener = repository.observeHistory(
            onUpdate = { records ->
                history = records
            },
            onError = ::showError
        )
    }

    fun backToLobby() {
        screen = Screen.LOBBY
        gameListener?.remove()
        game = null
    }

    fun backToGameOrLobby() {
        screen = if (game == null) {
            Screen.LOBBY
        } else {
            Screen.GAME
        }
    }

    fun dismissError() {
        error = null
    }

    fun myUid(): String {
        return repository.uid
    }

    private fun launchAction(
        block: suspend () -> Unit
    ) {
        viewModelScope.launch {
            busy = true

            try {
                block()
            } catch (throwable: Throwable) {
                showError(throwable)
            } finally {
                busy = false
            }
        }
    }

    private fun showError(throwable: Throwable) {
        error = throwable.message ?: "Произошла ошибка"
    }

    override fun onCleared() {
        gameListener?.remove()
        historyListener?.remove()
        super.onCleared()
    }
}

@Composable
private fun OnlineChessApp(
    viewModel: ChessViewModel = viewModel()
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFF7F3EA)
    ) {
        when (viewModel.screen) {
            Screen.LOBBY -> {
                LobbyScreen(viewModel)
            }

            Screen.GAME -> {
                viewModel.game?.let { currentGame ->
                    GameScreen(
                        game = currentGame,
                        uid = viewModel.myUid(),
                        onMove = viewModel::makeMove,
                        onExit = viewModel::backToLobby
                    )
                }
            }

            Screen.HISTORY -> {
                HistoryScreen(
                    records = viewModel.history,
                    onBack = viewModel::backToGameOrLobby
                )
            }
        }
    }

    viewModel.error?.let { errorMessage ->
        AlertDialog(
            onDismissRequest = viewModel::dismissError,
            title = {
                Text("Не получилось")
            },
            text = {
                Text(errorMessage)
            },
            confirmButton = {
                TextButton(
                    onClick = viewModel::dismissError
                ) {
                    Text("Понятно")
                }
            }
        )
    }
}

@Composable
private fun LobbyScreen(
    viewModel: ChessViewModel
) {
    var name by remember {
        mutableStateOf("")
    }

    var roomCode by remember {
        mutableStateOf("")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "ONLINE CHESS",
            fontSize = 30.sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFF254A3A)
        )

        Text(
            text = "Сыграйте партию с другом на двух телефонах",
            color = Color.Gray
        )

        Spacer(
            modifier = Modifier.height(28.dp)
        )

        OutlinedTextField(
            value = name,
            onValueChange = {
                name = it.take(24)
            },
            label = {
                Text("Ваше имя")
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        Button(
            onClick = {
                viewModel.create(name)
            },
            enabled = !viewModel.busy,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Создать новую игру")
        }

        Row(
            modifier = Modifier.padding(vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(
                modifier = Modifier.weight(1f)
            )

            Text(
                text = "  или  ",
                color = Color.Gray
            )

            HorizontalDivider(
                modifier = Modifier.weight(1f)
            )
        }

        OutlinedTextField(
            value = roomCode,
            onValueChange = {
                roomCode = it
                    .filter(Char::isDigit)
                    .take(6)
            },
            label = {
                Text("Код комнаты")
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        OutlinedButton(
            onClick = {
                viewModel.join(name, roomCode)
            },
            enabled = !viewModel.busy,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Подключиться")
        }

        TextButton(
            onClick = viewModel::showHistory,
            modifier = Modifier.align(
                Alignment.CenterHorizontally
            )
        ) {
            Text("История матчей")
        }

        if (viewModel.busy) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun GameScreen(
    game: Game,
    uid: String,
    onMove: (
        Pair<Int, Int>,
        Pair<Int, Int>
    ) -> Unit,
    onExit: () -> Unit
) {
    var selectedSquare by remember(game.fen) {
        mutableStateOf<Pair<Int, Int>?>(null)
    }

    val legalSquares = remember(
        game.fen,
        selectedSquare
    ) {
        selectedSquare?.let { selected ->
            ChessEngine.legalTargets(
                fen = game.fen,
                row = selected.first,
                col = selected.second
            )
        }.orEmpty()
    }

    val myColor = if (uid == game.whiteId) {
        "WHITE"
    } else {
        "BLACK"
    }

    val myTurn =
        game.status == "PLAYING" &&
                game.turn == myColor

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = onExit
            ) {
                Text("← Выйти")
            }

            Text(
                text = "Код: ${game.roomCode}",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        }

        PlayerCard(
            name = game.blackName.ifBlank {
                "Ждём второго игрока…"
            },
            colorName = "Чёрные",
            active = game.turn == "BLACK"
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
        ) {
            val cellSize = maxWidth / 8

            Column {
                repeat(8) { row ->
                    Row {
                        repeat(8) { column ->
                            val square = row to column
                            val isLight =
                                (row + column) % 2 == 0

                            val backgroundColor = when {
                                square == selectedSquare ->
                                    Color(0xFFF1C453)

                                square in legalSquares ->
                                    Color(0xFF8EBF75)

                                isLight ->
                                    Color(0xFFF0D9B5)

                                else ->
                                    Color(0xFF789461)
                            }

                            Box(
                                modifier = Modifier
                                    .size(cellSize)
                                    .background(backgroundColor)
                                    .clickable(enabled = myTurn) {
                                        when {
                                            selectedSquare == null -> {
                                                val piece =
                                                    ChessEngine.pieceAt(
                                                        fen = game.fen,
                                                        row = row,
                                                        col = column
                                                    )

                                                if (piece.isNotEmpty()) {
                                                    selectedSquare = square
                                                }
                                            }

                                            square in legalSquares -> {
                                                onMove(
                                                    selectedSquare!!,
                                                    square
                                                )

                                                selectedSquare = null
                                            }

                                            else -> {
                                                selectedSquare = square
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = ChessEngine.pieceAt(
                                        fen = game.fen,
                                        row = row,
                                        col = column
                                    ),
                                    fontSize = 34.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        PlayerCard(
            name = game.whiteName,
            colorName = "Белые",
            active = game.turn == "WHITE"
        )

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        val message = when (game.status) {
            "WAITING" -> {
                "Передайте код другу"
            }

            "FINISHED" -> {
                val winnerName =
                    if (game.winnerId == game.whiteId) {
                        game.whiteName
                    } else {
                        game.blackName
                    }

                "Мат! Победитель: $winnerName"
            }

            "DRAW" -> {
                "Партия завершилась вничью"
            }

            else -> {
                if (myTurn) {
                    "Ваш ход"
                } else {
                    "Ход соперника"
                }
            }
        }

        Text(
            text = message,
            fontWeight = FontWeight.Bold
        )

        if (game.lastMove.isNotBlank()) {
            Text(
                text = "Последний ход: ${game.lastMove}",
                color = Color.Gray
            )
        }
    }
}

@Composable
private fun PlayerCard(
    name: String,
    colorName: String,
    active: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (active) {
                Color(0xFFDDEBDD)
            } else {
                Color.White
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = name,
                fontWeight = FontWeight.Bold
            )

            Text(colorName)
        }
    }
}

@Composable
private fun HistoryScreen(
    records: List<MatchRecord>,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        TextButton(
            onClick = onBack
        ) {
            Text("← Назад")
        }

        Text(
            text = "История матчей",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        if (records.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Завершённых матчей пока нет")
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(
                    items = records,
                    key = {
                        it.gameId
                    }
                ) { match ->
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement =
                                Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text =
                                        "${match.whiteName} — ${match.blackName}",
                                    fontWeight = FontWeight.Bold
                                )

                                Text(
                                    text = SimpleDateFormat(
                                        "dd.MM.yyyy HH:mm",
                                        Locale.getDefault()
                                    ).format(
                                        Date(match.finishedAt)
                                    ),
                                    color = Color.Gray
                                )
                            }

                            Text(
                                text = match.result,
                                fontWeight = FontWeight.Black,
                                fontSize = 20.sp,
                                textAlign = TextAlign.End
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChessTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFF3D6652),
            secondary = Color(0xFFB78628)
        ),
        content = content
    )
}