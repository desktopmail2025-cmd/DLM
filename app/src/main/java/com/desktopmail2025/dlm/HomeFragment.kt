package com.desktopmail2025.dlm

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

class HomeFragment : Fragment() {
    private val section: String get() = requireArguments().getString(ARG_SECTION) ?: "MONEY"
    private lateinit var list: LinearLayout
    private lateinit var summary: TextView
    private val dao by lazy { DlmDatabase.get(requireContext()).dao() }

    override fun onCreateView(inflater: android.view.LayoutInflater, parent: ViewGroup?, state: Bundle?): View {
        val root = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 20, 20, 12)
        }
        val title = TextView(requireContext()).apply {
            text = when (section) { "STUDY" -> "Study planner"; "TUITION" -> "Tuition tracker"; else -> "Money manager" }
            textSize = 23f
            setTextColor(0xFF172554.toInt())
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        summary = TextView(requireContext()).apply {
            textSize = 14f
            setTextColor(0xFF526078.toInt())
            setPadding(0, 8, 0, 18)
        }
        list = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL }
        val scroll = android.widget.ScrollView(requireContext()).apply { addView(list) }
        val add = MaterialButton(requireContext()).apply {
            text = when (section) { "STUDY" -> "Add study task"; "TUITION" -> "Add tuition record"; else -> "Add transaction" }
            setOnClickListener { showAddDialog() }
        }
        root.addView(title)
        root.addView(summary)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(add, LinearLayout.LayoutParams(-1, -2))
        return root
    }

    override fun onViewCreated(view: View, state: Bundle?) {
        super.onViewCreated(view, state)
        viewLifecycleOwner.lifecycleScope.launch {
            dao.entries(section).collect { entries ->
                list.removeAllViews()
                if (entries.isEmpty()) {
                    list.addView(TextView(requireContext()).apply {
                        text = "Nothing here yet. Tap the button below to add your first item."
                        textSize = 15f
                        setTextColor(0xFF64748B.toInt())
                        setPadding(4, 24, 4, 24)
                    })
                } else entries.forEach { list.addView(entryCard(it)) }
            }
        }
        when (section) {
            "MONEY" -> viewLifecycleOwner.lifecycleScope.launch {
                dao.moneyBalance().collect { summary.text = "Current balance: " + String.format("%.2f", it) }
            }
            "TUITION" -> viewLifecycleOwner.lifecycleScope.launch {
                kotlinx.coroutines.flow.combine(dao.tuitionFees(), dao.tuitionPaid()) { fees, paid -> fees to paid }
                    .collect { (fees, paid) -> summary.text = "Total fees: %.2f   •   Paid: %.2f   •   Due: %.2f".format(fees, paid, fees - paid) }
            }
            else -> summary.text = "Tap a task to mark it complete. Long-press an item to delete it."
        }
    }

    private fun entryCard(entry: DlmEntry): View {
        val card = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 14, 16, 12)
            setBackgroundColor(0xFFFFFFFF.toInt())
            elevation = 2f
        }
        val heading = TextView(requireContext()).apply {
            text = (if (entry.completed) "✓  " else "") + entry.title
            textSize = 17f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(if (entry.completed) 0xFF16A34A.toInt() else 0xFF172554.toInt())
        }
        val date = DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(entry.createdAt))
        val detail = TextView(requireContext()).apply {
            text = buildString {
                if (entry.details.isNotBlank()) append(entry.details + "  •  ")
                when (entry.type) {
                    "INCOME" -> append("Income +%.2f".format(entry.amount))
                    "EXPENSE" -> append("Expense -%.2f".format(entry.amount))
                    "FEE" -> append("Fee %.2f".format(entry.amount))
                    "PAID" -> append("Paid %.2f".format(entry.amount))
                }
                append("  •  " + date)
            }
            textSize = 13f
            setTextColor(0xFF64748B.toInt())
        }
        card.addView(heading)
        card.addView(detail)
        if (section == "STUDY") {
            val done = MaterialButton(requireContext()).apply {
                text = if (entry.completed) "Mark as pending" else "Mark complete"
                setOnClickListener {
                    viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) { dao.setCompleted(entry.id, !entry.completed) }
                }
            }
            card.addView(done)
        }
        card.setOnLongClickListener {
            AlertDialog.Builder(requireContext()).setMessage("Delete this item?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete") { _, _ ->
                    viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) { dao.delete(entry.id) }
                }.show()
            true
        }
        card.layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 10 }
        return card
    }

    private fun showAddDialog() {
        val box = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(22, 8, 22, 0)
        }
        val title = EditText(requireContext()).apply {
            hint = when (section) { "STUDY" -> "Task title"; "TUITION" -> "Student / tuition title"; else -> "Transaction title" }
        }
        val detail = EditText(requireContext()).apply { hint = "Note (optional)" }
        val amount = if (section != "STUDY") EditText(requireContext()).apply {
            hint = if (section == "MONEY") "Amount" else "Fee / payment amount"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        } else null
        val type = if (section != "STUDY") Spinner(requireContext()).apply {
            val choices = if (section == "MONEY") listOf("Expense", "Income") else listOf("Fee", "Paid")
            adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, choices)
        } else null
        box.addView(title)
        box.addView(detail)
        type?.let { box.addView(it) }
        amount?.let { box.addView(it) }
        AlertDialog.Builder(requireContext())
            .setTitle("Add new item")
            .setView(box)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save") { _, _ ->
                val name = title.text.toString().trim()
                if (name.isNotEmpty()) {
                    val value = amount?.text?.toString()?.toDoubleOrNull() ?: 0.0
                    val selectedType = when (type?.selectedItem?.toString()) {
                        "Income" -> "INCOME"; "Fee" -> "FEE"; "Paid" -> "PAID"; else -> if (section == "MONEY") "EXPENSE" else "GENERAL"
                    }
                    viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                        dao.insert(DlmEntry(section = section, title = name, details = detail.text.toString().trim(), amount = value, type = selectedType))
                    }
                }
            }.show()
    }

    companion object {
        private const val ARG_SECTION = "section"
        fun newInstance(section: String) = HomeFragment().apply {
            arguments = Bundle().apply { putString(ARG_SECTION, section) }
        }
    }
}
