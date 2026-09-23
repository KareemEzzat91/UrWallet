package com.example.urwallet.features.backup.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.example.urwallet.core.database.DatabaseCallback
import com.example.urwallet.core.database.UrWalletDatabase
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
import com.example.urwallet.features.backup.data.parser.BackupJsonParser
import com.example.urwallet.features.backup.data.parser.BackupParseException
import com.example.urwallet.features.backup.data.parser.CsvExporter
import com.example.urwallet.features.backup.domain.model.BackupSummary
import com.example.urwallet.features.backup.domain.model.BackupValidationResult
import com.example.urwallet.features.backup.domain.model.ImportStrategy
import com.example.urwallet.features.backup.domain.repository.BackupRepository
import com.example.urwallet.features.budgets.data.dao.BudgetDao
import com.example.urwallet.features.budgets.data.entity.BudgetEntity
import com.example.urwallet.features.challenges.data.dao.ChallengeDao
import com.example.urwallet.features.challenges.data.entity.ChallengeEntity
import com.example.urwallet.features.goals.data.dao.GoalContributionDao
import com.example.urwallet.features.goals.data.dao.GoalDao
import com.example.urwallet.features.goals.data.entity.GoalContributionEntity
import com.example.urwallet.features.goals.data.entity.GoalEntity
import com.example.urwallet.features.more.data.dao.RecurringTransactionDao
import com.example.urwallet.features.more.data.entity.RecurringTransactionEntity
import com.example.urwallet.features.people.data.dao.FinancialObligationDao
import com.example.urwallet.features.people.data.dao.PersonDao
import com.example.urwallet.features.people.data.entity.FinancialObligationEntity
import com.example.urwallet.features.people.data.entity.ObligationSettlementEntity
import com.example.urwallet.features.people.data.entity.PersonEntity
import com.example.urwallet.features.transactions.data.dao.CategoryDao
import com.example.urwallet.features.transactions.data.dao.TransactionDao
import com.example.urwallet.features.transactions.data.entity.CategoryEntity
import com.example.urwallet.features.transactions.data.entity.TransactionEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class BackupRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val urWalletDatabase: UrWalletDatabase,
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao,
    private val goalDao: GoalDao,
    private val goalContributionDao: GoalContributionDao,
    private val budgetDao: BudgetDao,
    private val recurringTransactionDao: RecurringTransactionDao,
    private val challengeDao: ChallengeDao,
    private val personDao: PersonDao,
    private val obligationDao: FinancialObligationDao,
    private val backupJsonParser: BackupJsonParser,
    private val csvExporter: CsvExporter
) : BackupRepository {

    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    private fun getBackupsDir(): File {
        val dir = File(context.cacheDir, "backups")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    override suspend fun exportJsonBackup(): Result<File> = withContext(ioDispatcher) {
        try {
            val categories = categoryDao.getAllCategoriesSync().map {
                CategoryBackupDto(
                    id = it.id,
                    name = it.name,
                    type = it.type,
                    icon = it.icon,
                    color = it.color,
                    isDefault = it.isDefault,
                    isDeleted = it.isDeleted
                )
            }
            val goals = goalDao.getAllGoalsSync().map {
                GoalBackupDto(
                    id = it.id,
                    name = it.name,
                    icon = it.icon,
                    targetAmount = it.targetAmount,
                    paceMode = it.paceMode,
                    monthlyTarget = it.monthlyTarget,
                    deadline = it.deadline,
                    createdAt = it.createdAt,
                    isDeleted = it.isDeleted
                )
            }
            val contributions = goalContributionDao.getAllContributionsSync().map {
                GoalContributionBackupDto(
                    id = it.id,
                    goalId = it.goalId,
                    amount = it.amount,
                    note = it.note,
                    date = it.date
                )
            }
            val budgets = budgetDao.getAllBudgetsSync().map {
                BudgetBackupDto(
                    id = it.id,
                    categoryId = it.categoryId,
                    amount = it.amount,
                    month = it.month,
                    year = it.year,
                    alertThreshold = it.alertThreshold,
                    createdAt = it.createdAt
                )
            }
            val recurring = recurringTransactionDao.getAllRecurringTransactionsSync().map {
                RecurringTransactionBackupDto(
                    id = it.id,
                    title = it.title,
                    amount = it.amount,
                    type = it.type,
                    categoryId = it.categoryId,
                    frequency = it.frequency,
                    startDate = it.startDate,
                    endDate = it.endDate,
                    nextOccurrence = it.nextOccurrence,
                    isActive = it.isActive,
                    createdAt = it.createdAt
                )
            }
            val transactions = transactionDao.getAllTransactionsSync().map {
                TransactionBackupDto(
                    id = it.id,
                    amount = it.amount,
                    type = it.type,
                    categoryId = it.categoryId,
                    title = it.title,
                    note = it.note,
                    date = it.date,
                    receiptPath = it.receiptPath,
                    personId = it.personId,
                    createdAt = it.createdAt,
                    updatedAt = it.updatedAt
                )
            }
            val challenges = challengeDao.getAllChallengesSync().map {
                ChallengeBackupDto(
                    id = it.id,
                    title = it.title,
                    description = it.description,
                    type = it.type,
                    targetAmount = it.targetAmount,
                    targetDays = it.targetDays,
                    categoryId = it.categoryId,
                    startDate = it.startDate,
                    endDate = it.endDate,
                    currentProgress = it.currentProgress,
                    streakDays = it.streakDays,
                    isCompleted = it.isCompleted,
                    isActive = it.isActive
                )
            }
            val people = personDao.getAllPeopleSync().map {
                PersonBackupDto(
                    id = it.id,
                    name = it.name,
                    phoneNumber = it.phoneNumber,
                    notes = it.notes,
                    createdAt = it.createdAt,
                    updatedAt = it.updatedAt
                )
            }
            val obligations = obligationDao.getAllObligationsSync().map {
                FinancialObligationBackupDto(
                    id = it.id,
                    personId = it.personId,
                    amount = it.amount,
                    direction = it.direction,
                    status = it.status,
                    settledAmount = it.settledAmount,
                    remainingAmount = it.remainingAmount,
                    reason = it.reason,
                    dueDate = it.dueDate,
                    relatedTransactionId = it.relatedTransactionId,
                    createdAt = it.createdAt,
                    updatedAt = it.updatedAt
                )
            }
            val settlements = obligationDao.getAllSettlementsSync().map {
                ObligationSettlementBackupDto(
                    id = it.id,
                    obligationId = it.obligationId,
                    amount = it.amount,
                    date = it.date,
                    note = it.note,
                    relatedTransactionId = it.relatedTransactionId,
                    createdAt = it.createdAt
                )
            }

            val payload = BackupPayloadDto(
                version = BackupPayloadDto.CURRENT_BACKUP_VERSION,
                exportedAt = System.currentTimeMillis(),
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

            val jsonString = backupJsonParser.serialize(payload)
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val file = File(getBackupsDir(), "UrWallet_Backup_$timeStamp.json")
            file.writeText(jsonString, Charsets.UTF_8)
            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun exportTransactionsCsv(): Result<File> = withContext(ioDispatcher) {
        try {
            val transactions = transactionDao.getAllTransactionsSync()
            val categories = categoryDao.getAllCategoriesSync()
            val categoryMap = categories.associate { it.id to it.name }

            val csvContent = csvExporter.exportTransactions(transactions, categoryMap)
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val file = File(getBackupsDir(), "UrWallet_Transactions_$timeStamp.csv")
            file.writeText(csvContent, Charsets.UTF_8)
            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun validateBackupJson(jsonContent: String): BackupValidationResult =
        withContext(ioDispatcher) {
            try {
                val payload = backupJsonParser.parseAndValidate(jsonContent)
                val summary = BackupSummary(
                    categoriesCount = payload.data.categories.size,
                    goalsCount = payload.data.goals.size,
                    contributionsCount = payload.data.goalContributions.size,
                    budgetsCount = payload.data.budgets.size,
                    recurringCount = payload.data.recurringTransactions.size,
                    transactionsCount = payload.data.transactions.size,
                    challengesCount = payload.data.challenges.size,
                    peopleCount = payload.data.people.size,
                    obligationsCount = payload.data.obligations.size,
                    exportedAt = payload.exportedAt
                )
                BackupValidationResult.Valid(summary, payload)
            } catch (e: BackupParseException) {
                BackupValidationResult.Invalid(e.message ?: "ملف النسخة الاحتياطية غير صالح.")
            } catch (e: Exception) {
                BackupValidationResult.Invalid("حدث خطأ أثناء فحص ملف النسخة الاحتياطية: ${e.message}")
            }
        }

    override suspend fun restoreBackup(
        payload: BackupPayloadDto,
        strategy: ImportStrategy
    ): Result<BackupSummary> = withContext(ioDispatcher) {
        try {
            when (strategy) {
                ImportStrategy.REPLACE_ALL -> restoreReplaceAll(payload)
                ImportStrategy.MERGE -> restoreMerge(payload)
            }

            val summary = BackupSummary(
                categoriesCount = payload.data.categories.size,
                goalsCount = payload.data.goals.size,
                contributionsCount = payload.data.goalContributions.size,
                budgetsCount = payload.data.budgets.size,
                recurringCount = payload.data.recurringTransactions.size,
                transactionsCount = payload.data.transactions.size,
                challengesCount = payload.data.challenges.size,
                peopleCount = payload.data.people.size,
                obligationsCount = payload.data.obligations.size,
                exportedAt = payload.exportedAt
            )
            Result.success(summary)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun restoreReplaceAll(payload: BackupPayloadDto) {
        urWalletDatabase.withTransaction {
            // 1. Delete in reverse dependency order (children first)
            obligationDao.deleteAllSettlements()
            obligationDao.deleteAllObligations()
            goalContributionDao.deleteAllContributions()
            transactionDao.deleteAllTransactions()
            personDao.deleteAllPeople()
            recurringTransactionDao.deleteAllRecurringTransactions()
            budgetDao.deleteAllBudgets()
            goalDao.deleteAllGoals()
            challengeDao.deleteAllChallenges()
            categoryDao.deleteAllCategories()

            // 2. Insert in dependency order (parents first)
            val categoryEntities = payload.data.categories.map {
                CategoryEntity(
                    id = it.id,
                    name = it.name,
                    type = it.type,
                    icon = it.icon,
                    color = it.color,
                    isDefault = it.isDefault,
                    isDeleted = it.isDeleted
                )
            }
            categoryDao.insertCategories(categoryEntities)

            val personEntities = payload.data.people.map {
                PersonEntity(
                    id = it.id,
                    name = it.name,
                    phoneNumber = it.phoneNumber,
                    notes = it.notes,
                    createdAt = it.createdAt,
                    updatedAt = it.updatedAt
                )
            }
            personDao.insertPeople(personEntities)

            val goalEntities = payload.data.goals.map {
                GoalEntity(
                    id = it.id,
                    name = it.name,
                    icon = it.icon,
                    targetAmount = it.targetAmount,
                    paceMode = it.paceMode,
                    monthlyTarget = it.monthlyTarget,
                    deadline = it.deadline,
                    createdAt = it.createdAt,
                    isDeleted = it.isDeleted
                )
            }
            goalDao.insertGoals(goalEntities)

            val budgetEntities = payload.data.budgets.map {
                BudgetEntity(
                    id = it.id,
                    categoryId = it.categoryId,
                    amount = it.amount,
                    month = it.month,
                    year = it.year,
                    alertThreshold = it.alertThreshold,
                    createdAt = it.createdAt
                )
            }
            budgetDao.insertBudgets(budgetEntities)

            val recurringEntities = payload.data.recurringTransactions.map {
                RecurringTransactionEntity(
                    id = it.id,
                    title = it.title,
                    amount = it.amount,
                    type = it.type,
                    categoryId = it.categoryId,
                    frequency = it.frequency,
                    startDate = it.startDate,
                    endDate = it.endDate,
                    nextOccurrence = it.nextOccurrence,
                    isActive = it.isActive,
                    createdAt = it.createdAt
                )
            }
            recurringTransactionDao.insertRecurringTransactions(recurringEntities)

            val transactionEntities = payload.data.transactions.map {
                TransactionEntity(
                    id = it.id,
                    amount = it.amount,
                    type = it.type,
                    categoryId = it.categoryId,
                    title = it.title,
                    note = it.note,
                    date = it.date,
                    receiptPath = it.receiptPath,
                    personId = it.personId,
                    createdAt = it.createdAt,
                    updatedAt = it.updatedAt
                )
            }
            transactionDao.insertTransactions(transactionEntities)

            val contributionEntities = payload.data.goalContributions.map {
                GoalContributionEntity(
                    id = it.id,
                    goalId = it.goalId,
                    amount = it.amount,
                    note = it.note,
                    date = it.date
                )
            }
            goalContributionDao.insertContributions(contributionEntities)

            val challengeEntities = payload.data.challenges.map {
                ChallengeEntity(
                    id = it.id,
                    title = it.title,
                    description = it.description,
                    type = it.type,
                    targetAmount = it.targetAmount,
                    targetDays = it.targetDays,
                    categoryId = it.categoryId,
                    startDate = it.startDate,
                    endDate = it.endDate,
                    currentProgress = it.currentProgress,
                    streakDays = it.streakDays,
                    isCompleted = it.isCompleted,
                    isActive = it.isActive
                )
            }
            challengeDao.insertChallenges(challengeEntities)

            val obligationEntities = payload.data.obligations.map {
                FinancialObligationEntity(
                    id = it.id,
                    personId = it.personId,
                    amount = it.amount,
                    direction = it.direction,
                    reason = it.reason,
                    dueDate = it.dueDate,
                    status = it.status,
                    settledAmount = it.settledAmount,
                    remainingAmount = it.remainingAmount,
                    relatedTransactionId = it.relatedTransactionId,
                    createdAt = it.createdAt,
                    updatedAt = it.updatedAt
                )
            }
            obligationDao.insertObligations(obligationEntities)

            val settlementEntities = payload.data.obligationSettlements.map {
                ObligationSettlementEntity(
                    id = it.id,
                    obligationId = it.obligationId,
                    amount = it.amount,
                    date = it.date,
                    note = it.note,
                    relatedTransactionId = it.relatedTransactionId,
                    createdAt = it.createdAt
                )
            }
            obligationDao.insertSettlements(settlementEntities)
        }
    }

    private suspend fun restoreMerge(payload: BackupPayloadDto) {
        urWalletDatabase.withTransaction {
            // 1. Resolve & Map Categories (Match by name and type, insert if new)
            val existingCategories = categoryDao.getAllCategoriesSync()
            val categoryIdMap = mutableMapOf<Long, Long>()

            for (backupCat in payload.data.categories) {
                val matched = existingCategories.find {
                    it.name.trim().equals(backupCat.name.trim(), ignoreCase = true) &&
                            (it.type == backupCat.type || it.type.name == "BOTH" || backupCat.type.name == "BOTH")
                }
                if (matched != null) {
                    categoryIdMap[backupCat.id] = matched.id
                } else {
                    val newId = categoryDao.insertCategory(
                        CategoryEntity(
                            id = 0,
                            name = backupCat.name,
                            type = backupCat.type,
                            icon = backupCat.icon,
                            color = backupCat.color,
                            isDefault = false,
                            isDeleted = backupCat.isDeleted
                        )
                    )
                    categoryIdMap[backupCat.id] = newId
                }
            }

            // Fallback category ID in case of unmapped reference
            val fallbackCategoryId = categoryIdMap.values.firstOrNull()
                ?: existingCategories.firstOrNull()?.id
                ?: 1L

            // 2. Resolve & Map People
            val existingPeople = personDao.getAllPeopleSync()
            val personIdMap = mutableMapOf<Long, Long>()
            for (backupPerson in payload.data.people) {
                val matched = existingPeople.find {
                    (!it.phoneNumber.isNullOrBlank() && !backupPerson.phoneNumber.isNullOrBlank() && it.phoneNumber == backupPerson.phoneNumber) ||
                            (it.name.trim().equals(backupPerson.name.trim(), ignoreCase = true))
                }
                if (matched != null) {
                    personIdMap[backupPerson.id] = matched.id
                } else {
                    val newId = personDao.insertPerson(
                        PersonEntity(
                            id = 0,
                            name = backupPerson.name,
                            phoneNumber = backupPerson.phoneNumber,
                            notes = backupPerson.notes,
                            createdAt = backupPerson.createdAt,
                            updatedAt = backupPerson.updatedAt
                        )
                    )
                    personIdMap[backupPerson.id] = newId
                }
            }

            // 3. Resolve & Map Goals (Generate new goals and remap IDs)
            val goalIdMap = mutableMapOf<Long, Long>()
            for (backupGoal in payload.data.goals) {
                val newGoalId = goalDao.insertGoal(
                    GoalEntity(
                        id = 0,
                        name = backupGoal.name,
                        icon = backupGoal.icon,
                        targetAmount = backupGoal.targetAmount,
                        paceMode = backupGoal.paceMode,
                        monthlyTarget = backupGoal.monthlyTarget,
                        deadline = backupGoal.deadline,
                        createdAt = backupGoal.createdAt,
                        isDeleted = backupGoal.isDeleted
                    )
                )
                goalIdMap[backupGoal.id] = newGoalId
            }

            // 4. Insert Goal Contributions with remapped goalId
            for (backupContrib in payload.data.goalContributions) {
                val targetGoalId = goalIdMap[backupContrib.goalId] ?: continue
                goalContributionDao.insertContribution(
                    GoalContributionEntity(
                        id = 0,
                        goalId = targetGoalId,
                        amount = backupContrib.amount,
                        note = backupContrib.note,
                        date = backupContrib.date
                    )
                )
            }

            // 5. Insert Budgets (avoid unique constraint collisions on categoryId, month, year)
            for (backupBudget in payload.data.budgets) {
                val targetCatId = if (backupBudget.categoryId != null) {
                    categoryIdMap[backupBudget.categoryId] ?: fallbackCategoryId
                } else null

                val exists = if (targetCatId == null) {
                    budgetDao.getGlobalBudgetSync(backupBudget.month, backupBudget.year) != null
                } else {
                    budgetDao.getBudgetForCategorySync(targetCatId, backupBudget.month, backupBudget.year) != null
                }

                if (!exists) {
                    budgetDao.insertBudget(
                        BudgetEntity(
                            id = 0,
                            categoryId = targetCatId,
                            amount = backupBudget.amount,
                            month = backupBudget.month,
                            year = backupBudget.year,
                            alertThreshold = backupBudget.alertThreshold,
                            createdAt = backupBudget.createdAt
                        )
                    )
                }
            }

            // 6. Insert Recurring Transactions with remapped categoryId
            for (backupRec in payload.data.recurringTransactions) {
                val targetCatId = categoryIdMap[backupRec.categoryId] ?: fallbackCategoryId
                recurringTransactionDao.insertRecurringTransaction(
                    RecurringTransactionEntity(
                        id = 0,
                        title = backupRec.title,
                        amount = backupRec.amount,
                        type = backupRec.type,
                        categoryId = targetCatId,
                        frequency = backupRec.frequency,
                        startDate = backupRec.startDate,
                        endDate = backupRec.endDate,
                        nextOccurrence = backupRec.nextOccurrence,
                        isActive = backupRec.isActive,
                        createdAt = backupRec.createdAt
                    )
                )
            }

            // 7. Insert Transactions with remapped categoryId and personId
            val txIdMap = mutableMapOf<Long, Long>()
            for (backupTx in payload.data.transactions) {
                val targetCatId = categoryIdMap[backupTx.categoryId] ?: fallbackCategoryId
                val targetPersonId = if (backupTx.personId != null) personIdMap[backupTx.personId] else null
                val newTxId = transactionDao.insertTransaction(
                    TransactionEntity(
                        id = 0,
                        amount = backupTx.amount,
                        type = backupTx.type,
                        categoryId = targetCatId,
                        title = backupTx.title,
                        note = backupTx.note,
                        date = backupTx.date,
                        receiptPath = backupTx.receiptPath,
                        personId = targetPersonId,
                        createdAt = backupTx.createdAt,
                        updatedAt = backupTx.updatedAt
                    )
                )
                txIdMap[backupTx.id] = newTxId
            }

            // 8. Insert Challenges
            for (backupCh in payload.data.challenges) {
                val targetCatId = if (backupCh.categoryId != null) {
                    categoryIdMap[backupCh.categoryId]
                } else null

                challengeDao.insertChallenge(
                    ChallengeEntity(
                        id = 0,
                        title = backupCh.title,
                        description = backupCh.description,
                        type = backupCh.type,
                        targetAmount = backupCh.targetAmount,
                        targetDays = backupCh.targetDays,
                        categoryId = targetCatId,
                        startDate = backupCh.startDate,
                        endDate = backupCh.endDate,
                        currentProgress = backupCh.currentProgress,
                        streakDays = backupCh.streakDays,
                        isCompleted = backupCh.isCompleted,
                        isActive = backupCh.isActive
                    )
                )
            }

            // 9. Insert Obligations & Settlements with remapped personId and obligationId
            val obligationIdMap = mutableMapOf<Long, Long>()
            for (backupOb in payload.data.obligations) {
                val targetPersonId = personIdMap[backupOb.personId] ?: continue
                val targetTxId = if (backupOb.relatedTransactionId != null) txIdMap[backupOb.relatedTransactionId] else null
                val newObId = obligationDao.insertObligation(
                    FinancialObligationEntity(
                        id = 0,
                        personId = targetPersonId,
                        amount = backupOb.amount,
                        direction = backupOb.direction,
                        reason = backupOb.reason,
                        dueDate = backupOb.dueDate,
                        status = backupOb.status,
                        settledAmount = backupOb.settledAmount,
                        remainingAmount = backupOb.remainingAmount,
                        relatedTransactionId = targetTxId,
                        createdAt = backupOb.createdAt,
                        updatedAt = backupOb.updatedAt
                    )
                )
                obligationIdMap[backupOb.id] = newObId
            }

            for (backupSettlement in payload.data.obligationSettlements) {
                val targetObId = obligationIdMap[backupSettlement.obligationId] ?: continue
                val targetTxId = if (backupSettlement.relatedTransactionId != null) txIdMap[backupSettlement.relatedTransactionId] else null
                obligationDao.insertSettlement(
                    ObligationSettlementEntity(
                        id = 0,
                        obligationId = targetObId,
                        amount = backupSettlement.amount,
                        date = backupSettlement.date,
                        note = backupSettlement.note,
                        relatedTransactionId = targetTxId,
                        createdAt = backupSettlement.createdAt
                    )
                )
            }
        }
    }

    override suspend fun clearAllData(): Result<Unit> = withContext(ioDispatcher) {
        try {
            urWalletDatabase.withTransaction {
                // Delete user financial data in children-first order
                obligationDao.deleteAllSettlements()
                obligationDao.deleteAllObligations()
                goalContributionDao.deleteAllContributions()
                transactionDao.deleteAllTransactions()
                personDao.deleteAllPeople()
                recurringTransactionDao.deleteAllRecurringTransactions()
                budgetDao.deleteAllBudgets()
                goalDao.deleteAllGoals()
                challengeDao.deleteAllChallenges()
                categoryDao.deleteAllCategories()

                // Reseed default 12 categories
                DatabaseCallback.populateDefaultCategories(urWalletDatabase)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

