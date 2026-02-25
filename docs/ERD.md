# Entity Relationship Diagram (Detailed)

## Entity Definitions

### FARM
| Column | Type | Key | Null | Description |
|--------|------|-----|------|-------------|
| id | Long | PK | NO | Unique identifier |
| name | String | | NO | Farm name |
| location | String | | YES | Geographic location |
| totalHectares | Double | | NO | Total farm size |
| cashewHectares | Double | | NO | Hectares under cashew |
| treesPerHectare | Int | | NO | Planting density |
| plantingYear | Int | | YES | Year of initial planting |

### BLOCK
| Column | Type | Key | Null | Description |
|--------|------|-----|------|-------------|
| id | Long | PK | NO | Unique identifier |
| farmId | Long | FK | NO | Parent farm |
| blockName | String | | NO | e.g., "A1", "B2" |
| hectareSize | Double | | NO | Size of this block |
| status | Enum | | NO | Current operational status |
| plantingDate | Long | | YES | When planted |
| survivalRate | Double | | YES | % of trees alive |
| replacementCount | Int | | YES | Trees replaced |
| notes | String | | YES | Additional info |

### EXPENSE
| Column | Type | Key | Null | Description |
|--------|------|-----|------|-------------|
| id | Long | PK | NO | Unique identifier |
| farmId | Long | FK | NO | Parent farm |
| blockId | Long | FK | YES | Specific block (if applicable) |
| category | Enum | | NO | Type of expense |
| amount | Double | | NO | Cost in local currency |
| date | Long | | NO | When incurred |
| phase | String | | YES | Farm phase (clearing, planting, etc.) |
| notes | String | | YES | Additional details |

### TASK
| Column | Type | Key | Null | Description |
|--------|------|-----|------|-------------|
| id | Long | PK | NO | Unique identifier |
| blockId | Long | FK | NO | Target block |
| assignedTo | String | | YES | Worker/team name |
| taskType | Enum | | NO | Type of work |
| status | Enum | | NO | Current state |
| dueDate | Long | | YES | Target completion |
| completionDate | Long | | YES | Actual completion |

### WEATHER_CACHE
| Column | Type | Key | Null | Description |
|--------|------|-----|------|-------------|
| id | Long | PK | NO | Unique identifier |
| temperature | Double | | YES | Current temperature |
| humidity | Int | | YES | Humidity percentage |
| rainProb | Int | | YES | Rain probability % |
| timestamp | Long | | NO | When data was fetched |

## Enums

### BlockStatus
- `NOT_CLEARED` - Land not yet cleared
- `CLEARED` - Land cleared, ready for plowing
- `PLOWED` - Plowed, ready for planting
- `PLANTED` - Planted with cashew trees

### ExpenseCategory
- `LAND_CLEARING` - Deforestation, stumping
- `PLOWING` - Tractor work, soil preparation
- `SEEDLINGS` - Cashew seedlings purchase
- `LABOR_SUPERVISION` - Workers and supervision
- `SECURITY` - Security guard services
- `FENCING` - Perimeter fencing
- `OTHER` - Miscellaneous

### TaskType
- `CLEARING` - Land clearing
- `PLOWING` - Soil plowing
- `PLANTING` - Tree planting
- `REPLACEMENT` - Replacing dead trees
- `FERTILIZING` - Fertilizer application
- `PRUNING` - Tree pruning
- `WEEDING` - Weed control
- `HARVEST` - Cashew harvest
- `OTHER`

### TaskStatus
- `PENDING` - Not started
- `IN_PROGRESS` - Currently being worked on
- `COMPLETED` - Finished
- `CANCELLED` - No longer needed

## Foreign Key Constraints

| Entity | Column | References | On Delete |
|--------|--------|------------|-----------|
| Block | farmId | Farm(id) | CASCADE |
| Expense | farmId | Farm(id) | CASCADE |
| Expense | blockId | Block(id) | SET NULL |
| Task | blockId | Block(id) | CASCADE |

## Indexes

For performance optimization:
- `idx_block_farmId` on Block(farmId)
- `idx_expense_farmId` on Expense(farmId)
- `idx_expense_blockId` on Expense(blockId)
- `idx_expense_date` on Expense(date)
- `idx_task_blockId` on Task(blockId)
- `idx_task_status` on Task(status)