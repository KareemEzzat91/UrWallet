# UrWallet | محفظتك 💳

<div align="center">

![UrWallet Logo](app/src/main/res/drawable/ic_launcher_foreground.xml)

### **"محفظتك، بس أذكى"**
**تطبيق إدارة مالية شخصية ذكي، عربي بالكامل، ويعمل بدون إنترنت بنسبة 100% (Offline-First).**

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.24-7F52FF.svg?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Android](https://img.shields.io/badge/Platform-Android%20Native-3DDC84.svg?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com)
[![Architecture](https://img.shields.io/badge/Architecture-Clean%20%2B%20MVVM-000666.svg?style=for-the-badge)](https://developer.android.com/topic/architecture)
[![Hilt](https://img.shields.io/badge/DI-Hilt%20Dagger-2C5E8A.svg?style=for-the-badge)](https://dagger.dev/hilt/)
[![Room](https://img.shields.io/badge/Storage-Room%20(Offline--First)-4285F4.svg?style=for-the-badge)](https://developer.android.com/training/data-storage/room)
[![Tests](https://img.shields.io/badge/Unit%20Tests-62%20Passing-00732C.svg?style=for-the-badge)](https://junit.org)

</div>

---

## 📱 نظرة عامة على الشاشات (App Screens)

<div align="center">

| الرئيسية (Dashboard) | المعاملات والبحث (Transactions) |
| :---: | :---: |
| <img src="docs/screenshots/dashboard.jpg" width="340" alt="Dashboard Screen"/> | <img src="docs/screenshots/transactions.jpg" width="340" alt="Transactions Screen"/> |

| تحليل الفئات (Analytics) | الأهداف المالية (Goals & Savings) |
| :---: | :---: |
| <img src="docs/screenshots/analytics.jpg" width="340" alt="Analytics Screen"/> | <img src="docs/screenshots/goals.jpg" width="340" alt="Goals Screen"/> |

</div>

---

## ✨ المميزات الرئيسية (Core Features)

### 1. 🛡️ خصوصية تامة وبدون إنترنت (100% Offline-First)
* بياناتك المالية ملكك وحدك ولا تُرفع على أي خوادم سحابية.
* قاعدة بيانات محلية فائقة السرعة مدعومة بـ **Room Database** و **Coroutines Flow**.

### 2. 📊 لوحة التحكم الذكية (Smart Dashboard)
* **كارت الرصيد الصافي (Hero Balance Card):** متابعة فورية للرصيد الحالي، إجمالي الدخل الشهري، وإجمالي المصروفات.
* **مؤشر الصحة المالية (Financial Health Score):** قراءة سريعة لنسبة استهلاك الميزانية الشهرية.
* **قائمة أحدث المعاملات:** وصول فوري لمعاملاتك الأخيرة مع تصنيفات ملونة.

### 3. 🔍 البحث المتقدم والفلترة (Search & Filters)
* بحث حي وفوري بالاسم أو الملاحظات.
* تصنيف بحسب نوع المعاملة (دخل / مصروف).
* فلترة حسب الفئة (طعام، تسوق، فواتير، مواصلات، راتب، أعمال حرة...).
* فلترة زمنية ذكية (اليوم، هذا الأسبوع، هذا الشهر، أو مخصص).
* تجميع زمني تلقائي للمعاملات (اليوم، أمس، التواريخ السابقة).

### 4. 📈 تحليلات الفئات ومراقبة الميزانية (Category Analytics)
* تحليل تفصيلي لكل فئة يشمل: إجمالي الإنفاق، عدد المعاملات، متوسط الصرف لكل معاملة، وأعلى معاملة مسجلة.
* مقارنة الإنفاق بالشهر السابق لمعرفة مدى وفرتك أو تجاوزك.
* شاشات تفاصيل المعاملة مع إمكانية المشاركة (Share Transaction Details).

### 5. 🎯 محرك الأهداف المالية والادخار (Goals & Contribution Engine)
* **كارت الهيرو التجميعي (Overview Banner):** متابعة فورية لإجمالي مدخراتك عبر كافة الأهداف مع شريط تقدم إجمالي.
* **وتيرة الادخار الذكية (Pace Modes):** حساب الادخار الشهري المطلوب بناءً على الوتيرة (مريح، متوازن، سريع).
* **تنبيهات المواعيد الحرجة:** بادج تحذيري ديناميكي للأهداف التي اقترب موعد وصولها (15 يوماً أو أقل).
* **إيداع مباشر ومرن:** دعم الإيداع السريع ومبالغ الـ Presets الجاهزة (+100، +250، +500، +1000 ج.م).
* **سجل إيداعات تفصيلي:** تتبع تاريخ ووقت وملاحظة كل إيداع.
* **احتفال باكتمال الأهداف:** كروت إنجاز خاصة عند تحقيق 100% من الهدف.

### 6. 🚀 إعداد ذكي وعادات مالية (Smart Wizard & Onboarding)
* خطوات تعريفية ترشدك لقواعد الادخار الذهبية مثل قاعدة **50/30/20** وتتبع المصاريف الصغيرة.

---

## 🏗️ المعمارية البرمجية (Architecture & Engineering Standards)

تم بناء التطبيق باتباع معمارية **Feature-First Clean Architecture** وفقاً لأعلى معايير هندسة البرمجيات:

```
lib/ or app/src/main/java/com/example/urwallet/
│
├── core/
│   ├── common/              # DateUtils, Formatters, Enums, State Models
│   ├── database/            # Room Database, Type Converters, Migrations
│   └── designsystem/        # CategoryIconMapper, Theme, Colors, Dimens
│
└── features/
    ├── dashboard/           # Dashboard presentation, views, and viewmodels
    ├── transactions/        # Transaction entities, DAOs, use cases, filters, UI
    ├── analytics/           # Category analytics domain models, use cases, UI
    ├── goals/               # Goals domain models, calculators, use cases, adapters, UI
    │   ├── data/
    │   │   ├── datasources/local/ (GoalDao, GoalContributionDao)
    │   │   └── repositories/      (GoalRepositoryImpl)
    │   ├── domain/
    │   │   ├── calculator/        (GoalCalculator)
    │   │   ├── model/             (Goal, GoalSummary, GoalDetail, Contribution)
    │   │   ├── repository/        (GoalRepository Interface)
    │   │   └── usecase/           (GetGoals, AddGoal, Contribute, Delete...)
    │   └── presentation/
    │       ├── adapter/           (ActiveGoals, CompletedGoals, Contributions)
    │       ├── add/               (AddGoalBottomSheetFragment)
    │       ├── contribute/        (ContributeGoalBottomSheetFragment)
    │       ├── detail/            (GoalDetailFragment)
    │       └── GoalsFragment.kt & GoalsViewModel.kt
    └── onboarding/          # Wizard steps, tips, and financial habit engine
```

### القواعد البرمجية الصارمة المتبعة (Core Rules):
* **Single Source of Truth:** نسبة اكتمال الأهداف والمبالغ المدخرة مشتقة برمجياً من جدول سجل الإيداعات (`GoalContributionEntity`) لضمان تكامل البيانات بنسبة 100%.
* **Reactive Data Flow:** تدفق أحادي الاتجاه عبر `Room Flow` -> `Repository` -> `UseCase` -> `ViewModel (StateFlow)` -> `Fragment`.
* **Clean Separation:** طبقة الـ UI تتعامل فقط مع الـ `StateFlow` ولا تحتوي على أي منطق تجاري (Business Logic).
* **Dependency Injection:** حقن التبعيات بالكامل عبر **Dagger Hilt** مع فصل الموديولات (`@InstallIn(SingletonComponent::class)`).

---

## 🛠️ حزمة التقنيات (Tech Stack)

* **لغة البرمجة:** Kotlin (1.9.24)
* **واجهات المستخدم:** Native Android XML + Material Design 3 (M3) + ViewBinding & DataBinding
* **حقن التبعيات:** Hilt / Dagger
* **قاعدة البيانات:** Android Jetpack Room (SQLite)
* **البرمجة التفاعلية والتزامنية:** Kotlin Coroutines & Reactive Flow / StateFlow
* **إدارة التنقل:** Android Jetpack Navigation Component مع SafeArgs
* **الاختبارات الأحادية:** JUnit 4, MockK, Truth

---

## 🧪 الاختبارات والجودة (Testing & Verification)

المشروع مغطى باختبارات أحادية شاملة لكل من:
* محرك حسابات الأهداف (`GoalCalculatorTest`)
* كافة الـ UseCases (حالات الاستخدام)
* عمليات الـ Repository ومطابقة النتائج الحسابية

لتشغيل الاختبارات الأحادية:
```bash
./gradlew testDebugUnitTest
```
> **النتيجة:** `BUILD SUCCESSFUL` (جميع الاختبارات الـ 62 ناجحة بنسبة 100%).

---

## 🚀 البدء والتشغيل (Getting Started)

### المتطلبات الأساسية (Prerequisites):
* **Android Studio:** Ladybug (2024.2.1) أو أحدث.
* **JDK:** Java 17 أو أحدث.
* **Android SDK:** Min SDK 26 (Android 8.0) | Target SDK 34 (Android 14).

### خطوات التثبيت:
1. استنسخ المستودع (Clone):
   ```bash
   git clone https://github.com/KareemEzzat91/UrWallet.git
   cd UrWallet
   ```
2. بناء المشروع (Build):
   ```bash
   ./gradlew assembleDebug
   ```
3. تشغيل التطبيق على محاكي أو جهاز فعلي من خلال Android Studio.

---

## 📄 الترخيص (License)

هذا المشروع متاح تحت رخصة [MIT License](LICENSE).

<div align="center">
صُنِع بكل ❤️ لدعم الإدارة المالية الذكية في الوطن العربي.
</div>
