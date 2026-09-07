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

            // Icon & Name
            binding.ivGoalIcon.setImageResource(CategoryIconMapper.getIconDrawableRes(goal.icon))
            binding.tvGoalName.text = goal.name

            // Deadline
            binding.tvDeadline.text = context.getString(
                R.string.goals_deadline_format,
                DateUtils.formatDisplayDate(goal.deadline)
            )

            // Days Remaining
            val daysRemaining = GoalCalculator.calculateDaysRemaining(goal.deadline)
            binding.tvDaysRemaining.text = context.getString(
                R.string.goals_days_left_format,
                daysRemaining
            )
            if (daysRemaining <= 15) {
                binding.tvDaysRemaining.setBackgroundResource(R.drawable.bg_pill_warning)
                binding.tvDaysRemaining.setTextColor(context.getColor(R.color.urwallet_expense))
            } else {
                binding.tvDaysRemaining.setBackgroundResource(R.drawable.bg_pill_primary)
                binding.tvDaysRemaining.setTextColor(context.getColor(R.color.urwallet_primary))
            }

            // Progress Ring & Text
            val progressInt = goal.progressPercentage.toInt().coerceIn(0, 100)
            binding.progressIndicator.progress = progressInt
            binding.tvProgressPercentage.text = String.format(Locale.getDefault(), "%d%%", progressInt)

            // Financials
            binding.tvSavedAndTarget.text = "${Formatters.formatCurrency(goal.savedAmount)} / ${Formatters.formatCurrency(goal.targetAmount)}"
            binding.tvRemainingAmount.text = "${context.getString(R.string.goals_remaining_label)} ${Formatters.formatCurrency(goal.remainingAmount)}"

            // Click Listeners
            binding.cardGoal.setOnClickListener { onGoalClick(goal) }
            binding.btnQuickContribute.setOnClickListener { onContributeClick(goal) }
        }
    }

    companion object GoalDiffCallback : DiffUtil.ItemCallback<Goal>() {
        override fun areItemsTheSame(oldItem: Goal, newItem: Goal): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Goal, newItem: Goal): Boolean = oldItem == newItem
    }
}
