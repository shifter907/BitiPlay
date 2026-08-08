# Project Handover Summary - BitiPlay World

This document summarizes recent architectural fixes and configuration changes made to the project to ensure a smooth transition for subsequent development.

## 1. Critical Fixes

### Character Interaction while Riding (`Character.kt`)
*   **Issue**: When a character was riding a vehicle (bike, car, etc.) and the player tapped a world object (e.g., a grill or animal), the vehicle would drive to the object, but the interaction (the `onUse` callback) would never fire because it wasn't being tracked at the character level.
*   **Fix**: 
    *   Updated `Character.goTo(...)` to store `targetX` and the `arrive` callback even if `riding != null`.
    *   Updated the riding logic in `Character.update(...)` to detect when the vehicle has reached its destination (`v.targetX == null`). Upon arrival, it now teleports the character to the precise `targetX`, clears the target, and invokes the `arrive` callback, allowing the interaction to proceed.

## 2. Build & Environment

### Access Denied Build Failure
*   **Issue**: Gradle builds were failing with `java.nio.file.AccessDeniedException` on the `app/build/intermediates` folder. This is common on Windows when a process (likely a hung Gradle daemon or the app process) locks files.
*   **Resolution**: Forcefully deleted the `app/build` directory using shell commands. 
*   **Tip for next AI**: If builds fail with permission errors, run `.\gradlew.bat --stop` and manually wipe the `app/build` folder.

### SDK Version Downgrade
*   **Change**: Modified `app/build.gradle.kts` to set `compileSdk` and `targetSdk` to **35** (Android 15 Stable).
*   **Reason**: The project was previously targeting **36** (Android 16 Preview), which was causing some instability in the desugaring and dexing tasks on certain toolchains.

## 3. Minor Maintenance
*   **`Game.kt`**: Cleaned up minor IDE warnings (missing trailing commas in `scenes` list and added clarifying parentheses in touch handling logic).

## Current Project State
*   **Build Status**: Passing (`:app:assembleDebug`).
*   **Deployment**: Functional on landscape Android devices (API 24+).
*   **Key Files to Watch**:
    *   `GameView.kt`: Owns the render thread and input synchronization.
    *   `Game.kt`: Main logic hub for scene switching and input routing.
    *   `engine/Render.kt`: Custom procedural drawing engine logic.
    *   `ent/Character.kt`: Character state machine and interaction logic.
