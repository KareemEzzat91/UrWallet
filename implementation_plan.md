# UrWallet — Master Implementation Plan (Updated)

## 📌 Project Overview

**UrWallet (محفظتك، بس أذكى)** is an Arabic-first, offline-first native personal finance application built to act as a personal money coach.

* **Target Platform:** Android (Min SDK 26, Target SDK 34)
* **Architecture:** Clean Architecture + Feature-First (Data, Domain, Presentation)
* **Design Source of Truth:** Updated Figma Canvas (`ShQJ1qhXf15TpQpSkKIbIs`, node `0:1` across 7 organized sections)
* **Default Currency:** Egyptian Pound (`ج.م` / EGP) with dynamic multi-currency support
* **Core Philosophy:** 100% Offline-First, Zero forced network dependency, Proactive financial coaching insights

---

## 🎨 Design System & Visual Tokens (from Figma)

| Token | Hex / Value | Usage & Meaning |
|---|---|---|
| `Primary` | `#000666` / `#0A1172` | Royal Navy — primary buttons, hero balance card, active tab indicators |
| `Primary Variant` | `#1A237E` ➔ `#4C56AF` | Dynamic gradient for saving challenges & hero cards |
| `Income / Growth` | `#00732C` / `#5CFD80` | Income entries (`+`), goal progress rings, positive trend pills |
| `Expense / Danger` | `#BA1A1A` / `#D32F2F` | Expense entries (`-`), destructive actions, budget overages |
| `Warning / Alert` | `#FBBC00` / `#FF9800` | Budget warnings (>80%), smart proactive alert cards |
| `Background` | `#F8F9FA` | Clean, cool background canvas |
| `Surface Light` | `#FFFFFF` | Primary card background, bottom sheets, input containers |
| `Surface Glass` | `rgba(255,255,255,0.7)` | Frosted glass cards (`blur(12px)`) |
| `Text Primary` | `#191C1D` | Headings, amounts, primary labels (soft carbon black) |
| `Text Secondary` | `#454652` / `#767683` | Timestamps, notes, secondary subtitles, hints |
| `Radius Large` | `20dp` | Main cards, modals, hero containers |
| `Radius Medium` | `12dp` – `14dp` | Buttons, text fields, chips |
| `Typography` | **IBM Plex Sans Arabic** | Modern, highly legible Arabic font with clean numerals |

---

## 🗺️ Information Architecture & Navigation Graph

```
SplashActivity
    ↓ (First Launch)
OnboardingActivity (3 Value Slides)
    ↓ (Tap 'ابدأ الآن')
SmartSavingsWizard (5 Steps: Focus → Target → Pace Plan → Reminders → Habits)
    ↓ (Complete Setup)
    ↓ (If App Lock Enabled)
AppLockActivity (PIN / Biometrics)
    ↓
MainActivity (4-Tab BottomNavigation + Pinned FAB)
    ├── 🟢 Tab 1: Dashboard (الرئيسية)
    │   ├── Header (Avatar + Greeting + Notification Bell)
    │   ├── Hero Balance Card (Net Balance + Income/Expense Pills)
    │   ├── Quick Actions Row ([تحويل] [إيداع] [سحب] [دفع])
    │   ├── Smart Insight Banner (Proactive financial coach tip)
    │   ├── Quick Glance Bento Grid (Today's Spend + Budget Remaining + Weekly Trend)
    │   ├── Active Daily Challenge Card
    │   ├── Active Goal Card (Nearest deadline)
    │   └── Recent Transactions (Last 4) ➔ Link to Tab 2
    │
    ├── 🟣 Tab 2: Transactions (المعاملات)
    │   ├── Search Bar + Filter Button
    │   ├── Filter Bottom Sheet (Type, Category, Date Range, Amount Range)
    │   ├── Grouped List by Date (Today, Yesterday, Date Headers)
    │   ├── Transaction Detail Screen (Receipt Image, Metadata, Edit, Share, Delete)
    │   └── Category Analytics Deep Dive Screen (e.g. الأكل 🍔)
    │
    ├── 🟡 Tab 3: Goals (أهدافي)
    │   ├── Goals Overview Hero (Total Active Goals + Remaining Savings Needed)
    │   ├── Active Goals List (Progress rings + Deadline status)
    │   ├── New Goal Screen (Name, Target, Date, Pace Slider)
    │   │   └── Icon Picker Grid (Car, House, Plane, Laptop, etc.)
    │   ├── Goal Details Screen (31% Radial Ring, Insight, Contributions List)
    │   └── Add Money Bottom Sheet (Deposit amount + Note)
    │
    └── 🔵 Tab 4: More Hub (المزيد)
        ├── User Profile Summary Card
        ├── Grid Features:
        │   ├── 📊 Financial Analytics (Monthly trends, Bar chart, Category breakdown)
        │   ├── 🎯 Monthly Budgets (78% Gauge + Category limits + Add Budget Sheet)
        │   ├── 🏆 Saving Challenges (7-Day, Coffee, 1000 EGP + Detail screen)
        │   ├── 🧠 Financial Habits & Personality (المخطط الذكي + 50/30/20 Rule)
        │   ├── 🔁 Recurring Transactions (List + Add Recurring Sheet + Auto Worker)
        │   ├── 🏷️ Categories Management (8 Default + Add Custom Category)
        │   └── 🔔 Notifications Center (Goal alerts, budget warnings, daily nudges)
        └── System & Security:
            ├── 🔒 App Lock & PIN (3-Step setup + Biometrics)
            ├── 💾 Data Management (Local Backup, Export CSV/JSON, Import, Delete)
            ├── ⚙️ General Settings (Currency, Language, Dark Mode)
            └── ℹ️ About UrWallet (v1.0.0, Privacy, Terms)

    └── ➕ Global FAB:
        └── Add Transaction Sheet (Toggle [مصروف | دخل], Amount, Dynamic Categories, Date/Time, Receipt)
```

