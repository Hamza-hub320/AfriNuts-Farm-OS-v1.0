# AfriNuts Farm OS v2.0

**A production-ready, offline-first, multi-device sync farm management system for West African cashew farmers.**

---

## 🌍 Product Vision

AfriNuts Farm OS started as an internal tool for a 35-hectare cashew farm in Odienné, Côte d'Ivoire. After 6 months of development, it has evolved into a **comprehensive, production-grade farm operating system** with:

- ✅ **Complete offline-first architecture** with real-time sync
- ✅ **Role-based access control** with 4 user types
- ✅ **Photo documentation** with cloud backup
- ✅ **Professional PDF reporting** with financial statements
- ✅ **Predictive analytics** with yield forecasting
- ✅ **Multi-device sync** with conflict resolution

**Current Phase:** Production-ready Internal Tool with Cloud Sync  
**Next Phase:** Multi-tenant SaaS Platform

---

## ✨ Key Features

### 🔄 **Multi-Device Sync**
- **Real-time synchronization** across all devices
- **Offline-first operation** (works without internet)
- **Conflict resolution** with timestamp-based Last-Write-Wins
- **Background sync** with WorkManager
- **Sync queue** with exponential backoff retry

### 📋 **Block Management**
- 5x7 grid layout (A1-E7) for 35-hectare farm
- Status tracking (NOT_CLEARED → CLEARED → PLOWED → PLANTED)
- Survival rate monitoring with progress bars
- Replacement tracking
- Expandable/collapsible row groups with row-level summaries
- Timeline dates for each operational stage

### 💰 **Financial Tracking**
- Expense management with 12+ categories
- Revenue tracking with quality grades (⭐ Premium/✓ Standard/⚙️ Processing)
- Profit & Loss dashboard with real-time calculations
- Cost per hectare/tree metrics
- Financial statements PDF export
- Expense analytics with pie charts and trends

### ✅ **Task Management**
- Task creation with 10+ task types
- Status workflow (⏳ PENDING → 🔄 IN_PROGRESS → ✅ COMPLETED)
- Worker assignment with complete history tracking
- Assignment duration analytics
- Overdue task highlighting
- Task icons for visual identification

### 👥 **Multi-User System**
- **4 Roles** with graduated permissions:
  - 👑 **Owner** (100) - Full CRUD access
  - 📋 **Manager** (80) - Operational access, no user management
  - 👤 **Worker** (30) - View and update assigned tasks
  - 📈 **Investor** (10) - View-only reports and analytics
- **Authentication methods**:
  - 🔐 PIN (4-6 digits) for workers
  - 🔑 Email/Password for managers/owners
- Session management with SHA-256 hashing
- User profile management (edit, activate, deactivate, delete)

### 📸 **Photo Documentation**
- Camera integration with real-time capture
- Gallery picker for existing photos
- Photo attachments to blocks, tasks, expenses
- Firebase Storage backup
- Caption editing
- Photo sharing via intent
- Photo gallery with grid view

### 📊 **Analytics & Reports**
- **Yield predictions** based on historical data
- **Block performance ranking** with 🥇🥈🥉 medals
- **Revenue/expense trend analysis** with line charts
- **3-year harvest forecasts** with growth projections
- **Profit margin calculations**
- **Professional PDF reports** with brand colors
- **Weather integration** with 3-day forecast and caching

### 🌤️ **Weather Integration**
- Real-time current conditions
- 3-day forecast with rain probability
- Local caching with 1-hour expiry
- Weather-based recommendations
- Visual icons for conditions

---

## 🏗 Architecture Overview

Built with Clean Architecture principles to ensure scalability from single-farm internal tool to multi-tenant SaaS without rewrites.

```
com.afrinuts.farmos
├── data
│   ├── local
│   │   ├── entity      # Room entities (18+ tables)
│   │   ├── dao         # Data Access Objects
│   │   ├── converter   # Type converters for enums
│   │   ├── database    # AppDatabase with migrations (v1→v12)
│   │   └── sync        # SyncEntity for offline queue
│   ├── remote
│   │   ├── FirebaseManager      # Firestore operations
│   │   ├── FirestoreSyncManager # Real-time listeners
│   │   └── WeatherService       # Weather API
│   └── repository      # Repository pattern with sync queue
├── ui
│   ├── auth           # Login, session management
│   ├── blocks         # Block management with expandable groups
│   ├── tasks          # Task tracking with assignments
│   ├── expenses       # Expense logging with categories
│   ├── revenue        # Revenue tracking with quality grades
│   ├── users          # User management with roles
│   ├── photos         # Photo documentation with gallery
│   ├── reports        # PDF report generation
│   ├── analytics      # Predictive analytics dashboard
│   ├── profit         # Profit & Loss dashboard
│   ├── workers        # Worker performance analytics
│   ├── weather        # Weather integration
│   └── sync           # Sync status UI
├── utils              # Helpers, AuthUtils, SessionManager
└── theme              # Custom design system with vector icons
```

