# AfriNuts Farm OS v1.0

**An offline-first farm operations system built for West African cashew farmers.**

---

## 🌍 Product Vision

AfriNuts Farm OS started as an internal tool for a 35-hectare cashew farm in Odienné, Côte d'Ivoire. The vision is to evolve into a multi-tenant SaaS platform serving cashew farmers across West Africa.

**Current Phase:** Internal MVP  
**Next Phase:** Multi-farm SaaS

---

## 🏗 Architecture Overview

Built with Clean Architecture principles to ensure scalability from single-farm internal tool to multi-tenant SaaS without rewrites.

com.afrinuts.farmos
├── data
│ ├── local
│ │ ├── entity # Room database entities
│ │ ├── dao # Data Access Objects
│ │ └── database # Database configuration
│ ├── repository # Repository pattern (single source of truth)
│ └── model # Domain models
├── ui
│ ├── dashboard # Farm overview screen
│ ├── blocks # Block management
│ ├── tasks # Task tracking
│ ├── expenses # Expense logging
│ └── settings # Farm configuration
├── viewmodel # UI state management
└── utils # Helpers & extensions


**Key Design Decisions:**
- Repository pattern enables future cloud sync without UI changes
- Room database for offline-first operation
- Farm table from Day 1 enables multi-tenant future

---

## 📊 Database Schema (v1)
┌─────────────┐ ┌─────────────┐
│ Farm │ │ Block │
├─────────────┤ ├─────────────┤
│ id │◄──────│ farmId │
│ name │ │ blockName │
│ location │ │ hectareSize │
│ totalHectares│ │ status │
│ cashewHectares│ │ plantingDate│
│ treesPerHectare│ │ survivalRate│
│ plantingYear│ │ replacementCount│
└─────────────┘ └─────────────┘
│ │
│ │
▼ ▼
┌─────────────┐ ┌─────────────┐
│ Expense │ │ Task │
├─────────────┤ ├─────────────┤
│ id │ │ id │
│ farmId │ │ blockId │
│ blockId (nullable)│ │ assignedTo │
│ category │ │ taskType │
│ amount │ │ status │
│ date │ │ dueDate │
│ phase │ │ completionDate│
└─────────────┘ └─────────────┘

┌─────────────┐
│ WeatherCache│
├─────────────┤
│ id │
│ temperature │
│ humidity │
│ rainProb │
│ timestamp │
└─────────────┘


**Status Enum:**
- `NOT_CLEARED`
- `CLEARED`
- `PLOWED`
- `PLANTED`

**Expense Categories:**
- Land Clearing
- Plowing
- Seedlings
- Labor & Supervision
- Security
- Fencing
- Other

---

## 🗺 Navigation Flow
Splash Screen
│
▼
Dashboard ──────────────────────────────────┐
(Farm Summary, Weather, Quick Actions) │
│ │
├──► Blocks List ──► Block Detail │
│ │ │ │
│ │ ├──► Expenses for Block
│ │ │ │
│ │ └──► Tasks for Block
│ │ │
├──► All Expenses ──► Add/Edit Expense │
│ │ │
│ └─── Filter by Block/Category │
│ │
├──► All Tasks ────► Add/Edit Task │
│ │ │
│ └─── Filter by Block/Status │
│ │
└──► Weather ────── Current + Forecast │
│
└──► Settings ───── Farm Configuration │


---

## 🚀 MVP Scope (v1 Internal Release)

**Included (Phase 1):**
- ✅ Farm configuration (single farm)
- ✅ Block management (CRUD)
- ✅ Expense tracking with farm/block allocation
- ✅ Task management with history
- ✅ Dashboard with farm metrics
- ✅ Weather integration (OpenWeather)
- ✅ Offline-first operation
- ✅ Manual backup/export

**Excluded (Future Phases):**
- ❌ Individual tree tracking
- ❌ GPS/mapping
- ❌ Photo capture
- ❌ Multi-user login
- ❌ Cloud sync
- ❌ Multi-farm support

---

## 🛣 Development Roadmap

### Phase 0 — Architecture & Planning (Current)
- [x] Product vision definition
- [x] Core requirements documentation
- [x] Database schema design
- [x] Navigation flow mapping
- [x] Package structure definition
- [ ] GitHub repository setup
- [ ] Project creation in Android Studio

### Phase 1 — Core Data Layer (Weeks 1-3)
- [ ] Room entities implementation
- [ ] DAO interfaces
- [ ] Repository pattern
- [ ] Basic CRUD operations
- [ ] Unit tests

### Phase 2 — Farm Dashboard (Weeks 4-6)
- [ ] Dashboard UI
- [ ] Summary metrics
- [ ] Block list view
- [ ] Block detail view
- [ ] Basic data visualization

### Phase 3 — Task & Expense UI (Weeks 7-9)
- [ ] Expense entry forms
- [ ] Task assignment interface
- [ ] Filtering capabilities
- [ ] Reports view
- [ ] Data validation

### Phase 4 — Weather Integration (Week 10)
- [ ] OpenWeather API integration
- [ ] Local caching
- [ ] Weather display on dashboard
- [ ] Offline fallback

### Phase 5 — Field Testing (Weeks 11-16)
- [ ] APK distribution
- [ ] Real-world usage on 35-hectare farm
- [ ] Bug fixes
- [ ] UI refinements
- [ ] Workflow optimization

