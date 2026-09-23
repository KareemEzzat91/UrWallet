package com.example.urwallet.features.backup.data.parser

import com.example.urwallet.core.common.CategoryType
import com.example.urwallet.core.common.ChallengeType
import com.example.urwallet.core.common.Frequency
import com.example.urwallet.core.common.GoalPaceMode
import com.example.urwallet.core.common.TransactionType
import com.example.urwallet.features.backup.data.dto.BackupDataDto
import com.example.urwallet.features.backup.data.dto.BackupPayloadDto
import com.example.urwallet.features.backup.data.dto.BudgetBackupDto
import com.example.urwallet.features.backup.data.dto.CategoryBackupDto
import com.example.urwallet.features.backup.data.dto.ChallengeBackupDto
import com.example.urwallet.features.backup.data.dto.FinancialObligationBackupDto
import com.example.urwallet.features.backup.data.dto.GoalBackupDto
import com.example.urwallet.features.backup.data.dto.GoalContributionBackupDto
import com.example.urwallet.features.backup.data.dto.ObligationSettlementBackupDto
import com.example.urwallet.features.backup.data.dto.PersonBackupDto
import com.example.urwallet.features.backup.data.dto.RecurringTransactionBackupDto
import com.example.urwallet.features.backup.data.dto.TransactionBackupDto
import com.example.urwallet.features.people.domain.model.ObligationDirection
import com.example.urwallet.features.people.domain.model.ObligationStatus
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

sealed class BackupParseException(message: String) : Exception(message) {
    class MalformedJson(message: String, cause: Throwable? = null) : BackupParseException(message)
    class UnsupportedVersion(val version: Int) : BackupParseException("إصدار النسخة الاحتياطية ($version) غير مدعوم.")
    class InvalidData(message: String) : BackupParseException(message)
    class InternalReferenceError(message: String) : BackupParseException(message)
}

class BackupJsonParser {

