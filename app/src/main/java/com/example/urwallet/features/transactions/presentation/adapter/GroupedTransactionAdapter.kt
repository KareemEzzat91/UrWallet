package com.example.urwallet.features.transactions.presentation.adapter

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.urwallet.R
import com.example.urwallet.core.common.DateUtils
import com.example.urwallet.core.common.Formatters
import com.example.urwallet.core.common.TransactionType
import com.example.urwallet.databinding.ItemTransactionBinding
import com.example.urwallet.databinding.ItemTransactionHeaderBinding
import com.example.urwallet.features.transactions.domain.model.Transaction
import com.example.urwallet.core.designsystem.CategoryIconMapper
import com.example.urwallet.features.transactions.presentation.TransactionListItem

class GroupedTransactionAdapter(
    private val onTransactionClick: ((Transaction) -> Unit)? = null,
    private val onDeleteClick: (Transaction) -> Unit
) : ListAdapter<TransactionListItem, RecyclerView.ViewHolder>(TransactionDiffCallback) {

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_ITEM = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is TransactionListItem.Header -> VIEW_TYPE_HEADER
            is TransactionListItem.Item -> VIEW_TYPE_ITEM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_HEADER -> {
                val binding = ItemTransactionHeaderBinding.inflate(inflater, parent, false)
                HeaderViewHolder(binding)
            }
            VIEW_TYPE_ITEM -> {
                val binding = ItemTransactionBinding.inflate(inflater, parent, false)
                TransactionViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Unknown viewType: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is TransactionListItem.Header -> (holder as HeaderViewHolder).bind(item)
            is TransactionListItem.Item -> (holder as TransactionViewHolder).bind(item)
        }
    }

    inner class HeaderViewHolder(
        private val binding: ItemTransactionHeaderBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(header: TransactionListItem.Header) {
            val label = header.dateLabel
            if (label == "اليوم") {
                binding.tvDateHeader.text = DateUtils.formatDisplayDate(DateUtils.getCurrentEpochMs())
                binding.layoutTodayBadge.visibility = android.view.View.VISIBLE
                binding.tvTodayBadgeText.text = "اليوم"
            } else if (label == "أمس") {
                val yesterday = DateUtils.getCurrentEpochMs() - (24 * 60 * 60 * 1000)
                binding.tvDateHeader.text = DateUtils.formatDisplayDate(yesterday)
                binding.layoutTodayBadge.visibility = android.view.View.VISIBLE
                binding.tvTodayBadgeText.text = "أمس"
            } else {
                binding.tvDateHeader.text = label
                binding.layoutTodayBadge.visibility = android.view.View.GONE
            }
        }
    }

    inner class TransactionViewHolder(
        private val binding: ItemTransactionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: TransactionListItem.Item) {
            val context = binding.root.context
            val transaction = item.transaction
            val category = item.category

            binding.tvTransactionTitle.text = transaction.title

            val categoryName = category?.name ?: context.getString(R.string.cat_other)
            val timeFormatted = DateUtils.formatTimeArabic(transaction.date)
            binding.tvTransactionTime.text = timeFormatted
            binding.tvCategoryBadge.text = categoryName

            val iconRes = CategoryIconMapper.getIconDrawableRes(category?.icon ?: "ic_other")
            binding.ivCategoryIcon.setImageResource(iconRes)
            binding.ivCategoryBadgeIcon.setImageResource(iconRes)

            val categoryColor = CategoryIconMapper.parseColorSafely(
                category?.color ?: "#78909C"
            )
            binding.flIconContainer.backgroundTintList = ColorStateList.valueOf(categoryColor)

            val isIncome = transaction.type == TransactionType.INCOME
            val badgeBgColor = if (isIncome) android.graphics.Color.parseColor("#DCFCE7") else android.graphics.Color.parseColor("#FEE2E2")
            val amountColor = if (isIncome) ContextCompat.getColor(context, R.color.urwallet_income) else ContextCompat.getColor(context, R.color.urwallet_expense)

            binding.flCategoryBadge.backgroundTintList = ColorStateList.valueOf(badgeBgColor)
            binding.tvCategoryBadge.setTextColor(amountColor)
            binding.ivCategoryBadgeIcon.imageTintList = ColorStateList.valueOf(amountColor)

            // Formatted signed amount & color
            binding.tvTransactionAmount.text = Formatters.formatSignedAmount(
                amount = transaction.amount,
                type = transaction.type
            )
            binding.tvTransactionAmount.setTextColor(amountColor)

            binding.btnDelete.setOnClickListener {
                onDeleteClick(transaction)
            }

            binding.root.setOnClickListener {
                onTransactionClick?.invoke(transaction)
            }
        }
    }

    private object TransactionDiffCallback : DiffUtil.ItemCallback<TransactionListItem>() {
        override fun areItemsTheSame(
            oldItem: TransactionListItem,
            newItem: TransactionListItem
        ): Boolean {
            return when {
                oldItem is TransactionListItem.Header && newItem is TransactionListItem.Header ->
                    oldItem.dateLabel == newItem.dateLabel
                oldItem is TransactionListItem.Item && newItem is TransactionListItem.Item ->
                    oldItem.transaction.id == newItem.transaction.id
                else -> false
            }
        }

        override fun areContentsTheSame(
            oldItem: TransactionListItem,
            newItem: TransactionListItem
        ): Boolean {
            return oldItem == newItem
        }
    }
}
