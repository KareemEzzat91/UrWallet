package com.example.urwallet.features.challenges.presentation.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.urwallet.R
import com.example.urwallet.core.common.ChallengeType
import com.example.urwallet.core.common.Formatters
import com.example.urwallet.databinding.ItemActiveChallengeBinding
import com.example.urwallet.features.challenges.domain.model.ChallengeProgress

class ActiveChallengesAdapter(
    private val onChallengeClick: (Long) -> Unit
) : ListAdapter<ChallengeProgress, ActiveChallengesAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemActiveChallengeBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemActiveChallengeBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ChallengeProgress) {
            val context = binding.root.context
            val challenge = item.challenge

            binding.root.setOnClickListener {
                onChallengeClick(challenge.id)
            }

            binding.tvChallengeTitle.text = challenge.title

            // Subtitle & Days Left
            val typeText = when (challenge.type) {
                ChallengeType.NO_SPENDING -> context.getString(R.string.challenge_type_no_spending)
                ChallengeType.SAVE_AMOUNT -> context.getString(R.string.challenge_type_save_amount)
                ChallengeType.REDUCE_CATEGORY -> context.getString(R.string.challenge_type_reduce_category)
            }
            binding.tvChallengeSubtitle.text = context.getString(
                R.string.challenge_subtitle_format,
                typeText,
                item.remainingDays
            )

            // Streak
            binding.tvStreakCount.text = context.getString(R.string.streak_days_format, item.currentStreak)

            // Progress Bar & Percentage
            val pctInt = item.progressPercentage.toInt()
            binding.progressIndicator.progress = pctInt
            binding.tvProgressPercent.text = "${pctInt}%"

            // Progress Description
            binding.tvProgressDesc.text = when (challenge.type) {
                ChallengeType.NO_SPENDING -> {
                    val target = item.targetDays ?: 7
                    context.getString(R.string.challenge_progress_days_format, item.completedDays, target)
                }
                ChallengeType.SAVE_AMOUNT -> {
                    val target = item.targetAmount ?: 0.0
                    val currentFormatted = Formatters.formatCurrency(item.currentProgress)
                    val targetFormatted = Formatters.formatCurrency(target)
                    context.getString(R.string.challenge_progress_amount_format, currentFormatted, targetFormatted)
                }
                ChallengeType.REDUCE_CATEGORY -> {
                    val limit = item.targetAmount ?: 0.0
                    val spentFormatted = Formatters.formatCurrency(item.currentProgress)
                    val limitFormatted = Formatters.formatCurrency(limit)
                    context.getString(R.string.challenge_progress_limit_format, spentFormatted, limitFormatted)
                }
            }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<ChallengeProgress>() {
        override fun areItemsTheSame(oldItem: ChallengeProgress, newItem: ChallengeProgress): Boolean {
            return oldItem.challenge.id == newItem.challenge.id
        }

        override fun areContentsTheSame(oldItem: ChallengeProgress, newItem: ChallengeProgress): Boolean {
            return oldItem == newItem
        }
    }
}