    fun serialize(payload: BackupPayloadDto): String {
        val root = JSONObject()
        root.put("version", payload.version)
        root.put("exportedAt", payload.exportedAt)
        root.put("appName", payload.appName)
        root.put("appVersion", payload.appVersion)

        val dataObj = JSONObject()

        // Categories
        val catArray = JSONArray()
        payload.data.categories.forEach { cat ->
            val obj = JSONObject()
            obj.put("id", cat.id)
            obj.put("name", cat.name)
            obj.put("type", cat.type.name)
            obj.put("icon", cat.icon)
            obj.put("color", cat.color)
            obj.put("isDefault", cat.isDefault)
            obj.put("isDeleted", cat.isDeleted)
            catArray.put(obj)
        }
        dataObj.put("categories", catArray)

        // Goals
        val goalArray = JSONArray()
        payload.data.goals.forEach { g ->
            val obj = JSONObject()
            obj.put("id", g.id)
            obj.put("name", g.name)
            obj.put("icon", g.icon)
            obj.put("targetAmount", g.targetAmount)
            obj.put("paceMode", g.paceMode.name)
            obj.put("monthlyTarget", g.monthlyTarget)
            obj.put("deadline", g.deadline)
            obj.put("createdAt", g.createdAt)
            obj.put("isDeleted", g.isDeleted)
            goalArray.put(obj)
        }
        dataObj.put("goals", goalArray)

        // Goal Contributions
        val contribArray = JSONArray()
        payload.data.goalContributions.forEach { c ->
            val obj = JSONObject()
            obj.put("id", c.id)
            obj.put("goalId", c.goalId)
            obj.put("amount", c.amount)
            if (c.note != null) obj.put("note", c.note) else obj.put("note", JSONObject.NULL)
            obj.put("date", c.date)
            contribArray.put(obj)
        }
        dataObj.put("goalContributions", contribArray)

        // Budgets
        val budgetArray = JSONArray()
        payload.data.budgets.forEach { b ->
            val obj = JSONObject()
            obj.put("id", b.id)
            if (b.categoryId != null) obj.put("categoryId", b.categoryId) else obj.put("categoryId", JSONObject.NULL)
            obj.put("amount", b.amount)
            obj.put("month", b.month)
            obj.put("year", b.year)
            obj.put("alertThreshold", b.alertThreshold)
            obj.put("createdAt", b.createdAt)
            budgetArray.put(obj)
        }
        dataObj.put("budgets", budgetArray)

        // Recurring Transactions
        val recArray = JSONArray()
        payload.data.recurringTransactions.forEach { r ->
            val obj = JSONObject()
            obj.put("id", r.id)
            obj.put("title", r.title)
            obj.put("amount", r.amount)
            obj.put("type", r.type.name)
            obj.put("categoryId", r.categoryId)
            obj.put("frequency", r.frequency.name)
            obj.put("startDate", r.startDate)
            if (r.endDate != null) obj.put("endDate", r.endDate) else obj.put("endDate", JSONObject.NULL)
            obj.put("nextOccurrence", r.nextOccurrence)
            obj.put("isActive", r.isActive)
            obj.put("createdAt", r.createdAt)
            recArray.put(obj)
        }
        dataObj.put("recurringTransactions", recArray)

        // Transactions
        val txArray = JSONArray()
        payload.data.transactions.forEach { t ->
            val obj = JSONObject()
            obj.put("id", t.id)
            obj.put("amount", t.amount)
            obj.put("type", t.type.name)
            obj.put("categoryId", t.categoryId)
            obj.put("title", t.title)
            if (t.note != null) obj.put("note", t.note) else obj.put("note", JSONObject.NULL)
            obj.put("date", t.date)
            if (t.receiptPath != null) obj.put("receiptPath", t.receiptPath) else obj.put("receiptPath", JSONObject.NULL)
            if (t.personId != null) obj.put("personId", t.personId) else obj.put("personId", JSONObject.NULL)
            obj.put("createdAt", t.createdAt)
            obj.put("updatedAt", t.updatedAt)
            txArray.put(obj)
        }
        dataObj.put("transactions", txArray)

        // Challenges
        val chArray = JSONArray()
        payload.data.challenges.forEach { ch ->
            val obj = JSONObject()
            obj.put("id", ch.id)
            obj.put("title", ch.title)
            obj.put("description", ch.description)
            obj.put("type", ch.type.name)
            if (ch.targetAmount != null) obj.put("targetAmount", ch.targetAmount) else obj.put("targetAmount", JSONObject.NULL)
            if (ch.targetDays != null) obj.put("targetDays", ch.targetDays) else obj.put("targetDays", JSONObject.NULL)
            if (ch.categoryId != null) obj.put("categoryId", ch.categoryId) else obj.put("categoryId", JSONObject.NULL)
            obj.put("startDate", ch.startDate)
            obj.put("endDate", ch.endDate)
            obj.put("currentProgress", ch.currentProgress)
            obj.put("streakDays", ch.streakDays)
            obj.put("isCompleted", ch.isCompleted)
            obj.put("isActive", ch.isActive)
            chArray.put(obj)
        }
        dataObj.put("challenges", chArray)

        // People
        val peopleArray = JSONArray()
        payload.data.people.forEach { p ->
            val obj = JSONObject()
            obj.put("id", p.id)
            obj.put("name", p.name)
            if (p.phoneNumber != null) obj.put("phoneNumber", p.phoneNumber) else obj.put("phoneNumber", JSONObject.NULL)
            if (p.notes != null) obj.put("notes", p.notes) else obj.put("notes", JSONObject.NULL)
            obj.put("createdAt", p.createdAt)
            obj.put("updatedAt", p.updatedAt)
            peopleArray.put(obj)
        }
        dataObj.put("people", peopleArray)

        // Obligations
        val obArray = JSONArray()
        payload.data.obligations.forEach { o ->
            val obj = JSONObject()
            obj.put("id", o.id)
            obj.put("personId", o.personId)
            obj.put("amount", o.amount)
            obj.put("settledAmount", o.settledAmount)
            obj.put("remainingAmount", o.remainingAmount)
            obj.put("direction", o.direction.name)
            obj.put("status", o.status.name)
            if (o.reason != null) obj.put("reason", o.reason) else obj.put("reason", JSONObject.NULL)
            if (o.dueDate != null) obj.put("dueDate", o.dueDate) else obj.put("dueDate", JSONObject.NULL)
            if (o.relatedTransactionId != null) obj.put("relatedTransactionId", o.relatedTransactionId) else obj.put("relatedTransactionId", JSONObject.NULL)
            obj.put("createdAt", o.createdAt)
            obj.put("updatedAt", o.updatedAt)
            obArray.put(obj)
        }
        dataObj.put("obligations", obArray)

        // Obligation Settlements
        val setArray = JSONArray()
        payload.data.obligationSettlements.forEach { s ->
            val obj = JSONObject()
            obj.put("id", s.id)
            obj.put("obligationId", s.obligationId)
            obj.put("amount", s.amount)
            obj.put("date", s.date)
            if (s.note != null) obj.put("note", s.note) else obj.put("note", JSONObject.NULL)
            if (s.relatedTransactionId != null) obj.put("relatedTransactionId", s.relatedTransactionId) else obj.put("relatedTransactionId", JSONObject.NULL)
            obj.put("createdAt", s.createdAt)
            setArray.put(obj)
        }
        dataObj.put("obligationSettlements", setArray)

        root.put("data", dataObj)
        return root.toString(2)
    }

