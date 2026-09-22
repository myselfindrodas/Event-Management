# Event Manager

Kotlin Android client for personal event scheduling and review. XML UI on Material 3, MVVM with a Repository boundary, Firebase Auth / Firestore / FCM.

This README is the working reference for structure, data contracts, Firebase wiring, build, and the parts that usually break in the field.

**Start here if you are setting up a machine:** [§9 Setup instructions](#9-setup-instructions) → [§10 Firebase configuration](#10-firebase-configuration).

| | |
|---|---|
| Application id | `com.example.eventmanagement` |
| minSdk / targetSdk / compileSdk | 24 / 35 / 35 |
| Language | Kotlin |
| UI | XML, View Binding, Material 3 |
| Architecture | MVVM + Repository |
| JDK | 17 (required by AGP 8.x) |
| Backend | Firebase Auth, Cloud Firestore, FCM |
| Charting | MPAndroidChart (`v3.1.0` via JitPack) |

---

## 1. Purpose

Ship a production-shaped client that can:

- Authenticate with email/password and recover accounts
- Persist events per user in Firestore with live observers
- Stay usable offline after the first successful sync
- Surface searchable / filterable lists and a stats dashboard
- Receive push payloads over FCM
- Switch light/dark without fighting system bars or typography

It is not a backend. Reminder *scheduling* (cron-style “notify me 1h before”) belongs in Cloud Functions or a similar worker; the Android side already displays whatever FCM delivers.

---

## 2. Stack (concrete)

### App / UI
- AndroidX AppCompat, Core KTX
- Material Components 3
- ConstraintLayout, CoordinatorLayout, RecyclerView
- View Binding enabled in `app/build.gradle.kts`
- Bundled Poppins (regular / medium / semibold / bold) under `res/font`, wired through `res/values/type.xml` into Material `textAppearance*` attributes

### Architecture / async
- `ViewModel` + `LiveData` / `MediatorLiveData`
- Coroutines (`lifecycle-viewmodel-ktx`, `coroutines-android`, `coroutines-play-services`)
- Firestore listeners exposed as `Flow` via `callbackFlow`, collected as LiveData in the ViewModel

### Firebase BOM
- Platform: `com.google.firebase:firebase-bom:33.7.0`
- Artifacts: `firebase-auth-ktx`, `firebase-firestore-ktx`, `firebase-messaging-ktx`
- Plugin: `com.google.gms.google-services`

### Tests
- JUnit 4, Mockito (+ mockito-kotlin), `kotlinx-coroutines-test`, `androidx.arch.core:core-testing`

---

## 3. Architecture

```
┌──────────────────────────────┐
│  Activity / Fragment (UI)    │  binding, clicks, Snackbars, anim
└──────────────┬───────────────┘
               │ observe LiveData
┌──────────────▼───────────────┐
│  ViewModel                   │  validation, filter/search, stats
└──────────────┬───────────────┘
               │ suspend / Flow
┌──────────────▼───────────────┐
│  Repository                  │  Firebase I/O, error mapping
└──────────────┬───────────────┘
               │
┌──────────────▼───────────────┐
│  Firebase                    │  Auth · Firestore · FCM
└──────────────────────────────┘
```

### Layer rules

| Layer | Owns | Must not own |
|-------|------|----------------|
| UI | Views, navigation, permission prompts, one-shot UI feedback | Firebase SDK calls, business rules |
| ViewModel | State, validation, derived metrics, coordinating repos | Android View / Binding references |
| Repository | Auth/Firestore APIs, `Resource` mapping, batch writes | UI strings beyond error messages, navigation |
| Application | Process-wide setup (theme, cache, channels) | Screen logic |

### `Resource<T>`

Sealed envelope used across Auth and Events:

- `Loading`
- `Success(data)`
- `Error(message)`

Keeps UI observers boring and consistent (`when` on one type).

### Process entry

`EventManagementApp`:

1. Restores night mode from `ThemePrefs`
2. Enables Firestore persistent cache (`PersistentCacheSettings`)
3. Creates notification channel `event_reminders` (API 26+)

---

## 4. Module layout

```
Android_Test_kotlin/
├── app/
│   ├── build.gradle.kts
│   ├── google-services.json          # your Firebase config (not optional at runtime)
│   ├── proguard-rules.pro            # MPAndroidChart keep rules
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/example/eventmanagement/
│       │   │   ├── EventManagementApp.kt
│       │   │   ├── data/
│       │   │   │   ├── Resource.kt
│       │   │   │   ├── model/Event.kt
│       │   │   │   └── repository/
│       │   │   │       ├── AuthRepository.kt
│       │   │   │       └── EventRepository.kt
│       │   │   ├── ui/
│       │   │   │   ├── splash/SplashActivity.kt
│       │   │   │   ├── auth/
│       │   │   │   │   ├── AuthViewModel.kt
│       │   │   │   │   ├── LoginActivity.kt
│       │   │   │   │   ├── SignUpActivity.kt
│       │   │   │   │   └── ForgotPasswordActivity.kt
│       │   │   │   ├── events/
│       │   │   │   │   ├── EventViewModel.kt
│       │   │   │   │   ├── EventListFragment.kt
│       │   │   │   │   ├── EventAdapter.kt
│       │   │   │   │   └── EventEditorActivity.kt
│       │   │   │   ├── dashboard/DashboardFragment.kt
│       │   │   │   └── main/MainActivity.kt
│       │   │   ├── service/EventMessagingService.kt
│       │   │   └── util/Utils.kt
│       │   └── res/                  # layout, drawable, anim, font, values, values-night, mipmap*
│       ├── test/.../EventViewModelTest.kt
│       └── androidTest/...
├── firestore/firestore.rules
├── build.gradle.kts
├── settings.gradle.kts               # includes JitPack for MPAndroidChart
├── gradle.properties
└── README.md
```

---

## 5. Data contract

### Path

```
users/{userId}/events/{eventId}
```

One subcollection per authenticated user. List query:

```
eventsCollection(uid)
  .orderBy("dateTime", Query.Direction.DESCENDING)
```

### Fields (`Event`)

| Field | Type | Notes |
|-------|------|--------|
| `id` | String | `@DocumentId`; empty until after write |
| `title` | String | Required (validated in ViewModel) |
| `description` | String | Optional |
| `dateTime` | `Timestamp` | Sort key; drives filters / chart / countdown |
| `location` | String | Optional |
| `userId` | String | Set on write from `FirebaseAuth.uid` |
| `createdAt` | `Timestamp?` | Set on create; stripped from updates |
| `updatedAt` | `Timestamp?` | Touched on create/update |

`toMap()` is what gets written so we control field names and avoid serializing noise.

### Filters (client-side)

After the live snapshot lands, `EventViewModel.filteredEvents` (`MediatorLiveData`) applies:

- Text query against title / description / location (case-insensitive)
- `EventFilter.ALL | UPCOMING | PAST` vs `Date()`

Keeping filter/search on the client is fine at personal-event scale; if the corpus grows large, move predicates into query constraints and composite indexes.

### Security rules

File: `firestore/firestore.rules`

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{userId}/events/{eventId} {
      allow read, write: if request.auth != null
                         && request.auth.uid == userId;
    }
  }
}
```

Owner-only. Unsigned clients get nothing. Deploy these before demoing CRUD or you will see `PERMISSION_DENIED`.

### Offline

Persistent cache is enabled in `EventManagementApp`. Expected behaviour:

- First online session hydrates cache
- Subsequent cold starts can show cached events while re-attaching the listener
- Writes queue when offline and sync when connectivity returns (subject to Auth still being valid)

---

## 6. Feature map (by area)

### Authentication (`AuthRepository` / `AuthViewModel`)

| Operation | Behaviour |
|-----------|-----------|
| Sign up | `createUserWithEmailAndPassword` |
| Login | `signInWithEmailAndPassword` |
| Reset | `sendPasswordResetEmail` |
| Session | `FirebaseAuth.currentUser` / auth state |
| Logout | `signOut()` then clear activity stack |

Firebase error codes are mapped to short UI strings (`ERROR_WRONG_PASSWORD`, `ERROR_EMAIL_ALREADY_IN_USE`, network failures, etc.). Prefer that over dumping `exception.message` raw.

Screens: `LoginActivity`, `SignUpActivity`, `ForgotPasswordActivity` — gradient header, elevated form card, button-level progress.

### Events (`EventRepository` / `EventViewModel`)

| Operation | Implementation notes |
|-----------|----------------------|
| Observe | Snapshot listener → `Flow<Resource<List<Event>>>` |
| Add | `collection.add(map)` |
| Update | `document.update(map)` without overwriting `createdAt` |
| Delete | `document.delete()` |
| Get one | Used by editor in edit mode |
| Seed | `WriteBatch` of 10 demo docs |

Editor (`EventEditorActivity`):

- Date and time are separate `TextInputEditText`s
- Tap date → `DatePickerDialog`; tap time → `TimePickerDialog`
- Dialogs are dismissed in `onDestroy` to avoid window leaks
- Past dates are allowed on create and edit (no min-date lock, no ViewModel rejection)

List (`EventListFragment`):

- Search + chip filters
- Skeleton while first snapshot is in flight
- Distinct empty states for “no data” vs “no matches”
- Extended FAB shrinks on scroll; long-press seeds demo data

### Dashboard (`DashboardFragment`)

Derived entirely from the event list (no second collection):

- Hero: greeting by hour of day, avatar initial from email local-part, completion %
- Stat cards: total / upcoming / past (count-up animation)
- Next event + relative countdown (today / tomorrow / N days)
- Next-7-days strip with animated bar heights; today highlighted
- Insights: busiest month key, average events per non-empty month
- MPAndroidChart bar chart: gradient fill, integer value formatter, dashed grid, Poppins typeface, empty illustration when no months

### Push (`EventMessagingService`)

- Manifest service on `com.google.firebase.MESSAGING_EVENT`
- Default channel meta: `event_reminders`
- `MainActivity` requests `POST_NOTIFICATIONS` on API 33+ and subscribes to topic `event_reminders`
- Token: `FirebaseMessaging.getInstance().token` + `onNewToken` → Logcat tag **`FCM`**

### Demo seed

Ten documents (5 future offsets, 5 past offsets) with realistic titles/locations. Trigger:

- Empty state button **Load demo data**, or
- Long-press **Add Event** FAB

Uses Firestore batch commit; UI listens on `demoResult` separately from delete `actionResult` so Snackbars stay accurate.

---

## 7. Navigation & components

| Component | Role |
|-----------|------|
| `SplashActivity` | Launcher; animates brand; routes by `isLoggedIn()` |
| `LoginActivity` | Entry when signed out |
| `MainActivity` | Bottom nav host; theme; logout; FCM subscribe + token log |
| `EventListFragment` | Primary list |
| `EventEditorActivity` | Create / edit form |
| `DashboardFragment` | Analytics surface |

Bottom nav destinations: Events · Dashboard.  
Toolbar: custom title + brand mark; overflow-style actions for theme (sun/moon) and logout (confirm dialog).

Activity/fragment transitions live under `res/anim/` (`activity_*`, `fragment_*`, list fall-down, skeleton pulse).

---

## 8. Theming & resources

| Concern | Location |
|---------|----------|
| Light palette | `res/values/colors.xml` |
| Dark palette | `res/values-night/colors.xml` |
| Theme / widgets | `res/values/themes.xml`, `values-night/themes.xml` |
| Type scale | `res/values/type.xml` |
| Gradients / chips / pills | `res/drawable/bg_*.xml` |
| Launcher | `mipmap-anydpi-v26` adaptive + legacy `mipmap` |
| Theme persistence | `ThemePrefs` in `util/Utils.kt` (SharedPreferences) |

Insets helpers:

- `applySystemBarInsets` — full system bars (main shell)
- `applyTopInsetAsPadding` / `applyBottomInsetAsPadding` — gradient headers that bleed under the status bar without colliding with clock/cutout

Day/night uses `AppCompatDelegate` modes; toggle flips YES ↔ NO (and treats FOLLOW_SYSTEM as a jump to YES on first explicit toggle).

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

Confirm Java from a terminal:

```bash
java -version
# expect 17.x
```

If Gradle still picks the wrong JDK, set it in `gradle.properties`:

```properties
org.gradle.java.home=C:\\Program Files\\Java\\jdk-17
```

(Adjust the path for your machine. On macOS/Linux use the absolute JDK 17 home.)

### 9.2 Open the project

1. Android Studio → **File → Open**
2. Select the repo root (`Android_Test_kotlin`), not only the `app` module
3. Trust the project if prompted
4. Wait for Gradle sync

If sync fails on MPAndroidChart, confirm JitPack is in `settings.gradle.kts`:

```kotlin
maven { url = uri("https://jitpack.io") }
```

### 9.3 Firebase config file (`google-services.json`)

This file is **required**. The Google Services Gradle plugin reads it at build time and injects project/app ids into the APK.

| Item | Value |
|------|--------|
| Expected path | `app/google-services.json` |
| Expected package inside JSON | `com.example.eventmanagement` |
| Must match | `applicationId` in `app/build.gradle.kts` |

**Where it comes from**

1. [Firebase Console](https://console.firebase.google.com/) → your project  
2. Project settings (gear) → **Your apps** → Android app  
3. **Download google-services.json**  
4. Replace / place the file at:

```
Android_Test_kotlin/
└── app/
    └── google-services.json    ← here
