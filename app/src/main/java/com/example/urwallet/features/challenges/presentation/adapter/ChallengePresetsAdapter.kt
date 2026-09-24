package com.example.urwallet.features.challenges.presentation.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.urwallet.databinding.ItemChallengePresetBinding
import com.example.urwallet.features.challenges.domain.model.ChallengePreset

import com.example.urwallet.R

class ChallengePresetsAdapter(
    private val onJoinClick: (ChallengePreset) -> Unit
) : ListAdapter<ChallengePreset, ChallengePresetsAdapter.ViewHolder>(DiffCallback) {

    private data class PresetStrings(
        val titleRes: Int?,
        val descRes: Int?,
        val diffRes: Int?,
        val durRes: Int?
    )

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
            val context = binding.root.context
            val strings = when (item.id) {
                "preset_no_cafe_7d" -> PresetStrings(
                    R.string.preset_no_cafe_title,
                    R.string.preset_no_cafe_desc,
                    R.string.preset_difficulty_medium,
                    R.string.preset_duration_7d
                )
                "preset_zero_spend_1d" -> PresetStrings(
                    R.string.preset_zero_spend_title,
                    R.string.preset_zero_spend_desc,
                    R.string.preset_difficulty_easy,
                    R.string.preset_duration_1d
                )
                "preset_save_1000_30d" -> PresetStrings(
                    R.string.preset_save_1000_title,
                    R.string.preset_save_1000_desc,
                    R.string.preset_difficulty_hard,
                    R.string.preset_duration_30d
                )
                "preset_smart_shopping_14d" -> PresetStrings(
                    R.string.preset_smart_shopping_title,
                    R.string.preset_smart_shopping_desc,
                    R.string.preset_difficulty_medium,
                    R.string.preset_duration_14d
                )
                "preset_calm_weekend_2d" -> PresetStrings(
                    R.string.preset_calm_weekend_title,
                    R.string.preset_calm_weekend_desc,
                    R.string.preset_difficulty_easy,
                    R.string.preset_duration_2d
                )
                else -> PresetStrings(null, null, null, null)
            }

            binding.tvPresetTitle.text = strings.titleRes?.let { context.getString(it) } ?: item.title
            binding.tvPresetDesc.text = strings.descRes?.let { context.getString(it) } ?: item.description
            binding.chipDuration.text = strings.durRes?.let { context.getString(it) } ?: item.durationText
            binding.chipDifficulty.text = strings.diffRes?.let { context.getString(it) } ?: item.difficulty

            binding.btnJoinPreset.setOnClickListener {
                val localizedPreset = if (strings.titleRes != null && strings.descRes != null) {
                    item.copy(
                        title = context.getString(strings.titleRes),
                        description = context.getString(strings.descRes)
                    )
                } else item
                onJoinClick(localizedPreset)
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
