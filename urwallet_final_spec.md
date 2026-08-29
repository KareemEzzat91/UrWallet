# UrWallet — Final Consolidated Product Specification

> **Version:** 1.0 (Final)
> **Status:** Implementation-Ready
> **Last Updated:** 2026-08-24

---

## CONSISTENCY AUDIT — Resolved Issues

Before reading the specification, the following contradictions from previous documents have been **eliminated**:

| Issue | Previous | Corrected |
|---|---|---|
| Platform | Mixed Flutter/Android references | **Native Android (Kotlin, XML, MVVM)** only |
| State Mgmt | Cubit / Bloc mentioned | **ViewModel + StateFlow + Flow** only |
| Quick Actions | Transfer, Deposit, Withdraw, Payment | **Removed** — FAB is the primary action |
| Goal saved amount | `GoalEntity.savedAmount` editable | **Derived** from `GoalContributionEntity` (SUM) |
| Budget double-counting | Category + Global treated as additive | **Category budgets are subsets** of global |
| Conflicting percentages | 52% vs 78% for same state | **Single unified calculation function** |
| PIN security | "SHA-256 encryption" | **Android Keystore + salted hash (PBKDF2)** |
| Currency | Multi-currency conversion in MVP | **Single-currency display only** (EGP default) |
| Recurring duplicates | No deduplication rule | **Idempotent worker** with explicit MVP behavior |
| Backup | No version field | **Schema version required** in every backup file |
| Smart Wizard | Mandatory | **Optional** — user can skip |
| WorkManager timing | Claimed exact 00:00 | **Best-effort** — tolerates delay |

---

## 1. Product Identity & Vision

### 1.1 Identity

| Field | Value |
|---|---|
| Product Name | **UrWallet** |
| Arabic Name | **محفظتك** |
| Tagline | **"محفظتك، بس أذكى."** |
| Category | Personal Finance & Financial Coaching |
| Target Market | Arabic-speaking users (primary: Egypt / EGP) |
| Platform | Android Native (Kotlin) |
| Connectivity | **100% Offline-First (MVP)** |

### 1.2 Core Purpose

UrWallet is a **personal financial coach application**. It helps users:

1. Track income and expenses
2. Understand spending behavior
3. Manage budgets
4. Track and achieve financial goals
5. Analyze financial health
6. Receive actionable, rule-based financial insights
7. Build better financial habits
8. Engage in saving challenges
9. Manage recurring transactions
10. Protect and own their financial data locally

### 1.3 What UrWallet Is NOT

UrWallet is explicitly **not** any of the following:

- ❌ A banking application
- ❌ A payment wallet
- ❌ A money-transfer platform
- ❌ A cryptocurrency application
- ❌ An investment platform
- ❌ A loan/credit application
- ❌ A cloud-dependent service

> Do not implement banking-style actions (Transfer, Deposit, Withdraw, Payment) anywhere in the MVP.
> The primary financial data-entry action is **"إضافة معاملة"** via the global FAB.

---

## 2. Technology Stack (Native Android)

```
Language:         Kotlin
Min SDK:          26 (Android 8.0 Oreo)
Target SDK:       34 (Android 14)
UI Framework:     XML Views + Android ViewBinding
Design System:    Material 3 (Material Design 3)
Architecture:     MVVM + Clean Architecture (Feature-First)
Database:         Room (SQLite — local source of truth)
Preferences:      DataStore (Preferences)
Async:            Kotlin Coroutines + Flow / StateFlow
Navigation:       Jetpack Navigation Component
Background:       WorkManager (reliable) + AlarmManager (exact time)
Security:         Android Keystore + BiometricPrompt
Images:           Glide (for receipt previews)
Charts:           MPAndroidChart
Splash:           SplashScreen API (core-splashscreen)
```

---

## 2.1 Architecture & Package Structure (Feature-First Clean Architecture)

### 2.1.1 Architectural Principles

