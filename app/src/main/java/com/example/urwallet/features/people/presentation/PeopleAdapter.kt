package com.example.urwallet.features.people.presentation

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.urwallet.R
import com.example.urwallet.core.common.Formatters
import com.example.urwallet.databinding.ItemPersonBinding
import com.example.urwallet.features.people.domain.model.PersonFinancialSummary

class PeopleAdapter(
    private val onPersonClick: (PersonFinancialSummary) -> Unit
) : ListAdapter<PersonFinancialSummary, PeopleAdapter.ViewHolder>(PersonDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPersonBinding.inflate(
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
        private val binding: ItemPersonBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(summary: PersonFinancialSummary) {
            val context = binding.root.context
            val person = summary.person

            // Avatar initial
            binding.tvAvatarInitial.text = person.name.trim().firstOrNull()?.uppercase() ?: "؟"

            // Name
            binding.tvPersonName.text = person.name

            // Subtitle: phone & notes
            val details = listOfNotNull(
                person.phoneNumber?.takeIf { it.isNotBlank() },
                person.notes?.takeIf { it.isNotBlank() }
            )
            binding.tvPersonSubtitle.text = if (details.isNotEmpty()) {
                details.joinToString(" • ")
            } else {
                context.getString(R.string.cat_other)
            }

            // Debt Pill
            when {
                summary.totalOwedToMe > 0.001 -> {
                    binding.tvDebtBadge.text = context.getString(
                        R.string.net_obligation_owed_to_me_summary,
                        Formatters.formatCurrency(summary.totalOwedToMe)
                    )
                    binding.tvDebtBadge.setBackgroundResource(R.drawable.bg_pill_success_light)
                    binding.tvDebtBadge.setTextColor(ContextCompat.getColor(context, R.color.urwallet_income))
                }
                summary.totalIOwe > 0.001 -> {
                    binding.tvDebtBadge.text = context.getString(
                        R.string.net_obligation_i_owe_summary,
                        Formatters.formatCurrency(summary.totalIOwe)
                    )
                    binding.tvDebtBadge.setBackgroundResource(R.drawable.bg_pill_expense)
                    binding.tvDebtBadge.setTextColor(ContextCompat.getColor(context, R.color.urwallet_expense))
                }
                else -> {
                    binding.tvDebtBadge.text = "الحساب خالص"
                    binding.tvDebtBadge.setBackgroundResource(R.drawable.bg_tag_pill)
                    binding.tvDebtBadge.setTextColor(ContextCompat.getColor(context, R.color.urwallet_text_secondary))
                }
            }

            // Net Flow
            val flowFormatted = Formatters.formatCurrency(kotlin.math.abs(summary.netTransactionFlow))
            binding.tvNetFlow.text = when {
                summary.netTransactionFlow > 0.001 -> "+ $flowFormatted"
                summary.netTransactionFlow < -0.001 -> "- $flowFormatted"
                else -> "0 ج.م"
            }

            binding.root.setOnClickListener {
                onPersonClick(summary)
            }
        }
    }

    object PersonDiffCallback : DiffUtil.ItemCallback<PersonFinancialSummary>() {
        override fun areItemsTheSame(
            oldItem: PersonFinancialSummary,
            newItem: PersonFinancialSummary
        ): Boolean = oldItem.person.id == newItem.person.id

        override fun areContentsTheSame(
            oldItem: PersonFinancialSummary,
            newItem: PersonFinancialSummary
        ): Boolean = oldItem == newItem
    }
}
