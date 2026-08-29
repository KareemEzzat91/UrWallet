package com.example.urwallet.features.goals.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.urwallet.core.common.GoalPaceMode

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val icon: String,
    val targetAmount: Double,
    val paceMode: GoalPaceMode,
    val monthlyTarget: Double,
    val deadline: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val isCompleted: Boolean = false,
    val isDeleted: Boolean = false
)