    fun parseAndValidate(jsonString: String): BackupPayloadDto {
        val root = try {
            JSONObject(jsonString)
        } catch (e: JSONException) {
            throw BackupParseException.MalformedJson("صيغة الملف غير صالحة (JSON غير صحيح): ${e.message}", e)
        }

        if (!root.has("version")) {
            throw BackupParseException.InvalidData("حقل الإصدار (version) مفقود في ملف النسخة الاحتياطية.")
        }
        val version = root.optInt("version", -1)
        if (version != BackupPayloadDto.CURRENT_BACKUP_VERSION) {
            throw BackupParseException.UnsupportedVersion(version)
        }

        if (!root.has("data")) {
            throw BackupParseException.InvalidData("كائن البيانات (data) مفقود في ملف النسخة الاحتياطية.")
        }
        val dataObj = root.getJSONObject("data")

        val exportedAt = root.optLong("exportedAt", System.currentTimeMillis())
        val appName = root.optString("appName", "UrWallet")
        val appVersion = root.optString("appVersion", "1.0")

        // Parse Categories
        val categories = mutableListOf<CategoryBackupDto>()
        val catArray = dataObj.optJSONArray("categories") ?: JSONArray()
        for (i in 0 until catArray.length()) {
            val obj = catArray.getJSONObject(i)
            val typeStr = obj.optString("type")
            val type = try {
                CategoryType.valueOf(typeStr)
            } catch (e: IllegalArgumentException) {
                throw BackupParseException.InvalidData("نوع تصنيف غير صالح: $typeStr")
            }
            categories.add(
                CategoryBackupDto(
                    id = obj.optLong("id", 0),
                    name = obj.getString("name"),
                    type = type,
                    icon = obj.optString("icon", "ic_other"),
                    color = obj.optString("color", "#78909C"),
                    isDefault = obj.optBoolean("isDefault", false),
                    isDeleted = obj.optBoolean("isDeleted", false)
                )
            )
        }

        // Parse Goals
        val goals = mutableListOf<GoalBackupDto>()
        val goalArray = dataObj.optJSONArray("goals") ?: JSONArray()
        for (i in 0 until goalArray.length()) {
            val obj = goalArray.getJSONObject(i)
            val paceStr = obj.optString("paceMode")
            val paceMode = try {
                GoalPaceMode.valueOf(paceStr)
            } catch (e: IllegalArgumentException) {
                throw BackupParseException.InvalidData("نمط هدف غير صالح: $paceStr")
            }
            goals.add(
                GoalBackupDto(
                    id = obj.optLong("id", 0),
                    name = obj.getString("name"),
                    icon = obj.optString("icon", "ic_savings"),
                    targetAmount = obj.getDouble("targetAmount"),
                    paceMode = paceMode,
                    monthlyTarget = obj.optDouble("monthlyTarget", 0.0),
                    deadline = obj.getLong("deadline"),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                    isDeleted = obj.optBoolean("isDeleted", false)
                )
            )
        }

        // Parse Goal Contributions
        val contributions = mutableListOf<GoalContributionBackupDto>()
        val contribArray = dataObj.optJSONArray("goalContributions") ?: JSONArray()
        for (i in 0 until contribArray.length()) {
            val obj = contribArray.getJSONObject(i)
            contributions.add(
                GoalContributionBackupDto(
                    id = obj.optLong("id", 0),
                    goalId = obj.getLong("goalId"),
                    amount = obj.getDouble("amount"),
                    note = if (obj.isNull("note")) null else obj.optString("note"),
                    date = obj.optLong("date", System.currentTimeMillis())
                )
            )
        }

        // Parse Budgets
        val budgets = mutableListOf<BudgetBackupDto>()
        val budgetArray = dataObj.optJSONArray("budgets") ?: JSONArray()
        for (i in 0 until budgetArray.length()) {
            val obj = budgetArray.getJSONObject(i)
            val catId = if (obj.isNull("categoryId")) null else obj.optLong("categoryId")
            budgets.add(
                BudgetBackupDto(
                    id = obj.optLong("id", 0),
                    categoryId = catId,
                    amount = obj.getDouble("amount"),
                    month = obj.getInt("month"),
                    year = obj.getInt("year"),
                    alertThreshold = obj.optDouble("alertThreshold", 0.80),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                )
            )
        }

        // Parse Recurring Transactions
        val recurring = mutableListOf<RecurringTransactionBackupDto>()
        val recArray = dataObj.optJSONArray("recurringTransactions") ?: JSONArray()
        for (i in 0 until recArray.length()) {
            val obj = recArray.getJSONObject(i)
            val typeStr = obj.optString("type")
            val type = try {
                TransactionType.valueOf(typeStr)
            } catch (e: IllegalArgumentException) {
                throw BackupParseException.InvalidData("نوع معاملة دورية غير صالح: $typeStr")
            }
            val freqStr = obj.optString("frequency")
            val freq = try {
                Frequency.valueOf(freqStr)
            } catch (e: IllegalArgumentException) {
                throw BackupParseException.InvalidData("تكرار معاملة غير صالح: $freqStr")
            }
            recurring.add(
                RecurringTransactionBackupDto(
                    id = obj.optLong("id", 0),
                    title = obj.getString("title"),
                    amount = obj.getDouble("amount"),
                    type = type,
                    categoryId = obj.getLong("categoryId"),
                    frequency = freq,
                    startDate = obj.getLong("startDate"),
                    endDate = if (obj.isNull("endDate")) null else obj.optLong("endDate"),
                    nextOccurrence = obj.getLong("nextOccurrence"),
                    isActive = obj.optBoolean("isActive", true),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                )
            )
        }

        // Parse Transactions
        val transactions = mutableListOf<TransactionBackupDto>()
        val txArray = dataObj.optJSONArray("transactions") ?: JSONArray()
        for (i in 0 until txArray.length()) {
            val obj = txArray.getJSONObject(i)
            val typeStr = obj.optString("type")
            val type = try {
                TransactionType.valueOf(typeStr)
            } catch (e: IllegalArgumentException) {
                throw BackupParseException.InvalidData("نوع معاملة غير صالح: $typeStr")
            }
            transactions.add(
                TransactionBackupDto(
                    id = obj.optLong("id", 0),
                    amount = obj.getDouble("amount"),
                    type = type,
                    categoryId = obj.getLong("categoryId"),
                    title = obj.getString("title"),
                    note = if (obj.isNull("note")) null else obj.optString("note"),
                    date = obj.getLong("date"),
                    receiptPath = if (obj.isNull("receiptPath")) null else obj.optString("receiptPath"),
                    personId = if (obj.isNull("personId")) null else obj.optLong("personId"),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                    updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                )
            )
        }

        // Parse Challenges
        val challenges = mutableListOf<ChallengeBackupDto>()
        val chArray = dataObj.optJSONArray("challenges") ?: JSONArray()
        for (i in 0 until chArray.length()) {
            val obj = chArray.getJSONObject(i)
            val typeStr = obj.optString("type")
            val type = try {
                ChallengeType.valueOf(typeStr)
            } catch (e: IllegalArgumentException) {
                throw BackupParseException.InvalidData("نوع تحدي غير صالح: $typeStr")
            }
            challenges.add(
                ChallengeBackupDto(
                    id = obj.optLong("id", 0),
                    title = obj.getString("title"),
                    description = obj.optString("description", ""),
                    type = type,
                    targetAmount = if (obj.isNull("targetAmount")) null else obj.optDouble("targetAmount"),
                    targetDays = if (obj.isNull("targetDays")) null else obj.optInt("targetDays"),
                    categoryId = if (obj.isNull("categoryId")) null else obj.optLong("categoryId"),
                    startDate = obj.getLong("startDate"),
                    endDate = obj.getLong("endDate"),
                    currentProgress = obj.optDouble("currentProgress", 0.0),
                    streakDays = obj.optInt("streakDays", 0),
                    isCompleted = obj.optBoolean("isCompleted", false),
                    isActive = obj.optBoolean("isActive", true)
                )
            )
        }

        // Parse People
        val people = mutableListOf<PersonBackupDto>()
        val peopleArray = dataObj.optJSONArray("people") ?: JSONArray()
        for (i in 0 until peopleArray.length()) {
            val obj = peopleArray.getJSONObject(i)
            val createdAt = obj.optLong("createdAt", System.currentTimeMillis())
            people.add(
                PersonBackupDto(
                    id = obj.optLong("id", 0),
                    name = obj.getString("name"),
                    phoneNumber = if (obj.isNull("phoneNumber")) null else obj.optString("phoneNumber"),
                    notes = if (obj.isNull("notes")) null else obj.optString("notes"),
                    createdAt = createdAt,
                    updatedAt = obj.optLong("updatedAt", createdAt)
                )
            )
        }

        // Parse Obligations
        val obligations = mutableListOf<FinancialObligationBackupDto>()
        val obArray = dataObj.optJSONArray("obligations") ?: JSONArray()
        for (i in 0 until obArray.length()) {
            val obj = obArray.getJSONObject(i)
            val dirStr = obj.optString("direction")
            val direction = try {
                ObligationDirection.valueOf(dirStr)
            } catch (e: IllegalArgumentException) {
                throw BackupParseException.InvalidData("اتجاه التزام مالي غير صالح: $dirStr")
            }
            val statusStr = obj.optString("status")
            val status = try {
                ObligationStatus.valueOf(statusStr)
            } catch (e: IllegalArgumentException) {
                throw BackupParseException.InvalidData("حالة التزام مالي غير صالحة: $statusStr")
            }
            val createdAt = obj.optLong("createdAt", System.currentTimeMillis())
            obligations.add(
                FinancialObligationBackupDto(
                    id = obj.optLong("id", 0),
                    personId = obj.getLong("personId"),
                    amount = obj.getDouble("amount"),
                    direction = direction,
                    status = status,
                    settledAmount = obj.optDouble("settledAmount", 0.0),
                    remainingAmount = obj.getDouble("remainingAmount"),
                    reason = if (obj.isNull("reason")) null else obj.optString("reason"),
                    dueDate = if (obj.isNull("dueDate")) null else obj.optLong("dueDate"),
                    relatedTransactionId = if (obj.isNull("relatedTransactionId")) null else obj.optLong("relatedTransactionId"),
                    createdAt = createdAt,
                    updatedAt = obj.optLong("updatedAt", createdAt)
                )
            )
        }

        // Parse Obligation Settlements
        val settlements = mutableListOf<ObligationSettlementBackupDto>()
        val setArray = dataObj.optJSONArray("obligationSettlements") ?: JSONArray()
        for (i in 0 until setArray.length()) {
            val obj = setArray.getJSONObject(i)
            val date = obj.optLong("date", System.currentTimeMillis())
            settlements.add(
                ObligationSettlementBackupDto(
                    id = obj.optLong("id", 0),
                    obligationId = obj.getLong("obligationId"),
                    amount = obj.getDouble("amount"),
                    date = date,
                    note = if (obj.isNull("note")) null else obj.optString("note"),
                    relatedTransactionId = if (obj.isNull("relatedTransactionId")) null else obj.optLong("relatedTransactionId"),
                    createdAt = obj.optLong("createdAt", date)
                )
            )
        }

        // Validate internal relationships within the backup itself
        val categoryIds = categories.map { it.id }.toSet()
        val goalIds = goals.map { it.id }.toSet()
        val personIds = people.map { it.id }.toSet()
        val obligationIds = obligations.map { it.id }.toSet()

        for (tx in transactions) {
            if (!categoryIds.contains(tx.categoryId)) {
                throw BackupParseException.InternalReferenceError(
                    "المعاملة '${tx.title}' تشير إلى تصنيف غير موجود في ملف النسخة الاحتياطية (المعرف: ${tx.categoryId})."
                )
            }
        }

        for (rec in recurring) {
            if (!categoryIds.contains(rec.categoryId)) {
                throw BackupParseException.InternalReferenceError(
                    "المعاملة الدورية '${rec.title}' تشير إلى تصنيف غير موجود في ملف النسخة الاحتياطية (المعرف: ${rec.categoryId})."
                )
            }
        }

        for (b in budgets) {
            if (b.categoryId != null && !categoryIds.contains(b.categoryId)) {
                throw BackupParseException.InternalReferenceError(
                    "الميزانية تشير إلى تصنيف غير موجود في ملف النسخة الاحتياطية (المعرف: ${b.categoryId})."
                )
            }
        }

        for (c in contributions) {
            if (!goalIds.contains(c.goalId)) {
                throw BackupParseException.InternalReferenceError(
                    "المساهمة تشير إلى هدف غير موجود في ملف النسخة الاحتياطية (معرف الهدف: ${c.goalId})."
                )
            }
        }

        for (ob in obligations) {
            if (!personIds.contains(ob.personId)) {
                throw BackupParseException.InternalReferenceError(
                    "الالتزام المالي '${ob.reason ?: "بدون سبب"}' يشير إلى شخص غير موجود في ملف النسخة الاحتياطية (المعرف: ${ob.personId})."
                )
            }
        }

        for (s in settlements) {
            if (!obligationIds.contains(s.obligationId)) {
                throw BackupParseException.InternalReferenceError(
                    "تسوية الالتزام تشير إلى التزام مالي غير موجود في ملف النسخة الاحتياطية (المعرف: ${s.obligationId})."
                )
            }
        }

        return BackupPayloadDto(
            version = version,
            exportedAt = exportedAt,
            appName = appName,
            appVersion = appVersion,
            data = BackupDataDto(
                categories = categories,
                goals = goals,
                goalContributions = contributions,
                budgets = budgets,
                recurringTransactions = recurring,
                transactions = transactions,
                challenges = challenges,
                people = people,
                obligations = obligations,
                obligationSettlements = settlements
            )
        )
    }
}

