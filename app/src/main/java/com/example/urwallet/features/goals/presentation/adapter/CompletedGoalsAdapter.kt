package com.example.urwallet.features.goals.presentation.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.urwallet.R
import com.example.urwallet.core.common.Formatters
import com.example.urwallet.core.designsystem.CategoryIconMapper
import com.example.urwallet.databinding.ItemCompletedGoalBinding
import com.example.urwallet.features.goals.domain.model.Goal

class CompletedGoalsAdapter(
    private val onGoalClick: (Goal) -> Unit
) : ListAdapter<Goal, CompletedGoalsAdapter.CompletedGoalViewHolder>(GoalDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CompletedGoalViewHolder {
        val binding = ItemCompletedGoalBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CompletedGoalViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CompletedGoalViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CompletedGoalViewHolder(
        private val binding: ItemCompletedGoalBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(goal: Goal) {
            val context = binding.root.context
            // Category Pastel Squircle Icon & Tint
            binding.flGoalIconContainer.setBackgroundResource(CategoryIconMapper.getGoalPastelBgRes(goal.icon, goal.name))
            binding.ivGoalIcon.setImageResource(CategoryIconMapper.getGoalDrawableRes(goal.icon, goal.name))
            binding.ivGoalIcon.imageTintList = android.content.res.ColorStateList.valueOf(
                context.getColor(CategoryIconMapper.getGoalIconTintRes(goal.icon, goal.name))
            )
            binding.tvGoalName.text = goal.name
            binding.tvAchievedAmount.text = context.getString(
                R.string.goals_completed_sub_format,
                Formatters.formatCurrency(goal.targetAmount, includeDecimals = false)
            )
            binding.cardCompletedGoal.setOnClickListener {
                it.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                onGoalClick(goal)
            }
        }
    }

    companion object GoalDiffCallback : DiffUtil.ItemCallback<Goal>() {
        override fun areItemsTheSame(oldItem: Goal, newItem: Goal): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Goal, newItem: Goal): Boolean = oldItem == newItem
    }
}
