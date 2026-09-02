package com.example.urwallet.features.transactions.presentation.adapter

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.urwallet.R
import com.example.urwallet.databinding.ItemCategoryChipBinding
import com.example.urwallet.features.transactions.domain.model.Category
import com.example.urwallet.features.transactions.presentation.CategoryResourceHelper

class CategoryChipAdapter(
    private val onCategoryClick: (Category) -> Unit
) : ListAdapter<Category, CategoryChipAdapter.CategoryViewHolder>(CategoryDiffCallback) {

    var selectedCategoryId: Long? = null
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemCategoryChipBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CategoryViewHolder(
        private val binding: ItemCategoryChipBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(category: Category) {
            val context = binding.root.context
            binding.tvCategoryName.text = category.name

            val iconRes = CategoryResourceHelper.getIconDrawableRes(category.icon)
            binding.ivCategoryIcon.setImageResource(iconRes)

            val categoryColor = CategoryResourceHelper.parseColorSafely(category.color)
            binding.ivCategoryIcon.backgroundTintList = ColorStateList.valueOf(categoryColor)

            val isSelected = category.id == selectedCategoryId

            if (isSelected) {
                binding.cardCategory.strokeWidth = 2.dpToPx(context)
                binding.cardCategory.strokeColor = ContextCompat.getColor(context, R.color.urwallet_primary)
                binding.cardCategory.setCardBackgroundColor(
                    ContextCompat.getColor(context, R.color.urwallet_primary_light)
                )
            } else {
                binding.cardCategory.strokeWidth = 1.dpToPx(context)
                binding.cardCategory.strokeColor = ContextCompat.getColor(context, R.color.urwallet_divider)
                binding.cardCategory.setCardBackgroundColor(
                    ContextCompat.getColor(context, R.color.urwallet_surface)
                )
            }

            binding.root.setOnClickListener {
                onCategoryClick(category)
            }
        }

        private fun Int.dpToPx(context: android.content.Context): Int {
            return (this * context.resources.displayMetrics.density).toInt()
        }
    }

    private object CategoryDiffCallback : DiffUtil.ItemCallback<Category>() {
        override fun areItemsTheSame(oldItem: Category, newItem: Category): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Category, newItem: Category): Boolean {
            return oldItem == newItem
        }
    }
}
