package com.example.urwallet.features.challenges.presentation.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.urwallet.databinding.ItemCompletedChallengeBinding
import com.example.urwallet.features.challenges.domain.model.ChallengeProgress

class CompletedChallengesAdapter(
    private val onChallengeClick: (Long) -> Unit
) : ListAdapter<ChallengeProgress, CompletedChallengesAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCompletedChallengeBinding.inflate(
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
        private val binding: ItemCompletedChallengeBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ChallengeProgress) {
            binding.root.setOnClickListener {
                onChallengeClick(item.challenge.id)
            }
            binding.tvCompletedTitle.text = item.challenge.title
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
