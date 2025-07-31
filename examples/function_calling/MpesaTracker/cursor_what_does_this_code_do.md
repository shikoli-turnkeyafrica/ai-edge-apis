# What does this code do
_Exported on 7/2/2025 at 22:26:44 GMT+3 from Cursor (1.1.7)_

---

**User**

What does this code do ? 

---

**Cursor**

In short, this project is a Jetpack Compose Android demo that lets a user fill in a three-page medical form simply by speaking.  
It combines on-device speech recognition with an on-device large-language model (“Hammer 2.1 1.5 B”) that supports structured “function calling”.  
The main flow is:

1. A form-filling “toolkit” is defined.  
   ```16:161:app/src/main/java/com/google/sample/fcdemo/functioncalling/Tools.kt
   object Tools {
       /* Enumerated choices ─ sex, marital status, medical conditions … */

       // One protobuf Tool containing five function declarations
       val medicalFormTools: Tool =
           Tool.newBuilder()
               .addFunctionDeclarations(…provide_name…)
               .addFunctionDeclarations(…provide_dob…)
               .addFunctionDeclarations(…provide_demographics…)
               .addFunctionDeclarations(…provide_occupation…)
               .addFunctionDeclarations(…update_medical_history…)
               .build()
   }
   ```
   • Each declaration specifies the function name, a natural-language description, and a JSON-schema-like list of parameters.  
   • Those declarations tell the LLM exactly what structured outputs it may return.

2. `FormViewModel` wires everything together.  
   ```1:118:app/src/main/java/com/google/sample/fcdemo/viewmodel/FormViewModel.kt
   val generativeModel by lazy { createGenerativeModel() }
   …
   private fun createGenerativeModel(): GenerativeModel {
       // Build a HammerFormatter, LlmInference backend, and a “system” prompt
       val model = GenerativeModel(llmInferenceBackend,
                                   systemInstruction,
                                   listOf(Tools.medicalFormTools))
       return model
   }
   ```
   • When the user speaks, `processVoiceInput()` sends the raw text to `chatSession.sendMessage()`.  
   • The Hammer model decides whether to emit plain text or call one of the declared functions; the tool call arrives as a structured protobuf in the `Part.functionCall` field.  
   • `parseResponse()` extracts each returned argument and updates the appropriate `StateFlow` that backs a text field, radio-group, or checkbox list.

3. Speech recognition UI (`SpeechRecognizer.kt`) streams partial results and volume levels while the user talks; when speech ends it hands the transcript to the view-model.

4. The composable screens (`ui/screens/…`) simply collect the reactive state from `FormViewModel` and render form fields, progress indicators, button bars, etc.

5. When the user taps “Submit”, `submitForm()` validates mandatory fields, shows a fake 2.5 s “saving” spinner, logs the collected data, and raises an error snack-bar if something is missing.

So the codebase showcases:

• Jetpack Compose UI with Material 3 design.  
• On-device speech-to-text via the Android `SpeechRecognizer` API.  
• On-device LLM inference (MediaPipe GenAI `LlmInference`) plus Google Edge Local Agents “function calling” to map natural language into well-typed form fields.  
• Modern Kotlin patterns (StateFlow, coroutines, one-way UI state).

That is the essence of what the code does.

```plaintext
   object Tools {
       /* Enumerated choices ─ sex, marital status, medical conditions … */

       // One protobuf Tool containing five function declarations
       val medicalFormTools: Tool =
           Tool.newBuilder()
               .addFunctionDeclarations(…provide_name…)
               .addFunctionDeclarations(…provide_dob…)
               .addFunctionDeclarations(…provide_demographics…)
               .addFunctionDeclarations(…provide_occupation…)
               .addFunctionDeclarations(…update_medical_history…)
               .build()
   }
```

```plaintext
   val generativeModel by lazy { createGenerativeModel() }
   …
   private fun createGenerativeModel(): GenerativeModel {
       // Build a HammerFormatter, LlmInference backend, and a “system” prompt
       val model = GenerativeModel(llmInferenceBackend,
                                   systemInstruction,
                                   listOf(Tools.medicalFormTools))
       return model
   }
```

---

**User**

