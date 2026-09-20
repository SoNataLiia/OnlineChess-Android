# Online Chess — Assurance Tests

This document records evidence-driven tests for distributed game-state behaviour.

The goal is not only to confirm that the application works under normal conditions,
but to verify how it behaves when requests are duplicated, delayed, conflicting,
or based on stale state.

---

## TEST-01 — Duplicate / Stale Move Rejection

### Objective

Verify that a duplicated move based on an outdated board state cannot overwrite
the newer authoritative state stored in Firestore.

### Hypothesis

If the same move is submitted twice with the same `expectedFen`:

1. The first submission should be accepted.
2. Firestore should update the authoritative game state.
3. The second submission should detect that its `expectedFen` is stale.
4. The duplicated submission should be rejected without modifying the current game state.

### Test setup

- Two Android emulator clients connected to the same online game.
- Firebase Firestore used as the authoritative game-state store.
- Room code: `465317`
- White submitted move: `g2-g4`
- A test-only replay submitted the same move a second time using the original `expectedFen`.

### Evidence

Logcat recorded the following sequence:

```text
SUBMIT attempt
TRANSACTION check
ACCEPTED | move=g2-g4
TRANSACTION check
REJECTED | stale state



---

## TEST-02 — Wrong-Turn Move Rejection

### Objective

Verify that a client cannot submit a move when the authoritative Firestore state
indicates that it is the opponent's turn.

### Hypothesis

If a client attempts to submit a move while Firestore indicates that the turn
belongs to the opponent:

1. The client may attempt to send the request.
2. The transaction should read the authoritative game state from Firestore.
3. The server-side turn check should detect that the requesting player does not own the current turn.
4. The move should be rejected without modifying the game state.

### Test setup

- Two Android emulator clients connected to the same online game.
- Firebase Firestore used as the authoritative game-state store.
- Room code: `727813`
- Firestore authoritative turn: `WHITE`
- Black client attempted to submit a move using a temporary test-only action that bypassed the normal UI turn restriction.

### Evidence

The application displayed:

```text
Не получилось
Сейчас ход соперника
```

Logcat recorded the following sequence:

```text
TEST-02 attempt
TRANSACTION check
REJECTED | wrong turn | serverTurn=WHITE
```

### Result

**PASSED**

The unauthorized wrong-turn move was rejected by the Firestore transaction.
The test demonstrates that turn ownership is verified against the authoritative
Firestore state rather than relying only on the client UI.


---

## TEST-03 — Concurrent Conflicting Move Rejection

### Objective

Verify that two conflicting moves submitted concurrently from the same initial
game state cannot both modify the authoritative Firestore state.

### Hypothesis

If two different legal moves are submitted concurrently using the same
`expectedFen`:

1. Both requests may begin from the same valid game state.
2. Firestore transactions may initially evaluate both requests.
3. Only one conflicting move should ultimately update the authoritative state.
4. The other transaction should detect the changed state and be rejected as stale.

### Test setup

- Two Android emulator clients connected to the same online game.
- Firebase Firestore used as the shared authoritative game-state store.
- Room code: `967574`
- Initial turn: `WHITE`
- Two different legal moves were submitted concurrently from the same `expectedFen`:
    - `e2-e4`
    - `d2-d4`

### Evidence

The application displayed:

```text
Не получилось
Позиция уже изменилась
```

The final synchronized game state showed:

```text
Последний ход: d2-d4
```

Logcat recorded the transaction sequence:

```text
TEST-03 conflict start

TRANSACTION check
ACCEPTED | move=d2-d4 | expectedFen matched

TRANSACTION check
ACCEPTED | move=e2-e4 | expectedFen matched

TRANSACTION check
REJECTED | stale state
```

The intermediate `ACCEPTED` log entries were produced while the transaction
callbacks were evaluating the requests. One transaction was subsequently
re-evaluated after the concurrent state change and rejected as stale.

### Result

**PASSED**

Two conflicting submissions started from the same expected game state, but only
one move became the final authoritative state. The competing transaction detected
that the position had changed and was rejected instead of overwriting the newer state.

This test provides evidence that concurrent conflicting updates converge to a
single game state rather than producing two committed moves from the same position.