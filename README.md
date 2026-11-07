# AfriNuts Farm OS v2.0

**An offline-first, multi-device farm management and cashew trading system for West African cashew operations — built on a working farm in Odienné, Côte d'Ivoire.**

---

## 🌍 Product Vision

AfriNuts Farm OS started as an internal tool for a 35-hectare cashew farm in Odienné, Côte d'Ivoire. It has evolved into a **complete operating system for cashew farming and raw cashew nut (RCN) trading**, with:

- ✅ **Offline-first architecture** with real-time multi-device sync
- ✅ **Role-based access control** with 4 user types
- ✅ **Block, task, expense and revenue management** for the farm itself
- ✅ **RCN quality grading** built on the 1 kg cut test (KOR in g/kg)
- ✅ **Supplier management with a six-dimension Supplier Trust Score**
- ✅ **End-to-end batch traceability** with verification codes and consent-gated public trace views
- ✅ **Photo documentation** with cloud backup, including trust-record evidence photos
- ✅ **Professional PDF reporting**, with deep analytics in the companion web portals

**Current Phase:** Phase One trading readiness — supplier onboarding, quality grading, trust scoring and traceability hardened and under end-to-end simulation before live trading.
**Next Phase:** Live Phase One trading (buy RCN → grade → resell to local exporters), then multi-farm evolution.

---

## ✨ Key Features

### 🔄 **Multi-Device Sync**

- **Real-time synchronization** across all devices via Firestore listeners (20+ collections)
- **Offline-first operation** — Room DB is the source of truth; the app works fully without internet
- **Conflict resolution** with timestamp-based Last-Write-Wins on `updatedAt`
- **Background sync** with WorkManager (periodic sync every 6 hours + on-connect triggers)
- **Sync loop prevention** with an `isRemoteUpdate` guard in the repositories

### 🧱 **Block Management**

- 5x7 grid layout (A1–E7) for the 35-hectare farm
- Status tracking (NOT_CLEARED → CLEARED → PLOWED → PLANTED)
- Survival rate monitoring with progress bars and replacement tracking
- Timeline dates for each operational stage
- **Per-block harvest history** — blocks record their own harvests (date, quantity, grade) via `BlockEntity.harvestHistory`

### 🌰 **Quality Grading (RCN Intake)**

- Quality batches recorded at intake with supplier, source type and weight
- **Cut-test KOR**: shell a 1 kg sample, weigh the good kernels — stored as **g/kg** (lbs shown for market negotiation, % derived only)
- Grades computed by `QualityGradeCalculator`: **PREMIUM ≥ 272 g/kg · STANDARD 249–271 · REJECT < 249**, with hard floors on moisture, mould, defects and foreign matter applied before any score
- Inspector-entered inspection date and inspector name on every graded batch
- **Saleability gates enforced in code**: REJECT and uninspected batches cannot be sold and are skipped by traceability

### 🤝 **Suppliers & Trust Score**

- Supplier profiles (individuals and cooperatives) with GPS and **per-farmer data consent** (identity / location / photo scopes, captured at onboarding)
- **Per-season commitments** with an append-only revision trail — corrections are new revisions, never silent overwrites
- Supplier inspections and disputes, with photo evidence (uploaded to Storage first; a `file://` link is never pushed)
- **Supplier Trust Score** — six dimensions: Quality 25% · Delivery 20% · Traceability 20% · Compliance 15% · Sustainability 10% · Cooperation 10%
- Scores are computed by the Trust Engine (web portal) from **validated evidence only** — records count once an Owner/Manager validates them in the portal — and stored as append-only snapshots in `supplier_score_history`
- Critical violations raise a separate flag; they are never averaged into the score

### 🔗 **Traceability**

- Batch traceability records linked to their quality batch, supplier and **revenue record** (`revenueId`)
- Master batches group child batches for shipments; membership is stored on the child (`masterBatchId`) and the master's batch lists are generated from it
- Verification codes and URLs, digital signature and verification hash on every trace record
- Public trace view is **consent-gated**: a buyer sees a farmer's identity, location and photo only where that farmer consented

### 💰 **Financial Tracking**

- Expense management with categories and receipt photos
- **Revenue records model batch sales** — batch, supplier, quantity, price, buyer, quality grade and the sale date (`createdAt`); per-block profit is computed in the web portal by allocating batch revenue across blocks by output share
- Financial Snapshot card on the home screen with live totals
- Financial PDF reports (farm summary, revenue/expense statements)