---

## 🗄️ Room Database Schema

### 1. `TransactionEntity`
* `id`: Long (PK autoGenerate)
* `amount`: Double
* `type`: String (`INCOME` / `EXPENSE`)
* `categoryId`: Long (FK ➔ `CategoryEntity`)
* `title`: String
* `note`: String?
* `date`: Long (epoch timestamp)
* `receiptPath`: String? (local file path)
* `createdAt`: Long
* `updatedAt`: Long

### 2. `CategoryEntity`
* `id`: Long (PK autoGenerate)
* `name`: String
* `type`: String (`INCOME` / `EXPENSE` / `BOTH`)
* `icon`: String (resource identifier or emoji)
* `color`: String (hex code)
* `isDefault`: Boolean
* `isDeleted`: Boolean

### 3. `GoalEntity`
* `id`: Long (PK autoGenerate)
* `name`: String
* `icon`: String
* `targetAmount`: Double
* `savedAmount`: Double (derived from contributions)
* `paceMode`: String (`RELAXED` / `BALANCED` / `AGGRESSIVE`)
* `monthlyTarget`: Double
* `deadline`: Long
* `createdAt`: Long
* `isCompleted`: Boolean
* `isDeleted`: Boolean

### 4. `GoalContributionEntity`
* `id`: Long (PK autoGenerate)
* `goalId`: Long (FK ➔ `GoalEntity`)
* `amount`: Double
* `note`: String?
* `date`: Long

### 5. `BudgetEntity`
* `id`: Long (PK autoGenerate)
* `categoryId`: Long? (null = global monthly budget)
* `amount`: Double
* `month`: Int
* `year`: Int
* `alertThreshold`: Double (default 0.80 = 80%)
* `createdAt`: Long

### 6. `RecurringTransactionEntity`
* `id`: Long (PK autoGenerate)
* `title`: String
* `amount`: Double
* `type`: String (`INCOME` / `EXPENSE`)
* `categoryId`: Long
* `frequency`: String (`DAILY` / `WEEKLY` / `MONTHLY` / `YEARLY`)
* `startDate`: Long
* `endDate`: Long?
* `nextOccurrence`: Long
* `isActive`: Boolean
* `createdAt`: Long

### 7. `ChallengeEntity`
* `id`: Long (PK autoGenerate)
* `title`: String
* `description`: String
* `type`: String (`NO_SPENDING` / `SAVE_AMOUNT` / `REDUCE_CATEGORY`)
* `targetAmount`: Double?
* `targetDays`: Int?
* `categoryId`: Long?
* `startDate`: Long
* `endDate`: Long
* `currentProgress`: Double
* `streakDays`: Int
* `isCompleted`: Boolean
* `isActive`: Boolean

---

## 🚀 Phased Build & Execution Plan