```

**Do not** put it under `app/src/main/` or the project root. The plugin only looks in the app module root by default.

**Sanity check after drop-in**

Open the JSON and verify:

```json
"android_client_info": {
  "package_name": "com.example.eventmanagement"
}
```

If `package_name` differs from `applicationId`, Auth and FCM will fail in confusing ways. Fix by registering the correct package in Console and re-downloading.

**Gradle wiring (already in this repo)**

Root `build.gradle.kts`:

```kotlin
id("com.google.gms.google-services") version "4.4.2" apply false
```

App `build.gradle.kts`:

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
}
```

After replacing the JSON: **File → Sync Project with Gradle Files**.

### 9.4 Run a debug build

Studio: select device → Run.

CLI:

```bash
./gradlew assembleDebug
```

Windows:

```powershell
.\gradlew.bat assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`

You can launch before enabling Auth/Firestore, but login and event CRUD will fail until sections 10.2–10.3 are done.

---

## 10. Firebase configuration

Complete Console setup for the three products this client uses: **Authentication**, **Cloud Firestore**, **Cloud Messaging**.

### 10.1 Create project and register the Android app

1. Open [Firebase Console](https://console.firebase.google.com/)
2. **Add project** (or open an existing one) → disable Analytics if you do not need it
3. On the project overview, **Add app** → **Android**
4. Fill in:

| Field | Value |
|-------|--------|
| Android package name | `com.example.eventmanagement` |
| App nickname | Event Manager (optional) |
| Debug signing certificate SHA-1 | Optional now; add later if Auth/FCM misbehave (see 10.5) |

5. Download `google-services.json` and place it under `app/` as in §9.3  
6. Skip the Console “add SDK” steps — dependencies are already in `app/build.gradle.kts`

### 10.2 Enable Email/Password Authentication

1. Console → **Build → Authentication**  
2. **Get started** if the product was never opened  
3. **Sign-in method** tab  
4. Click **Email/Password**  
5. Enable the first toggle (**Email/Password**) → **Save**  

Leave “Email link (passwordless)” off unless you intentionally add that flow (this app does not use it).

**What the app calls**

| Screen | Firebase API |
|--------|----------------|
| Sign up | `createUserWithEmailAndPassword` |
| Login | `signInWithEmailAndPassword` |
| Forgot password | `sendPasswordResetEmail` |
| Logout | `signOut` |

Password reset emails use Firebase’s template (Console → Authentication → Templates). Customize sender/name there if needed.

### 10.3 Create Firestore and deploy security rules

1. Console → **Build → Firestore Database** → **Create database**  
2. Choose production mode (rules will lock it down) or start in test mode **only** for a few minutes, then replace rules immediately  
3. Pick a region close to your users; you cannot casually move it later  
4. Open the **Rules** tab  
5. Replace the editor contents with the repo file `firestore/firestore.rules`:

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{userId}/events/{eventId} {
      allow read, write: if request.auth != null
                         && request.auth.uid == userId;
    }
  }
}
```

6. **Publish**

**Data shape the rules protect**

```
users/{userId}/events/{eventId}
```

Only the signed-in user whose `uid` equals `{userId}` can read/write. There is no public list and no cross-user access.

**Deploy via CLI (optional)**

From a machine with Firebase CLI logged into the same project:

```bash
npm i -g firebase-tools
# or: npx -y firebase-tools@latest