1. **Feature-First Organization:**
   * Each business capability lives in its own feature module under `features/`.
   * A feature encapsulates its own `data/`, `domain/`, and `presentation/` layers ONLY when that feature genuinely requires them (no empty boilerplate folders).
   * Feature data (Entities, DAOs, Repositories) stays inside that feature's `data/` package.
   * Feature business logic (Models, Calculator rules, UseCases, Repository interfaces) stays inside that feature's `domain/` package.

2. **Strict Core Boundaries:**
   * `core/` contains **ONLY** genuinely shared infrastructure, utilities, and components used across multiple features.
   * Do NOT treat `core/` as a generic architectural dumping ground (no `core/data/` or `core/domain/`).
   * Room Database infrastructure (`UrWalletDatabase.kt`) is in `core/database/` to connect the feature entities and DAOs without moving entity ownership to core.
   * App preferences wrapper (`AppPreferences.kt`) is in `core/datastore/` for app-wide settings (onboarding status, currency symbol, PIN hash/salt).

3. **No Over-Engineering:**
   * Single `app` Gradle module (scalable package structure ready for future modularization).
   * No unnecessary generic abstractions, empty manager classes, or redundant layers.

### 2.1.2 Complete Package Tree

```
com.example.urwallet
│
├── core/                                  # Shared infrastructure & utilities ONLY
│   ├── common/                            # Cross-feature constants, enums, extensions, formatters
│   │   ├── Constants.kt
│   │   ├── Enums.kt                       # Shared enums (TransactionType, etc.)
│   │   ├── Resource.kt / Result.kt
│   │   ├── DateUtils.kt
│   │   ├── Formatters.kt                  # Currency & number formatting
│   │   └── Extensions.kt                  # View & Flow helper extensions
│   │
│   ├── database/                          # Central Room database wiring ONLY
│   │   ├── UrWalletDatabase.kt            # RoomDatabase declaration referencing feature entities/DAOs
│   │   └── DatabaseCallback.kt            # Pre-population callback (seeds default categories)
│   │
│   ├── datastore/                         # App-wide preferences
│   │   └── AppPreferences.kt              # Onboarding flag, PIN salt/hash, display currency
│   │
│   ├── designsystem/                      # Design system tokens & reusable UI components
│   │   ├── theme/
│   │   │   └── ThemeTokens.kt
│   │   └── components/
│   │       ├── UrWalletCard.kt
│   │       ├── UrWalletButton.kt
│   │       └── RingProgressBar.kt
│   │
│   └── navigation/                        # Shared navigation contracts & helpers
│       └── NavContracts.kt
│
└── features/                              # Business features (Feature-First)
    ├── onboarding/                        # First-launch value proposition & Smart Wizard
    │   └── presentation/
    │       ├── SplashActivity.kt
    │       ├── OnboardingActivity.kt
    │       ├── OnboardingPagerAdapter.kt
    │       ├── wizard/
    │       │   └── SmartSavingsWizardActivity.kt
    │       ├── OnboardingViewModel.kt
    │       └── OnboardingState.kt
    │
    ├── dashboard/                         # Dashboard Hub (الرئيسية)
    │   ├── domain/
    │   │   └── InsightEngine.kt           # Proactive rule-based financial coach tips for dashboard
    │   └── presentation/
    │       ├── DashboardFragment.kt
    │       ├── DashboardViewModel.kt
    │       ├── DashboardUiState.kt
    │       └── adapter/
    │           └── RecentTransactionsAdapter.kt
    │
    ├── transactions/                      # Transactions management & FAB entry
    │   ├── data/
    │   │   ├── entity/
    │   │   │   ├── TransactionEntity.kt
    │   │   │   └── CategoryEntity.kt
    │   │   ├── dao/
    │   │   │   ├── TransactionDao.kt
    │   │   │   └── CategoryDao.kt
    │   │   └── repository/
    │   │       └── TransactionRepositoryImpl.kt
    │   ├── domain/
    │   │   ├── model/
    │   │   │   ├── Transaction.kt
    │   │   │   └── Category.kt
    │   │   ├── repository/
    │   │   │   └── TransactionRepository.kt
    │   │   └── usecase/
    │   │       ├── GetTransactionsUseCase.kt
    │   │       ├── AddTransactionUseCase.kt
    │   │       ├── DeleteTransactionUseCase.kt
    │   │       └── GetCategoriesUseCase.kt
    │   └── presentation/
    │       ├── TransactionsFragment.kt
    │       ├── AddTransactionBottomSheetFragment.kt
    │       ├── TransactionDetailFragment.kt
    │       ├── FilterBottomSheetFragment.kt
    │       ├── CategoryAnalyticsFragment.kt
    │       ├── TransactionsViewModel.kt
    │       └── adapter/
    │           └── GroupedTransactionAdapter.kt
    │
    ├── goals/                             # Goals & Contributions
    │   ├── data/
    │   │   ├── entity/
    │   │   │   ├── GoalEntity.kt
    │   │   │   └── GoalContributionEntity.kt  # Single source of truth for goal savings
    │   │   ├── dao/
    │   │   │   ├── GoalDao.kt
    │   │   │   └── GoalContributionDao.kt
    │   │   └── repository/
    │   │       └── GoalRepositoryImpl.kt
    │   ├── domain/
    │   │   ├── model/
    │   │   │   ├── Goal.kt
    │   │   │   └── GoalContribution.kt
    │   │   ├── repository/
    │   │   │   └── GoalRepository.kt
    │   │   └── usecase/
    │   │       ├── GetGoalsUseCase.kt
    │   │       ├── AddGoalUseCase.kt
    │   │       └── AddGoalContributionUseCase.kt
    │   └── presentation/
    │       ├── GoalsFragment.kt
    │       ├── AddGoalFragment.kt
    │       ├── GoalDetailFragment.kt
    │       ├── ContributeGoalBottomSheetFragment.kt
    │       ├── GoalsViewModel.kt
    │       └── adapter/
    │           └── GoalsAdapter.kt
    │
    ├── budgets/                           # Budgets & Overspending tracking
    │   ├── data/
    │   │   ├── entity/
    │   │   │   └── BudgetEntity.kt
    │   │   ├── dao/
    │   │   │   └── BudgetDao.kt
    │   │   └── repository/
    │   │       └── BudgetRepositoryImpl.kt
    │   ├── domain/
    │   │   ├── model/
    │   │   │   └── Budget.kt
    │   │   ├── repository/
    │   │   │   └── BudgetRepository.kt
    │   │   ├── calculator/
    │   │   │   └── BudgetCalculator.kt    # Single source of truth for budget % & status
    │   │   └── usecase/
    │   │       ├── GetBudgetsUseCase.kt
    │   │       └── SaveBudgetUseCase.kt
    │   └── presentation/
    │       ├── BudgetsFragment.kt
    │       ├── AddBudgetBottomSheetFragment.kt
    │       ├── BudgetsViewModel.kt
    │       └── adapter/
    │           └── BudgetCategoryAdapter.kt
    │
    ├── analytics/                         # Financial Analytics, Health Score & Habits
    │   ├── domain/
    │   │   ├── calculator/
    │   │   │   └── FinancialHealthCalculator.kt # 0–100 score across Planning, Saving, Control
    │   │   └── model/
    │   │       └── FinancialHealthScore.kt
    │   └── presentation/
    │       ├── AnalyticsFragment.kt
    │       ├── HabitsFragment.kt
    │       └── AnalyticsViewModel.kt
    │
    ├── challenges/                        # Saving Challenges gamification
    │   ├── data/
    │   │   ├── entity/
    │   │   │   └── ChallengeEntity.kt
    │   │   ├── dao/
    │   │   │   └── ChallengeDao.kt
    │   │   └── repository/
    │   │       └── ChallengeRepositoryImpl.kt
    │   ├── domain/
    │   │   ├── model/
    │   │   │   └── Challenge.kt
    │   │   └── repository/
    │   │       └── ChallengeRepository.kt
    │   └── presentation/
    │       ├── ChallengesFragment.kt
    │       ├── ChallengeDetailFragment.kt
    │       └── ChallengesViewModel.kt
    │
    └── more/                              # More Hub, Recurring, Security & Data Management
        ├── data/
        │   ├── entity/
        │   │   └── RecurringTransactionEntity.kt
        │   ├── dao/
        │   │   └── RecurringTransactionDao.kt
        │   ├── backup/
        │   │   └── BackupManager.kt       # Atomic JSON/CSV export, import & restore
        │   └── repository/
        │       └── RecurringRepositoryImpl.kt
        ├── domain/
        │   └── repository/
        │       └── RecurringRepository.kt
        └── presentation/
            ├── MoreFragment.kt
            ├── recurring/
            │   ├── RecurringFragment.kt
            │   ├── AddRecurringBottomSheetFragment.kt
            │   └── RecurringTransactionWorker.kt # WorkManager idempotent daily runner
            ├── security/
            │   ├── AppLockActivity.kt
            │   └── PinSetupFragment.kt
            ├── backup/
            │   └── DataManagementFragment.kt
            └── MoreViewModel.kt
```