```mermaid
gantt
    title UrWallet Phased Implementation Timeline
    dateFormat  YYYY-MM-DD
    section Core Foundation
    Phase 1: Project Setup & Room Database       :p1, 2026-08-25, 3d
    Phase 2: Splash, Onboarding & Smart Wizard   :p2, after p1, 3d
    Phase 3: Main Shell & 4-Tab Navigation       :p3, after p2, 2d
    section Core Transactions
    Phase 4: Transactions Engine & FAB Form      :p4, after p3, 4d
    Phase 5: Dashboard Hub & Proactive Cards     :p5, after p4, 3d
    Phase 6: Search, Filters & Category Dive     :p6, after p5, 2d
    section Financial Coach Features
    Phase 7: Goals & Contribution Engine         :p7, after p6, 3d
    Phase 8: Budgets & Overspending Alerts       :p8, after p7, 2d
    Phase 9: Analytics, Health Score & Habits    :p9, after p8, 3d
    Phase 10: Saving Challenges Gamification     :p10, after p9, 2d
    section Offline Automation & Security
    Phase 11: Recurring Transactions Worker      :p11, after p10, 2d
    Phase 12: Notifications & Reminders Engine   :p12, after p11, 2d
    Phase 13: PIN Lock & Biometrics              :p13, after p12, 2d
    Phase 14: Data Management (Backup/Export)    :p14, after p13, 2d
    Phase 15: Polish, RTL, Skeleton & QA         :p15, after p14, 3d
```

---

### 📦 Phase 1 — Foundation & Data Layer
* [ ] Initialize project with Kotlin / Android SDK (Min 26, Target 34)
* [ ] Configure dependencies (`Room`, `DataStore`, `Navigation`, `WorkManager`, `MPAndroidChart`, `Biometric`)
* [ ] Setup theme tokens in `colors.xml`, `themes.xml`, `dimens.xml`, and Arabic `strings.xml`
* [ ] Create `UrWalletDatabase` with 7 entities, 7 DAOs, and pre-seeded default categories
* [ ] Implement Clean Architecture Repository pattern with `Flow` reactivity

### 📦 Phase 2 — Splash, Onboarding & Smart Savings Wizard
* [ ] Splash screen with brand logo, tagline, and instant local DB warm-up
* [ ] 3-slide Onboarding ViewPager2 with 3D artwork and smooth page indicators
* [ ] **Smart Savings Wizard (Section `الادخار`):** 5-step interactive first goal setup:
  - Step 1: Goal Focus selection (Emergency, Travel, Home, Retirement, Custom)
  - Step 2: Target Amount slider & quick presets
  - Step 3: Pace Plan slider (هادئ / متوازن / مكثف) with live monthly calculation
  - Step 4: Reminder time selector
  - Step 5: Financial habits introduction
* [ ] Save onboarding state to `DataStore` and route to `MainActivity`

### 📦 Phase 3 — Main Shell & 4-Tab Navigation
* [ ] `MainActivity` hosting `BottomNavigationView` + `NavController`
* [ ] Setup 4 primary destinations: `Dashboard`, `Transactions`, `Goals`, `More`
* [ ] Pinned Floating Action Button (`+`) with smooth ripple and sheet launcher
* [ ] RTL layout verification for bottom navigation bar and Arabic typography

### 📦 Phase 4 — Transactions Core & Add Form
* [ ] `AddTransactionBottomSheetFragment`:
  - Toggle `[مصروف | دخل]` with dynamic category switching
  - Big amount input with custom numeric keypad
  - 8-grid category picker with active badge styling
  - Date & Time pickers (Arabic localized)
  - Optional receipt photo picker with local file storage
* [ ] `TransactionsFragment`: Date-grouped `RecyclerView` with income (`+` green) and expense (`-` red)
* [ ] `TransactionDetailFragment`: Receipt preview, metadata table, Edit, Share, and Delete with red confirmation dialog