firebase login
firebase use <your-project-id>
firebase deploy --only firestore:rules --project <your-project-id>
```

If you use CLI deploy, keep a `firebase.json` that points at `firestore/firestore.rules`, or pass the file path according to your CLI setup. Console paste is enough for this project.

**Indexes**

The current list query is a single-field `orderBy("dateTime")` — no composite index required. If you later add `where` + `orderBy` on different fields, follow the deep-link Firestore prints in Logcat to create the index.

### 10.4 Cloud Messaging (FCM)

1. Messaging works for Android apps once `google-services.json` is present and Play Services is available on the device  
2. No extra “enable FCM” toggle for basic send/receive  
3. This app:

| Client behaviour | Detail |
|------------------|--------|
| Channel | `event_reminders` (created in `EventManagementApp`) |
| Topic subscribe | `event_reminders` from `MainActivity` after login |
| Service | `EventMessagingService` |
| Token log | Logcat tag `FCM` |

**Send a test notification**

1. Run the app, sign in  
2. Logcat filter: `tag:FCM` → copy `FCM token: …`  
3. Console → **Messaging** → **Create campaign** / **Send test message**  
4. Paste the token, or target topic `event_reminders`  
5. On API 33+, accept the notification permission dialog when prompted  

### 10.5 SHA-1 fingerprint (Auth / Play Services issues)

Add this when you see `DEVELOPER_ERROR`, odd Auth failures, or empty OAuth clients in Console.

**Debug keystore (default Android Studio debug builds)**

macOS / Linux:

```bash
keytool -list -v \
  -keystore ~/.android/debug.keystore \
  -alias androiddebugkey \
  -storepass android \
  -keypass android