**Key Design Decisions:**
- ✅ **Offline-first**: Room DB is source of truth
- ✅ **Sync queue**: All changes tracked in SyncEntity table
- ✅ **Conflict resolution**: Timestamp-based Last-Write-Wins
- ✅ **Repository pattern**: Enables cloud sync without UI changes
- ✅ **Role-based access**: Built-in permissions matrix
- ✅ **Vector icon system**: No emojis, all Material Icons
- ✅ **82 unit tests**: 100% pass rate

---

## 🔄 Sync Architecture

### Write Path (Local → Cloud)
```
1. User action in UI
2. Repository saves to Room DB (immediate)
3. SyncManager.queueForSync() creates PENDING item
4. AutoSyncWorker uploads to Firestore (background)
5. On success → mark COMPLETED
6. On failure → retry with exponential backoff
```

### Read Path (Cloud → Local)
```
1. FirestoreSyncManager listener detects change
2. isRemoteUpdate flag set (prevents sync loops)
3. EntityMapper converts Firestore → Entity
4. Timestamp comparison: remote.updatedAt > local
5. Update local DB (no queue)
6. UI refreshes via LiveData automatically
```

### Conflict Resolution
- **Last-Write-Wins** using `updatedAt` timestamps
- Local changes never overwritten by older remote updates
- Sync loop prevention with `isRemoteUpdate` flag
- Detailed logging for debugging

---

## 📊 Database Schema (v12)

```
┌─────────────┐       ┌─────────────┐       ┌─────────────┐
│    Farm     │       │    Block    │       │    User     │
├─────────────┤       ├─────────────┤       ├─────────────┤
│ id (PK)     │◄──────│ farmId (FK) │       │ localId (PK)│
│ name        │       │ blockName   │       │ uid (Firebase)│
│ location    │       │ hectareSize │       │ name        │
│ totalHectares│      │ status      │       │ role        │
│ cashewHectares│     │ clearedDate │       │ phone       │
│ treesPerHectare│    │ plowedDate  │       │ email       │
│ plantingYear│       │ plantedDate │       │ authMethod  │
└─────────────┘       │ survivalRate│       │ isActive    │
│ replacementCount│   └─────────────┘
└─────────────┘
│
▼
┌─────────────┐       ┌─────────────┐       ┌─────────────┐
│  Expense    │       │   Revenue   │       │    Task     │
├─────────────┤       ├─────────────┤       ├─────────────┤
│ id (PK)     │       │ id (PK)     │       │ id (PK)     │
│ farmId (FK) │       │ farmId (FK) │       │ farmId (FK) │
│ blockId (FK)│       │ blockId (FK)│       │ blockId (FK)│
│ category    │       │ harvestDate │       │ title       │
│ amount      │       │ quantityKg  │       │ description │
│ date        │       │ pricePerKg  │       │ type        │
│ description │       │ totalAmount │       │ status      │
│ receiptPath │       │ quality     │       │ dueDate     │
└─────────────┘       │ buyer       │       │ completedDate│
└─────────────┘       └─────────────┘
│                       │
▼                       ▼
┌─────────────┐       ┌─────────────┐       ┌─────────────┐
│   Photo     │       │SyncEntity   │       │TaskAssignment│
├─────────────┤       ├─────────────┤       ├─────────────┤
│ id (PK)     │       │ id (PK)     │       │ id (PK)     │
│ farmId (FK) │       │ entityType  │       │ taskId (FK) │
│ blockId (FK)│       │ entityId    │       │ assignedTo  │
│ taskId (FK) │       │ action      │       │ assignedAt  │
│ expenseId(FK)│      │ status      │       │ unassignedAt│
│ filePath    │       │ attemptCount│       └─────────────┘
│ caption     │       │ lastAttemptAt│
│ takenAt     │       └─────────────┘
└─────────────┘
```