---

## 3. Design System

### 3.1 Color Tokens

| Token | Hex | Usage |
|---|---|---|
| `Primary` | `#000666` / `#0A1172` | Buttons, active states, hero cards, prominent numbers |
| `Primary Variant` | `#1A237E` → `#4C56AF` | Gradient backgrounds (challenges, hero sections) |
| `Income / Success` | `#00732C` / `#5CFD80` | Income transactions (`+`), goal progress, completions |
| `Expense / Danger` | `#BA1A1A` / `#D32F2F` | Expense entries (`-`), budget overages, destructive actions |
| `Warning` | `#FBBC00` / `#FF9800` | Budget near-limit badges (>80%), alert banners |
| `Background` | `#F8F9FA` | App background canvas |
| `Surface` | `#FFFFFF` | Cards, bottom sheets, input fields |
| `Surface Glass` | `rgba(255,255,255,0.7)` | Frosted glass overlays |
| `Text Primary` | `#191C1D` | Headings, amounts, main labels |
| `Text Secondary` | `#454652` / `#767683` | Subtitles, timestamps, hints |
| `Divider` | `rgba(198,197,212,0.35)` | List separators, card borders |

### 3.2 Typography (IBM Plex Sans Arabic)

| Style | Size | Weight | Usage |
|---|---|---|---|
| Hero Display | 36–44sp | Bold | Balance amounts, large numbers |
| Title Large | 22–24sp | Bold | Screen headers |
| Title Medium | 18–20sp | SemiBold | Section headers, card titles |
| Body Large | 15–16sp | Medium | Merchant names, category labels |
| Body Medium | 13–14sp | Regular | Notes, dates, descriptions |
| Caption | 11–12sp | Regular/Medium | Badges, hints, percentages |

