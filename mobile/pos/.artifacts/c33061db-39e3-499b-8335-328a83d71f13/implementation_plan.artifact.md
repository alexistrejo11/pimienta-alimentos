# Implementation Plan - Fix stuck mode switch in Sandbox

The user is stuck in Sandbox mode because an open shift prevents mode switching, and the error message ("Cierra el turno antes de cambiar de modo") is not visible in the `Sale` screen, leading to a silent failure.

## User Review Required

- In **Sandbox/Playground** mode, we will now allow switching to Production even if a shift is open. This prevents "ghost" shifts from locking the user out of Production in development/training environments.
- The `notice` error message will now be displayed at the top of the `Sale` screen as a red banner if it is set.

## Proposed Changes

### MainActivity

#### [MODIFY] [MainActivity.kt](file:///D:/code/pimienta_alimentos/mobile/pos/app/src/main/java/io/github/alexistrejo/pimienta/pos/MainActivity.kt)

- Update `switchMode` to only block switching if the current mode is `PRODUCTION`.
- Update `resetTrainingDemo` to allow resetting even if a shift is open.
- Add a global error banner in `PosApp` root `Column` to display `notice` when a shift is active (Sale screen).

## Verification Plan

### Automated Tests
- N/A (UI and logic flow fix)

### Manual Verification
1. Enter Sandbox mode.
2. Open a shift.
3. Try to switch to Production mode via `RuntimeModeBanner`.
4. Verify that the switch is successful (or that the error is visible if it still blocks for some reason).
5. Verify that "Reiniciar datos demo" works even with an open shift in Sandbox.