---

## 🗺 Navigation Flow

```
Login Screen (User Selection + PIN/Password)
│
▼
Main Dashboard
(Farm Summary, Weather, Quick Actions)
│
├──► Blocks ──► Block Detail
│   │            ├──► Timeline (Cleared/Plowed/Planted dates)
│   │            ├──► Planting Details (Survival Rate, Replacements)
│   │            ├──► Photos Gallery
│   │            └──► Notes
│
├──► Tasks ──► Task Detail
│   │            ├──► Status Update (PENDING → IN_PROGRESS → COMPLETED)
│   │            ├──► Worker Assignment with History
│   │            └──► Due Date Tracking
│
├──► Expenses ──► Expense Detail
│   │              ├──► Analytics Dashboard
│   │              ├──► Category Charts
│   │              └──► Monthly Trends
│
├──► Revenue ──► Revenue Detail
│   │              ├──► Quality Grades (Premium/Standard/Processing)
│   │              └──► Buyer Information
│
├──► Users ──► User Detail
│   │            ├──► Role Management
│   │            ├──► Status Toggle (Active/Inactive)
│   │            └──► Delete User
│
├──► Reports ──► PDF Generation
│   │              ├──► Farm Summary Report
│   │              └──► Financial Statement
│
├──► Analytics ──► Dashboard (4 tabs)
│   │              ├──► Yield Predictions with Confidence Levels
│   │              ├──► Revenue/Expense Trend Analysis
│   │              ├──► Block Performance Rankings (🥇🥈🥉)
│   │              └──► 3-Year Harvest Forecasts
│
├──► Weather ──► Current + 3-Day Forecast
│
└──► Sync Button ──► Manual Sync with Animation
```

---

## 🚀 Implemented Features (v2.0)

### ✅ **Core Features**
- [x] Farm configuration with 5x7 block grid (A1-E7)
- [x] Block management with timeline tracking
- [x] Expense tracking with 12+ categories
- [x] Revenue tracking with quality grades
- [x] Task management with assignment history
- [x] Worker performance analytics
- [x] Weather integration with 3-day forecast

### ✅ **Sync & Cloud**
- [x] Offline-first architecture with Room
- [x] Sync queue with exponential backoff
- [x] Real-time Firestore listeners
- [x] Conflict resolution (timestamp-based)
- [x] Multi-device sync
- [x] Firebase Authentication (email/password)
- [x] Firebase Storage for photos

### ✅ **User System**
- [x] Role-based access (Owner/Manager/Worker/Investor)
- [x] PIN authentication (4-6 digits) for workers
- [x] Email/Password authentication for managers/owners
- [x] Session management with SHA-256 hashing
- [x] User profile management (CRUD)

### ✅ **Photo Management**
- [x] Camera integration with real-time capture
- [x] Gallery picker
- [x] Photo attachments to blocks/tasks/expenses
- [x] Caption editing
- [x] Photo sharing
- [x] Firebase Storage backup

### ✅ **Reports & Analytics**
- [x] PDF report generation (iText7)
- [x] Farm summary reports
- [x] Financial statements (revenue/expense/profit)
- [x] Yield predictions with confidence levels
- [x] Block performance rankings (🥇🥈🥉)
- [x] Revenue/expense trend analysis
- [x] 3-year harvest forecasts