```

Windows (PowerShell):

```powershell
keytool -list -v `
  -keystore "$env:USERPROFILE\.android\debug.keystore" `
  -alias androiddebugkey `
  -storepass android `
  -keypass android
```

Copy the **SHA-1** line → Firebase → Project settings → your Android app → **Add fingerprint** → Save.  
Re-download `google-services.json` if the Console prompts you, then sync Gradle.

For release builds, add the **release** keystore SHA-1 the same way.

### 10.6 Firebase config checklist

Use this before demoing:

- [ ] Firebase project created  
- [ ] Android app registered with package `com.example.eventmanagement`  
- [ ] `app/google-services.json` present and package matches  
- [ ] Google Services plugin applied; Gradle sync OK  
- [ ] Email/Password sign-in enabled  
- [ ] Firestore database created  
- [ ] Rules from `firestore/firestore.rules` published  
- [ ] App runs; can register / login  
- [ ] Can create an event; it appears in Console → Firestore under `users/{uid}/events`  
- [ ] (Optional) SHA-1 added  
- [ ] (Optional) FCM token visible in Logcat; test message received  

### 10.7 Verify from Firebase Console

| Check | Where |
|-------|--------|
| User created | Authentication → Users |
| Event documents | Firestore → `users` → `{uid}` → `events` |
| Rules live | Firestore → Rules (published version) |
| App id | Project settings → Your apps → `mobilesdk_app_id` matches JSON |

---

## 11. Build commands (reference)

### Prerequisites reminder

- JDK 17  
- Valid `app/google-services.json`  
- Emulator/device (Play image for FCM)

### Studio

Open root → sync → Run.

### CLI

```bash
./gradlew assembleDebug
./gradlew test
./gradlew lintDebug
```

Windows:

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat test
.\gradlew.bat lintDebug
```

