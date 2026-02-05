# Application Architecture

This project follows a strict layered architecture.
The architecture is FINAL and MUST NOT be changed.
LLMs and developers are allowed to implement code
ONLY inside existing layers.

---

## High-level architecture (left → right)

UI (Jetpack Compose)
→ ViewModel
→ Orchestrator (Agent core)
→ Adapters
→ Repositories
→ Local Database (Room)
→ DAO
→ Tables

---

## Core principles

- Each layer has a single responsibility
- Layers communicate only in one direction
- No shortcuts between layers
- No business logic in data layers
- No data access in UI layers

---

## Layer responsibilities

### UI (Jetpack Compose)

Responsibilities:
- Display UI state
- Handle user input
- Forward events to ViewModel

Rules:
- NO business logic
- NO data access
- NO Flow collection (UI only collects StateFlow)

---

### ViewModel

Responsibilities:
- Hold UI state (`StateFlow`)
- Convert Repository `Flow` → `StateFlow`
- Forward user actions to Orchestrator

Rules:
- NO business rules
- NO direct database access
- NO intent analysis
- NO DAO usage

---

### Orchestrator (Agent core)

Responsibilities:
- Analyze user input
- Build execution plans (intents)
- Decide which Adapters to call and in what order

Rules:
- Orchestrator decides execution order
- Orchestrator does NOT execute actions itself
- Orchestrator does NOT access repositories or database
- Orchestrator does NOT hold UI state

---

### Adapters

Adapters are **NOT classic adapter-pattern classes**.
They are **action executors** called by Orchestrator.

Adapters execute domain actions and use Repositories.

#### FoodParsingAdapter
- Parses user text into structured food data
- NO database access
- NO state
- NO Flow collection

#### AppControlAdapter
- Applies business rules
- Updates daily nutrition data and limits
- Uses DayEntryRepository and UserProfileRepository
- NO text parsing
- NO UI interaction

#### ChatAIAdapter
- Generates chat responses
- Stores messages via ChatRepository
- NO business rules
- NO database access outside Repository

---

### Repositories

Repositories abstract data sources.
They hide Room / Cloud / Cache implementations.

Repositories:
- DayEntryRepository
- UserProfileRepository
- ChatRepository

Rules:
- Repositories know DAO
- Repositories return `Flow` or `suspend` results
- NO business logic
- NO UI logic
- NO intent logic

---

### Local Database (Room)

LocalDatabase contains DAO only.

DAO:
- DayEntryDao → DayEntry table
- UserProfileDao → UserProfile table
- ChatDao → ChatMessages table

Rules:
- DAO = pure SQL
- NO business logic
- NO conditions or calculations
- NO Flow transformation logic

---

### Tables (Data Model)

Tables:
- DayEntry
- UserProfile
- ChatMessages

Relations:
- 1 UserProfile → N DayEntry
- 1 UserProfile → N ChatMessages

Tables contain only raw data.
No derived or calculated values.

---

## Data flow rules

- DAO exposes `Flow`
- Repository passes or maps `Flow`
- ViewModel converts `Flow` → `StateFlow`
- UI collects `StateFlow`

Adapters and Orchestrator MUST NOT collect Flow.

---

## Strict rules (must not be violated)

- DO NOT add new architectural layers
- DO NOT move logic between layers
- DO NOT merge Adapter and Repository
- DO NOT add business logic to DAO
- DO NOT access database outside Repository
- DO NOT collect Flow outside ViewModel

If a requested change breaks these rules — STOP.