Let's brainstorm on what are the other possible applications that can be developed with such on device large-language models 

---

**Cursor**

Below are 25 concrete ideas—grouped by theme—for apps that benefit uniquely from running a large-language model entirely on the user’s device (no cloud calls).

Key advantages you can leverage:
• Instant responses (sub-100 ms)  
• Works offline / in poor connectivity  
• Data never leaves the phone → strong privacy & regulatory compliance  
• No per-query costs, easier scaling  

────────────────────────────────────────
A. Personal productivity & organization
1. “Offline GTD Coach” – watches clipboard, messages, calendar; rewrites quick notes into tasks, suggests priorities, and schedules them.  
2. Contextual email & SMS summarizer – shows one-line TL;DR bubbles and auto-drafts replies without uploading mail content to servers.  
3. Personal journal companion – takes voice logs each night, asks reflective questions, tags mood & topics for later search.  
4. Smart clipboard – automatically classifies copied text (address, product link, quote, code-snippet), offers the right next step (map, share, format, run).

B. Accessibility & language
5. Live caption + translation overlay – transcribes ambient speech and translates into user’s language fully offline (good for travel or deaf users).  
6. On-device dyslexia assistant – reflows any selected text into simpler sentences, adds image hints, preserves privacy because reading material never leaves phone.  
7. ASL (sign-language) voiceover – camera → gesture recognition → LLM turns recognized signs into natural-sounding speech.

C. Creativity & content
8. Story-co-writer – offline playground for kids to dictate or type story ideas; LLM suggests plot twists, character names, illustrations using local diffusion models.  
9. “Pocket Dungeon-Master” – generates tabletop RPG encounters, NPC dialog, and keeps track of the campaign state without Internet.  
10. Instant lyrics & chord generator – listens to guitar strumming pattern, proposes matching chords and original lyrics in real time.

D. Health & wellbeing
11. Private mental-health journaling chatbot – CBT-style questioning, mood tracking, crisis resources, all processed locally for maximal confidentiality.  
12. Medication Q&A – scan pill bottle, ask dosage or interaction questions; LLM reasons over an on-device pharmacology database.  
13. Personal nutrition log – voice-describe meal, model extracts ingredients, estimates macros, syncs summary (not raw data) to cloud later.

E. Education
14. Offline tutor for STEM – solve math problems step-by-step, explain misconceptions, all on a school-locked tablet with no Internet.  
15. “Explain this photo” science field app – students snap plant/insect images, model gives taxonomy info, related facts, quiz questions.  
16. Flash-card generator – reads textbook PDF locally, produces spaced-repetition decks; respects copyright via on-device processing.

F. Smart device interaction
17. Natural-language home-automation hub – voice-control smart lights, thermostats, media without routing commands through vendor clouds.  
18. In-car assistant – plan routes, summarize messages, adjust climate; continues to work in tunnels or rural areas.  
19. Wear-OS workout coach – parses user-spoken metrics (“ran 5 K in 27 minutes”), tracks progress, suggests intervals, all on the watch.

G. Security & compliance
20. Law-enforcement field note taker – officers dictate reports; sensitive data stays local, model outputs structured templates (dates, names, location).  
21. Corporate meeting summarizer – BYOD phone records meeting, produces minutes & assigned actions; complies with strict data-residency rules.

H. Code & tech
22. Offline code-snippet explainer – IDE plugin on a tablet/phone that can explain or refactor snippets while flying.  
23. IoT device setup wizard – user describes network/environment; LLM outputs correct config scripts flashed directly over BLE/Wi-Fi.

I. Entertainment & social
24. Dynamic visual novel engine – runs story logic & branching dialogue locally; user choices passed to LLM for personalized plot.  
25. Privacy-preserving dating-profile coach – analyzes photos & bio locally, suggests improvements, drafts openers based on matches’ bios without uploading any personal data.

────────────────────────────────────────
How to extend the current demo codebase
• Swap the medical form tool schema for any domain schema—e.g., home-automation commands, journaling entries, workout metrics.  
• Adjust UI composables to display new structured fields.  
• Point `LlmInferenceOptions` to a domain-fine-tuned Hammer model or another quantized LLM.  
• Reuse the speech-recognition & state-flow scaffolding as-is.

