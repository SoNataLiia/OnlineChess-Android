# ♟️ Online Chess — Android / Kotlin

A real-time multiplayer chess application for Android, built with **Kotlin** and **Jetpack Compose**.

Two players can play a complete chess match from separate Android devices. One player creates a room and receives a **6-digit game code**, while the second player joins using that code. Game state and moves are synchronized in real time through **Cloud Firestore**.

> A portfolio project focused on real-time synchronization, Firebase integration, Android state management, and chess game logic.

## ✨ Features

- ♟️ Real-time multiplayer chess between two Android devices
- 🔑 Create a game room and join using a 6-digit code
- 🔐 Anonymous Firebase Authentication
- ☁️ Real-time synchronization with Cloud Firestore
- ✅ Legal move validation
- 👑 Check, checkmate and stalemate detection
- 🏰 Castling support
- 🎯 En passant support
- 🤝 Draw handling
- 🏆 Winner persistence
- 🕒 History of the latest 50 completed matches

## 🛠️ Tech Stack

- **Kotlin**
- **Jetpack Compose**
- **Firebase Authentication**
- **Cloud Firestore**
- **Kotlin Coroutines**
- **Gradle Kotlin DSL**
- **Android Studio**

## 🎮 How It Works

1. **Player 1** enters a name and creates a new game.
2. The application generates a unique **6-digit room code**.
3. **Player 2** enters a name and joins using the room code.
4. White makes the first move.
5. Every move updates the shared Firestore game state and is reflected on both devices in real time.
6. When the game finishes, the result is stored in match history.

## 🔥 Firebase Setup

To run the project with your own Firebase configuration:

1. Open the [Firebase Console](https://console.firebase.google.com/) and create a project.
2. Add an Android application with the package name:
   ```text
   com.nataliia.onlinechess
   ```
3. Download `google-services.json` and place it inside the `app/` directory.
4. In `app/build.gradle.kts`, enable the Google Services plugin if required:
   ```kotlin
   id("com.google.gms.google-services")
   ```
5. Enable **Authentication → Anonymous** in Firebase.
6. Create a **Cloud Firestore** database.
7. Publish the Firestore rules included in `firestore.rules`.
8. Open the project in Android Studio and wait for Gradle Sync to complete.
9. Run the application on two Android devices or emulators.

> `google-services.json` is intentionally excluded from this repository. Add your own Firebase configuration locally.

## 🧪 Testing Multiplayer

**Device 1:** enter a player name → select **Create New Game** → copy the generated room code.

**Device 2:** enter another player name → enter the room code → select **Join Game**.

The two clients then share the same game state through Cloud Firestore.

## 🧠 What I Practiced

This project gave me hands-on experience with:

- designing real-time multiplayer state synchronization;
- integrating Firebase services into an Android application;
- managing asynchronous operations with Kotlin Coroutines;
- implementing and validating chess rules;
- handling shared game state across independent clients;
- structuring an Android/Kotlin project for further development.

## 🚀 Future Improvements

- Move authoritative move validation to a trusted backend or Cloud Functions
- Add promotion-piece selection
- Add player-specific match history
- Add a resign-game action
- Add chess clocks and configurable time controls
- Add rematches and player profiles
- Expand automated testing for chess rules and multiplayer flows

## 👩‍💻 Author

**Nataliia Sokhatska**

Junior Java / Android Developer focused on **Java, Kotlin, backend and mobile development**.

[LinkedIn](https://www.linkedin.com/in/nataliia-sokhatska/) · [GitHub](https://github.com/SoNataLiia)

---

⭐ If you find this project interesting, feel free to explore the code and follow its development.
