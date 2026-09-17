package com.example.urwallet.features.more.presentation.categories

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.urwallet.R
import com.example.urwallet.databinding.ItemCategoryIconPickerBinding

class IconPickerAdapter(
    private val icons: List<String>,
    private var selectedIcon: String,
    private val onIconSelected: (String) -> Unit
) : RecyclerView.Adapter<IconPickerAdapter.IconViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IconViewHolder {
        val binding = ItemCategoryIconPickerBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return IconViewHolder(binding)
    }

    override fun onBindViewHolder(holder: IconViewHolder, position: Int) {
        holder.bind(icons[position])
    }

    override fun getItemCount(): Int = icons.size

    fun getSelectedIcon(): String = selectedIcon

    fun setSelectedIcon(icon: String) {
        val prevIndex = icons.indexOf(selectedIcon)
        selectedIcon = icon
        val newIndex = icons.indexOf(selectedIcon)
        if (prevIndex != -1) notifyItemChanged(prevIndex)
        if (newIndex != -1) notifyItemChanged(newIndex)
    }

    inner class IconViewHolder(
        private val binding: ItemCategoryIconPickerBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(iconName: String) {
            val context = binding.root.context
            val isSelected = iconName == selectedIcon

            val resId = context.resources.getIdentifier(iconName, "drawable", context.packageName)
            if (resId != 0) {
                binding.ivIcon.setImageResource(resId)
            } else {
                binding.ivIcon.setImageResource(R.drawable.ic_other)
            }

            if (isSelected) {
                binding.cardIcon.strokeWidth = 3
                binding.cardIcon.strokeColor = context.getColor(R.color.urwallet_primary)
                binding.cardIcon.setCardBackgroundColor(context.getColor(R.color.urwallet_primary_light))
                binding.ivIcon.imageTintList = ColorStateList.valueOf(context.getColor(R.color.urwallet_primary))
            } else {
                binding.cardIcon.strokeWidth = 1
                binding.cardIcon.strokeColor = context.getColor(R.color.urwallet_divider)
                binding.cardIcon.setCardBackgroundColor(context.getColor(R.color.urwallet_background))
                binding.ivIcon.imageTintList = ColorStateList.valueOf(context.getColor(R.color.urwallet_text_primary))
            }

            binding.root.setOnClickListener {
                setSelectedIcon(iconName)
                onIconSelected(iconName)
            }
        }
    }
}
