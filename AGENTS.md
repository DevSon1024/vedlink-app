# Agent Rules for VedLink

These are the core development guidelines and rules for the VedLink project.

## Architecture and Patterns

- **Architecture:** Follow the MVVM (Model-View-ViewModel) architectural pattern along with Clean Architecture principles (Domain layer with Use Cases).
- **UI State Management:** ViewModels should expose UI state via `StateFlow` and handle one-off UI events (like Snackbars) via `SharedFlow`.
- **Dependency Injection:** Use Dagger Hilt for all dependency injection.

## UI / Jetpack Compose

- **Framework:** Jetpack Compose is the primary UI framework. Avoid XML layouts.
- **Material Design:** Utilize Material 3 (M3) components and styling (`androidx.compose.material3.*`).
- **Composables:** Keep composable functions focused and modular. Extract reusable UI elements into the `components` package.
- **State Hoisting:** Prefer state hoisting for UI components to keep them stateless and reusable where appropriate.

## Code Quality

- **Language:** Kotlin is the primary language.
- **Coroutines:** Use Kotlin Coroutines and Flows for all asynchronous programming. Avoid RxJava or traditional callbacks.
- **Nullable Types:** Handle nullable types safely and avoid using the `!!` operator unless absolutely necessary.

## Save After Details:

- **Save Content** in update_details.md file after every update made in the chats it can be
- Type of Details:
- error solving
- New Updtate
- More
- After latest update add '---' for ending the chat area.

_Note: You can update or add more specific rules to this file as the project evolves._
