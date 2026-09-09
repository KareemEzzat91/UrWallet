package com.example.urwallet.features.budgets.presentation.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.urwallet.R
import com.example.urwallet.core.common.BudgetStatus
import com.example.urwallet.core.common.Formatters
import com.example.urwallet.core.designsystem.CategoryIconMapper
import com.example.urwallet.databinding.ItemCategoryBudgetBinding
import com.example.urwallet.features.budgets.domain.model.BudgetSummary
import java.util.Locale

class CategoryBudgetsAdapter(
    private val onBudgetClick: (BudgetSummary) -> Unit = {},
    private val onDeleteClick: (BudgetSummary) -> Unit
) : ListAdapter<BudgetSummary, CategoryBudgetsAdapter.CategoryBudgetViewHolder>(BudgetDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryBudgetViewHolder {
        val binding = ItemCategoryBudgetBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CategoryBudgetViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryBudgetViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CategoryBudgetViewHolder(
        private val binding: ItemCategoryBudgetBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(budget: BudgetSummary) {
            val context = binding.root.context

            // Icon & Category Name
            binding.ivCategoryIcon.setImageResource(CategoryIconMapper.getIconDrawableRes(budget.categoryIcon))
            binding.tvCategoryName.text = budget.categoryName

            // Spent and Limit
            binding.tvSpentAndLimit.text = "${Formatters.formatCurrency(budget.spentAmount)} / ${Formatters.formatCurrency(budget.limitAmount)}"

            // Progress Indicator (clamped 0..100 for visual sanity)
            binding.progressIndicator.progress = budget.visualProgress
            binding.tvProgressPercentage.text = String.format(Locale.getDefault(), "%.0f%%", budget.progressPercentage)

            // Dynamic styling based on status
            when (budget.status) {
                BudgetStatus.HEALTHY -> {
                    binding.tvBudgetStatus.text = context.getString(R.string.budget_status_healthy)
                    binding.tvBudgetStatus.setBackgroundResource(R.drawable.bg_pill_income)
                    binding.tvBudgetStatus.setTextColor(context.getColor(R.color.urwallet_income))
                    binding.progressIndicator.setIndicatorColor(context.getColor(R.color.urwallet_income))

                    binding.tvRemainingAmount.text = "${context.getString(R.string.budget_remaining_label)} ${Formatters.formatCurrency(budget.remainingAmount)}"
                    binding.tvRemainingAmount.setTextColor(context.getColor(R.color.urwallet_text_secondary))
                }
                BudgetStatus.NEAR_LIMIT -> {
                    binding.tvBudgetStatus.text = context.getString(R.string.budget_status_near_limit)
                    binding.tvBudgetStatus.setBackgroundResource(R.drawable.bg_pill_warning)
                    binding.tvBudgetStatus.setTextColor(context.getColor(R.color.urwallet_warning))
                    binding.progressIndicator.setIndicatorColor(context.getColor(R.color.urwallet_warning))

                    binding.tvRemainingAmount.text = "${context.getString(R.string.budget_remaining_label)} ${Formatters.formatCurrency(budget.remainingAmount)}"
                    binding.tvRemainingAmount.setTextColor(context.getColor(R.color.urwallet_text_secondary))
                }
                BudgetStatus.EXCEEDED -> {
                    binding.tvBudgetStatus.text = context.getString(R.string.budget_status_exceeded)
                    binding.tvBudgetStatus.setBackgroundResource(R.drawable.bg_pill_expense)
                    binding.tvBudgetStatus.setTextColor(context.getColor(R.color.urwallet_expense))
                    binding.progressIndicator.setIndicatorColor(context.getColor(R.color.urwallet_expense))

                    binding.tvRemainingAmount.text = "${context.getString(R.string.budget_overspent_label)} ${Formatters.formatCurrency(budget.overspentAmount)}"
                    binding.tvRemainingAmount.setTextColor(context.getColor(R.color.urwallet_expense))
                }
            }

            // Click Handlers
            binding.cardCategoryBudget.setOnClickListener { onBudgetClick(budget) }
            binding.btnDeleteBudget.setOnClickListener { onDeleteClick(budget) }
        }
    }

    companion object BudgetDiffCallback : DiffUtil.ItemCallback<BudgetSummary>() {
        override fun areItemsTheSame(oldItem: BudgetSummary, newItem: BudgetSummary): Boolean =
            oldItem.budgetId == newItem.budgetId

        override fun areContentsTheSame(oldItem: BudgetSummary, newItem: BudgetSummary): Boolean =
            oldItem == newItem
    }
}
