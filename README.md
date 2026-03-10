# AfriNuts Farm OS v2.0

**A production-ready, offline-first farm management system for West African cashew farmers.**

---

## 🌍 Product Vision

AfriNuts Farm OS started as an internal tool for a 35-hectare cashew farm in Odienné, Côte d'Ivoire. After 6 months of development and testing, it has evolved into a **comprehensive farm operating system** with role-based access, photo documentation, PDF reporting, and predictive analytics.

**Current Phase:** Production-ready Internal Tool  
**Next Phase:** Multi-tenant SaaS Platform

---

## ✨ Key Features

### 📋 **Block Management**
- 5x7 grid layout (A1-E7) for 35-hectare farm
- Status tracking (NOT_CLEARED → CLEARED → PLOWED → PLANTED)
- Survival rate monitoring
- Replacement tracking
- Expandable/collapsible row groups

### 💰 **Financial Tracking**
- Expense management with categories
- Revenue tracking with quality grades (Premium/Standard/Processing)
- Profit & Loss dashboard
- Cost per hectare/tree calculations
- Financial statements PDF export

### ✅ **Task Management**
- Task creation and assignment
- Status workflow (PENDING → IN_PROGRESS → COMPLETED)
- Worker assignment with history tracking
- Overdue task highlighting
- Assignment duration analytics

### 👥 **Multi-User System**
- Role-based access control (Owner/Manager/Worker/Investor)
- PIN authentication for workers
- Password authentication for managers/owners
- Session management
- User profile management

### 📸 **Photo Documentation**
- Camera integration
- Gallery picker
- Photo attachments to blocks, tasks, expenses
- Caption editing
- Photo sharing

### 📊 **Analytics & Reports**
- Yield predictions based on historical data
- Block performance ranking (🥇🥈🥉)
- Revenue/expense trend analysis
- 3-year harvest forecasts
- Professional PDF reports
- Weather integration with caching

---

## 🏗 Architecture Overview

Built with Clean Architecture principles to ensure scalability from single-farm internal tool to multi-tenant SaaS without rewrites.

com.afrinuts.farmos
├── data
│ ├── local
│ │ ├── entity # Room entities (18+ tables)
│ │ ├── dao # Data Access Objects
│ │ ├── converter # Type converters
│ │ └── database # AppDatabase with migrations (v1→v9)
│ ├── remote # Weather API integration
│ └── repository # Repository pattern implementation
├── ui
│ ├── auth # Login, session management
│ ├── blocks # Block management
│ ├── tasks # Task tracking with assignments
│ ├── expenses # Expense logging
│ ├── revenue # Revenue tracking
│ ├── users # User management
│ ├── photos # Photo documentation
│ ├── reports # PDF report generation
│ ├── analytics # Predictive analytics dashboard
│ ├── profit # Profit & Loss dashboard
│ ├── workers # Worker performance analytics
│ └── weather # Weather integration
├── utils # Helpers, AuthUtils, SessionManager
└── theme # Custom design system


**Key Design Decisions:**
- ✅ Repository pattern enables future cloud sync without UI changes
- ✅ Room database for offline-first operation
- ✅ Farm table from Day 1 enables multi-tenant future
- ✅ Role-based access control built-in
- ✅ Vector icon system (no emojis)
- ✅ 82 unit tests with 100% pass rate

---

## 📊 Database Schema (v9)
┌─────────────┐ ┌─────────────┐ ┌─────────────┐
│ Farm │ │ Block │ │ User │
├─────────────┤ ├─────────────┤ ├─────────────┤
│ id │◄──────│ farmId │ │ id │
│ name │ │ blockName │ │ name │
│ location │ │ hectareSize │ │ role │
│ totalHectares│ │ status │ │ phone │
│ cashewHectares│ │ clearedDate │ │ email │
│ treesPerHectare│ │ plowedDate │ │ passwordHash│
│ plantingYear│ │ plantedDate │ │ pinHash │
└─────────────┘ │ survivalRate│ │ authMethod │
│ │ replacementCount│ └─────────────┘
│ └─────────────┘ │
│ │ │
▼ ▼ ▼
┌─────────────┐ ┌─────────────┐ ┌─────────────┐
│ Expense │ │ Task │ │ Photo │
├─────────────┤ ├─────────────┤ ├─────────────┤
│ id │ │ id │ │ id │
│ farmId │ │ farmId │ │ farmId │
│ blockId │ │ blockId │ │ blockId │
│ category │ │ title │ │ taskId │
│ amount │ │ description │ │ expenseId │
│ date │ │ type │ │ filePath │
└─────────────┘ │ status │ │ caption │
│ │ dueDate │ │ takenAt │
│ └─────────────┘ └─────────────┘
│ │
▼ ▼
┌─────────────┐ ┌─────────────────┐
│ Revenue │ │TaskAssignment │
├─────────────┤ ├─────────────────┤
│ id │ │ id │
│ farmId │ │ taskId │
│ blockId │ │ assignedTo │
│ harvestDate │ │ assignedAt │
│ quantityKg │ │ unassignedAt │
│ pricePerKg │ └─────────────────┘
│ quality │
└─────────────┘


---

## 🗺 Navigation Flow

