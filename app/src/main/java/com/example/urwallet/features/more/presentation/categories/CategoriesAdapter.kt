package com.example.urwallet.features.more.presentation.categories

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.urwallet.R
import com.example.urwallet.databinding.ItemCategoryManagementBinding
import com.example.urwallet.features.transactions.domain.model.Category

class CategoriesAdapter(
    private val onEditClick: (Category) -> Unit,
    private val onDeleteClick: (Category) -> Unit
) : ListAdapter<Category, CategoriesAdapter.CategoryViewHolder>(CategoryDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemCategoryManagementBinding.inflate(
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
        private val binding: ItemCategoryManagementBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(category: Category) {
            val context = binding.root.context
            binding.tvCategoryName.text = category.name

            // Background tint with category color
            val colorInt = try {
                Color.parseColor(category.color)
            } catch (_: Exception) {
                Color.parseColor("#78909C")
            }
            binding.flIconContainer.backgroundTintList = ColorStateList.valueOf(colorInt)

            // Resolve category icon
            val resId = context.resources.getIdentifier(
                category.icon,
                "drawable",
                context.packageName
            )
            if (resId != 0) {
                binding.ivCategoryIcon.setImageResource(resId)
            } else {
                binding.ivCategoryIcon.setImageResource(R.drawable.ic_other)
            }

            // Default vs Custom badge
            if (category.isDefault) {
                binding.tvCategoryBadge.text = context.getString(R.string.category_default_badge)
                binding.tvCategoryBadge.setTextColor(context.getColor(R.color.urwallet_primary))
                binding.layoutActions.isVisible = false
            } else {
                binding.tvCategoryBadge.text = context.getString(R.string.category_custom_badge)
                binding.tvCategoryBadge.setTextColor(context.getColor(R.color.urwallet_text_secondary))
                binding.layoutActions.isVisible = true

                binding.btnEditCategory.setOnClickListener { onEditClick(category) }
                binding.btnDeleteCategory.setOnClickListener { onDeleteClick(category) }
            }
        }
    }

    companion object CategoryDiffCallback : DiffUtil.ItemCallback<Category>() {
        override fun areItemsTheSame(oldItem: Category, newItem: Category): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Category, newItem: Category): Boolean {
            return oldItem == newItem
        }
    }
}
