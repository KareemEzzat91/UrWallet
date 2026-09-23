package com.example.urwallet.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.urwallet.core.common.Constants
import com.example.urwallet.features.budgets.data.dao.BudgetDao
import com.example.urwallet.features.budgets.data.entity.BudgetEntity
import com.example.urwallet.features.challenges.data.dao.ChallengeDao
import com.example.urwallet.features.challenges.data.entity.ChallengeEntity
import com.example.urwallet.features.goals.data.dao.GoalContributionDao
import com.example.urwallet.features.goals.data.dao.GoalDao
import com.example.urwallet.features.goals.data.entity.GoalContributionEntity
import com.example.urwallet.features.goals.data.entity.GoalEntity
import com.example.urwallet.features.events.data.dao.CounterpartyMappingDao
import com.example.urwallet.features.events.data.dao.FinancialInboxDao
import com.example.urwallet.features.events.data.entity.CounterpartyMappingEntity
import com.example.urwallet.features.events.data.entity.FinancialInboxEntity
import com.example.urwallet.features.more.data.dao.RecurringTransactionDao
import com.example.urwallet.features.more.data.entity.RecurringTransactionEntity
import com.example.urwallet.features.transactions.data.dao.CategoryDao
import com.example.urwallet.features.transactions.data.dao.TransactionDao
import com.example.urwallet.features.people.data.dao.FinancialObligationDao
import com.example.urwallet.features.people.data.dao.PersonDao
import com.example.urwallet.features.people.data.entity.FinancialObligationEntity
import com.example.urwallet.features.people.data.entity.ObligationSettlementEntity
import com.example.urwallet.features.people.data.entity.PersonEntity
import com.example.urwallet.features.events.data.dao.CategoryMappingDao
import com.example.urwallet.features.events.data.entity.CategoryMappingEntity
import com.example.urwallet.features.transactions.data.entity.CategoryEntity
import com.example.urwallet.features.transactions.data.entity.TransactionEntity

@Database(
    entities = [
        TransactionEntity::class,
        CategoryEntity::class,
        GoalEntity::class,
        GoalContributionEntity::class,
        BudgetEntity::class,
        RecurringTransactionEntity::class,
        ChallengeEntity::class,
        FinancialInboxEntity::class,
        CounterpartyMappingEntity::class,
        PersonEntity::class,
        FinancialObligationEntity::class,
        ObligationSettlementEntity::class,
        CategoryMappingEntity::class
    ],
    version = 6,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class UrWalletDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun goalDao(): GoalDao
    abstract fun goalContributionDao(): GoalContributionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun recurringTransactionDao(): RecurringTransactionDao
    abstract fun challengeDao(): ChallengeDao
    abstract fun financialInboxDao(): FinancialInboxDao
    abstract fun counterpartyMappingDao(): CounterpartyMappingDao
    abstract fun categoryMappingDao(): CategoryMappingDao
    abstract fun personDao(): PersonDao
    abstract fun financialObligationDao(): FinancialObligationDao

    companion object {
        @Volatile
        private var INSTANCE: UrWalletDatabase? = null

        val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `financial_inbox` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `sourceType` TEXT NOT NULL,
                        `sourceIdentifier` TEXT NOT NULL,
                        `rawMessage` TEXT,
                        `sender` TEXT NOT NULL,
                        `amount` REAL NOT NULL,
                        `currency` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `date` INTEGER NOT NULL,
                        `accountOrCard` TEXT,
                        `counterpartyName` TEXT,
                        `counterpartyType` TEXT,
                        `phoneNumber` TEXT,
                        `suggestedCategoryId` INTEGER,
                        `confidence` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `matchStatus` TEXT NOT NULL,
                        `matchedTransactionId` INTEGER,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_financial_inbox_sourceIdentifier` ON `financial_inbox` (`sourceIdentifier`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_financial_inbox_status` ON `financial_inbox` (`status`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_financial_inbox_date` ON `financial_inbox` (`date`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `counterparty_mappings` (
                        `phoneNumber` TEXT PRIMARY KEY NOT NULL,
                        `name` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_4_5 = object : androidx.room.migration.Migration(4, 5) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                // 1. Add personId to transactions table
                db.execSQL("ALTER TABLE `transactions` ADD COLUMN `personId` INTEGER DEFAULT NULL")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_personId` ON `transactions` (`personId`)")

                // 2. Create people table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `people` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `phoneNumber` TEXT,
                        `notes` TEXT,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_people_name` ON `people` (`name`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_people_phoneNumber` ON `people` (`phoneNumber`)")

                // 3. Create financial_obligations table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `financial_obligations` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `personId` INTEGER NOT NULL,
                        `amount` REAL NOT NULL,
                        `direction` TEXT NOT NULL,
                        `reason` TEXT,
                        `dueDate` INTEGER,
                        `status` TEXT NOT NULL,
                        `settledAmount` REAL NOT NULL DEFAULT 0.0,
                        `remainingAmount` REAL NOT NULL,
                        `relatedTransactionId` INTEGER,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        FOREIGN KEY(`personId`) REFERENCES `people`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_financial_obligations_personId` ON `financial_obligations` (`personId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_financial_obligations_status` ON `financial_obligations` (`status`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_financial_obligations_dueDate` ON `financial_obligations` (`dueDate`)")

                // 4. Create obligation_settlements table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `obligation_settlements` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `obligationId` INTEGER NOT NULL,
                        `amount` REAL NOT NULL,
                        `date` INTEGER NOT NULL,
                        `note` TEXT,
                        `relatedTransactionId` INTEGER,
                        `createdAt` INTEGER NOT NULL,
                        FOREIGN KEY(`obligationId`) REFERENCES `financial_obligations`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_obligation_settlements_obligationId` ON `obligation_settlements` (`obligationId`)")

                // 5. Add optional personId to counterparty_mappings table
                db.execSQL("ALTER TABLE `counterparty_mappings` ADD COLUMN `personId` INTEGER DEFAULT NULL")
            }
        }

        val MIGRATION_5_6 = object : androidx.room.migration.Migration(5, 6) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `category_mappings` (
                        `pattern` TEXT PRIMARY KEY NOT NULL,
                        `categoryId` INTEGER NOT NULL,
                        `usageCount` INTEGER NOT NULL DEFAULT 1,
                        `updatedAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        val ALL_MIGRATIONS: Array<androidx.room.migration.Migration> = arrayOf(
            MIGRATION_3_4,
            MIGRATION_4_5,
            MIGRATION_5_6
        )

        fun getInstance(context: Context): UrWalletDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): UrWalletDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                UrWalletDatabase::class.java,
                Constants.DATABASE_NAME
            )
                .addMigrations(*ALL_MIGRATIONS)
                .addCallback(DatabaseCallback { INSTANCE ?: buildDatabase(context) })
                .build()
        }
    }
}