Login Screen (User Selection + PIN/Password)
│
▼
Main Dashboard
(Farm Summary, Weather, Quick Actions)
│
├──► Blocks ──► Block Detail
│ │ ├──► Timeline
│ │ ├──► Planting Details
│ │ ├──► Photos
│ │ └──► Notes
│
├──► Tasks ──► Task Detail
│ │ ├──► Status Update
│ │ ├──► Worker Assignment
│ │ └──► Assignment History
│
├──► Expenses ──► Expense Detail
│ │ ├──► Analytics
│ │ └──► Charts
│
├──► Revenue ──► Revenue Detail
│ │ └──► Quality Grades
│
├──► Users ──► User Detail
│ │ ├──► Role Management
│ │ └──► Status Toggle
│
├──► Reports ──► PDF Generation
│ │ ├──► Farm Summary
│ │ └──► Financial Statement
│
├──► Analytics ──► Dashboard
│ │ ├──► Yield Predictions
│ │ ├──► Trend Analysis
│ │ ├──► Block Rankings
│ │ └──► 3-Year Forecast
│
└──► Weather ──► Current + Forecast


---

## 🚀 Implemented Features (v2.0)

### ✅ **Core Features**
- [x] Farm configuration with 5x7 block grid (A1-E7)
- [x] Block management with timeline tracking
- [x] Expense tracking with categories
- [x] Revenue tracking with quality grades
- [x] Task management with assignment history
- [x] Worker performance analytics
- [x] Weather integration with caching

### ✅ **User System**
- [x] Role-based access (Owner/Manager/Worker/Investor)
- [x] PIN authentication for workers
- [x] Password authentication for managers/owners
- [x] Session management
- [x] User profile management

### ✅ **Photo Management**
- [x] Camera integration
- [x] Gallery picker
- [x] Photo attachments to blocks/tasks/expenses
- [x] Caption editing
- [x] Photo sharing

### ✅ **Reports & Analytics**
- [x] PDF report generation
- [x] Farm summary reports
- [x] Financial statements
- [x] Yield predictions
- [x] Block performance rankings
- [x] Revenue/expense trends
- [x] 3-year forecasts

### ✅ **UI/UX**
- [x] Material Design system
- [x] Vector icons (no emojis)
- [x] Dark/light theme support
- [x] Responsive layouts
- [x] Loading states
- [x] Error handling

### ✅ **Quality Assurance**
- [x] 82 unit tests
- [x] 100% test pass rate
- [x] Database migrations (v1→v9)
- [x] Offline-first operation
- [x] Role-based access enforcement

---

## 🛠 Technical Stack

| Component | Technology | Justification |
|-----------|------------|---------------|
| Language | Java 11 | Stable, proven, compatible |
| UI | XML + Material Components | Professional look, good tooling |
| Database | Room 2.6.1 | SQLite with compile-time checks |
| Architecture | MVVM + Repository | Clean separation, testable |
| Authentication | SHA-256 hashing | Secure PIN/password storage |
| Charts | MPAndroidChart | Beautiful visualizations |
| PDF Generation | iText7 | Professional reports |
| Image Loading | Glide | Efficient caching |
| Weather API | WeatherAPI.com | Real-time data |
| Testing | JUnit + Mockito | 82 tests, 100% pass rate |
| Version Control | Git + GitHub | Industry standard |

---

## 📦 Deployment

### Internal Release (v2.0)
1. Generate signed APK
2. Install on:
   - Farm owner device
   - Farm manager devices
   - Investor devices (view-only)
3. Manual backup (database export)
4. Future: Encrypted cloud backup

### Requirements
- Android 7.0 (API 24) or higher
- 100MB free storage
- Camera (for photo features)
- Internet (for weather updates)

---

## 📈 Success Metrics Achieved

- ✅ **Cost per hectare**: Tracked with 100% accuracy
- ✅ **Survival rate**: Per-block monitoring
- ✅ **Tree count**: Accurate 3,500 trees
- ✅ **Task completion**: Full audit trail
- ✅ **Weather integration**: Real-time with caching
- ✅ **Financial reporting**: Professional PDFs
- ✅ **User adoption**: Role-based access
- ✅ **Test coverage**: 82 tests, 100% pass rate

---

## 🎯 Future Roadmap

### Phase 6 — SaaS Evolution (Q3 2026)
- [ ] Cloud backend (Firebase/AWS)
- [ ] Multi-farm support
- [ ] Data sync engine
- [ ] Web dashboard
- [ ] Investor portal

### Phase 7 — Advanced Features (Q4 2026)
- [ ] Machine learning predictions
- [ ] GPS/mapping integration
- [ ] Individual tree tracking
- [ ] Automated alerts
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
| **Test Coverage** | 82 tests, 100% pass |
| **Database Version** | v9 with migrations |

**Founder:** Hamza  
**Location:** Odienné, Côte d'Ivoire  
**Website:** [Coming Soon]

---

## 🙏 Acknowledgments

- Weather data provided by [WeatherAPI.com](https://www.weatherapi.com)
- Charts powered by [MPAndroidChart](https://github.com/PhilJay/MPAndroidChart)
- PDF generation by [iText7](https://itextpdf.com)

---

*"We built this system on a 35-hectare cashew farm in Odienné and validated operational efficiency improvements. Now it's ready for the world."*

**— The AfriNuts Team** 🇨🇮