### 📦 Phase 5 — Dashboard Hub (الرئيسية)
* [ ] Hero Balance Card (Current Balance + Income & Expense pills)
* [ ] **Quick Actions Row:** 4 rapid 1-tap buttons: **[تحويل]**, **[إيداع]**, **[سحب]**, **[دفع]**
* [ ] Smart Insight Banner (Proactive coach tip from local `InsightEngine`)
* [ ] Quick Glance Bento Grid (Today's spend + Budget remaining + Weekly bar chart)
* [ ] Active Daily Challenge Card with progress bar
* [ ] Active Goal Card with percentage ring
* [ ] Recent Transactions (Last 4 entries with category icons)

### 📦 Phase 6 — Search, Filters & Category Deep Dive
* [ ] Toolbar search with instant debounce filtering
* [ ] `FilterBottomSheetFragment` (Type, Category multi-chips, Date range, Amount slider)
* [ ] `CategoryAnalyticsFragment` (Category header, total spent, average transaction, budget status, top merchant list)

### 📦 Phase 7 — Goals & Contribution Engine
* [ ] `GoalsFragment`: Active goals list with radial progress rings and completed goals section
* [ ] `AddGoalFragment`: Goal name, target amount, deadline, pace selector, and `IconPickerGrid`
* [ ] `GoalDetailFragment`: 31% radial ring, target date, monthly required savings, and contribution history
* [ ] `ContributeGoalBottomSheetFragment`: Rapid deposit sheet updating goal progress in real-time

### 📦 Phase 8 — Budgets & Overspending Alerts
* [ ] `BudgetsFragment`: Total monthly consumption gauge (78%) + Category budget cards
* [ ] `AddBudgetBottomSheetFragment`: Category selector and monthly limit input
* [ ] Proactive budget warning badges (Green = Healthy, Yellow = Close to Limit >80%, Red = Exceeded)

### 📦 Phase 9 — Financial Analytics, Health Score & Habits
* [ ] `AnalyticsFragment`: Monthly selector, Spending Trend Bar Chart (MPAndroidChart), Net Savings card
* [ ] `FinancialHealthScore`: 0–100 radial score based on 3 pillars (Planning, Saving, Control)
* [ ] `HabitsFragment`: Financial personality card ("المخطط الذكي 🧠"), peak spending day, 50/30/20 rule breakdown

### 📦 Phase 10 — Saving Challenges (Gamification)
* [ ] `ChallengesFragment`: 7-Day No Spend 🔥, Coffee Savings ☕, 1,000 EGP Monthly 💰
* [ ] `ChallengeDetailFragment`: Days remaining, current progress bar, streak counter, and completion celebration

### 📦 Phase 11 — Recurring Transactions Worker
* [ ] `RecurringFragment`: List of recurring bills & income with next due date badges
* [ ] `AddRecurringBottomSheetFragment`: Frequency selector (Daily, Weekly, Monthly, Yearly)
* [ ] `RecurringTransactionWorker` (WorkManager): Background worker running daily at 00:00 to log due transactions automatically

### 📦 Phase 12 — Notifications & Local Reminders
* [ ] `NotificationChannels` setup (Goals, Budgets, Daily Reminders, Challenges)
* [ ] `DailyReminderReceiver` with `AlarmManager` for scheduled daily logging nudges
* [ ] Budget warning and goal milestone notifications with direct deep-links

### 📦 Phase 13 — App Lock & Security
* [ ] `AppLockFragment`: 4-digit PIN setup + Biometric toggle
* [ ] Custom numeric keypad screen shown on app cold start and resume
* [ ] Encrypted SHA-256 PIN storage in `DataStore` with BiometricPrompt fallback

### 📦 Phase 14 — Data Management & Local Backup
* [ ] `DataManagementFragment` (Dark mode surface)
* [ ] Export transactions to CSV
* [ ] Encrypted full JSON database backup export
* [ ] Import backup file with conflict resolution
* [ ] "حذف جميع البيانات" (Clear All Data) with destructive confirmation dialog

### 📦 Phase 15 — Polish, RTL, Skeleton Loaders & QA
* [ ] Verify 100% RTL alignment (all arrows, padding, alignments)
* [ ] Skeleton shimmering loaders for offline loading states
* [ ] Empty states with 3D illustrations and CTA buttons
* [ ] Success snackbars with haptic feedback
* [ ] End-to-end QA pass across all 10 user journeys (A–J)

---

## 🔍 Verification & Acceptance Criteria

1. **Offline Integrity:** The app runs and executes all operations without any network connection.
2. **Reactivity:** Adding/editing a transaction immediately reflects on the Dashboard, Goals, Budgets, and Analytics without manual refresh.
3. **RTL Consistency:** All back arrows point right (`→`), action chevrons point left (`←`), and numbers align properly with currency symbols (`ج.م`).
4. **Zero Data Loss:** Database migrations and local backups preserve 100% of user data.
