# 🛠️ Detailed Development Task List – On-Device M-PESA Expense Tracker

> Audience: absolute beginner Android/Kotlin developer. Follow tasks **in order**. Check items off as you complete them.

---

## 0. Prerequisites & Glossary

- **Android Studio** – the official IDE for Android development.
- **JDK (Java Development Kit)** – required by Android Studio; ships inside Studio.
- **Gradle** – the build system Android Studio uses.
- **ADB (Android Debug Bridge)** – lets your computer talk to Android devices/emulators.
- **Jetpack Compose** – modern toolkit for building Android UIs with Kotlin.
- **LLM (Large-Language Model)** – an AI model that understands & generates text.
- **Room** – Android library for on-device SQL databases.
- **SMS Content Provider** – Android API that lets apps read SMS messages (needs permission).

If any term is unfamiliar, pause and look it up before proceeding.

---

## 1. Set up Your Development Environment

1. Download & install **Android Studio Hedgehog or newer**: <https://developer.android.com/studio>.
2. Launch Studio → accept all SDK component downloads.
3. Inside Studio, open **Device Manager** → create an emulator (Pixel 4 API 34 is fine).
4. Verify you can run *any* sample project: `File ▸ New ▸ New Project ▸ Empty Compose Activity` → Run ►.
   - Troubleshoot until the emulator shows "Hello Android".
5. Enable **Developer Options** & **USB Debugging** on a real phone (optional but helpful): <https://developer.android.com/studio/debug/dev-options>.

---

## 2. Get the Demo Code Base Running

> Skip if you already ran the Healthcare demo earlier.

1. In Android Studio: *File ▸ Open* → choose your **`MpesaTracker`** folder (the renamed project directory).
2. Let Gradle sync; wait for **Build finished successfully**.
3. Press **Run**. Ensure the healthcare form app installs & works on emulator.
4. Commit a clean baseline in Git (`git commit -a -m "baseline demo compiles"`).

---

## 3. Create a New Git Branch for M-PESA Project

```bash
# from project root
git checkout -b mpesa-tracker
```

Keep this branch isolated; you can always return to `main`.

---

## 4. Project Renaming (**COMPLETED – you already did this**)

✔️  All rename steps are done – nothing more to do here.

---

## 5. App Permissions & Manifest Changes

1. Open `AndroidManifest.xml` under `app/src/main`.
2. Add permissions **above** the `<application>` tag:
   ```xml
   <uses-permission android:name="android.permission.READ_SMS"/>
   <uses-permission android:name="android.permission.RECEIVE_SMS"/>
   <!-- READ_PHONE_STATE no longer required just for SMS parsing -->
   ```
3. Inside `<application>` keep the existing activities.
4. *Run* the app → allow SMS permission when OS prompts.

> **Runtime request**: from Android 6 (API 23) onward you must also ask for `READ_SMS` at runtime—see new Section 17.

---

## 6. Define the Structured Tool for SMS Parsing

1. Create new file `app/src/main/java/com/google/sample/fcdemo/functioncalling/MpesaTools.kt` (same package path as `Tools.kt`).
   If you later change your root package, adjust accordingly.
2. Copy the **`mpesaSmsTool`** schema from the earlier assistant message.
3. Replace occurrences of `yourpkg` with your actual package path (e.g., `com.example.mpesa`).
4. Press **Sync Now** if Android Studio shows a Gradle bar.

---

## 7. Plug the Tool into the Generative Model

1. Open `FormViewModel.kt` (or create new `MpesaViewModel.kt` if you prefer not to touch the healthcare code).
2. In `createGenerativeModel()` replace `Tools.medicalFormTools` with `MpesaTools.mpesaSmsTool`.
3. Change the system prompt to something like:
   ```kotlin
   "You are an assistant that extracts structured transaction data from M-PESA SMS messages."
   ```
4. Rebuild project.

---

## 8. Design the Database Layer (Room)

1. Add Room dependencies **and the KAPT plugin** in `app/build.gradle.kts`:
   ```kotlin
   plugins {
       id("com.android.application")
       kotlin("android")
       kotlin("kapt")   // <- enables annotation processing for Room
   }
   ```
   Then add the Room libraries:
    ```kotlin
    implementation("androidx.room:room-runtime:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ```
2. Press **Sync Now**.
3. Create *data* package → `TransactionEntity.kt`:
   ```kotlin
   @Entity(tableName = "transactions")
   data class TransactionEntity(
       @PrimaryKey val transactionId: String,
       val direction: String,
       val amountKes: Double,
       val feeKes: Double,
       val counterparty: String,
       val channel: String,
       val dateTime: String,
       val balanceAfter: Double?,
       val rawMessage: String
   )
   ```