### 3.3 Shape & Elevation

| Element | Radius | Shadow |
|---|---|---|
| Main cards | `16–20dp` | `0 2 8 rgba(0,6,102,0.05)` |
| Hero Balance Card | `20dp` | `0 8 24 rgba(0,6,102,0.14)` |
| Buttons, Inputs | `12–14dp` | none |
| Pills, Chips | `9999dp` | none |
| FAB | `16dp` (circle) | `0 6 16 rgba(0,6,102,0.28)` |

### 3.4 RTL Standard

- Use `android:layoutDirection="rtl"` at app level
- Use `start/end` attributes — **never** `left/right`
- Back button points **right** `→`; Forward chevron points **left** `←`
- Numbers and currency symbols remain readable as-is
- Charts: bar order progresses left-to-right (do not mirror chart axes)

---

## 4. Information Architecture & Navigation

### 4.1 Navigation Structure

```
SplashActivity
    │
    ├─[First Launch]──► OnboardingActivity (3 slides, skip available)
    │                        │
    │                        └──► Smart Savings Wizard (optional, 5 steps, skip available)
    │                                   │
    │                   [Skip or Complete]
    ├─[Lock Enabled]──► AppLockActivity (PIN / Biometrics)
    │
    └──────────────────► MainActivity
                              │
              ┌───────────────┼────────────────┬──────────────────┐
              ▼               ▼                ▼                  ▼
      Tab 1             Tab 2            Tab 3              Tab 4
   الرئيسية         المعاملات          أهدافي            المزيد
  (Dashboard)      (Transactions)       (Goals)          (More Hub)
```

