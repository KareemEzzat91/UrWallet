package com.example.urwallet.features.challenges.presentation.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.urwallet.R
import com.example.urwallet.core.common.DateUtils
import com.example.urwallet.core.common.Formatters
import com.example.urwallet.databinding.ItemChallengeDayBinding
import com.example.urwallet.features.challenges.domain.model.ChallengeDayStatus
import com.example.urwallet.features.challenges.domain.model.DayStatus

class ChallengeDaysAdapter : ListAdapter<ChallengeDayStatus, ChallengeDaysAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemChallengeDayBinding.inflate(
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
        private val binding: ItemChallengeDayBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ChallengeDayStatus) {
            val context = binding.root.context

            binding.tvDayNumber.text = context.getString(R.string.day_number_format, item.dayNumber)
            binding.tvDayDate.text = DateUtils.formatDateArabic(item.dateMillis)

            if (item.spentAmount > 0.0) {
                binding.tvDaySpent.text = Formatters.formatCurrency(item.spentAmount)
                binding.tvDaySpent.setTextColor(ContextCompat.getColor(context, R.color.expense_red))
            } else {
                binding.tvDaySpent.text = Formatters.formatCurrency(0.0)
                binding.tvDaySpent.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            }

            when (item.status) {
                DayStatus.COMPLETED -> {
                    binding.tvDayStatus.text = context.getString(R.string.day_status_completed)
                    binding.tvDayStatus.setBackgroundResource(R.drawable.bg_pill_success_light)
                    binding.tvDayStatus.setTextColor(ContextCompat.getColor(context, R.color.income_green))
                }
                DayStatus.FAILED -> {
                    binding.tvDayStatus.text = context.getString(R.string.day_status_failed)
                    binding.tvDayStatus.setBackgroundResource(R.drawable.bg_pill_expense)
                    binding.tvDayStatus.setTextColor(ContextCompat.getColor(context, R.color.expense_red))
                }
                DayStatus.TODAY -> {
                    binding.tvDayStatus.text = context.getString(R.string.day_status_today)
                    binding.tvDayStatus.setBackgroundResource(R.drawable.bg_pill_primary)
                    binding.tvDayStatus.setTextColor(ContextCompat.getColor(context, R.color.primary))
                }
                DayStatus.PENDING -> {
                    binding.tvDayStatus.text = context.getString(R.string.day_status_pending)
                    binding.tvDayStatus.setBackgroundResource(R.drawable.bg_pill_warning)
                    binding.tvDayStatus.setTextColor(Color.parseColor("#757575"))
                }
            }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<ChallengeDayStatus>() {
        override fun areItemsTheSame(oldItem: ChallengeDayStatus, newItem: ChallengeDayStatus): Boolean {
            return oldItem.dayNumber == newItem.dayNumber && oldItem.dateMillis == newItem.dateMillis
        }

        override fun areContentsTheSame(oldItem: ChallengeDayStatus, newItem: ChallengeDayStatus): Boolean {
            return oldItem == newItem
        }
    }
}
