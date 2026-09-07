package com.example.urwallet.features.goals.presentation.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
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
            binding.ivGoalIcon.setImageResource(CategoryIconMapper.getIconDrawableRes(goal.icon))
            binding.tvGoalName.text = goal.name
            binding.tvAchievedAmount.text = "تم توفير ${Formatters.formatCurrency(goal.targetAmount)} بالكامل"
            binding.cardCompletedGoal.setOnClickListener { onGoalClick(goal) }
        }
    }

    companion object GoalDiffCallback : DiffUtil.ItemCallback<Goal>() {
        override fun areItemsTheSame(oldItem: Goal, newItem: Goal): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Goal, newItem: Goal): Boolean = oldItem == newItem
    }
}
