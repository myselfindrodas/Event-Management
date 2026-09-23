# Event Manager

Kotlin Android client for personal event scheduling and review. XML UI on Material 3, Clean Architecture with Hilt, Firebase Auth / Firestore / FCM.

This README is the working reference for structure, data contracts, Firebase wiring, build, and the parts that usually break in the field.

**Start here if you are setting up a machine:** [§9 Setup instructions](#9-setup-instructions) → [§10 Firebase configuration](#10-firebase-configuration).

| | |
|---|---|
| Application id | `com.example.eventmanagement` |
| minSdk / targetSdk / compileSdk | 24 / 35 / 35 |
| Language | Kotlin |
| UI | XML, View Binding, Material 3 |
| Architecture | Clean Architecture + MVVM + Hilt |
| JDK | 17 (required by AGP 8.x) |
| Backend | Firebase Auth, Cloud Firestore, FCM |
| Charting | MPAndroidChart (`v3.1.0` via JitPack) |
| Navigation | Jetpack Navigation Component |
| Time APIs | `java.time` via core library desugaring |

---

## 1. Purpose

Ship a production-shaped client that can:

- Authenticate with email/password and recover accounts
- Persist events per user in Firestore with live observers
- Stay usable offline after the first successful sync
- Surface searchable / filterable lists and a stats dashboard
- Receive push payloads over FCM and register device tokens per user
- Switch light/dark without fighting system bars or typography

It is not a backend. Reminder *scheduling* (cron-style “notify me 1h before”) belongs in Cloud Functions or a similar worker; the Android side stores device tokens and displays whatever FCM delivers.

---

## 2. Stack (concrete)

### App / UI
- AndroidX AppCompat, Core KTX
- Material Components 3
- ConstraintLayout, CoordinatorLayout, RecyclerView
- View Binding + BuildConfig enabled in `app/build.gradle.kts`
- Jetpack Navigation (`navigation-fragment-ktx` / `navigation-ui-ktx`)
- Bundled Poppins under `res/font`, wired through `res/values/type.xml`

### Architecture / async
- Hilt (`hilt-android` + KSP `hilt-compiler`)
- `@HiltViewModel` + constructor injection; `@AndroidEntryPoint` on Activities / Fragments / FCM service
- Use cases in the domain layer
- Repository interfaces in domain; Firebase implementations in data
- `StateFlow` for screen state, `SharedFlow` for one-shot UI effects
- Coroutines + lifecycle-aware collection (`repeatOnLifecycle(STARTED)`)
- Shared Firestore event stream via `shareIn` on an application-scoped `CoroutineScope`

### Firebase BOM
- Platform: `com.google.firebase:firebase-bom:33.7.0`
- Artifacts: `firebase-auth-ktx`, `firebase-firestore-ktx`, `firebase-messaging-ktx`
- Plugin: `com.google.gms.google-services`

### Tests
- JUnit 4, Mockito (+ mockito-kotlin), `kotlinx-coroutines-test`, Turbine, `FakeAppClock`

---

## 3. Architecture

```
┌─────────────────────────────────┐
│  Activity / Fragment (UI)       │  binding, clicks, Snackbars, anim
└───────────────┬─────────────────┘
                │ StateFlow / SharedFlow
┌───────────────▼─────────────────┐
│  ViewModel (@HiltViewModel)     │  UI state, effects, orchestration
└───────────────┬─────────────────┘
                │ invoke use cases
┌───────────────▼─────────────────┐
│  Use Cases (domain)             │  validation, analytics, filters
└───────────────┬─────────────────┘
                │ repository interfaces
┌───────────────▼─────────────────┐
│  Repository Interfaces          │  Auth / Event / Notification
└───────────────┬─────────────────┘
                │ Hilt @Binds
┌───────────────▼─────────────────┐
│  Firebase*Repository (data)     │  SDK I/O, DTO mapping, AppError
└───────────────┬─────────────────┘
                │
┌───────────────▼─────────────────┐
│  Firebase                       │  Auth · Firestore · FCM
└─────────────────────────────────┘
```

### Layer rules

| Layer | Owns | Must not own |
|-------|------|----------------|
| Presentation | Views, navigation, string resources, permission prompts | Firebase SDK, repository construction |
| ViewModel | Immutable UI state, effects, calling use cases | View / Binding references, Firebase types |
| Domain | Models (`Instant`), repository contracts, use cases | Android / Firebase SDK |
| Data | Firebase I/O, DTO ↔ domain mapping, error mapping | UI strings, navigation |
| DI | Providing Firebase singletons, binding repos, app scope | Feature logic |
| Application | Theme restore, notification channel | Screen / auth navigation |

### Dependency direction

```
UI → ViewModel → Use Cases → Repository Interfaces → Repository Implementations → Firebase
```

Presentation packages must never call `FirebaseAuth.getInstance()`, `FirebaseFirestore.getInstance()`, `FirebaseMessaging.getInstance()`, or `AuthRepository()` / `EventRepository()` constructors.

### Session

```
FirebaseAuth
    ↓
FirebaseUserSession (data)
    ↓
UserSession (domain)
    ↓
FirebaseEventRepository / FirebaseNotificationRepository
```

Event and notification repositories resolve the current UID through `UserSession`, not by injecting `FirebaseAuth` directly.

### Shared event stream

```
UserSession.userId
        ↓
flatMapLatest → Firestore snapshot listener
        ↓
shareIn(applicationScope, WhileSubscribed(5s), replay=1)
      ↙                    ↘
EventListViewModel     DashboardViewModel
```

### FCM registration

```
EventMessagingService.onNewToken(token)
        ↓
RegisterFcmTokenUseCase(token)
        ↓
NotificationRepository.registerToken(token)

MainViewModel.onLoggedIn()
        ↓
EnsureNotificationRegistrationUseCase
        ↓
getToken → registerToken(token) → subscribeToTopic
```

`onNewToken` passes the provided token through; it does not call `getToken()` again.

### Screen ViewModels

| Screen | ViewModel |
|--------|-----------|
| Event list | `EventListViewModel` |
| Event editor | `EventEditorViewModel` |
| Dashboard | `DashboardViewModel` (+ `BuildDashboardDataUseCase`) |
| Auth | `AuthViewModel` |
| Splash | `SplashViewModel` |
| Main | `MainViewModel` |

### `Resource<T>` and `AppError`

```kotlin
sealed class Resource<out T> {
    data class Success<T>(val data: T) : Resource<T>()
    data class Error(val error: AppError) : Resource<Nothing>()
    data object Loading : Resource<Nothing>()
}
```

`AppError` is a sealed interface (`Network`, `Unauthorized`, `Validation`, …). Repositories map Firebase exceptions → `AppError`. The UI maps `AppError` → `strings.xml` via `ErrorMapper.toUserMessage()`.

### Shared event stream

See the diagram under §3. `FirebaseEventRepository` owns one shared listener via `UserSession.userId` → `flatMapLatest` → `shareIn`.

### Process entry

`EventManagementApp` (`@HiltAndroidApp`):

1. Restores night mode from `ThemePreferences`
2. Creates notification channel `event_reminders` (API 26+)

Firestore persistent cache is configured in `FirebaseModule` when providing `FirebaseFirestore`.

---

## 4. Module layout

```
Android_Test_kotlin/
├── app/
│   ├── build.gradle.kts              # Hilt, KSP, Navigation, desugaring
│   ├── google-services.json
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/example/eventmanagement/
│       │   │   ├── EventManagementApp.kt
│       │   │   ├── core/
│       │   │   │   ├── error/          # AppError, Resource, ValidationField
│       │   │   │   ├── time/           # AppClock, DateFormatter
│       │   │   │   ├── theme/          # ThemePreferences
│       │   │   │   ├── validation/     # AuthValidator, EventValidator
│       │   │   │   ├── ui/             # insets, animations, ErrorMapper
│       │   │   │   └── log/            # AppLogger (debug-gated)
│       │   │   ├── data/
│       │   │   │   ├── dto/EventDto.kt
│       │   │   │   ├── mapper/EventMapper.kt
│       │   │   │   ├── firebase/FirebaseErrorMapper.kt
│       │   │   │   └── repository/
│       │   │   │       ├── FirebaseAuthRepository.kt
│       │   │   │       ├── FirebaseEventRepository.kt
│       │   │   │       └── FirebaseNotificationRepository.kt
│       │   │   ├── domain/
│       │   │   │   ├── model/
│       │   │   │   ├── repository/
│       │   │   │   ├── session/UserSession.kt
│       │   │   │   └── usecase/{auth,event,dashboard,notification}/
│       │   │   ├── data/
│       │   │   │   ├── dto/ mapper/ firebase/
│       │   │   │   ├── session/FirebaseUserSession.kt
│       │   │   │   └── repository/Firebase*Repository.kt
│       │   │   ├── presentation/
│       │   │   │   ├── events/
│       │   │   │   │   ├── EventListViewModel.kt
│       │   │   │   │   ├── EventEditorViewModel.kt
│       │   │   │   │   ├── EventListFragment.kt
│       │   │   │   │   ├── EventEditorActivity.kt
│       │   │   │   │   └── EventAdapter.kt
│       │   │   │   ├── dashboard/ auth/ splash/ main/
│       │   │   ├── di/
│       │   │   └── service/EventMessagingService.kt
│       │   └── res/
│       │       ├── navigation/nav_graph.xml
│       │       └── ...
│       └── test/...                    # use case + ViewModel tests, FakeAppClock
├── firestore/firestore.rules
├── build.gradle.kts                    # AGP, Kotlin, GMS, Hilt, KSP plugins
├── settings.gradle.kts
├── gradle.properties
└── README.md
```

---

## 5. Data contract

### Paths

```
users/{userId}/events/{eventId}     # event documents
users/{userId}/devices/{deviceId}   # FCM token registration
```

List query:

```
eventsCollection(uid)
  .orderBy("dateTime", Query.Direction.DESCENDING)
```

### Domain model vs Firestore

| Layer | Type for date/time |
|-------|--------------------|
| Domain / presentation | `java.time.Instant` |
| Firestore wire format | `Timestamp` via `EventDto` + `EventMapper` |

Existing documents stay compatible — field names are unchanged.

### Fields (Firestore document)

| Field | Type | Notes |
|-------|------|--------|
| `title` | String | Required (validated in `EventValidator` / use cases) |
| `description` | String | Optional |
| `dateTime` | `Timestamp` | Sort key; drives filters / chart / countdown |
| `location` | String | Optional |
| `userId` | String | Must equal `request.auth.uid` (enforced in rules) |
| `createdAt` | `Timestamp?` | Set on create; stripped from updates |
| `updatedAt` | `Timestamp` | Touched on create/update |

### Filters (client-side)

`FilterEventsUseCase` applies:

- Text query against title / description / location (case-insensitive)
- `EventFilter.ALL | UPCOMING | PAST` vs `AppClock.now()`

### Security rules

File: `firestore/firestore.rules`

- Owner-only read/write on `users/{userId}/events/{eventId}`
- Create/update validate field types, non-empty title, and `userId == auth.uid`
- Updates cannot change ownership (`userId`) or `createdAt`
- Updates may only affect `title`, `description`, `dateTime`, `location`, `updatedAt`
- Owner-only CRUD on `users/{userId}/devices/{deviceId}` for FCM tokens

Deploy these before demoing CRUD or you will see `PERMISSION_DENIED`.

### Offline

Persistent cache is enabled when providing `FirebaseFirestore` in `FirebaseModule`:

- First online session hydrates cache
- Subsequent cold starts can show cached events while re-attaching the listener
- Writes queue when offline and sync when connectivity returns (subject to Auth still being valid)

---

## 6. Feature map (by area)

### Authentication

Flow: `AuthViewModel` → `LoginUseCase` / `SignUpUseCase` / `ResetPasswordUseCase` → `AuthRepository` → `FirebaseAuthRepository`.

| Operation | Behaviour |
|-----------|-----------|
| Sign up | `createUserWithEmailAndPassword` |
| Login | `signInWithEmailAndPassword` |
| Reset | `sendPasswordResetEmail` |
| Session | `ObserveSessionUseCase` / `GetCurrentUserUseCase` |
| Logout | Unregister device token, then `signOut()`, clear activity stack |

Firebase failures map to `AppError`; UI shows localized strings.

Screens: `LoginActivity`, `SignUpActivity`, `ForgotPasswordActivity`.

### Events

Flow: `EventViewModel` → event use cases → `EventRepository` → `FirebaseEventRepository`.

| Operation | Implementation notes |
|-----------|----------------------|
| Observe | Shared snapshot `Flow` |
| Add / Update / Delete | Use cases + validators |
| Get one | Editor edit mode |
| Search / filter | `FilterEventsUseCase` |

Editor (`EventEditorActivity`):

- Date and time are separate inputs → `DatePickerDialog` / `TimePickerDialog`
- Past dates are allowed
- Dialogs dismissed in `onDestroy`

List (`EventListFragment`):

- Search + chip filters
- Skeleton on first load
- Empty states for “no data” vs “no matches”
- Extended FAB shrinks on scroll

### Dashboard

`DashboardViewModel` owns analytics (not `EventViewModel`):

- `GetEventStatsUseCase` — total / upcoming / past / this week / completion %
- `GetNextUpcomingEventUseCase`
- `GetNextSevenDaysUseCase`
- `GetMonthlyEventCountsUseCase` — chart + busiest month + average
- `GetCurrentUserUseCase` — greeting identity

Uses the same shared event stream; time math goes through `AppClock` for testability.

### Push / notifications

```
MainActivity → MainViewModel
  → EnsureNotificationRegistrationUseCase
       → getToken → registerToken(token) → subscribe (event_announcements)

EventMessagingService.onNewToken(token)
  → RegisterFcmTokenUseCase(token)
```

| Concern | Detail |
|---------|--------|
| Channel | `event_reminders` |
| Global topic | `event_announcements` (non-personal announcements) |
| Device docs | `users/{uid}/devices/{tokenId}` with `token`, `platform`, `updatedAt` |
| Service | `EventMessagingService` (`@AndroidEntryPoint`); uses use case on token refresh |
| Logging | Token details only when `BuildConfig.DEBUG` |

Personal event reminders should target device tokens via Cloud Functions — not the global announcements topic.

---

## 7. Navigation & components

| Component | Role |
|-----------|------|
| `SplashActivity` + `SplashViewModel` | Launcher; observes `SessionState` |
| `LoginActivity` | Entry when signed out |
| `MainActivity` + `MainViewModel` | Nav host, theme, logout, FCM registration |
| `EventListFragment` | Primary list (`nav_graph` start) |
| `EventEditorActivity` | Create / edit (separate activity) |
| `DashboardFragment` | Analytics surface |

Bottom navigation uses NavigationUI with destinations in `res/navigation/nav_graph.xml` (`eventListFragment`, `dashboardFragment`). Menu item ids match destination ids.

UI state pattern:

```kotlin
private val _uiState = MutableStateFlow(EventListUiState())
val uiState = _uiState.asStateFlow()

private val _effects = MutableSharedFlow<EventUiEffect>()
val effects = _effects.asSharedFlow()
```

Collect with `viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED)`.

---

## 8. Theming & resources

| Concern | Location |
|---------|----------|
| Light / dark palettes | `res/values/colors.xml`, `values-night/colors.xml` |
| Theme / widgets | `themes.xml`, `values-night/themes.xml` |
| Type scale | `res/values/type.xml` |
| User-facing errors | `res/values/strings.xml` (`error_*`) |
| Theme persistence | `core/theme/ThemePreferences` |
| Insets / motion | `core/ui/*Extensions.kt` |

---

## 9. Setup instructions

Do this in order. Skip Firebase pieces and the app will compile only until Auth/Firestore are hit at runtime.

### 9.1 Machine prerequisites

| Requirement | Notes |
|-------------|--------|
| Android Studio | Hedgehog / Iguana / newer is fine |
| JDK 17 | AGP 8.x will not run on 11 |
| Android SDK | Platform 35 installed via SDK Manager |
| Device | Emulator or phone; **Google Play** system image if you need FCM |
| Network | First Auth/Firestore calls need internet |

```bash
java -version
# expect 17.x
```

If Gradle picks the wrong JDK, set in `gradle.properties`:

```properties
org.gradle.java.home=C:\\Program Files\\Java\\jdk-17
```

### 9.2 Open the project

1. Android Studio → **File → Open** → repo root (`Android_Test_kotlin`)
2. Wait for Gradle sync (Hilt / KSP will run on first build)
3. If IDE shows stale “Unresolved reference: dagger / HiltViewModel”, use **Sync Project with Gradle Files** or **Invalidate Caches**

If sync fails on MPAndroidChart, confirm JitPack in `settings.gradle.kts`:

```kotlin
maven { url = uri("https://jitpack.io") }
```

### 9.3 Firebase config file (`google-services.json`)

| Item | Value |
|------|--------|
| Expected path | `app/google-services.json` |
| Expected package inside JSON | `com.example.eventmanagement` |
| Must match | `applicationId` in `app/build.gradle.kts` |

Place the downloaded file at:

```
Android_Test_kotlin/
└── app/
    └── google-services.json
```

**Do not** put it under `app/src/main/` or the project root.

**Gradle plugins (already in this repo)**

Root `build.gradle.kts`:

```kotlin
id("com.google.gms.google-services") version "4.4.2" apply false
id("com.google.dagger.hilt.android") version "2.52" apply false
id("com.google.devtools.ksp") version "2.0.21-1.0.27" apply false
```

App `build.gradle.kts`:

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
}
```

### 9.4 Run a debug build

```bash
./gradlew assembleDebug
./gradlew test
```

Windows:

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat test
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

If install fails with `Broken pipe` and you have both a phone and an emulator attached, pick a single run target in Studio (or `adb -s <serial> install …`).

---

## 10. Firebase configuration

### 10.1 Create project and register the Android app

1. [Firebase Console](https://console.firebase.google.com/) → Add project  
2. Add Android app with package `com.example.eventmanagement`  
3. Download `google-services.json` → `app/` (§9.3)  
4. Skip Console “add SDK” steps — dependencies are already present  

### 10.2 Enable Email/Password Authentication

Console → **Authentication** → **Sign-in method** → enable **Email/Password**.

| Screen | Firebase API |
|--------|----------------|
| Sign up | `createUserWithEmailAndPassword` |
| Login | `signInWithEmailAndPassword` |
| Forgot password | `sendPasswordResetEmail` |
| Logout | `signOut` |

### 10.3 Create Firestore and deploy security rules

1. Console → **Firestore Database** → Create database  
2. Publish rules from `firestore/firestore.rules` (events + devices validation)  
3. Paths in use:

```
users/{userId}/events/{eventId}
users/{userId}/devices/{deviceId}
```

**Indexes:** current list query is single-field `orderBy("dateTime")` — no composite index required.

Optional CLI:

```bash
firebase deploy --only firestore:rules --project <your-project-id>
```

### 10.4 Cloud Messaging (FCM)

| Client behaviour | Detail |
|------------------|--------|
| Channel | `event_reminders` |
| Topic | `event_announcements` (global announcements only) |
| Device registration | `users/{uid}/devices/...` after login |
| Service | `EventMessagingService` |
| Debug token log | Only in debug builds via `AppLogger` |

**Send a test notification**

1. Run the app, sign in  
2. Console → **Messaging** → send test message to the device token, or target topic `event_announcements`  
3. On API 33+, accept the notification permission dialog  

### 10.5 SHA-1 fingerprint

Add when you see `DEVELOPER_ERROR` or odd Auth failures.

```powershell
keytool -list -v `
  -keystore "$env:USERPROFILE\.android\debug.keystore" `
  -alias androiddebugkey `
  -storepass android `
  -keypass android
