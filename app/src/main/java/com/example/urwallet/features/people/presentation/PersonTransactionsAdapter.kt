package com.example.urwallet.features.people.presentation

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
import com.example.urwallet.features.transactions.domain.model.Transaction

class PersonTransactionsAdapter(
    private val onTransactionClick: ((Transaction) -> Unit)? = null
) : ListAdapter<Transaction, PersonTransactionsAdapter.ViewHolder>(TransactionDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTransactionBinding.inflate(
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
        private val binding: ItemTransactionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Transaction) {
            val context = binding.root.context
            val isIncome = item.type == TransactionType.INCOME

            binding.tvTransactionTitle.text = item.title
            binding.tvTransactionTime.text = DateUtils.formatDisplayDate(item.date)

            val amountText = Formatters.formatSignedAmount(item.amount, item.type)
            binding.tvTransactionAmount.text = amountText

            val amountColor = if (isIncome) {
                ContextCompat.getColor(context, R.color.urwallet_income)
            } else {
                ContextCompat.getColor(context, R.color.urwallet_expense)
            }
            binding.tvTransactionAmount.setTextColor(amountColor)

            if (isIncome) {
                binding.ivCategoryIcon.setImageResource(R.drawable.ic_trending_up)
                binding.flIconContainer.setBackgroundResource(R.drawable.bg_pill_income)
            } else {
                binding.ivCategoryIcon.setImageResource(R.drawable.ic_trending_down)
                binding.flIconContainer.setBackgroundResource(R.drawable.bg_pill_expense)
            }

            binding.root.setOnClickListener {
                onTransactionClick?.invoke(item)
            }
        }
    }

    object TransactionDiffCallback : DiffUtil.ItemCallback<Transaction>() {
        override fun areItemsTheSame(oldItem: Transaction, newItem: Transaction): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Transaction, newItem: Transaction): Boolean =
            oldItem == newItem
    }
}