JitPack is registered in `settings.gradle.kts` for MPAndroidChart.

---

## 12. Runtime walkthrough

1. Cold start → `SplashActivity` (brand motion ~1.7s)  
2. Auth check → `LoginActivity` or `MainActivity`  
3. Events tab loads `EventListFragment`; ViewModel attaches Firestore observer  
4. FAB → editor → save → snapshot pushes new list  
5. Dashboard tab recomputes metrics from the same stream  
6. Theme toggle recreates with saved mode; logout confirms then `finishAffinity()` to login  

**Seed:** empty list CTA or FAB long-press.  
**FCM:** Logcat `tag:FCM` after login on Main.

---

## 13. Testing

### Unit — `EventViewModelTest`

Path: `app/src/test/java/com/example/eventmanagement/EventViewModelTest.kt`

Approach: mock `EventRepository`, stub `observeEvents()` with a `MutableStateFlow`, drive Main with `UnconfinedTestDispatcher`, `InstantTaskExecutorRule` for LiveData.

Cases:

| Test | Asserts |
|------|---------|
| `computeStats_countsTotalUpcomingAndPast` | total/upcoming/past arithmetic |
| `saveEvent_rejectsBlankTitle` | validation error string |
| `saveEvent_allowsPastDateForNewEvent` | past date succeeds when repo returns Success |
| `saveEvent_callsRepositoryOnValidInput` | happy-path Success |