These ideas exploit the same tech stack you already have: local speech-to-text + function-calling LLM + Compose UI, just wrapped around different schemas and contexts.

---

**User**

interested in using it to summarize my mpesa sms messages... That would be cool to help me track my expenses: 
-----------------
TG20A8OHCO Confirmed. Ksh800.00 sent to DTB PREPAID for account 050265 on 2/7/25 at 1:54 PM New M-PESA balance is Ksh0.00. Transaction cost, Ksh10.00.Amount you can transact within the day is 496,200.00. Save frequent paybills for quick payment on M-PESA app https://bit.ly/mpesalnk

TG1043W5HO Confirmed. Ksh490.00 paid to Artcaffe Coffee And Bakery Ltd - Lavington Mall. on 1/7/25 at 10:06 AM.New M-PESA balance is Ksh0.00. Transaction cost, Ksh0.00. Amount you can transact within the day is 499,510.00. Save frequent Tills for quick payment on M-PESA app https://bit.ly/mpesalnk

TFU1Z6MQJD Confirmed. Ksh2,100.00 paid to BOULANGERIE AND COMPANY LTD RIVERSIDE. on 30/6/25 at 2:27 PM.New M-PESA balance is Ksh0.00. Transaction cost, Ksh0.00. Amount you can transact within the day is 471,400.00. Save frequent Tills for quick payment on M-PESA app https://bit.ly/mpesalnk

TFT0VMWQ5Y Confirmed.on 29/6/25 at 6:36 PMKsh400.00 received from 254723339995 BRIAN Tugee TUGEE MALINGA. New Account balance is Ksh4,773.94. Transaction cost, Ksh2.20.

---

**Cursor**

Great idea!  An on-device LLM can act as a private “personal accountant” that reads your incoming M-PESA SMS, extracts the important fields, and keeps a running ledger & dashboards—no cloud, no servers, no APIs to trust.

Below is a concrete blueprint showing:

1. What the model needs to extract  
2. A “function-calling” schema you can plug into the current demo code  
3. Overall app architecture & data flow  
4. Optional extras you can add later  

────────────────────────────────────
1. Data you care about
For each SMS we typically want:

• `direction`           → “sent”, “paid”, “received”  
• `amount_kes`          → 800.00, 490.00 …  
• `fee_kes`             → 10.00, 0.00, 2.20 …  
• `counterparty`        → “DTB PREPAID”, “Artcaffe Coffee…”, phone number + name when P2P  
• `channel`             → “Paybill”, “Till”, “P2P”, “Withdrawal”, “Airtime”, etc.  
• `date` & `time`       → 2025-07-02 13:54  
• `transaction_id`      → TG20A8OHCO …  
• `balance_after_txn`   (optional)  
• `raw_message`         (for traceability)

That is all the LLM must return; you can compute “expense vs income”, running totals, category tags, etc. in regular Kotlin code.

────────────────────────────────────
2. Function-calling tool schema (Kotlin snippet)

```kotlin
val mpesaSmsTool: Tool =
  Tool.newBuilder()
    .addFunctionDeclarations(
      FunctionDeclaration.newBuilder()
        .setName("parse_mpesa_sms")
        .setDescription("Parse an M-PESA transaction SMS and return structured data.")
        .setParameters(
          Schema.newBuilder()
            .setType(OBJECT)
            .putProperties("transaction_id",
              Schema.newBuilder().setType(STRING).setDescription("Alphanumeric code at start").build())
            .putProperties("direction",
              Schema.newBuilder().setType(STRING).setFormat("enum")
                .addEnum("sent").addEnum("paid").addEnum("received").addEnum("withdrawn")
                .setDescription("Money flow direction").build())
            .putProperties("amount_kes",
              Schema.newBuilder().setType(STRING).setDescription("Amount in Kenyan shillings").build())
            .putProperties("fee_kes",
              Schema.newBuilder().setType(STRING).setDescription("Transaction fee in KES").build())
            .putProperties("counterparty",
              Schema.newBuilder().setType(STRING).setDescription("Payee / payer / business name").build())
            .putProperties("channel",
              Schema.newBuilder().setType(STRING).setDescription("Paybill, Till, P2P, etc.").build())
            .putProperties("date_time",
              Schema.newBuilder().setType(STRING).setDescription("Timestamp in ISO 8601").build())
            .putRequired("transaction_id")
            .addRequired("direction")
            .addRequired("amount_kes")
            .addRequired("counterparty")
            .build()
        )
    )
    .build()
```

