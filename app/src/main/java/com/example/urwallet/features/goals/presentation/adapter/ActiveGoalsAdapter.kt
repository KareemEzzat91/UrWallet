package com.example.urwallet.features.goals.presentation.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.urwallet.R
import com.example.urwallet.core.common.DateUtils
import com.example.urwallet.core.common.Formatters
import com.example.urwallet.core.designsystem.CategoryIconMapper
import com.example.urwallet.databinding.ItemActiveGoalBinding
import com.example.urwallet.features.goals.domain.calculator.GoalCalculator
import com.example.urwallet.features.goals.domain.model.Goal
import java.util.Locale

class ActiveGoalsAdapter(
    private val onGoalClick: (Goal) -> Unit,
    private val onContributeClick: (Goal) -> Unit
) : ListAdapter<Goal, ActiveGoalsAdapter.ActiveGoalViewHolder>(GoalDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActiveGoalViewHolder {
        val binding = ItemActiveGoalBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ActiveGoalViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ActiveGoalViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ActiveGoalViewHolder(
        private val binding: ItemActiveGoalBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(goal: Goal) {
            val context = binding.root.context

            // Category Pastel Squircle Icon & Tint (with smart emoji and name fallback)
            binding.flGoalIconContainer.setBackgroundResource(CategoryIconMapper.getGoalPastelBgRes(goal.icon, goal.name))
            binding.ivGoalIcon.setImageResource(CategoryIconMapper.getGoalDrawableRes(goal.icon, goal.name))
            binding.ivGoalIcon.imageTintList = android.content.res.ColorStateList.valueOf(
                context.getColor(CategoryIconMapper.getGoalIconTintRes(goal.icon, goal.name))
            )
            binding.tvGoalName.text = goal.name

            // Pace Mode Badge with subtle status dot
            val paceLabel = when (goal.paceMode) {
                com.example.urwallet.core.common.GoalPaceMode.RELAXED -> "● وتيرة هادئة"
                com.example.urwallet.core.common.GoalPaceMode.BALANCED -> "● وتيرة متوازنة"
                com.example.urwallet.core.common.GoalPaceMode.AGGRESSIVE -> "● وتيرة مكثفة"
            }
            binding.tvPaceBadge.text = paceLabel

            // Days Remaining
            val daysRemaining = GoalCalculator.calculateDaysRemaining(goal.deadline)
            binding.tvDaysRemaining.text = context.getString(
                R.string.goals_days_left_format,
                daysRemaining
            )

            // Progress Bar & Percentage (Accurate 0% handling)
            val progressInt = goal.progressPercentage.toInt().coerceIn(0, 100)
            binding.progressIndicatorCircular.progress = progressInt
            binding.progressIndicator.progress = progressInt
            binding.tvProgressPercentage.text = "$progressInt%"

            if (progressInt == 0) {
                binding.progressIndicator.trackColor = android.graphics.Color.parseColor("#F1F5F9")
                binding.progressIndicator.setIndicatorColor(android.graphics.Color.TRANSPARENT)
            } else {
                binding.progressIndicator.trackColor = android.graphics.Color.parseColor("#F1F5F9")
                binding.progressIndicator.setIndicatorColor(context.getColor(R.color.urwallet_accent))
            }

            // Financials (Hero Saved Amount + Target Subtitle)
            binding.tvSavedAndTarget.text = Formatters.formatCurrency(goal.savedAmount, includeDecimals = false)
            binding.tvTargetSub.text = "من ${Formatters.formatCurrency(goal.targetAmount, includeDecimals = false)}"
            binding.tvRemainingAmount.text = Formatters.formatCurrency(goal.remainingAmount, includeDecimals = false)

            // Monthly Needed Target (Clean rounded number)
            if (goal.monthlyTarget > 0) {
                binding.tvMonthlyNeeded.visibility = android.view.View.VISIBLE
                val monthlyClean = Formatters.formatCurrency(goal.monthlyTarget, includeDecimals = false)
                binding.tvMonthlyNeeded.text = context.getString(
                    R.string.goals_monthly_needed_format,
                    monthlyClean
                )
            } else {
                binding.tvMonthlyNeeded.visibility = android.view.View.GONE
            }

            // Click Listeners with Haptic Feedback
            binding.cardGoal.setOnClickListener {
                it.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                onGoalClick(goal)
            }
            binding.btnGoalDetailChevron.setOnClickListener {
                binding.cardGoal.performClick()
            }
            binding.layoutRemainingBox.setOnClickListener {
                binding.cardGoal.performClick()
            }
            binding.btnQuickContribute.setOnClickListener {
                it.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                onContributeClick(goal)
            }
        }
    }

    companion object GoalDiffCallback : DiffUtil.ItemCallback<Goal>() {
        override fun areItemsTheSame(oldItem: Goal, newItem: Goal): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Goal, newItem: Goal): Boolean = oldItem == newItem
    }
}