### Phase 6 — SaaS Evolution (Future)
- [ ] Authentication layer
- [ ] Cloud backend
- [ ] Multi-farm support
- [ ] Data sync engine
- [ ] Web dashboard

---

## 🛠 Technical Stack

| Component | Technology | Justification |
|-----------|------------|---------------|
| Language | Java | Consistent with existing codebase |
| UI | XML | Stable, proven, good tooling |
| Database | Room | SQLite abstraction with compile-time checks |
| Architecture | MVVM + Repository | Clean separation, testable, scalable |
| Background | WorkManager | Reliable task scheduling |
| Weather API | OpenWeather | Free tier, comprehensive data |
| Version Control | Git + GitHub | Industry standard |

---

## 📦 Deployment Strategy (Internal)

1. Generate signed APK
2. Install on:
    - Farm owner device
    - Farm manager device
3. Weekly manual backup (export database)
4. Future: Encrypted cloud backup

---

## 📈 Success Metrics (6-Month Target)

- ✅ Clear cost per hectare tracked
- ✅ Survival rate metrics per block
- ✅ Accurate total tree count
- ✅ Historical task log
- ✅ Weather-based decision support
- ✅ Clean operational reporting

---

## 🤝 Contributing

Internal development only until Phase 6.

---

## 📄 License

Private — All rights reserved. AfriNuts Farm OS.

---

## 👨‍💻 About

Built for AfriNuts cashew farm operations in Odienné, Côte d'Ivoire. Designed to scale across West Africa.

**Founder:** [Your Name]
**Location:** Odienné, Côte d'Ivoire
**Farm Size:** 35 hectares
**Trees:** 3,500 (100 per hectare)

---

*"We tested this system on a 35-hectare cashew operation in Odienné and validated operational efficiency improvements."*


---

## 🚀 MVP Scope (v1 Internal Release)

**Included (Phase 1):**
- ✅ Farm configuration (single farm)
- ✅ Block management (CRUD)
- ✅ Expense tracking with farm/block allocation
- ✅ Task management with history
- ✅ Dashboard with farm metrics
- ✅ Weather integration (OpenWeather)
- ✅ Offline-first operation
- ✅ Manual backup/export

**Excluded (Future Phases):**
- ❌ Individual tree tracking
- ❌ GPS/mapping
- ❌ Photo capture
- ❌ Multi-user login
- ❌ Cloud sync
- ❌ Multi-farm support

---

## 🛣 Development Roadmap

### Phase 0 — Architecture & Planning (Current)
- [x] Product vision definition
- [x] Core requirements documentation
- [x] Database schema design
- [x] Navigation flow mapping
- [x] Package structure definition
- [ ] GitHub repository setup
- [ ] Project creation in Android Studio

### Phase 1 — Core Data Layer (Weeks 1-3)
- [ ] Room entities implementation
- [ ] DAO interfaces
- [ ] Repository pattern
- [ ] Basic CRUD operations
- [ ] Unit tests

### Phase 2 — Farm Dashboard (Weeks 4-6)
- [ ] Dashboard UI
- [ ] Summary metrics
- [ ] Block list view
- [ ] Block detail view
- [ ] Basic data visualization

### Phase 3 — Task & Expense UI (Weeks 7-9)
- [ ] Expense entry forms
- [ ] Task assignment interface
- [ ] Filtering capabilities
- [ ] Reports view
- [ ] Data validation

### Phase 4 — Weather Integration (Week 10)
- [ ] OpenWeather API integration
- [ ] Local caching
- [ ] Weather display on dashboard
- [ ] Offline fallback

### Phase 5 — Field Testing (Weeks 11-16)
- [ ] APK distribution
- [ ] Real-world usage on 35-hectare farm
- [ ] Bug fixes
- [ ] UI refinements
- [ ] Workflow optimization

### Phase 6 — SaaS Evolution (Future)
- [ ] Authentication layer
- [ ] Cloud backend
- [ ] Multi-farm support
- [ ] Data sync engine
- [ ] Web dashboard

---

## 🛠 Technical Stack

| Component | Technology | Justification |
|-----------|------------|---------------|
| Language | Java | Consistent with existing codebase |
| UI | XML | Stable, proven, good tooling |
| Database | Room | SQLite abstraction with compile-time checks |
| Architecture | MVVM + Repository | Clean separation, testable, scalable |
| Background | WorkManager | Reliable task scheduling |
| Weather API | OpenWeather | Free tier, comprehensive data |
| Version Control | Git + GitHub | Industry standard |

---

## 📦 Deployment Strategy (Internal)

1. Generate signed APK
2. Install on:
    - Farm owner device
    - Farm manager device
3. Weekly manual backup (export database)
4. Future: Encrypted cloud backup

---

## 📈 Success Metrics (6-Month Target)

- ✅ Clear cost per hectare tracked
- ✅ Survival rate metrics per block
- ✅ Accurate total tree count
- ✅ Historical task log
- ✅ Weather-based decision support
- ✅ Clean operational reporting

---

## 🤝 Contributing

Internal development only until Phase 6.

---

## 📄 License

Private — All rights reserved. AfriNuts Farm OS.

---

## 👨‍💻 About

Built for AfriNuts cashew farm operations in Odienné, Côte d'Ivoire. Designed to scale across West Africa.

**Founder:** [Your Name]
**Location:** Odienné, Côte d'Ivoire
**Farm Size:** 35 hectares
**Trees:** 3,500 (100 per hectare)

---

*"We tested this system on a 35-hectare cashew operation in Odienné and validated operational efficiency improvements."*