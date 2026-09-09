package com.example.urwallet.features.analytics.presentation.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.urwallet.core.common.Formatters
import com.example.urwallet.core.designsystem.CategoryIconMapper
import com.example.urwallet.databinding.ItemCategoryAnalyticsBarBinding
import com.example.urwallet.features.analytics.domain.model.CategorySpendingSummary
import java.util.Locale

class CategorySpendingAdapter(
    private val onCategoryClick: (CategorySpendingSummary) -> Unit
) : ListAdapter<CategorySpendingSummary, CategorySpendingAdapter.CategorySpendingViewHolder>(CategoryDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategorySpendingViewHolder {
        val binding = ItemCategoryAnalyticsBarBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CategorySpendingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategorySpendingViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CategorySpendingViewHolder(
        private val binding: ItemCategoryAnalyticsBarBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CategorySpendingSummary) {
            binding.tvCategoryName.text = item.categoryName
            binding.ivCategoryIcon.setImageResource(CategoryIconMapper.getIconDrawableRes(item.categoryIcon))
            binding.tvCategoryAmount.text = Formatters.formatCurrency(item.amount)
            binding.tvCategoryPercentage.text = String.format(Locale.getDefault(), "%.0f%%", item.percentage)

            // Progress clamped to 0..100
            binding.progressCategory.progress = item.percentage.toInt().coerceIn(0, 100)

            // Custom category tint
            val parsedColor = CategoryIconMapper.parseColorSafely(item.categoryColor)
            binding.progressCategory.setIndicatorColor(parsedColor)

            binding.cardCategorySpend.setOnClickListener {
                onCategoryClick(item)
            }
        }
    }

    companion object CategoryDiffCallback : DiffUtil.ItemCallback<CategorySpendingSummary>() {
        override fun areItemsTheSame(
            oldItem: CategorySpendingSummary,
            newItem: CategorySpendingSummary
        ): Boolean = oldItem.categoryId == newItem.categoryId

        override fun areContentsTheSame(
            oldItem: CategorySpendingSummary,
            newItem: CategorySpendingSummary
        ): Boolean = oldItem == newItem
    }
}