### ✅ **Task Management**

- Task creation with 10+ task types and a status workflow (PENDING → IN_PROGRESS → COMPLETED)
- Worker assignment with complete history tracking (`task_assignments`)
- Overdue task highlighting and assignment duration analytics

### 👥 **Multi-User System**

- **4 Roles** with graduated permissions:
    - 👑 **Owner** (100) — Full CRUD access, validates trust evidence
    - 📋 **Manager** (80) — Operational access, validates trust evidence, no user management
    - 👤 **Worker** (30) — View and update assigned tasks
    - 📈 **Investor** (10) — View-only reports
- **Authentication**: PIN (4–6 digits) for workers; email/password for managers/owners
- Session management with SHA-256 hashing; user profile management (edit, activate, deactivate, delete)

### 📸 **Photo Documentation**

- Camera integration and gallery picker; photos attach to blocks, tasks and expenses
- All photo metadata lives in **one flat Firestore collection** (`farms/{farmId}/photos`); the parent link travels on the record itself
- Firebase Storage backup, caption editing, photo sharing, grid gallery

### 🛡 **Compliance**

- Certifications, audits, field/processing inspections, compliance verifications and documents tracked in-app (five dedicated tables)
- Certification status is honest by design: only *issued* certifications are recorded as certifications

### 📊 **Analytics & Reports**

- In-app Analytics hub and PDF reports (iText7)
- The intelligence layer lives in the **web portals**: an internal intelligence portal (analytics, trust validation and dispute queues, score recalculation) and a buyer portal (consented trace views; REJECT batches never listed)

### 🌤️ **Weather Integration**

- Current conditions and 3-day forecast (WeatherAPI.com), cached locally in `weather_cache`

---

## 🏗 Architecture Overview

```
com.afrinuts.farmos
├── data
│   ├── local
│   │   ├── entity      # Room entities — 23 tables
│   │   ├── dao         # Data Access Objects
│   │   ├── converter   # Type converters for enums
│   │   ├── database    # AppDatabase (v41) + 40 migration classes, v1→v41
│   │   └── sync        # Sync queue entity for offline writes
│   ├── remote
│   │   ├── FirebaseManager      # Firestore operations
│   │   ├── FirestoreHelper      # Firestore writes (delegates maps to EntityMapper)
│   │   ├── FirestoreSyncManager # Real-time listeners (Firestore → Room)
│   │   ├── EntityMapper         # Single source of Firestore ↔ Entity maps
│   │   └── WeatherApiClient     # Weather API
│   └── repository      # Repository pattern; writes gated on isRemoteUpdate
├── ui
│   ├── auth           # Login, session management
│   ├── blocks         # Block management + harvest recording
│   ├── tasks          # Task tracking with assignments
│   ├── expenses       # Expense logging with categories
│   ├── revenue        # Batch-sale revenue tracking
│   ├── quality        # RCN intake + grading (cut test)
│   ├── suppliers      # Supplier profiles, commitments, inspections, disputes
│   ├── traceability   # Batch + master traceability
│   ├── compliance     # Certifications, audits, inspections, documents
│   ├── finance        # Financial snapshot + statements
│   ├── reports        # PDF report generation
│   ├── analytics      # Analytics hub
│   ├── photos         # Photo documentation with gallery
│   ├── users          # User management with roles
│   ├── workers        # Worker views
│   ├── weather        # Weather integration
│   ├── settings       # App settings
│   ├── sync           # Sync status UI
│   └── farm           # Farm profile
├── utils              # Helpers, AuthUtils, SessionManager, QualityGradeCalculator
└── theme              # Custom design system with vector icons
```

**Key Design Decisions:**

- ✅ **Offline-first**: Room DB is the source of truth; Firestore is a mirror
- ✅ **One mapper**: every Firestore write goes through `EntityMapper` — no divergent per-writer field subsets
- ✅ **Conflict resolution**: Last-Write-Wins by `updatedAt`; repositories refuse writes while a remote update is being applied
- ✅ **Evidence-photo ordering**: upload to Storage → rewrite URLs → persist → sync; a `file://` link is never pushed
- ✅ **Real migrations only**: every version step v1→v41 has a hand-written migration; no destructive fallback
- ✅ **Role-based access**: built-in permissions matrix; trust validation restricted to Owner/Manager

