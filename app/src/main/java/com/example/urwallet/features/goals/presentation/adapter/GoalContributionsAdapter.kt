package com.example.urwallet.features.goals.presentation.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.urwallet.core.common.DateUtils
import com.example.urwallet.core.common.Formatters
import com.example.urwallet.databinding.ItemGoalContributionBinding
import com.example.urwallet.features.goals.domain.model.GoalContribution

class GoalContributionsAdapter :
    ListAdapter<GoalContribution, GoalContributionsAdapter.GoalContributionViewHolder>(ContributionDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GoalContributionViewHolder {
        val binding = ItemGoalContributionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return GoalContributionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GoalContributionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class GoalContributionViewHolder(
        private val binding: ItemGoalContributionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: GoalContribution) {
            binding.tvContributionAmount.text = "+${Formatters.formatCurrency(item.amount)}"
            binding.tvContributionDate.text = "${DateUtils.formatDisplayDate(item.date)} • ${DateUtils.formatTime(item.date)}"

            val hasNote = !item.note.isNullOrBlank()
            binding.tvContributionNote.isVisible = hasNote
            if (hasNote) {
                binding.tvContributionNote.text = item.note
            }
        }
    }

    companion object ContributionDiffCallback : DiffUtil.ItemCallback<GoalContribution>() {
        override fun areItemsTheSame(oldItem: GoalContribution, newItem: GoalContribution): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: GoalContribution, newItem: GoalContribution): Boolean = oldItem == newItem
    }
}
