# BoldSign Webhook & Database Migration
## Fintech Insurance – Technical Documentation
**Date:** March 2, 2026  
**Module:** Contract Signing Integration

---

## Table of Contents

1. [Overview](#1-overview)
2. [BoldSign Webhook – What Is It?](#2-boldsign-webhook--what-is-it)
3. [How the Webhook Is Integrated](#3-how-the-webhook-is-integrated)
4. [Step-by-Step Contract Signing Flow](#4-step-by-step-contract-signing-flow)
5. [WebhookController – Code Explained](#5-webhookcontroller--code-explained)
6. [Webhook JSON Payload Format](#6-webhook-json-payload-format)
7. [RequestStatus Lifecycle](#7-requeststatus-lifecycle)
8. [Database Migration – What Is It?](#8-database-migration--what-is-it)
9. [DbMigration – Code Explained](#9-dbmigration--code-explained)
10. [Why Migration Instead of Manual SQL?](#10-why-migration-instead-of-manual-sql)
11. [How Spring Boot and JavaFX Start Together](#11-how-spring-boot-and-javafx-start-together)
12. [BoldSign Webhook Setup Guide](#12-boldsign-webhook-setup-guide)
13. [Summary Diagram](#13-summary-diagram)

---

## 1. Overview

The Fintech Insurance application integrates two backend mechanisms that work together to automate the contract signing lifecycle:

| Mechanism | Purpose |
|-----------|---------|
| **BoldSign Webhook** | Automatically updates a contract request status in the database when the user signs (or declines) the contract via email |
| **Database Migration** | Automatically adds the `boldsign_document_id` column to the `contract_request` table at application startup, without requiring manual SQL |

Together, these two features close the contract signing loop:  
**Admin approves → PDF generated → Sent to user via BoldSign → User signs → Webhook fires → Status set to SIGNED**

---

## 2. BoldSign Webhook – What Is It?

### Background

**BoldSign** is an electronic signature service. When the admin approves a contract request, the application:
1. Generates a PDF contract using `PDFService`
2. Sends that PDF to the user's email via the BoldSign API (`BoldSignService`)
3. BoldSign hosts the document and lets the user sign it electronically

### The Problem Without a Webhook

Without a webhook, the application would have **no way to know** when the user actually signed the document. The contract request would stay in `APPROVED` status forever, and the admin would have to manually check BoldSign's dashboard.

### The Solution: Webhook

A **webhook** is an HTTP callback. When an event happens in BoldSign (e.g., a user signs a document), BoldSign sends an automatic **HTTP POST request** to a URL that you configure in your BoldSign dashboard. This URL points to our application's `WebhookController`.

```
User signs in BoldSign  →  BoldSign sends POST  →  /webhook/boldsign  →  DB updated to SIGNED
```

---

## 3. How the Webhook Is Integrated

The webhook endpoint is a **Spring Boot REST controller** running on port `8080` inside the same process as the JavaFX application.

### Architecture

```
┌─────────────────────────────────────────────────────┐
│                  JavaFX Process                      │
│                                                      │
│  ┌──────────────┐      ┌────────────────────────┐   │
│  │  JavaFX UI   │      │  Spring Boot (port 8080)│   │
│  │  (FX Thread) │      │  WebhookController      │   │
│  └──────────────┘      │  POST /webhook/boldsign │   │
│                         └───────────┬────────────┘   │
└───────────────────────────────────┬─────────────────┘
                                    │ HTTP POST
                              ┌─────▼──────┐
                              │  BoldSign  │
                              │  Servers   │
                              └────────────┘
```

### Spring Boot Startup (Main.java)

Spring Boot starts in a **background daemon thread** before the JavaFX UI appears. A `CountDownLatch` ensures JavaFX waits until Spring is fully ready before showing the login screen:

```java
// Main.java – init() runs before start()
private static final CountDownLatch springLatch = new CountDownLatch(1);

@Override
public void init() {
    Thread springThread = new Thread(() -> {
        try {
            springContext = new SpringApplicationBuilder(SpringBootApp.class)
                    .headless(false)
                    .run(new String[0]);
            System.out.println("[Spring] ✓ Started – webhook on http://localhost:8080/webhook/boldsign");
        } finally {
            springLatch.countDown();  // release JavaFX to proceed
        }
    }, "spring-boot-thread");
    springThread.setDaemon(true);
    springThread.start();

    springLatch.await(30, TimeUnit.SECONDS);  // wait up to 30s
}
```

### Why a Daemon Thread?

Setting `springThread.setDaemon(true)` means the Spring thread automatically dies when the JavaFX application exits — no zombie processes, no manual cleanup needed (although `stop()` also calls `springContext.close()`).

---

## 4. Step-by-Step Contract Signing Flow

```
1. USER submits a contract request (status = PENDING)
         ↓
2. ADMIN opens "Contract Requests" tab, selects the request
         ↓
3. ADMIN clicks APPROVE
         ↓
4. AdminDashboardController.handleApproveRequest():
   a. Sets status = APPROVED in DB
   b. Starts background thread (boldsign-thread)
         ↓
5. boldsign-thread:
   a. Loads User, InsuredAsset, InsurancePackage from DB
   b. Builds BoldSignService with:
         - userName       (from User)
         - assetReference (from InsuredAsset)
         - packageName    (from InsurancePackage)
         - approvedValue  (from InsuredAsset.approvedValue or declaredValue)
         - contractDate   (today)
         - signerEmail    (from User)
   c. Calls boldSign.sendForSignature():
         → PDFService generates contract PDF
         → HTTP POST to BoldSign API /v1/document/send
         → BoldSign emails the PDF to the user
         → BoldSign returns JSON: {"documentId": "xxxx-xxxx"}
   d. Saves documentId to contract_request.boldsign_document_id in DB
         ↓
6. USER receives email from BoldSign, opens document, signs it
         ↓
7. BoldSign sends POST to http://<server>:8080/webhook/boldsign
   with payload:
   {
     "Event": { "EventType": "document_completed" },
     "Data":  { "DocumentId": "xxxx-xxxx" }
   }
         ↓
8. WebhookController.handleBoldSignWebhook():
   a. Parses EventType and DocumentId from JSON
   b. Calls contractRequestDAO.findByBoldSignDocumentId("xxxx-xxxx")
   c. Sets status = SIGNED
   d. Calls contractRequestDAO.update(req)
         ↓
9. DB: contract_request.status = 'SIGNED'
   Admin refreshes → sees SIGNED in cyan in the table
```

---

## 5. WebhookController – Code Explained

**File:** `src/main/java/controller/WebhookController.java`

```java
@RestController              // Registers as a Spring REST controller
@RequestMapping("/webhook")  // Base URL path
public class WebhookController {

    private final ContractRequestDAO contractRequestDAO = new ContractRequestDAO();

    @PostMapping("/boldsign")  // Listens on POST /webhook/boldsign
    public ResponseEntity<String> handleBoldSignWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "X-BoldSign-Signature", required = false) String signature) {
```

### Key Decisions Explained

| Decision | Reason |
|----------|--------|
| `@RequestBody String payload` | Receives the raw JSON string so we can parse it ourselves without needing extra Jackson config |
| `required = false` for signature header | BoldSign may or may not include the header; making it optional prevents a 400 error if absent |
| Always returns `ResponseEntity.ok("Received")` | BoldSign retries failed webhooks. Returning 200 even on internal errors prevents infinite retries |
| PascalCase + camelCase fallback | BoldSign uses `EventType` / `DocumentId` (PascalCase) but some SDK versions use camelCase — we handle both |

### Event Handling

```java
switch (eventType.toLowerCase()) {
    case "document_completed" -> markAsSigned(documentId);    // User signed
    case "document_declined"  -> markAsDeclined(documentId);  // User declined
    case "document_revoked"   -> markAsDeclined(documentId);  // Admin revoked
    default -> System.out.println("[Webhook] Unhandled event: " + eventType);
}
```

### Database Lookup by Document ID

```java
private void markAsSigned(String documentId) {
    // Find the contract request that was sent with this BoldSign document ID
    ContractRequest req = contractRequestDAO.findByBoldSignDocumentId(documentId);
    if (req == null) return; // No match – log and ignore

    req.setStatus(RequestStatus.SIGNED);
    contractRequestDAO.update(req);
}
```

The key is `findByBoldSignDocumentId()` — this queries:
```sql
SELECT * FROM contract_request WHERE boldsign_document_id = ?
```
This is only possible because we stored the document ID at approval time (step 5d above).

---

## 6. Webhook JSON Payload Format

BoldSign sends the following JSON to the webhook URL:

```json
{
  "Event": {
    "EventType": "document_completed",
    "EventTime": "2026-03-02T10:00:00Z"
  },
  "Data": {
    "DocumentId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "Title": "Insurance Contract - REF-2026-001",
    "Status": "Completed",
    "CreatedDate": "2026-03-01T08:00:00Z"
  }
}
```

### Event Types We Handle

| EventType | Action in our app |
|-----------|-------------------|
| `document_completed` | All signers have signed → status = **SIGNED** |
| `document_declined` | A signer declined to sign → status = **REJECTED** |
| `document_revoked` | Admin revoked the document → status = **REJECTED** |
| Any other | Logged and ignored |

---

## 7. RequestStatus Lifecycle

The `RequestStatus` enum tracks where a contract request is in its lifecycle:

```
                ┌─────────┐
  User submits  │ PENDING │
                └────┬────┘
                     │
          ┌──────────┴──────────┐
          ▼                     ▼
     ┌──────────┐         ┌──────────┐
     │ APPROVED │         │ REJECTED │  ← Admin rejects
     └────┬─────┘         └──────────┘
          │
          │  BoldSign email sent
          │  User receives & signs
          │
          ▼  (webhook fires)
       ┌────────┐
       │ SIGNED │  ← document_completed event
       └────────┘
          │
          │  OR if user declines in BoldSign
          ▼  (webhook fires)
       ┌──────────┐
       │ REJECTED │  ← document_declined / document_revoked
       └──────────┘
```

### UI Colour Coding

| Status | Colour | Meaning |
|--------|--------|---------|
| PENDING | Gold `#f5c800` | Waiting for admin action |
| APPROVED | Green `#44cc44` | Admin approved, contract sent |
| REJECTED | Red `#cc2200` | Rejected by admin or declined by user |
| SIGNED | Cyan `#00ccff` | User signed — contract is complete |

---

## 8. Database Migration – What Is It?

### The Problem

When we added the BoldSign integration, the `contract_request` table in the database needed a new column:

```sql
ALTER TABLE contract_request
ADD COLUMN boldsign_document_id VARCHAR(255) DEFAULT NULL;
```

Without this column, the application cannot store the BoldSign document ID, which means the webhook cannot look up which contract request was signed.

### The Challenge

Every developer and every deployed environment has their own database. If we just added the column to the schema file, everyone would need to:
1. Notice that the schema changed
2. Manually run the `ALTER TABLE` SQL
3. Remember to do this every time the schema changes

This is error-prone and breaks the application silently if forgotten.

### The Solution: Automatic Migration

`DbMigration.java` runs the `ALTER TABLE` statement **automatically** every time the application starts. It is safe to run multiple times — if the column already exists, it is simply skipped.

---

## 9. DbMigration – Code Explained

**File:** `src/main/java/tn/esprit/utils/DbMigration.java`

```java
public class DbMigration {

    public static void run() {
        Connection conn = MyDB.getInstance().getConx();
        if (conn == null) {
            System.out.println("[Migration] Skipped – no DB connection.");
            return;  // App still opens, just without DB
        }

        runStatement(conn,
            "ALTER TABLE `contract_request` " +
            "ADD COLUMN IF NOT EXISTS `boldsign_document_id` VARCHAR(255) DEFAULT NULL",
            "contract_request.boldsign_document_id");
    }

    private static void runStatement(Connection conn, String sql, String description) {
        try (Statement st = conn.createStatement()) {
            st.executeUpdate(sql);
            System.out.println("[Migration] OK  : " + description);
        } catch (SQLException e) {
            // Handle MySQL < 8.0 that doesn't support IF NOT EXISTS
            if (e.getMessage().toLowerCase().contains("duplicate column")) {
                System.out.println("[Migration] SKIP: " + description + " (already exists)");
            } else {
                System.out.println("[Migration] WARN: " + description + " – " + e.getMessage());
            }
        }
    }
}
```

### Line-by-Line Explanation

| Code | Purpose |
|------|---------|
| `MyDB.getInstance().getConx()` | Reuses the singleton DB connection — no new connection opened |
| `if (conn == null) return` | Graceful degradation — app opens even if DB is down |
| `ADD COLUMN IF NOT EXISTS` | MySQL/MariaDB 10.3+ syntax — safe to run on any existing database |
| `contains("duplicate column")` | Fallback for older MySQL that doesn't support `IF NOT EXISTS` |
| `[Migration] OK / SKIP / WARN` | Console output so developers can see what happened at startup |

### Where It Is Called

```java
// Main.java – start() runs after Spring Boot is ready
@Override
public void start(Stage stage) throws Exception {
    DbMigration.run();                        // ← runs migrations first
    SceneManager.setPrimaryStage(stage);
    SceneManager.switchScene("/View/Login.fxml", "Fintech Insurance - Login");
}
```

It runs **after** Spring Boot is up but **before** the login screen is shown — guaranteeing the schema is correct before any user can interact with the app.

### Console Output at Startup

```
Connected to DB!
[Migration] OK  : contract_request.boldsign_document_id    ← first run
[Spring] ✓ Started – webhook listening on http://localhost:8080/webhook/boldsign
```

On subsequent runs:
```
Connected to DB!
[Migration] SKIP: contract_request.boldsign_document_id (already exists)
[Spring] ✓ Started – webhook listening on http://localhost:8080/webhook/boldsign
```

---

## 10. Why Migration Instead of Manual SQL?

| Approach | Manual SQL | DbMigration (our approach) |
|----------|-----------|---------------------------|
| Developer needs to run SQL manually | ✅ Yes | ❌ No — automatic |
| Safe to run multiple times | ❌ No — errors if column exists | ✅ Yes — idempotent |
| Works on fresh database | ✅ Yes | ✅ Yes |
| Works on existing database | ⚠️ Only if they remember | ✅ Always |
| Visible in version control | ⚠️ Scattered in docs/notes | ✅ In Java code, same commit |
| App breaks if skipped | ✅ Yes | ❌ Never |

---

## 11. How Spring Boot and JavaFX Start Together

The `Main.java` startup sequence is carefully ordered to ensure both frameworks coexist without conflict:

```
JVM starts Main.main()
        │
        ▼
Application.launch()  →  calls init() on a non-FX thread
        │
        ├── Starts spring-boot-thread (daemon)
        │       └── SpringApplicationBuilder(SpringBootApp.class).run()
        │               └── Spring registers WebhookController
        │               └── Tomcat binds to port 8080
        │               └── springLatch.countDown()  ← releases JavaFX
        │
        ├── springLatch.await(30s)  ← waits here until Spring is ready
        │
        ▼
JavaFX calls start(Stage stage)
        │
        ├── DbMigration.run()     ← ensures schema is up to date
        ├── SceneManager.setPrimaryStage(stage)
        └── switchScene("/View/Login.fxml")  ← UI appears
```

### Key Property: `scanBasePackages`

`SpringBootApp.java` tells Spring to scan **both** packages:

```java
@SpringBootApplication(
    exclude = { DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class },
    scanBasePackages = {"tn.esprit", "controller"}  // ← includes WebhookController
)
public class SpringBootApp { }
```

Without `"controller"` in `scanBasePackages`, Spring would only scan `tn.esprit.*` and `WebhookController` would never be registered — the webhook endpoint would return 404.

---

## 12. BoldSign Webhook Setup Guide

To make the webhook work in production, BoldSign must be able to reach your application over the internet.

### Step 1 — Expose Port 8080

**For local development**, use [ngrok](https://ngrok.com):
```bash
ngrok http 8080
# Output: Forwarding  https://iridaceous-misty-vivaciously.ngrok-free.dev -> http://localhost:8080
```

**For production**, ensure port 8080 is open on your server's firewall.

### Step 2 — Configure Webhook in BoldSign Dashboard

1. Log in to [app.boldsign.com](https://app.boldsign.com)
2. Go to **Settings → Webhooks → Add Webhook**
3. Set the URL:
   ```
   https://iridaceous-misty-vivaciously.ngrok-free.dev/webhook/boldsign
   ```
4. Select these events:
   - ✅ `document_completed`
   - ✅ `document_declined`
   - ✅ `document_revoked`
5. Click **Save**

### Step 3 — Verify It Works

When a document is signed, you should see in the console:
```
[Webhook] ═══════════════════════════════════════
[Webhook] BoldSign event received
[Webhook] Payload: {"Event":{"EventType":"document_completed"},"Data":{"DocumentId":"xxxx"}}
[Webhook] EventType  : document_completed
[Webhook] DocumentId : xxxx-xxxx-xxxx
[Webhook] Request #7 → SIGNED ✓
[Webhook] ═══════════════════════════════════════
```

---

## 13. Summary Diagram

```
┌──────────────────────────────────────────────────────────────────┐
│                    Full Contract Flow                            │
├──────────────────────────────────────────────────────────────────┤
│                                                                  │
│  [User]  submits request ──────────────────────► DB: PENDING     │
│                                                                  │
│  [Admin] clicks APPROVE                                          │
│       │                                                          │
│       ├─► DB: status = APPROVED                                  │
│       │                                                          │
│       └─► [boldsign-thread]                                      │
│               │                                                  │
│               ├─► PDFService generates contract.pdf              │
│               ├─► BoldSign API sends email to user               │
│               └─► DB: boldsign_document_id = "xxxx-xxxx"         │
│                                                                  │
│  [User]  opens email, signs document in BoldSign                 │
│                                                                  │
│  [BoldSign] POST /webhook/boldsign                               │
│       │     { EventType: "document_completed",                   │
│       │       DocumentId: "xxxx-xxxx" }                          │
│       │                                                          │
│       └─► WebhookController                                      │
│               ├─► findByBoldSignDocumentId("xxxx-xxxx")          │
│               ├─► req.setStatus(SIGNED)                          │
│               └─► DB: status = SIGNED                            │
│                                                                  │
│  [Admin] refreshes table ──────────────────────► sees SIGNED ✓  │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────┐
│                   DbMigration at Startup                         │
├──────────────────────────────────────────────────────────────────┤
│                                                                  │
│  App starts → DbMigration.run()                                  │
│       │                                                          │
│       ├─► ALTER TABLE contract_request                           │
│       │   ADD COLUMN IF NOT EXISTS boldsign_document_id          │
│       │                                                          │
│       ├─► Column exists? → SKIP (no error)                       │
│       └─► Column missing? → ADD (enables webhook lookup)         │
│                                                                  │
│  Without this column:                                            │
│  • BoldSign document ID cannot be stored at approval time        │
│  • Webhook cannot find the contract request to mark as SIGNED    │
│  • The entire signing loop is broken                             │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

---

*Documentation generated for Fintech Insurance Application — March 2, 2026*