---

## 🔄 Sync Architecture

### Write Path (Local → Cloud)

```
1. User action in UI
2. Repository saves to Room DB (immediate)
3. Record queued for sync (sync_queue / isSynced flag)
4. SyncManager / AutoSyncWorker uploads to Firestore (background)
5. On success → marked synced
6. On failure → retried on the next sync pass
```

### Read Path (Cloud → Local)

```
1. FirestoreSyncManager listener detects a change
2. isRemoteUpdate flag set (prevents sync loops)
3. EntityMapper converts Firestore → Entity
4. Timestamp comparison: remote wins only if newer (updatedAt)
5. Local DB updated; UI refreshes via LiveData
```

### Trust Evidence Path (Commitments / Inspections / Disputes)

```
1. Captured in-app (Room-first) — capture never sets validatedAt
2. Evidence photos uploaded to Storage; evidenceUrls rewritten to https URLs
3. Record syncs to Firestore (isSynced = reached the cloud)
4. Owner/Manager validates in the web portal (validatedAt / validatedBy)
5. Trust Engine scores validated evidence only; snapshots append to supplier_score_history
6. Snapshots sync back down to devices (insert-if-absent, append-only)
```

---

## 🗄 Database Schema (v41)

Room database version **41** — 23 tables, every step from v1 migrated by hand (40 migration classes, no destructive fallback). The exported schema (`app/schemas/…/41.json`) is committed to the repo.

**Core farm**

| Table                        | Holds                                                        |
| ---------------------------- | ------------------------------------------------------------ |
| `farms`                      | Farm profile                                                 |
| `blocks`                     | Blocks (A1–E7) with timeline dates, survival, and `harvestHistory` (JSON) |
| `tasks` / `task_assignments` | Tasks and their assignment history                           |
| `expenses`                   | Expenses with category, amount, receipt path                 |
| `revenues`                   | **Batch sales** — see schema notes                           |
| `photos`                     | Photo metadata (block/task/expense links on the record)      |
| `users`                      | Users, roles, auth method                                    |
| `weather_cache`              | Cached weather                                               |
| `sync_queue`                 | Offline write queue                                          |

**Quality & trade**

| Table             | Holds                                                        |
| ----------------- | ------------------------------------------------------------ |
| `suppliers`       | Supplier profiles incl. consent fields (`dataConsentGiven/Date/Scope/…`) |
| `quality_batches` | RCN batches: KOR (`korGPerKg`, `korSampleGrams`, `korKernelGrams`), defect components, moisture, nut count, grade, score, `inspectionDate`, `inspectedBy`, `productType` (RCN / KERNEL) |

**Traceability**

| Table                       | Holds                                                        |
| --------------------------- | ------------------------------------------------------------ |
| `batch_traceability`        | Per-batch trace: `revenueId` (FK → revenues), `qualityBatchId`, `supplierId`, `masterBatchId`, verification code/URL, `digitalSignature`, `verificationHash`, `signedAt`, `harvestYear` |
| `master_batch_traceability` | Shipment masters: `farmId`, `masterBatchNumber`, grades, certifications, and the batch lists (JSON) generated from children's `masterBatchId` |

**Compliance**

| Table                      | Holds                               |
| -------------------------- | ----------------------------------- |
| `certifications`           | Certification programmes and status |
| `audits`                   | Audit records                       |
| `inspections`              | Field / processing inspections      |
| `compliance_verifications` | Verification records                |
| `compliance_documents`     | Compliance documents                |

**Supplier trust**

| Table                    | Holds                                                        |
| ------------------------ | ------------------------------------------------------------ |
| `supplier_commitments`   | Per-season commitments: target/delivered kg, delay reason, buyer-requested-change flag, append-only `revisions`, `validatedAt/By` |
| `supplier_inspections`   | Trust inspections with outcomes, evidence URLs, validation fields |
| `supplier_disputes`      | Disputes: `targetId`, status, resolution, evidence, validation fields |
| `supplier_score_history` | Append-only score snapshots (dimension scores, weights used, scoring version, critical-violation flag) |

**Schema notes (the v41 model):**