### ✅ **UI/UX**
- [x] Material Design system with brand colors (#5A7411, #E04E00)
- [x] Vector icons (30+ custom Material icons)
- [x] Responsive layouts with CardView
- [x] Loading states with ProgressBar
- [x] Error handling with Snackbar
- [x] Animations (scale, fade, slide)
- [x] Sync button with rotation animation

### ✅ **Quality Assurance**
- [x] 82 unit tests
- [x] 100% test pass rate
- [x] Database migrations (v1→v12)
- [x] Offline-first operation
- [x] Role-based access enforcement
- [x] Real-time sync testing (2+ devices)

---

## 🛠 Technical Stack

| Component | Technology | Version | Justification |
|-----------|------------|---------|---------------|
| **Language** | Java | 11 | Stable, proven, compatible |
| **UI** | XML + Material Components | 1.11.0 | Professional look, good tooling |
| **Database** | Room | 2.6.1 | SQLite with compile-time checks |
| **Architecture** | MVVM + Repository | - | Clean separation, testable |
| **Authentication** | Firebase Auth + SHA-256 | 32.7.0 | Secure, managed |
| **Cloud Sync** | Firebase Firestore | 32.7.0 | Real-time, offline support |
| **File Storage** | Firebase Storage | 32.7.0 | Scalable photo storage |
| **Background Sync** | WorkManager | 2.9.0 | Reliable background tasks |
| **Charts** | MPAndroidChart | 3.1.0 | Beautiful visualizations |
| **PDF Generation** | iText7 | 7.2.5 | Professional reports |
| **Image Loading** | Glide | 4.16.0 | Efficient caching |
| **Weather API** | WeatherAPI.com | - | Real-time data |
| **Testing** | JUnit + Mockito | 4.13.2 / 5.7.0 | 82 tests, 100% pass |
| **Version Control** | Git + GitHub | - | Industry standard |

---

## 📦 Deployment

### Internal Release (v2.0)
1. Generate signed APK
2. Install on:
   - Farm owner device (full access)
   - Farm manager devices (operational access)
   - Worker devices (task-only access)
   - Investor devices (view-only reports)

### Requirements
- Android 7.0 (API 24) or higher
- 100MB free storage
- Camera (for photo features)
- Internet (for weather updates and sync)

### Testing
- 82 unit tests passed
- Multi-device sync verified
- Offline → online transition tested
- Conflict resolution confirmed

---

## 📈 Success Metrics Achieved

- ✅ **Cost per hectare**: Tracked with 100% accuracy
- ✅ **Survival rate**: Per-block monitoring with progress bars
- ✅ **Tree count**: Accurate 3,500 trees across 35 blocks
- ✅ **Task completion**: Full audit trail with worker history
- ✅ **Weather integration**: Real-time with 1-hour cache
- ✅ **Financial reporting**: Professional PDFs with brand colors
- ✅ **User adoption**: 4 roles with graduated permissions
- ✅ **Test coverage**: 82 tests, 100% pass rate
- ✅ **Multi-device sync**: Verified across 2+ devices
- ✅ **Offline operation**: Fully functional without internet

---

## 🎯 Future Roadmap

### Phase 6 — Cloud Enablement (Complete ✅)
- [x] Firebase Authentication
- [x] Cloud Firestore
- [x] Firebase Storage
- [x] Real-time listeners
- [x] Conflict resolution
- [x] Background sync with WorkManager

### Phase 7 — Web Dashboard (Q3 2026)
- [ ] React web dashboard
- [ ] Investor portal
- [ ] Basic farm mapping
- [ ] PDF report generation from web

### Phase 8 — Advanced Features (Q4 2026+)
- [ ] Machine learning predictions (after 1-2 harvests)
- [ ] GPS/mapping integration
- [ ] Individual tree tracking with QR codes
- [ ] Automated alerts (weather, tasks)
- [ ] Multi-language support (French/English)

---

## 📄 License

**Private — All Rights Reserved**

AfriNuts Farm OS is proprietary software owned by AfriNuts Export. Unauthorized copying, distribution, or modification is prohibited.

---

## 👨‍💻 About

Built for **AfriNuts Export** cashew farm operations in Odienné, Côte d'Ivoire. Designed to scale across West Africa.

| Metric | Value |
|--------|-------|
| **Farm Size** | 35 hectares |
| **Blocks** | 35 (A1-E7 grid) |
| **Trees** | 3,500 (100 per hectare) |
| **Users** | Owner, Manager, Worker, Investor |
| **Database Version** | v12 with migrations |
| **Sync Entities** | 7 (Blocks, Expenses, Revenue, Tasks, Photos, Users, Assignments) |
| **Test Coverage** | 82 tests, 100% pass |
| **Icon Set** | 30+ custom Material vectors |

**Founder:** Hamza  
**Location:** Odienné, Côte d'Ivoire  
**Website:** www.afrinutsexport.com

---

## 🙏 Acknowledgments

- Cloud infrastructure by [Firebase](https://firebase.google.com)
- Weather data by [WeatherAPI.com](https://www.weatherapi.com)
- Charts by [MPAndroidChart](https://github.com/PhilJay/MPAndroidChart)
- PDF generation by [iText7](https://itextpdf.com)
- Icons by [Material Design](https://material.io/icons)

---

*"We built this system on a 35-hectare cashew farm in Odienné and validated operational efficiency improvements across multiple devices. Now it's ready for the world."*

**— The AfriNuts Team** 🇨🇮