package com.example.urwallet.features.people.presentation

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.urwallet.R
import com.example.urwallet.core.common.DateUtils
import com.example.urwallet.core.common.Formatters
import com.example.urwallet.databinding.ItemObligationBinding
import com.example.urwallet.features.people.domain.model.FinancialObligation
import com.example.urwallet.features.people.domain.model.ObligationDirection
import com.example.urwallet.features.people.domain.model.ObligationStatus

class PersonObligationsAdapter(
    private val onSettleClick: (FinancialObligation) -> Unit
) : ListAdapter<FinancialObligation, PersonObligationsAdapter.ViewHolder>(ObligationDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemObligationBinding.inflate(
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
        private val binding: ItemObligationBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: FinancialObligation) {
            val context = binding.root.context
            val isOwedToMe = item.direction == ObligationDirection.OWED_TO_ME

            // Direction Icon & Styling
            if (isOwedToMe) {
                binding.flDirectionBadge.setBackgroundResource(R.drawable.bg_pill_income)
                binding.ivDirectionIcon.setImageResource(R.drawable.ic_trending_up)
                binding.ivDirectionIcon.setColorFilter(ContextCompat.getColor(context, R.color.urwallet_income))
            } else {
                binding.flDirectionBadge.setBackgroundResource(R.drawable.bg_pill_expense)
                binding.ivDirectionIcon.setImageResource(R.drawable.ic_trending_down)
                binding.ivDirectionIcon.setColorFilter(ContextCompat.getColor(context, R.color.urwallet_expense))
            }

            // Title
            binding.tvObligationTitle.text = item.reason ?: "التزام مالي"

            // Due Date
            if (item.dueDate != null) {
                binding.tvDueDate.text = "الاستحقاق: ${DateUtils.formatDisplayDate(item.dueDate)}"
                binding.tvDueDate.isVisible = true
            } else {
                binding.tvDueDate.isVisible = false
            }

            // Total Amount
            binding.tvTotalAmount.text = Formatters.formatCurrency(item.amount)

            // Status Badge
            when (item.status) {
                ObligationStatus.OPEN -> {
                    binding.tvStatusBadge.text = context.getString(R.string.obligation_status_open)
                    binding.tvStatusBadge.setBackgroundResource(R.drawable.bg_pill_warning)
                    binding.tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.urwallet_text_primary))
                }
                ObligationStatus.PARTIALLY_SETTLED -> {
                    binding.tvStatusBadge.text = context.getString(R.string.obligation_status_partially_settled)
                    binding.tvStatusBadge.setBackgroundResource(R.drawable.bg_squircle_pastel_amber)
                    binding.tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.urwallet_text_primary))
                }
                ObligationStatus.SETTLED -> {
                    binding.tvStatusBadge.text = context.getString(R.string.obligation_status_settled)
                    binding.tvStatusBadge.setBackgroundResource(R.drawable.bg_pill_success_light)
                    binding.tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.urwallet_income))
                }
            }

            // Progress Bar & Settlement Breakdown
            val progressPercent = if (item.amount > 0.0) {
                ((item.settledAmount / item.amount) * 100).toInt().coerceIn(0, 100)
            } else 0
            binding.progressSettlement.progress = progressPercent

            val settledText = Formatters.formatCurrency(item.settledAmount)
            val remainingText = Formatters.formatCurrency(item.remainingAmount)
            binding.tvSettledAndRemaining.text = "تم سداد $settledText • متبقي $remainingText"

            // Settle Button
            val isSettled = item.status == ObligationStatus.SETTLED || item.remainingAmount <= 0.001
            binding.btnSettle.isVisible = !isSettled
            binding.btnSettle.setOnClickListener {
                onSettleClick(item)
            }

            // Notes / Reason details if any
            binding.tvNotes.isVisible = false
        }
    }

    object ObligationDiffCallback : DiffUtil.ItemCallback<FinancialObligation>() {
        override fun areItemsTheSame(
            oldItem: FinancialObligation,
            newItem: FinancialObligation
        ): Boolean = oldItem.id == newItem.id

        override fun areContentsTheSame(
            oldItem: FinancialObligation,
            newItem: FinancialObligation
        ): Boolean = oldItem == newItem
    }
}