- **Revenue models a batch sale, nothing else** (since migration 40→41). It carries `batchId`, `supplierId`, quantity, price, `totalAmount`, buyer, quality and notes; the sale date is `createdAt`. All harvest fields (`blockId`, `harvestDate`, `harvestCycleId`, `harvestYear`, `harvestSeason`) were removed — blocks record their own harvests, and per-block P&L is computed in the portal by allocating batch revenue across blocks by output share.
- `batch_traceability.revenueId` is the link from a trace to its sale (renamed from a legacy field that held revenue ids under a harvest name).
- Foreign keys are deliberate: deleting a supplier **never** deletes QC history (`quality_batches.supplierId` is `SET_NULL`, single declaration); farm deletion cascades; a deleted revenue nulls the trace's `revenueId`.
- Quality grades are **PREMIUM / STANDARD / REJECT** (the legacy PROCESSING grade was retired in the v36 quality build and mapped to REJECT).

---

## 🗺 Navigation Flow

```
Login (User Selection + PIN / Password)
│
▼
Main Dashboard (Farm Summary, Financial Snapshot, Weather, Quick Actions)
│
├──► Blocks ──► Block Detail (Timeline, Planting, Harvest History, Photos, Notes)
├──► Tasks ──► Task Detail (Status, Assignment History, Due Dates)
├──► Expenses ──► Expense Detail / Analytics
├──► Revenue ──► Revenue Detail (batch + supplier, sale date, buyer, grade)
├──► Quality ──► Batch Intake ──► Cut-Test Grading ──► Grade + Score
├──► Suppliers ──► Supplier Detail
│   │            ├──► Commitments (with revisions)
│   │            ├──► Inspections (evidence photos)
│   │            ├──► Disputes
│   │            └──► Latest Trust Score (read-back from portal snapshots)
├──► Traceability ──► Batch Trace ──► Master Batches / Shipments
├──► Compliance ──► Certifications, Audits, Inspections, Documents
├──► Users ──► User Detail (Roles, Active/Inactive)
├──► Reports ──► PDF Generation (Farm Summary, Financial Statement)
├──► Analytics ──► Analytics Hub
├──► Weather ──► Current + 3-Day Forecast
└──► Sync ──► Sync Status / Manual Sync
```

---

## 🚀 Implemented Features

### ✅ **Core Farm**

- [x] Farm configuration with 5x7 block grid (A1–E7)
- [x] Block management with timeline + per-block harvest history
- [x] Expense tracking with categories and receipts
- [x] Revenue tracking as batch sales
- [x] Task management with assignment history
- [x] Weather integration with 3-day forecast and caching

### ✅ **Quality & Trading Readiness**

- [x] RCN intake with cut-test KOR in g/kg and computed PREMIUM / STANDARD / REJECT grades
- [x] Saleability gates: REJECT / uninspected batches cannot be sold or traced
- [x] Supplier profiles with per-farmer consent capture
- [x] Batch + master traceability with verification codes and signatures

### ✅ **Supplier Trust**

- [x] Commitments with append-only revisions
- [x] Supplier inspections and disputes with Storage-backed evidence photos
- [x] Trust Engine v2.0 (web portal): validated-evidence-only scoring, six dimensions, append-only snapshots
- [x] In-app capture for all trust records; Latest Trust Score read-back on the supplier screen

### ✅ **Sync & Cloud**

- [x] Offline-first Room with Firestore mirror and real-time listeners (20+ collections)
- [x] Last-Write-Wins conflict resolution with sync-loop prevention
- [x] Flat photo metadata collection; evidence-photo upload ordering enforced
- [x] Firebase Authentication (email/password) + PIN sessions
- [x] Firebase Storage for photos and evidence

### ✅ **User System**

- [x] Role-based access (Owner / Manager / Worker / Investor)
- [x] PIN authentication for workers; email/password for managers/owners
- [x] Session management with SHA-256 hashing; user CRUD

### ✅ **Reports**

- [x] PDF report generation (iText7) — farm summary and financial statements
- [x] Financial Snapshot on the home screen
- [x] Web intelligence portal (analytics, trust validation, dispute queue) and buyer portal (consent-gated traces)

### ✅ **Quality Assurance**

- [x] JUnit / Mockito test suites (`app/src/test`, `app/src/androidTest`)
- [x] Database migrations v1→v41, exported schema committed per version
- [x] Offline-first operation; multi-device sync in daily use

---

