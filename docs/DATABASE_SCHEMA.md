# Database Schema (ASCII ERD)

\`\`\`
┌─────────────────┐         ┌─────────────────┐
│      FARM       │         │      BLOCK      │
├─────────────────┤         ├─────────────────┤
│ PK │ id         │◄────┐   │ PK │ id         │
│    │ name       │     └───┤ FK │ farmId     │
│    │ location   │         │    │ blockName  │
│    │ totalHectares│       │    │ hectareSize│
│    │ cashewHectares│      │    │ status     │
│    │ treesPerHectare│     │    │ plantingDate│
│    │ plantingYear│         │    │ survivalRate│
└─────────────────┘         │    │ replacementCount│
│                   │    │ notes      │
│                   └─────────────────┘
│                            │
│                            │
▼                            ▼
┌─────────────────┐         ┌─────────────────┐
│    EXPENSE      │         │      TASK       │
├─────────────────┤         ├─────────────────┤
│ PK │ id         │         │ PK │ id         │
│ FK │ farmId     │         │ FK │ blockId    │
│ FK │ blockId    │◄────┐   │    │ assignedTo │
│    │ category   │     └───┤    │ taskType   │
│    │ amount     │         │    │ status     │
│    │ date       │         │    │ dueDate    │
│    │ phase      │         │    │ completionDate│
│    │ notes      │         └─────────────────┘
└─────────────────┘
│
│
▼
┌─────────────────┐
│  WEATHER_CACHE  │
├─────────────────┤
│ PK │ id         │
│    │ temperature│
│    │ humidity   │
│    │ rainProb   │
│    │ timestamp  │
└─────────────────┘
\`\`\`

## Relationships
- One Farm → Many Blocks (1:M)
- One Farm → Many Expenses (1:M)
- One Block → Many Expenses (1:M) [optional]
- One Block → Many Tasks (1:M)
- WeatherCache is standalone