Plug this into the `GenerativeModel` tool list exactly like the medical-form demo did with `Tools.medicalFormTools`.

────────────────────────────────────
3. End-to-end flow

```
┌── Android SMS content provider
│
│   (new message arrives or on initial import)
│        ▼
│ 1. Raw SMS string
│        ▼
┌───── ViewModel ─────┐
│ _isProcessing = true│
│ chatSession.sendMessage(sms)  ─┐
└──────────────────────────────┐ │
                               ▼ ▼
                    On-device LLM (Hammer / Gemma / ★) 
                    + function-calling schema
                               │
               Structured call: parse_mpesa_sms{...}
                               │
┌───── ViewModel ──────────────┤
│ parseResponse() fills a Room │
│  table TxnEntity(…)          │
└─────────────┬────────────────┘
              ▼
      Compose UI screens
   • “Today’s spend    1 290 KES”
   • categories, charts
   • search by payee, date range
```

Implementation notes:

• SMS import  
  ```kotlin
  val projection = arrayOf("address", "body", "date")
  contentResolver.query(Uri.parse("content://sms/inbox"), projection, null, null, null)
  ```  
  Watch `ContentObserver` for real-time updates.

• One ViewModel similar to `FormViewModel` keeps Trackers: totals today, this month, per channel, etc.

• Persist transactions in Room so they survive reboots and can be aggregated quickly.

• The LLM is only invoked once per new message; afterwards you run pure SQL/Kotlin.

• Average M-PESA SMS ≤ 180 chars → the 1.5 B Q8 model answers in ≈ 10 ms on modern phones.

────────────────────────────────────
4. Nice extras to plan for

1. Duplicate detection  
   Ignore messages whose `transaction_id` already exists in DB.

2. Auto-categorisation  
   After parsing, send just the fields (not raw SMS) back through the LLM with a small prompt:  
   “Return one word budget category for counterparty & channel …”.  
   Store the category string.

3. Offline PDF / CSV export  
   Generate monthly statements on device and let the user share them.

4. Budget alerts  
   Compose rules: “Notify when dining out > Ksh5 000 this week”.

5. Voice queries  
   Re-use the speech recogniser to ask: “How much did I spend on coffee in June?”

6. Privacy lock  
   Use BiometricPrompt so data is revealed only after fingerprint.

────────────────────────────────────
Getting started in this repo

1. Create a new `MpesaTools.kt` with the schema above.  
2. Replace `Tools.medicalFormTools` in `createGenerativeModel()` with your new tool.  
3. Swap the UI: instead of form fields, show a `LazyColumn` of parsed transactions.  
4. Add an `SmsImporter` service that feeds each SMS string to `processSmsMessage(smsText)`, which mirrors `processVoiceInput()` but uses the SMS body.  
5. Persist results in Room.

That’s it—you now have a fully offline M-PESA expense tracker powered by an on-device LLM!

```plaintext
val mpesaSmsTool: Tool =
  Tool.newBuilder()
    .addFunctionDeclarations(
      FunctionDeclaration.newBuilder()
        .setName("parse_mpesa_sms")
        .setDescription("Parse an M-PESA transaction SMS and return structured data.")
        .setParameters(
          Schema.newBuilder()
            .setType(OBJECT)
            .putProperties("transaction_id",
              Schema.newBuilder().setType(STRING).setDescription("Alphanumeric code at start").build())
            .putProperties("direction",
              Schema.newBuilder().setType(STRING).setFormat("enum")
                .addEnum("sent").addEnum("paid").addEnum("received").addEnum("withdrawn")
                .setDescription("Money flow direction").build())
            .putProperties("amount_kes",
              Schema.newBuilder().setType(STRING).setDescription("Amount in Kenyan shillings").build())
            .putProperties("fee_kes",
              Schema.newBuilder().setType(STRING).setDescription("Transaction fee in KES").build())
            .putProperties("counterparty",
              Schema.newBuilder().setType(STRING).setDescription("Payee / payer / business name").build())
            .putProperties("channel",
              Schema.newBuilder().setType(STRING).setDescription("Paybill, Till, P2P, etc.").build())
            .putProperties("date_time",
              Schema.newBuilder().setType(STRING).setDescription("Timestamp in ISO 8601").build())
            .putRequired("transaction_id")
            .addRequired("direction")
            .addRequired("amount_kes")
            .addRequired("counterparty")
            .build()
        )
    )
    .build()
```