## 🛠 Technical Stack

| Component           | Technology                       | Version    |
| ------------------- | -------------------------------- | ---------- |
| **Language**        | Java                             | 11         |
| **UI**              | XML + Material Components        | 1.11.0     |
| **Database**        | Room                             | 2.6.1      |
| **Architecture**    | MVVM + Repository                | —          |
| **Authentication**  | Firebase Auth + SHA-256 sessions | BoM 32.7.4 |
| **Cloud Sync**      | Firebase Firestore               | BoM 32.7.4 |
| **File Storage**    | Firebase Storage                 | BoM 32.7.4 |
| **Background Sync** | WorkManager                      | 2.9.0      |
| **Charts**          | MPAndroidChart                   | 3.1.0      |
| **PDF Generation**  | iText7                           | 7.2.5      |
| **Image Loading**   | Glide                            | 4.16.0     |
| **Weather API**     | WeatherAPI.com                   | —          |
| **Testing**         | JUnit + Mockito                  | —          |

---

## 📦 Deployment

### Internal Release

Signed APK installed per role: farm owner device (full access), manager devices (operational access), worker devices (task access), investor devices (view-only reports).

### Requirements

- Android 7.0 (API 24) or higher; target SDK 34
- ~100 MB free storage; camera for photo features
- Internet for weather updates and sync (the app itself is fully functional offline)

---

## 📈 Where It Stands

- ✅ **Farm**: 35 hectares, 35 blocks (A1–E7), 3,500 trees tracked
- ✅ **Trading readiness**: grading, supplier trust and traceability built and under end-to-end simulation (20-supplier dataset) before first live purchases
- ✅ **Database**: v41, 23 tables, full migration history
- ✅ **Sync**: 20+ Firestore collections mirrored in real time
- ✅ **Data ethics**: per-farmer consent captured at onboarding; farmers' data is never sold and buyers see only consented fields

---

## 🎯 Roadmap

### Delivered

- [x] Offline-first farm management (blocks, tasks, expenses, revenue, photos, weather)
- [x] Cloud sync (Firestore, Storage, Auth, real-time listeners, WorkManager)
- [x] Quality grading with cut-test KOR (v36 quality build)
- [x] Supplier Trust Score framework — capture in-app, engine + validation in the web portal
- [x] Web portals: intelligence portal and consent-gated buyer portal

### In progress

- [ ] End-to-end simulation sign-off, then pre-onboarding purge of all demo data
- [ ] Phase One live trading (RCN purchase → grading → resale to local exporters)

### Later

- [ ] Kernel processing support (Phase Two) — product type and grading paths already modelled
- [ ] Multi-farm / multi-tenant evolution
- [ ] GPS mapping and individual tree tracking
- [ ] Multi-language support (French / English)

---

## 📄 License

**Private — All Rights Reserved**

AfriNuts Farm OS is proprietary software owned by AfriNuts Export SARL. Unauthorized copying, distribution, or modification is prohibited.

---

## 👨‍💻 About

Built for **AfriNuts Export SARL** cashew operations in Odienné, Côte d'Ivoire. Designed to scale across West Africa.

| Metric               | Value                                |
| -------------------- | ------------------------------------ |
| **Farm Size**        | 35 hectares (70 ha secured in total) |
| **Blocks**           | 35 (A1–E7 grid)                      |
| **Trees**            | 3,500 (100 per hectare)              |
| **Users**            | Owner, Manager, Worker, Investor     |
| **Database Version** | v41 — 23 tables, migrations v1→v41   |
| **Sync Scope**       | 20+ Firestore collections            |
| **Founder**          | Hamza                                |
| **Location**         | Odienné, Côte d'Ivoire               |
| **Website**          | www.afrinutsexport.com               |

---

## 🙏 Acknowledgments

- Cloud infrastructure by [Firebase](https://firebase.google.com)
- Weather data by [WeatherAPI.com](https://www.weatherapi.com)
- Charts by [MPAndroidChart](https://github.com/PhilJay/MPAndroidChart)
- PDF generation by [iText7](https://itextpdf.com)
- Icons by [Material Design](https://material.io/icons)

---

*"We built this system on a 35-hectare cashew farm in Odienné. Every lot we buy will be graded, recorded and traceable from the first purchase — that's the point of it."*

**— The AfriNuts Team** 🇨🇮