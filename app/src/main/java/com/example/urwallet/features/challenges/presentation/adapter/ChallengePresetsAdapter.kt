package com.example.urwallet.features.challenges.presentation.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.urwallet.databinding.ItemChallengePresetBinding
import com.example.urwallet.features.challenges.domain.model.ChallengePreset

class ChallengePresetsAdapter(
    private val onJoinClick: (ChallengePreset) -> Unit
) : ListAdapter<ChallengePreset, ChallengePresetsAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemChallengePresetBinding.inflate(
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
        private val binding: ItemChallengePresetBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ChallengePreset) {
            binding.tvPresetTitle.text = item.title
            binding.tvPresetDesc.text = item.description
            binding.chipDuration.text = item.durationText
            binding.chipDifficulty.text = item.difficulty

            binding.btnJoinPreset.setOnClickListener {
                onJoinClick(item)
            }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<ChallengePreset>() {
        override fun areItemsTheSame(oldItem: ChallengePreset, newItem: ChallengePreset): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ChallengePreset, newItem: ChallengePreset): Boolean {
            return oldItem == newItem
        }
    }
}
