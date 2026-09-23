package com.example.urwallet.features.events.presentation

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.urwallet.R
import com.example.urwallet.core.common.DateUtils
import com.example.urwallet.core.common.Formatters
import com.example.urwallet.core.common.TransactionType
import com.example.urwallet.databinding.ItemFinancialInboxBinding
import com.example.urwallet.features.events.domain.model.CounterpartyType
import com.example.urwallet.features.events.domain.model.DuplicateMatchStatus
import com.example.urwallet.features.events.domain.model.FinancialEvent
import com.example.urwallet.features.events.domain.model.InboxStatus

class FinancialInboxAdapter(
    private val onConfirmClick: (FinancialEvent) -> Unit,
    private val onEditClick: (FinancialEvent) -> Unit,
    private val onDismissClick: (FinancialEvent) -> Unit,
    private val onSelectToggle: (FinancialEvent) -> Unit,
    private val onSameTransactionClick: (FinancialEvent) -> Unit,
    private val onDifferentTransactionClick: (FinancialEvent) -> Unit,
    private val onDismissDuplicateClick: (FinancialEvent) -> Unit
) : ListAdapter<FinancialEvent, FinancialInboxAdapter.ViewHolder>(EventDiffCallback) {

    private var isSelectionMode: Boolean = false
    private var selectedEventIds: Set<Long> = emptySet()

    fun updateSelectionState(selectionMode: Boolean, selectedIds: Set<Long>) {
        val modeChanged = this.isSelectionMode != selectionMode
        this.isSelectionMode = selectionMode
        this.selectedEventIds = selectedIds
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemFinancialInboxBinding.inflate(
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
        private val binding: ItemFinancialInboxBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: FinancialEvent) {
            val context = binding.root.context
            val isIncome = item.type == TransactionType.INCOME

            // Selection Checkbox
            binding.cbSelectEvent.isVisible = isSelectionMode
            binding.cbSelectEvent.isChecked = selectedEventIds.contains(item.id)
            binding.cbSelectEvent.setOnClickListener {
                onSelectToggle(item)
            }

            binding.cardInboxItem.setOnLongClickListener {
                onSelectToggle(item)
                true
            }

            binding.cardInboxItem.setOnClickListener {
                if (isSelectionMode) {
                    onSelectToggle(item)
                }
            }

            // 1. Amount & Direction
            binding.tvAmount.text = Formatters.formatSignedAmount(item.amount, item.type)
            val amountColor = if (isIncome) {
                context.getColor(R.color.urwallet_income)
            } else {
                context.getColor(R.color.urwallet_expense)
            }
            binding.tvAmount.setTextColor(amountColor)

            // Icon Badge
            if (isIncome) {
                binding.flDirectionBadge.setBackgroundResource(R.drawable.bg_pill_income)
                binding.ivDirectionIcon.setImageResource(R.drawable.ic_trending_up)
                binding.ivDirectionIcon.imageTintList = ColorStateList.valueOf(context.getColor(R.color.urwallet_income))
            } else {
                binding.flDirectionBadge.setBackgroundResource(R.drawable.bg_pill_expense)
                binding.ivDirectionIcon.setImageResource(R.drawable.ic_trending_down)
                binding.ivDirectionIcon.imageTintList = ColorStateList.valueOf(context.getColor(R.color.urwallet_expense))
            }

            // 2. Sender and Account/Card
            binding.tvSender.text = item.sender
            val hasCardOrAccount = !item.accountOrCard.isNullOrBlank()
            binding.tvAccountCard.isVisible = hasCardOrAccount
            if (hasCardOrAccount) {
                binding.tvAccountCard.text = item.accountOrCard
            }

            // 3. Date
            binding.tvDate.text = "${DateUtils.formatDateArabic(item.date)} • ${DateUtils.formatTimeArabic(item.date)}"

            // 4. Counterparty
            val hasCounterparty = item.counterparty != null && item.counterparty.name.isNotBlank()
            binding.layoutCounterparty.isVisible = hasCounterparty
            if (hasCounterparty) {
                val cp = item.counterparty!!
                val phoneSuffix = if (!cp.phoneNumber.isNullOrBlank() && cp.phoneNumber != cp.name) " (${cp.phoneNumber})" else ""
                binding.tvCounterpartyName.text = "${cp.name}$phoneSuffix"

                val cpIcon = when (cp.type) {
                    CounterpartyType.MERCHANT -> R.drawable.ic_shopping
                    CounterpartyType.ORGANIZATION -> R.drawable.ic_shield
                    else -> R.drawable.ic_avatar_placeholder
                }
                binding.ivCounterpartyType.setImageResource(cpIcon)
            }

            // 5. Possible Duplicate Handling
            val isDuplicate = (item.matchStatus == DuplicateMatchStatus.POSSIBLE_MATCH || item.matchStatus == DuplicateMatchStatus.EXACT_MATCH)
                && item.status == InboxStatus.PENDING

            binding.layoutDuplicateResolution.isVisible = isDuplicate
            if (isDuplicate) {
                val detectedText = "${context.getString(R.string.duplicate_detected_label)} ${item.sender} • ${Formatters.formatCurrency(item.amount)} • ${DateUtils.formatTimeArabic(item.date)}"
                binding.tvDuplicateDetectedInfo.text = detectedText

                val existingText = "${context.getString(R.string.duplicate_existing_label)} معاملة مسجلة بنفس القيمة والتاريخ تقريباً (معرف: #${item.matchedTransactionId ?: ""})"
                binding.tvDuplicateExistingInfo.text = existingText

                binding.btnSameTransaction.setOnClickListener { onSameTransactionClick(item) }
                binding.btnDifferentTransaction.setOnClickListener { onDifferentTransactionClick(item) }
                binding.btnDismissDuplicate.setOnClickListener { onDismissDuplicateClick(item) }
            }

            // 6. Action buttons (Visible only for normal PENDING items, not when duplicate card is shown)
            val isPendingNormal = item.status == InboxStatus.PENDING && !isDuplicate
            binding.layoutActionButtons.isVisible = isPendingNormal

            if (isPendingNormal) {
                binding.btnConfirm.setOnClickListener { onConfirmClick(item) }
                binding.btnEdit.setOnClickListener { onEditClick(item) }
                binding.btnDismiss.setOnClickListener { onDismissClick(item) }
            }
        }
    }

    companion object EventDiffCallback : DiffUtil.ItemCallback<FinancialEvent>() {
        override fun areItemsTheSame(oldItem: FinancialEvent, newItem: FinancialEvent): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: FinancialEvent, newItem: FinancialEvent): Boolean = oldItem == newItem
    }
}
