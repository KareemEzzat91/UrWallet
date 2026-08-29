package com.example.urwallet.features.challenges.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.urwallet.core.common.ChallengeType

@Entity(tableName = "challenges")
data class ChallengeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String,
    val type: ChallengeType,
    val targetAmount: Double? = null,
    val targetDays: Int? = null,
    val categoryId: Long? = null,
    val startDate: Long,
    val endDate: Long,
    val currentProgress: Double = 0.0,
    val streakDays: Int = 0,
    val isCompleted: Boolean = false,
    val isActive: Boolean = true
)
