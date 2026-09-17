package com.example.urwallet.features.more.presentation.categories

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.example.urwallet.databinding.ItemCategoryColorPickerBinding

class ColorPickerAdapter(
    private val colors: List<String>,
    private var selectedColor: String,
    private val onColorSelected: (String) -> Unit
) : RecyclerView.Adapter<ColorPickerAdapter.ColorViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColorViewHolder {
        val binding = ItemCategoryColorPickerBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ColorViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ColorViewHolder, position: Int) {
        holder.bind(colors[position])
    }

    override fun getItemCount(): Int = colors.size

    fun getSelectedColor(): String = selectedColor

    fun setSelectedColor(color: String) {
        val prevIndex = colors.indexOf(selectedColor)
        selectedColor = color
        val newIndex = colors.indexOf(selectedColor)
        if (prevIndex != -1) notifyItemChanged(prevIndex)
        if (newIndex != -1) notifyItemChanged(newIndex)
    }

    inner class ColorViewHolder(
        private val binding: ItemCategoryColorPickerBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(hexColor: String) {
            val isSelected = hexColor.equals(selectedColor, ignoreCase = true)
            val parsedColor = try {
                Color.parseColor(hexColor)
            } catch (_: Exception) {
                Color.parseColor("#78909C")
            }

            binding.cardColor.setCardBackgroundColor(parsedColor)
            binding.ivSelectedCheck.isVisible = isSelected

            if (isSelected) {
                binding.cardColor.strokeWidth = 4
                binding.cardColor.strokeColor = Color.WHITE
            } else {
                binding.cardColor.strokeWidth = 0
            }

            binding.root.setOnClickListener {
                setSelectedColor(hexColor)
                onColorSelected(hexColor)
            }
        }
    }
}