```bash
./gradlew test
```

### Instrumented

Smoke test under `androidTest` for package/context. Not a substitute for UI automation.

### What is intentionally not unit-tested here

Firebase SDK integration (needs device/emulator + project), chart rendering, and animation timing. Those are better as instrumented / manual QA.

---

## 14. Permissions & manifest

| Entry | Why |
|-------|-----|
| `INTERNET` | Auth / Firestore / FCM |
| `POST_NOTIFICATIONS` | API 33+ runtime gate before `notify()` |
| `SplashActivity` exported + LAUNCHER | Entry |
| Other activities not exported | Hardening |
| `EventMessagingService` | FCM callbacks |
| `default_notification_channel_id` | Points at `event_reminders` |

---

## 15. Failure modes

| Symptom | Diagnosis / fix |
|---------|-----------------|
| Gradle: Android Gradle Plugin requires Java 17 | Point Studio + `org.gradle.java.home` at JDK 17 |
| Plugin / missing google-services | File not under `app/`, or wrong package inside JSON |
| Auth `DEVELOPER_ERROR` / GMS package warnings | Add SHA-1; refresh `google-services.json` |
| Firestore `PERMISSION_DENIED` | Rules not published; user signed out; wrong uid path |
| Empty list after save | Listener error (Logcat); rules; offline without prior cache |
| Chart empty / crash | No events yet; keep rules for MPAndroidChart if minify ever enabled |
| No notification | Permission denied; token not from this install; emulator without Play Services |
| Date field looks clipped | Editor uses shorter `dd MMM yyyy` format + minHeight padding (already addressed in layout) |
| WindowLeaked on editor | Pickers dismissed in `onDestroy` (already handled) |

---

## 16. Extension points

If you take this beyond a sample:

1. **Per-event reminders** — Cloud Function on `onCreate`/`onUpdate` of event docs; schedule with Cloud Tasks / a queue; send FCM data+notification payloads the existing service already renders.  
2. **Indexes** — if you push filters into Firestore queries (`where` + `orderBy`), add composite indexes from the Console link in the exception.  
3. **Pagination** — swap full snapshot for query cursors once event counts leave “personal calendar” scale.  
4. **Release signing** — minify is off; ProGuard already keeps MPAndroidChart if you turn minify on later.  
5. **Token persistence** — store FCM token on the user doc for targeted (non-topic) sends.

---

## 17. Summary

Client-complete sample: Auth, owner-scoped live CRUD with offline cache, search/filter, dashboard metrics + chart, Material 3 theming, FCM receive path, demo seeder, and focused ViewModel unit tests. Follow §9 for local setup and §10 for Firebase config, then run on JDK 17.
