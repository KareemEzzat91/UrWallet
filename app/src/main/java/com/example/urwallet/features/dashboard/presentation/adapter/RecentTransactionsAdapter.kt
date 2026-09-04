package com.example.urwallet.features.dashboard.presentation.adapter

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
import com.example.urwallet.databinding.ItemRecentTransactionBinding
import com.example.urwallet.features.dashboard.domain.model.DashboardTransactionItem
import com.example.urwallet.features.transactions.presentation.CategoryResourceHelper

class RecentTransactionsAdapter(
    private val onTransactionClick: ((DashboardTransactionItem) -> Unit)? = null
) : ListAdapter<DashboardTransactionItem, RecentTransactionsAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRecentTransactionBinding.inflate(
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
        private val binding: ItemRecentTransactionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: DashboardTransactionItem) {
            val context = binding.root.context
            val transaction = item.transaction
            val category = item.category

            binding.tvTitle.text = transaction.title

            val categoryName = category?.name ?: context.getString(R.string.cat_other)
            val dateStr = DateUtils.formatDateArabic(transaction.date)
            binding.tvSubtitle.text = "$categoryName • $dateStr"

            val iconRes = CategoryResourceHelper.getIconDrawableRes(category?.icon ?: "ic_other")
            binding.ivIcon.setImageResource(iconRes)

            val color = CategoryResourceHelper.parseColorSafely(category?.color ?: "#78909C")
            binding.flIconContainer.backgroundTintList = ColorStateList.valueOf(color)

            binding.tvAmount.text = Formatters.formatSignedAmount(
                amount = transaction.amount,
                type = transaction.type
            )

            val amountColor = when (transaction.type) {
                TransactionType.INCOME -> ContextCompat.getColor(context, R.color.urwallet_income)
                TransactionType.EXPENSE -> ContextCompat.getColor(context, R.color.urwallet_expense)
            }
            binding.tvAmount.setTextColor(amountColor)

            binding.root.setOnClickListener {
                onTransactionClick?.invoke(item)
            }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<DashboardTransactionItem>() {
        override fun areItemsTheSame(
            oldItem: DashboardTransactionItem,
            newItem: DashboardTransactionItem
        ): Boolean = oldItem.transaction.id == newItem.transaction.id

        override fun areContentsTheSame(
            oldItem: DashboardTransactionItem,
            newItem: DashboardTransactionItem
        ): Boolean = oldItem == newItem
    }
}
