package com.example.urwallet.features.more.presentation.recurring.adapter

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.urwallet.R
import com.example.urwallet.core.common.DateUtils
import com.example.urwallet.core.common.Frequency
import com.example.urwallet.core.common.TransactionType
import com.example.urwallet.databinding.ItemRecurringTransactionBinding
import com.example.urwallet.features.more.domain.model.RecurringTransactionWithCategory
import java.text.DecimalFormat

class RecurringAdapter(
    private val onToggleActive: (RecurringTransactionWithCategory, Boolean) -> Unit,
    private val onDelete: (RecurringTransactionWithCategory) -> Unit
) : ListAdapter<RecurringTransactionWithCategory, RecurringAdapter.ViewHolder>(DiffCallback) {

    private val decimalFormat = DecimalFormat("#,##0.00")

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRecurringTransactionBinding.inflate(
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
        private val binding: ItemRecurringTransactionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: RecurringTransactionWithCategory) {
            val context = binding.root.context

            binding.tvTitle.text = item.recurring.title

            val freqText = when (item.recurring.frequency) {
                Frequency.DAILY -> context.getString(R.string.freq_daily)
                Frequency.WEEKLY -> context.getString(R.string.freq_weekly)
                Frequency.MONTHLY -> context.getString(R.string.freq_monthly)
                Frequency.YEARLY -> context.getString(R.string.freq_yearly)
            }

            binding.tvCategoryAndFrequency.text = "${item.categoryName} • $freqText"

            val formattedAmount = decimalFormat.format(item.recurring.amount)
            if (item.recurring.type == TransactionType.INCOME) {
                binding.tvAmount.text = "+ $formattedAmount ${context.getString(R.string.currency_symbol)}"
                binding.tvAmount.setTextColor(ContextCompat.getColor(context, R.color.urwallet_income))
            } else {
                binding.tvAmount.text = "- $formattedAmount ${context.getString(R.string.currency_symbol)}"
                binding.tvAmount.setTextColor(ContextCompat.getColor(context, R.color.urwallet_expense))
            }

            val nextDateFormatted = DateUtils.formatDisplayDate(item.recurring.nextOccurrence)
            binding.tvNextOccurrence.text = context.getString(R.string.recurring_next_occurrence_format, nextDateFormatted)

            // Category Icon & Color
            try {
                val colorInt = Color.parseColor(item.categoryColor)
                binding.flIconContainer.backgroundTintList = ColorStateList.valueOf(colorInt)
            } catch (e: Exception) {
                binding.flIconContainer.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.urwallet_primary)
                )
            }

            val iconResId = context.resources.getIdentifier(
                item.categoryIcon, "drawable", context.packageName
            )
            if (iconResId != 0) {
                binding.ivCategoryIcon.setImageResource(iconResId)
            } else {
                binding.ivCategoryIcon.setImageResource(R.drawable.ic_bills)
            }

            // Switch toggle (prevent recursive trigger during binding)
            binding.switchActive.setOnCheckedChangeListener(null)
            binding.switchActive.isChecked = item.recurring.isActive
            binding.switchActive.setOnCheckedChangeListener { _, isChecked ->
                onToggleActive(item, isChecked)
            }

            binding.btnDelete.setOnClickListener {
                onDelete(item)
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<RecurringTransactionWithCategory>() {
        override fun areItemsTheSame(
            oldItem: RecurringTransactionWithCategory,
            newItem: RecurringTransactionWithCategory
        ): Boolean = oldItem.recurring.id == newItem.recurring.id

        override fun areContentsTheSame(
            oldItem: RecurringTransactionWithCategory,
            newItem: RecurringTransactionWithCategory
        ): Boolean = oldItem == newItem
    }
}