```

Copy SHA-1 → Firebase → Project settings → Android app → **Add fingerprint**.

### 10.6 Firebase config checklist

- [ ] Firebase project created  
- [ ] Android app registered with package `com.example.eventmanagement`  
- [ ] `app/google-services.json` present and package matches  
- [ ] Hilt / GMS plugins sync OK  
- [ ] Email/Password sign-in enabled  
- [ ] Firestore created; **updated** rules published (events + devices)  
- [ ] Register / login works  
- [ ] Create event → appears under `users/{uid}/events`  
- [ ] After login, a device doc appears under `users/{uid}/devices`  
- [ ] (Optional) SHA-1 added; FCM test message received  

### 10.7 Verify from Firebase Console

| Check | Where |
|-------|--------|
| User created | Authentication → Users |
| Event documents | Firestore → `users` → `{uid}` → `events` |
| Device tokens | Firestore → `users` → `{uid}` → `devices` |
| Rules live | Firestore → Rules |

---

## 11. Build commands (reference)

```bash
./gradlew assembleDebug
./gradlew test
./gradlew lintDebug
```

Windows: `.\gradlew.bat` with the same tasks.

---

## 12. Runtime walkthrough

1. Cold start → `SplashActivity` (~1.7s) → `SplashViewModel` session state  
2. Auth check → `LoginActivity` or `MainActivity`  
3. `MainViewModel.onLoggedIn()` subscribes to announcements topic and registers FCM token  
4. Events tab → shared Firestore observer via `EventViewModel`  
5. FAB → editor → save → snapshot updates list + dashboard  
6. Dashboard tab → `DashboardViewModel` analytics from the same stream  
7. Theme toggle; logout unregisters device then returns to login  

---

## 13. Testing

| Area | Location |
|------|----------|
| Event list ViewModel | `presentation/events/EventListViewModelTest.kt` |
| Event editor ViewModel | same file (`EventEditorViewModelTest`) |
| Auth use cases | `domain/usecase/auth/AuthUseCasesTest.kt` |
| Event use cases | `domain/usecase/event/EventUseCasesTest.kt` |
| Dashboard analytics | `DashboardUseCasesTest` + `BuildDashboardDataUseCaseTest` |
| FCM registration | `domain/usecase/notification/NotificationUseCasesTest.kt` |
| Error mapping | `data/firebase/FirebaseErrorMapperTest.kt` |
| Fixed clock | `core/time/FakeAppClock.kt` |

```bash
./gradlew test
```

Unit tests do not hit the network. Time-sensitive stats use `FakeAppClock`.

### What is intentionally not unit-tested here

Firebase SDK integration, chart rendering, and animation timing — prefer instrumented / manual QA.

---

## 14. Permissions & manifest

| Entry | Why |
|-------|-----|
| `INTERNET` | Auth / Firestore / FCM |
| `POST_NOTIFICATIONS` | API 33+ runtime gate |
| `SplashActivity` exported + LAUNCHER | Entry (`presentation.splash`) |
| Other activities not exported | Hardening |
| `EventMessagingService` | FCM callbacks |
| `default_notification_channel_id` | `event_reminders` |

---

## 15. Failure modes

| Symptom | Diagnosis / fix |
|---------|-----------------|
| Gradle: AGP requires Java 17 | Point Studio + `org.gradle.java.home` at JDK 17 |
| Unresolved `dagger` / `@HiltViewModel` in IDE | Sync Gradle / invalidate caches (CLI compile may still succeed) |
| Missing google-services | File not under `app/`, or wrong package inside JSON |
| Auth `DEVELOPER_ERROR` | Add SHA-1; refresh `google-services.json` |
| Firestore `PERMISSION_DENIED` | Publish updated rules; signed-out user; `userId` mismatch on write |
| Empty list after save | Listener error (Logcat); rules; offline without prior cache |
| Install `Broken pipe` | Unstable emulator/ADB; use one device serial; restart ADB |
| No notification | Permission denied; wrong topic; emulator without Play Services |
| WindowLeaked on editor | Pickers dismissed in `onDestroy` (already handled) |

---

## 16. Extension points

1. **Per-event reminders** — Cloud Function on event create/update; read `users/{uid}/devices`; send to tokens (not the global announcements topic).  
2. **Indexes** — if you add `where` + `orderBy` on different fields, follow the Console deep-link from Logcat.  
3. **Pagination** — swap full snapshot for query cursors at larger scale.  
4. **Release signing** — minify is off; ProGuard keeps MPAndroidChart if you enable minify later.  
5. **Editor in Navigation** — optional; currently a separate Activity by design.

---

## 17. Summary

Clean Architecture Android client: Hilt DI, domain use cases, Firebase-backed repositories with a shared live event stream, owner-scoped Firestore rules (events + devices), Material 3 UI with Navigation Component, StateFlow/SharedFlow presentation, injectable `AppClock`, structured `AppError` mapping, and focused unit tests. Follow §9 for local setup and §10 for Firebase config, then run on JDK 17.