4. Create `TransactionDao.kt` with CRUD + query functions.
5. Create `MpesaDatabase.kt` extending `RoomDatabase`.
6. Write a quick *instrumented test* inserting & reading one entity.

---

## 9. Build an SMS Importer Service

1. Add a new `SmsReceiver.kt` extending `BroadcastReceiver` listening for `android.provider.Telephony.SMS_RECEIVED`.
2. Inside `onReceive` loop through `pdus`, build full SMS `body` string.
3. Call `MpesaViewModel.processSmsMessage(body)` (you may need a singleton or Hilt injection—keep it simple initially).
4. Register receiver in `AndroidManifest.xml` **inside** `<application>`:
   ```xml
   <receiver android:name=".SmsReceiver" android:exported="false">
       <!-- exported=false keeps broadcasts internal; no extra permission tag needed -->
       <intent-filter>
           <action android:name="android.provider.Telephony.SMS_RECEIVED" />
       </intent-filter>
   </receiver>
   ```
5. Test by sending yourself an M-PESA message using developer options or real phone.

---

## 10. Parse LLM Response & Persist Data

1. Copy `parseResponse()` pattern from healthcare demo.
2. Before inserting, call `TransactionDao.exists(transactionId)` (or a `SELECT` query) to **skip duplicates**; then insert the new entity.
3. After the flow updates, write via `TransactionDao.insert()`.
4. Wrap DB calls in `viewModelScope.launch(Dispatchers.IO) { … }`.

---

## 11. Compose UI – Phase 1 (List Only)

1. Create `MainScreen.kt` with a `LazyColumn` that observes `transactionsFlow` from ViewModel, e.g.
   ```kotlin
   val transactions by viewModel.transactionsFlow.collectAsState(emptyList())
   ```
2. Each list item shows: `date`, `counterparty`, `amount` (red for sent/paid, green for received).
3. Place a running daily/weekly total at top using `derivedStateOf`.
4. Make sure list updates as soon as a new SMS is parsed.

---

## 12. Compose UI – Phase 2 (Dashboards)

*Optional for later* – pie chart by category, bar chart by day. Libraries: `androidx.compose.ui:graphics`, or `ChartsCompose`.

---

## 13. Testing the Full Flow

1. In emulator (API 34 images only): use *Extended controls ▸ Phone ▸ SMS* to inject sample messages. For older images run `adb emu sms send <num> "body"`.
2. Verify list populates correctly.
3. Kill app ➜ reopen ➜ transactions persist (Room test).
4. Turn on airplane mode ➜ send SMS ➜ ensure parsing works offline.
5. Check performance: first parse should be < 200 ms on mid-range phone.

---

## 14. Play-store Readiness (Privacy & Compliance)

1. Create a *privacy policy* explaining SMS data stays on device.
2. In Play Console, request **READ_SMS permission approval** (required as of Android 13).
3. Add BiometricPrompt lock screen before showing transactions.
4. Update app icon, name, package ID.

---

## 15. Nice-to-Have Enhancements

- Auto-categorise spend using second LLM call.
- Export to CSV/PDF – `Storage Access Framework`.
- Voice queries: "How much did I spend on food this week?" → reuse speech recogniser.
- Backup/restore Room DB to Google Drive (optional).

---

## 16. Learning Resources

- *Android Basics with Compose* – free course: <https://developer.android.com/courses/android-basics-compose/course>
- *Room persistence* codelab: <https://developer.android.com/codelabs/android-room-with-a-view>
- *BroadcastReceiver* guide: <https://developer.android.com/guide/components/broadcasts>
- *Edge Local Agents & Hammer* repo: <https://github.com/google-gemini/edge-local-agents>

Good luck—tick off each section and you'll have a fully offline, privacy-respecting M-PESA expense tracker! 🚀 

---

## 17. Runtime SMS Permission Helper  

Add the following snippet to your first screen so beginners don't forget the runtime request:

```kotlin
private const val READ_SMS_REQ = 100

fun Activity.ensureSmsPermission() {
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_SMS), READ_SMS_REQ)
    }
}

override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
    if (requestCode == READ_SMS_REQ && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
        // permission granted, proceed
    }
}
```

Call `ensureSmsPermission()` from your `MainActivity.onCreate()`.

--- 