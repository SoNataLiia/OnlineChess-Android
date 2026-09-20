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