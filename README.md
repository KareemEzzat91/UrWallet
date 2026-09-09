# UrWallet 💳

**Your money, smarter.**

A privacy-first **native Android personal finance application** built with Kotlin and designed around **Feature-First Clean Architecture, MVVM, Room, and an offline-first approach**.

UrWallet helps users track income and expenses, understand spending habits, and build savings goals without depending on a cloud backend for core financial data.

## ✨ Key Features

### 💰 Personal Finance
- 📊 Smart financial dashboard
- 💵 Income and expense tracking
- 🔎 Instant transaction search
- 🎛️ Advanced filtering by type, category, and date
- 📅 Smart transaction date grouping
- 📋 Detailed transaction history

### 📈 Analytics
- 📊 Category-based spending analytics
- 📅 Monthly spending comparison
- ❤️ Financial health score
- 📉 Visual financial insights and charts

### 🎯 Goals & Savings
- Create multiple financial goals
- Track target amounts and deadlines
- Monitor contribution history
- Smart saving pace calculations
- Contribution presets
- Deadline awareness
- Completion tracking

### 🧭 Onboarding & Education
- Guided onboarding experience
- Practical budgeting and saving guidance
- Financial awareness content

### 🔒 Privacy & Offline-First
- Local-first financial data
- Room-powered SQLite persistence
- Reactive database updates with Flow and StateFlow
- No network dependency for core finance workflows

## 🏗️ Architecture

UrWallet follows **Feature-First Clean Architecture + MVVM**:

```text
UI / Fragment
     ↓
ViewModel
     ↓
UseCase
     ↓
Repository
     ↓
DAO
     ↓
Room Database
```

### Engineering Principles

- Single Source of Truth
- Unidirectional Data Flow
- Separation of Concerns
- Dependency Injection with Hilt
- Reactive state with Flow / StateFlow
- Centralized domain calculations

## 🛠️ Tech Stack

| Technology | Purpose |
|---|---|
| **Kotlin** | Native Android development |
| **Android XML + Material 3** | UI |
| **Jetpack Room** | Local persistence |
| **Coroutines** | Asynchronous programming |
| **Flow / StateFlow** | Reactive state |
| **Dagger Hilt** | Dependency injection |
| **Navigation Component** | Navigation |
| **Safe Args** | Type-safe navigation |
| **JUnit 4** | Unit testing |
| **MockK** | Mocking |
| **Truth** | Test assertions |

## 📱 App Preview

<div align="center">

| Dashboard | Transactions |
| :---: | :---: |
| <img src="docs/screenshots/dashboard.jpg" width="340" alt="Dashboard"/> | <img src="docs/screenshots/transactions.jpg" width="340" alt="Transactions"/> |

| Analytics | Goals & Savings |
| :---: | :---: |
| <img src="docs/screenshots/analytics.jpg" width="340" alt="Analytics"/> | <img src="docs/screenshots/goals.jpg" width="340" alt="Goals"/> |

</div>

## 🧪 Testing

The project includes unit tests covering business logic, use cases, repositories, and financial calculations.

```bash
./gradlew testDebugUnitTest
```

> **Current test status:** 62 unit tests passing.

## 🚀 Getting Started

### Requirements

- Android Studio Ladybug (2024.2.1) or newer
- JDK 17+
- Android SDK
- Min SDK 26
- Target SDK 34

### Run

```bash
git clone https://github.com/KareemEzzat91/UrWallet.git
cd UrWallet
./gradlew assembleDebug
```

Open the project in Android Studio and run it on an emulator or physical Android device.

## 📌 Project Highlights

- Offline-first personal finance architecture
- Feature-First Clean Architecture
- MVVM with reactive state
- Room database and local persistence
- Hilt dependency injection
- Advanced transaction filtering and analytics
- Dedicated goals and contribution engine
- Unit-tested business logic
- Privacy-focused design

## 👨‍💻 Author

**Kareem Ezzat** — Flutter & Android Developer

## 📄 License

MIT License