```plaintext
┌── Android SMS content provider
│
│   (new message arrives or on initial import)
│        ▼
│ 1. Raw SMS string
│        ▼
┌───── ViewModel ─────┐
│ _isProcessing = true│
│ chatSession.sendMessage(sms)  ─┐
└──────────────────────────────┐ │
                               ▼ ▼
                    On-device LLM (Hammer / Gemma / ★) 
                    + function-calling schema
                               │
               Structured call: parse_mpesa_sms{...}
                               │
┌───── ViewModel ──────────────┤
│ parseResponse() fills a Room │
│  table TxnEntity(…)          │
└─────────────┬────────────────┘
              ▼
      Compose UI screens
   • “Today’s spend    1 290 KES”
   • categories, charts
   • search by payee, date range
```

```plaintext
  val projection = arrayOf("address", "body", "date")
  contentResolver.query(Uri.parse("content://sms/inbox"), projection, null, null, null)
```

---

**User**

@/mpesaapptracker Can you create a detailed task list in this folder for all the Development activities I need to make to make the above work. Remember the task list is for an novice android developer with like 0 knowledge of android so be detailed. 

---

**Cursor**

```markdown
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
   - Troubleshoot until the emulator shows “Hello Android”.
5. Enable **Developer Options** & **USB Debugging** on a real phone (optional but helpful): <https://developer.android.com/studio/debug/dev-options>.

---

## 2. Get the Demo Code Base Running

> Skip if you already ran the Healthcare demo earlier.

1. In Android Studio: *File ▸ Open* → choose the folder `healthcare_form_demo` (your local copy).
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

## 4. Project Renaming (Optional but Recommended)

1. Inside *settings.gradle.kts* update `rootProject.name` to `MpesaTracker`.
2. Rename the top-level folder in Android Studio *Refactor ▸ Rename* (Studio handles packages).
3. Run the app again to ensure nothing broke.

---

## 5. App Permissions & Manifest Changes

1. Open `AndroidManifest.xml` under `app/src/main`.
2. Add permissions **above** the `<application>` tag:
   ```xml
   <uses-permission android:name="android.permission.READ_SMS"/>
   <uses-permission android:name="android.permission.RECEIVE_SMS"/>
   <uses-permission android:name="android.permission.READ_PHONE_STATE"/>
   ```
3. Inside `<application>` keep the existing activities.
4. *Run* the app → allow SMS permission when OS prompts.

---

## 6. Define the Structured Tool for SMS Parsing

1. Create new file `app/src/main/java/com/yourpkg/mpesatracker/functioncalling/MpesaTools.kt`.
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

1. Add Room dependencies in `app/build.gradle.kts`:
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
   <receiver android:name=".SmsReceiver" android:exported="true">
       <intent-filter>
           <action android:name="android.provider.Telephony.SMS_RECEIVED" />
       </intent-filter>
   </receiver>
   ```
5. Test by sending yourself an M-PESA message using developer options or real phone.

---

## 10. Parse LLM Response & Persist Data

1. Copy `parseResponse()` pattern from healthcare demo.
2. After the flow updates, convert data to `TransactionEntity` and write via `TransactionDao.insert()` **if** `transactionId` not already present.
3. Wrap DB calls in `viewModelScope.launch(Dispatchers.IO) { … }`.

---

## 11. Compose UI – Phase 1 (List Only)

1. Create `MainScreen.kt` with a `LazyColumn` that observes `transactionsFlow` from ViewModel.
2. Each list item shows: `date`, `counterparty`, `amount` (red for sent/paid, green for received).
3. Place a running daily/weekly total at top using `derivedStateOf`.
4. Make sure list updates as soon as a new SMS is parsed.

