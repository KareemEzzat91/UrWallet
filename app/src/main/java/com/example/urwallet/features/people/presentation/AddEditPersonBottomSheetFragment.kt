package com.example.urwallet.features.people.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import com.example.urwallet.R
import com.example.urwallet.databinding.BottomSheetAddEditPersonBinding
import com.example.urwallet.features.people.domain.model.Person
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AddEditPersonBottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: BottomSheetAddEditPersonBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PeopleViewModel by activityViewModels()

    private var personId: Long = 0L
    private var existingPerson: Person? = null

    companion object {
        const val TAG = "AddEditPersonBottomSheet"
        private const val ARG_PERSON_ID = "arg_person_id"
        private const val ARG_NAME = "arg_name"
        private const val ARG_PHONE = "arg_phone"
        private const val ARG_RELATIONSHIP = "arg_relationship"
        private const val ARG_NOTES = "arg_notes"

        fun newInstance(person: Person? = null): AddEditPersonBottomSheetFragment {
            val fragment = AddEditPersonBottomSheetFragment()
            if (person != null) {
                val args = Bundle().apply {
                    putLong(ARG_PERSON_ID, person.id)
                    putString(ARG_NAME, person.name)
                    putString(ARG_PHONE, person.phoneNumber)
                    putString(ARG_NOTES, person.notes)
                }
                fragment.arguments = args
            }
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            personId = it.getLong(ARG_PERSON_ID, 0L)
            if (personId > 0L) {
                existingPerson = Person(
                    id = personId,
                    name = it.getString(ARG_NAME, ""),
                    phoneNumber = it.getString(ARG_PHONE),
                    notes = it.getString(ARG_NOTES)
                )
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetAddEditPersonBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (existingPerson != null) {
            binding.tvSheetTitle.text = getString(R.string.edit_person_title)
            binding.etPersonName.setText(existingPerson?.name)
            binding.etPersonPhone.setText(existingPerson?.phoneNumber)
            binding.etPersonNotes.setText(existingPerson?.notes)
        } else {
            binding.tvSheetTitle.text = getString(R.string.add_person_title)
        }

        binding.btnSavePerson.setOnClickListener {
            val name = binding.etPersonName.text?.toString()?.trim().orEmpty()
            if (name.isBlank()) {
                binding.tilPersonName.error = getString(R.string.error_person_name_empty)
                return@setOnClickListener
            }
            binding.tilPersonName.error = null

            val phone = binding.etPersonPhone.text?.toString()?.trim()
            val relationship = binding.etPersonRelationship.text?.toString()?.trim()
            val notes = binding.etPersonNotes.text?.toString()?.trim()

            val combinedNotes = listOfNotNull(
                relationship?.takeIf { it.isNotBlank() }?.let { "العلاقة: $it" },
                notes?.takeIf { it.isNotBlank() }
            ).joinToString(" • ").ifBlank { null }

            if (existingPerson != null) {
                val updated = existingPerson!!.copy(
                    name = name,
                    phoneNumber = phone?.takeIf { it.isNotBlank() },
                    notes = combinedNotes
                )
                viewModel.updatePerson(updated)
            } else {
                viewModel.createPerson(
                    name = name,
                    phoneNumber = phone,
                    notes = combinedNotes
                )
            }
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