### 4.2 Dashboard (Tab 1 — الرئيسية)

The Dashboard answers:
- What is my current balance?
- How much have I earned and spent?
- Where am I on my budget?
- What should I know right now? (Insight)
- How is my nearest goal progressing?
- What did I recently spend?
- Do I have an active challenge?

**Dashboard sections (top to bottom):**
1. Top App Bar (Avatar + Greeting + Notification Bell)
2. Hero Balance Card (Net Balance + Income pill + Expense pill)
3. Budget Status Strip (unified single percentage)
4. Smart Insight Banner (rule-based coaching tip)
5. Quick Glance Bento (Today's spend + Budget remaining + Weekly sparkline)
6. Active Challenge Card
7. Nearest Active Goal Card (percentage ring + "عرض الكل" link)
8. Recent Transactions (last 4)
9. "سجل المعاملات" link → Tab 2

**Primary Action:** Global FAB `+` → Add Transaction Bottom Sheet

> The Dashboard does NOT contain banking quick actions (Transfer, Deposit, Withdraw, Payment).

---

## 5. Room Database Schema

### TransactionEntity
```
id:           Long    PK, autoGenerate
amount:       Double  Always positive
type:         String  INCOME | EXPENSE
categoryId:   Long    FK → CategoryEntity.id
title:        String
note:         String?
date:         Long    Epoch ms
receiptPath:  String? Local file path
createdAt:    Long
updatedAt:    Long
```

### CategoryEntity
```
id:         Long    PK, autoGenerate
name:       String
type:       String  INCOME | EXPENSE | BOTH
icon:       String
color:      String  Hex
isDefault:  Boolean
isDeleted:  Boolean
```

**Default EXPENSE categories:** أكل، مواصلات، تسوق، فواتير، ترفيه، صحة، تعليم، أخرى
**Default INCOME categories:** راتب، عمل حر، مكافأة، أخرى

### GoalEntity
```
id:            Long    PK, autoGenerate
name:          String
icon:          String
targetAmount:  Double  > 0
paceMode:      String  RELAXED | BALANCED | AGGRESSIVE
monthlyTarget: Double  Calculated from targetAmount / months
deadline:      Long    Epoch ms
createdAt:     Long
isCompleted:   Boolean  (savedAmount >= targetAmount)
isDeleted:     Boolean
```

> GoalEntity does NOT have a `savedAmount` column.
> savedAmount = SUM(GoalContributionEntity.amount WHERE goalId = this.id)

### GoalContributionEntity (Source of Truth for Goal Progress)
```
id:      Long    PK, autoGenerate
goalId:  Long    FK → GoalEntity.id
amount:  Double  Always positive
note:    String?
date:    Long
```

**Always computed (never stored separately):**
```
savedAmount     = SUM(amount) for goalId
remainingAmount = targetAmount - savedAmount
progressPct     = (savedAmount / targetAmount) * 100, capped at 100
isCompleted     = savedAmount >= targetAmount
```

### BudgetEntity
```
id:             Long    PK, autoGenerate
categoryId:     Long?   NULL = global; FK = category budget
amount:         Double  Spending limit
month:          Int     1–12
year:           Int
alertThreshold: Double  Default 0.80
createdAt:      Long
```

**Budget Hierarchy Rule:** Category budgets are SUBSETS of the global budget. Never sum them together.

### RecurringTransactionEntity
```
id:             Long    PK, autoGenerate
title:          String
amount:         Double  Always positive
type:           String  INCOME | EXPENSE
categoryId:     Long    FK → CategoryEntity.id
frequency:      String  DAILY | WEEKLY | MONTHLY | YEARLY
startDate:      Long
endDate:        Long?
nextOccurrence: Long    Next scheduled auto-transaction date
isActive:       Boolean
createdAt:      Long
```

### ChallengeEntity
```
id:              Long    PK, autoGenerate
title:           String
description:     String
type:            String  NO_SPENDING | SAVE_AMOUNT | REDUCE_CATEGORY
targetAmount:    Double?
targetDays:      Int?
categoryId:      Long?
startDate:       Long
endDate:         Long
currentProgress: Double
streakDays:      Int
isCompleted:     Boolean
isActive:        Boolean
```

---

## 6. Budget Calculation Rules (features/budgets/domain/calculator/BudgetCalculator.kt — Single Source)

```
Global spent    = SUM(EXPENSE transactions in current month)
Category spent  = SUM(EXPENSE transactions WHERE categoryId = X in current month)
Percentage      = spent / budgetAmount * 100

Status:
  < 80%    → HEALTHY   (green)
  80–99%   → NEAR_LIMIT (yellow ⚠️)
  >= 100%  → EXCEEDED  (red 🔴)
```

This logic lives **once** in `features/budgets/domain/calculator/BudgetCalculator.kt` and is reused by Dashboard, Budgets screen, Analytics, and Insights. Contradictory percentages across screens are a critical bug.

---

## 7. Goal Contribution Flow

```
User taps "إضافة مبلغ" → enters amount
↓
Validate amount > 0
↓
BEGIN ROOM TRANSACTION
  1. INSERT GoalContributionEntity(goalId, amount, note, date)
  2. SELECT SUM(amount) WHERE goalId = X → newSavedAmount
  3. UPDATE GoalEntity SET isCompleted = (newSavedAmount >= targetAmount)
COMMIT
↓
Room Flow emits new value
↓
UI updates: progress ring, remaining amount, Dashboard card
```

---

## 8. Recurring Transactions — Idempotent Worker

**MVP Behavior (Strategy B):** Record only the next due occurrence per run. Advance `nextOccurrence`. Do not catch up all missed days.

```
Worker runs (best-effort, daily via WorkManager):
  For each active recurring item WHERE nextOccurrence <= today:
    Check: no existing transaction for (recurringId, nextOccurrence date)
    If safe:
      INSERT TransactionEntity
      UPDATE nextOccurrence = calculateNext(current, frequency)
```

WorkManager does **not** guarantee exact timing. The idempotency check ensures correctness regardless of when the worker runs.

---

## 9. WorkManager vs AlarmManager

| Responsibility | Tool |
|---|---|
| Recurring transaction evaluation | **WorkManager** (best-effort, daily) |
| Local database maintenance | **WorkManager** |
| Automatic local backup | **WorkManager** |
| Future API synchronization | **WorkManager** |
| Exact user-requested reminder time | **AlarmManager** (exact) |
| Daily logging nudge notification | **AlarmManager** |
| Time-sensitive alerts | **AlarmManager** |

---

## 10. App Lock & Security

**PIN Verification (Correct Approach):**
1. Generate random **salt** on first PIN setup (stored in `DataStore`)
2. Derive key from `PIN + salt` using **PBKDF2WithHmacSHA256** (`SecretKeyFactory`)
3. Store only the **derived key hash + salt** in `DataStore`
4. Unlock: re-derive from entered PIN + stored salt → compare

> This is application-level privacy protection (not device-level encryption). Goal: prevent casual access to financial data.

**Biometric:** Use `BiometricPrompt`. Fallback to PIN if biometric fails or unavailable.

---

## 11. Backup & Restore

**Backup file format (versioned):**
```json
{
  "version": 1,
  "appVersion": "1.0.0",
  "exportDate": "2026-08-24T19:00:00Z",
  "currency": "EGP",
  "transactions": [...],
  "categories": [...],
  "goals": [...],
  "goalContributions": [...],
  "budgets": [...],
  "recurringTransactions": [...],
  "challenges": [...]
}
```

**Import strategy:** User explicitly chooses:
- **"استبدال البيانات الحالية"** → Clear all Room data, then import in single transaction
- **"إضافة البيانات"** → Import, skip conflicting IDs

---

## 12. Currency — Single-Currency MVP

Default: **EGP / ج.م**

Changing currency in Settings only changes the **display symbol**. It does NOT convert amounts. No exchange-rate API. True multi-currency is a future feature.

---

## 13. Smart Insights Engine (features/dashboard/domain/InsightEngine.kt — Local & Rule-Based)

| Rule | Example |
|---|---|
| Category spike >20% vs last month | "مصروفات الأكل زادت 23% عن الشهر الماضي." |
| Peak spending day | "أعلى معدل إنفاق عندك يوم الجمعة." |
| Below-average spending week | "أنت أنفقت أقل من متوسطك هذا الأسبوع. 🎉" |
| Goal acceleration | "لو وفرت 500 ج.م شهريًا، هتوصل لهدفك أسرع." |
| Budget near limit | "اقتربت من ميزانية الأكل. المتبقي 200 ج.م فقط." |
| No transactions today | "متنساش تسجل مصاريف النهارده 💰" |

The engine is located at `features/dashboard/domain/InsightEngine.kt` to generate proactive coaching tips directly for the Dashboard Hub. No AI or external API required. Future AI enhancement is a separate phase.

---

## 14. Financial Health Score (features/analytics/domain/calculator/FinancialHealthCalculator.kt)

Located at `features/analytics/domain/calculator/FinancialHealthCalculator.kt`.

```
Score = (PlanningScore + SavingScore + ControlScore) / 3

PlanningScore (0–100):
  Has global budget set this month     → +40 pts
  Has at least one active goal         → +30 pts
  Uses recurring transactions          → +30 pts

SavingScore (0–100):
  SUM(goal contributions this month) / monthlyTarget * 100, capped at 100

ControlScore (0–100):
  No budget set     → 50 (neutral)
  Budget set:       → 100 - max(0, (globalBudgetPct - 50) * 2)
                       Full control at ≤50% spent; 0 at 100%+
```

Deterministic: same Room data → same score. Documented as rule-based approximation.

---

## 15. Smart Savings Wizard

**Flow:**
```
Onboarding → "ابدأ الآن →"
↓
Smart Savings Wizard (5 Steps):
  Step 1: Goal Focus (Emergency / Travel / Home / Retirement / Custom)
  Step 2: Target Amount (slider + presets)
  Step 3: Pace Plan (هادئ / متوازن / مكثف) — live monthly calculation
  Step 4: Reminders (toggles + time picker)
  Step 5: Habits education
↓
[Complete] → Creates GoalEntity + first GoalContributionEntity(0)
[Skip "تخطي الآن"] → Dismiss wizard → Dashboard directly
```

"تخطي الآن" must be visible on every wizard step.

---

## 16. User Journeys

### A — First Launch
Splash → Onboarding (3 slides, skippable) → Smart Wizard (optional) → Dashboard (with or without first goal)

### B — Quick Expense Logging
FAB `+` → AddTransactionBottomSheet → Enter amount, pick category → Save → Room emits → Dashboard/Budget/List update → Success snackbar

### C — Income Logging
FAB `+` → Toggle `دخل` → Category grid switches to income categories → Enter amount → Save

### D — Filtering Transactions
Tab 2 → Filter button → FilterBottomSheet → Select Type/Category/Period → Apply → Filtered list with result count

### E — Goal Contribution (GoalContributionEntity = source of truth)
Tab 3 → Goal Detail → progress = SUM(contributions) → "+ إضافة مبلغ" → ContributeBottomSheet → DB transaction → progress ring updates

### F — Budget Warning (unified calculation)
Transaction logged → BudgetCalculator computes percentage → Dashboard banner + Budget chip both show same value → Alert at 80%

### G — Recurring Transactions (idempotent)
WorkManager runs → checks nextOccurrence ≤ today → checks no existing transaction for that date → INSERT + advance nextOccurrence → local notification

### H — Financial Analytics & Health
Tab 4 → Analytics → local bar chart + health score from FinancialHealthCalculator → Habits personality card

### I — Backup & Security
App Lock: PIN → PBKDF2+salt → DataStore
Export: Room → JSON with version field → encrypted → SAF file save
Import: Select file → decrypt → validate → [Replace/Add] → single Room transaction

### J — Full Offline Operation
All features (transactions, goals, budgets, analytics, recurring, notifications, backup) work with zero network. Offline is the default mode.

---

## 17. Implementation Phases

| Phase | Focus | Deliverable |
|---|---|---|
| 1 | Foundation: Project, Room, Design System | App compiles, DB seeds, BudgetCalculator built |
| 2 | Splash, Onboarding, Smart Savings Wizard | First launch experience, optional first goal |
| 3 | MainActivity, BottomNavigation, FAB | 4-tab navigation works |
| 4 | Transactions CRUD, Add Transaction, Filters | Full transaction management |
| 5 | Dashboard Hub | Live data from Room across all cards |
| 6 | Search, Filters, Category Analytics | Drill-down into spending categories |
| 7 | Goals, Icon Picker, Contributions | Goal progress from GoalContributionEntity |
| 8 | Budgets, AddBudget, Status Indicators | Unified budget calculations |
| 9 | Analytics, Health Score, Habits, Insights | Financial coaching feel alive |
| 10 | Saving Challenges | Gamified engagement |
| 11 | Recurring Transactions, WorkManager | Idempotent auto-logging |
| 12 | Notifications, AlarmManager, Reminders | Proactive alerts |
| 13 | App Lock, Biometrics, PBKDF2+Keystore | Security protection |
| 14 | Backup, Export, Import, Delete All | Full data ownership |
| 15 | Polish, RTL, Skeleton, Empty States, QA | Production quality |

> Networking is NOT part of these phases. It is a separate future phase.

---

## 18. Acceptance Criteria

1. ✅ Entire MVP works without internet connection
2. ✅ Room is the local source of truth — UI never queries Room directly
3. ✅ UI observes Room through Flow/StateFlow via ViewModels
4. ✅ Adding a transaction immediately updates Dashboard, Transactions, Budgets, Analytics
5. ✅ Goal progress always derived from GoalContributionEntity (never an editable savedAmount)
6. ✅ Goal completion auto-checked after every contribution
7. ✅ Budget percentages are identical on Dashboard, Budgets, and Analytics (single BudgetCalculator)
8. ✅ Category budgets are never summed with global budget
9. ✅ Recurring transactions cannot be duplicated for the same occurrence date
10. ✅ App Lock does not store raw PINs — uses PBKDF2 with salt and Android Keystore
11. ✅ Backup files contain a version field
12. ✅ Backup restore is a single atomic Room transaction
13. ✅ RTL consistent on all screens (start/end attributes, correct arrow directions)
14. ✅ Currency symbol ج.م is consistent everywhere
15. ✅ No banking/payment features exist (no Transfer, Deposit, Withdraw, Payment)
16. ✅ No networking exists in MVP
17. ✅ All screens handle Loading, Success, Error, and Empty states
18. ✅ Smart Savings Wizard can be skipped at any step

---

## 19. Future Networking Phase

When introduced: Repository extends to read from both Room (local) and Remote API. API responses update Room → Room Flow notifies UI. App remains functional offline. UI never observes API directly.

Potential future features (not MVP):
- Cloud backup and device sync
- Multi-currency with live exchange rates
- Bank statement CSV/OFX import
- AI-enhanced insights
- Shared household budgets