---

## 12. Compose UI – Phase 2 (Dashboards)

*Optional for later* – pie chart by category, bar chart by day. Libraries: `androidx.compose.ui:graphics`, or `ChartsCompose`.

---

## 13. Testing the Full Flow

1. In emulator: use *Extended controls ▸ Phone ▸ SMS* to inject sample M-PESA messages.
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
- Voice queries: “How much did I spend on food this week?” → reuse speech recogniser.
- Backup/restore Room DB to Google Drive (optional).

---

## 16. Learning Resources

- *Android Basics with Compose* – free course: <https://developer.android.com/courses/android-basics-compose/course>
- *Room persistence* codelab: <https://developer.android.com/codelabs/android-room-with-a-view>
- *BroadcastReceiver* guide: <https://developer.android.com/guide/components/broadcasts>
- *Edge Local Agents & Hammer* repo: <https://github.com/google-gemini/edge-local-agents>

Good luck—tick off each section and you’ll have a fully offline, privacy-respecting M-PESA expense tracker! 🚀
```

I’ve added a new file `mpesaapptracker/DETAILED_TASK_LIST.md` containing a step-by-step roadmap—from installing Android Studio all the way to Play-Store compliance—written for someone completely new to Android development.  

Open the file when you’re ready and start working through each section. Feel free to let me know whenever you need clarification or extra guidance on any step!

Glad the task list is in place and you’ve already created the `mpesa-tracker` branch!  
If there’s anything else you’d like clarified—code snippets, deeper explanations of any step, or help implementing the very first tasks—just let me know.

---

**User**

Inside *settings.gradle.kts* update `rootProject.name` to `MpesaTracker`.

---

**Cursor**

```gradle-kotlin-dsl
// ... existing code ...
-rootProject.name = "Function Calling Demo"
+rootProject.name = "MpesaTracker"
// ... existing code ...
```

Updated `settings.gradle.kts`: the project is now named `MpesaTracker`.  
Sync Gradle in Android Studio (or run `./gradlew build`) to confirm everything still compiles. Let me know if you hit any issues!

---

**User**

Please explain what this in detail ? Rename from what to what ? 

---

**Cursor**

“Rename the top-level folder” means changing the **physical directory name** that contains your whole Android project so that it matches the new project name you just put in `settings.gradle.kts`.

Why do it?
• Keeps the folder name, Gradle project name (`rootProject.name`) and eventual GitHub repo name consistent.  
• Makes it easier to recognise the project on disk and in Android Studio’s Recent Projects list.

What are we renaming?
• Current folder on disk: `healthcare_form_demo`  
  (that’s the directory that holds `settings.gradle.kts`, `build.gradle.kts`, the `app/` module, etc.)  
• Desired new folder name: `MpesaTracker`  (or any identical spelling you used in `rootProject.name`).

Detailed step-by-step (Windows or macOS):

1. **Close the project in Android Studio**  
   • File ▸ Close Project (puts you back on the Welcome screen).  
   Closing avoids confusing Studio while its files move.

2. **Rename the directory**  
   • File Explorer / Finder: navigate to the parent folder that contains `healthcare_form_demo`.  
   • Right-click `healthcare_form_demo` ▸ Rename → type `MpesaTracker` → press Enter.  
   (If you prefer, you can do the same with Git-Bash or PowerShell:  
   `mv healthcare_form_demo MpesaTracker`)

3. **Re-open the project in Android Studio**  
   • Welcome screen ▸ Open ▸ select the new `MpesaTracker` folder ▸ OK.  
   Studio will resync Gradle; the window title will now read “MpesaTracker”.

4. **Update Git** (one-time)  
   Your Git history is intact because the hidden `.git` directory moved with the rename.  
   If you had the repo open in a terminal, just `cd MpesaTracker`.  
   Git status should show no changes except maybe IDE files.

5. **Run the app**  
   Press the green Run ► button to confirm everything still compiles after the rename.

That’s all—you’ve renamed the project folder from **`healthcare_form_demo` → `MpesaTracker`** to keep everything consistent.

