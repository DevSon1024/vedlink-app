# AI Agent Instructions for VedLink Development

This document serves as the absolute source of truth for any AI agent or LLM assisting with the development of the **VedLink** project (`com.devson.vedlink`). You must strictly adhere to these rules, architectural guidelines, security constraints, and developer preferences before generating or modifying any code.

---

## 1. Core Development Philosophy

- **Goal:** VedLink is a high-performance, offline-first, native Android link management and Link-First Note application built with Kotlin, Jetpack Compose, Room Database, Dagger Hilt, Coroutines/Flow, and Material Design 3.
- **Greeting & Verification:** To verify you are not hallucinating and are actively reading these project instructions, start every conversation or reply with the words **'Hey Devson'**.
- **No Hallucinations:** Only use existing APIs, classes, entities, use cases, and packages within the project. If unsure about an existing implementation or database schema, inspect the files before assuming.
- **One Line Explanation / Directness:** Do not explain things in overly long phrases and paragraphs. Be short, direct, and precise. Avoid unnecessary filler or unsolicited architecture essays.
- **Zero Crash Tolerance:** Ensure the app never crashes under any network, metadata scraping, or data conditions. Always prioritize graceful degradation (e.g., fallback cards, retry mechanisms, empty states) over throwing unhandled exceptions.
- **Production Realism:** Never display "demo", "demo session", or mock labels in user-facing UI screens, snackbars, or dialogs.

---

## 2. Strict Security: API Keys & Secrets Protection

- **Zero Hardcoded Secrets:** NEVER hardcode real API keys, secrets, access tokens, or sensitive credentials in the Android codebase.
- **No Secrets Behind Fallback Operators:** Never use real production secrets as hardcoded fallbacks (e.g., after `?:`). Secret fallbacks must strictly default to empty strings `""` or `null`.
- **Environment & Build Configuration:** All sensitive secrets or external API tokens must be loaded via `BuildConfig`, Gradle properties (`local.properties`), or encrypted DataStore/Keystore.
- **Git Protection:** Never commit `.env`, `keystore.properties`, or credentials to source control.

---

## 3. UI / Jetpack Compose & Material 3 Guidelines

- **Mobile-First & Touch-First Design:** Prioritize touch targets (minimum 48x48 dp / 44x44 dp), responsive edge-to-edge layouts, and safe area insets (`WindowInsets`, `statusBars`, `navigationBars`) across all Android screen sizes, foldables, and aspect ratios.
- **Material Design 3 (M3):** Exclusively utilize Material 3 components and styling (`androidx.compose.material3.*`). Avoid legacy XML layouts.
- **Keyboard & IME Resilience:** All forms and scrollable views must handle keyboard popups smoothly using `Modifier.imePadding()` and `android:windowSoftInputMode="adjustResize"`.
- **Input Field Fluidity:** Text fields must allow smooth character navigation. Never trap selection or automatically highlight the entire text on tap without collapsing to the cursor offset.
- **State Hoisting & Clean Architecture:** Keep composable functions focused, modular, and stateless where appropriate. Extract reusable UI elements into the `components` package. Never perform heavy transformations, disk I/O, or database queries inside Composable functions.

---

## 4. Code Quality & Performance Optimization

- **Architecture:** Follow the MVVM (Model-View-ViewModel) architectural pattern along with Clean Architecture principles (Domain layer with Use Cases, Repository abstractions).
- **UI State Management:** ViewModels should expose UI state via `StateFlow` and handle one-off UI events (like Snackbars) via `SharedFlow`.
- **Dependency Injection:** Use Dagger Hilt for all dependency injection (`@HiltViewModel`, `@AndroidEntryPoint`, `@Inject`).
- **Language:** Kotlin with strict sound null safety. Handle nullable types safely and **never** use the double-bang assertion operator (`!!`) blindly.
- **Asynchronous Architecture:** Use Kotlin Coroutines and Flows for all asynchronous programming. Avoid RxJava or traditional callbacks. Run database, network, and scraping tasks strictly on `Dispatchers.IO` or `Dispatchers.Default`.
- **Eliminate Bottlenecks:**
  - Never block the main (UI) thread with heavy JSON decoding, Jsoup parsing, or database queries.
  - Optimize list scrolling using cached images (Coil with memory/disk caching) and virtualized lazy builders (`LazyColumn`, `LazyVerticalStaggeredGrid`).
- **Code Writing Format:** Do NOT add `─` anywhere in code files.

---

## 5. Documentation & Update Tracking

You must actively maintain the project's changelog. After every completed task, error resolution, or feature update, append an entry to the `update_details.md` file.

**Format and Rules for `update_details.md`:**
- Do NOT rewrite or overwrite the whole file. Simply append the new entry at the very end of the document.
- Include a Date and Time stamp for the update (`[YYYY-MM-DD HH:MM]`).
- Document using the following template:

```markdown
### [YYYY-MM-DD HH:MM] <Feature or Fix Title>
- **Issue / Goal:** (Briefly describe the exact issue, bottleneck, or feature addressed)
- **Type:** (Error Solving, Bug Fix, UI, Performance, Security, Architecture, New Update, More)
- **Solution:** (Explain how the issue was solved. Maximum 10 lines.)
---
```
- Append exactly `---` on a new line after each entry.
- Do not include conversational filler in `update_details.md`.

---

## 6. Version Control (Git) Protocol

- **Do not commit or push** any changes to the repository until explicitly requested to do so by the developer.
- **Do not Change .gitignore:** Never modify `.gitignore` automatically. Leave all `.gitignore` files as they are.
