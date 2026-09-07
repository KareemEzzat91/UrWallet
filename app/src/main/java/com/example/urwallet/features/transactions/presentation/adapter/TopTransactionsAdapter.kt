package com.example.urwallet.features.transactions.presentation.adapter

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.urwallet.core.common.DateUtils
import com.example.urwallet.core.common.Formatters
import com.example.urwallet.core.designsystem.CategoryIconMapper
import com.example.urwallet.databinding.ItemTopTransactionBinding
import com.example.urwallet.features.transactions.domain.model.Category
import com.example.urwallet.features.transactions.domain.model.Transaction

class TopTransactionsAdapter(
    private val onTransactionClick: (Transaction) -> Unit
) : ListAdapter<Transaction, TopTransactionsAdapter.ViewHolder>(DiffCallback) {

    var category: Category? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTopTransactionBinding.inflate(
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
        private val binding: ItemTopTransactionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(transaction: Transaction) {
            binding.tvTitle.text = transaction.title

            val timeFormatted = DateUtils.formatTimeArabic(transaction.date)
            val dateFormatted = DateUtils.formatDateArabic(transaction.date)
            binding.tvSubtitle.text = "$dateFormatted • $timeFormatted"

            binding.tvAmount.text = Formatters.formatSignedAmount(
                amount = transaction.amount,
                type = transaction.type
            )

            val iconRes = CategoryIconMapper.getIconDrawableRes(category?.icon ?: "ic_other")
            binding.ivIcon.setImageResource(iconRes)

            val color = CategoryIconMapper.parseColorSafely(category?.color ?: "#78909C")
            binding.flIconContainer.backgroundTintList = ColorStateList.valueOf(color)

            binding.root.setOnClickListener {
                onTransactionClick(transaction)
            }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<Transaction>() {
        override fun areItemsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
            return oldItem == newItem
        }
    }
}
