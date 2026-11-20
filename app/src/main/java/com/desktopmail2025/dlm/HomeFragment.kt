package com.desktopmail2025.dlm

import android.app.AlertDialog
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
        val add = com.google.android.material.button.MaterialButton(requireContext()).apply {
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
                    val empty = TextView(requireContext()).apply {
                        text = "Nothing here yet. Tap the button below to add your first item."
                        textSize = 15f
                        setTextColor(0xFF64748B.toInt())
                        setPadding(4, 24, 4, 24)
                    }
                    list.addView(empty)
                } else {
                    entries.forEach { entry -> list.addView(entryCard(entry)) }
                }
            }
        }
        if (section == "MONEY") {
            viewLifecycleOwner.lifecycleScope.launch {
                dao.totalMoney().collect { total ->
                    summary.text = "Recorded total: " + String.format("%.2f", total)
                }
            }
        } else {
            summary.text = when (section) {
                "STUDY" -> "Keep your learning tasks in one place."
                else -> "Track students, lessons and payments."
            }
        }
    }

    private fun entryCard(entry: DlmEntry): View {
        val card = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 14, 16, 14)
            setBackgroundColor(0xFFFFFFFF.toInt())
            elevation = 2f
        }
        val heading = TextView(requireContext()).apply {
            text = entry.title
            textSize = 17f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(0xFF172554.toInt())
        }
        val detail = TextView(requireContext()).apply {
            val date = DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(entry.createdAt))
            text = buildString {
                if (entry.details.isNotBlank()) append(entry.details + "  •  ")
                if (section == "MONEY") append(String.format("%.2f", entry.amount) + "  •  ")
                append(date)
            }
            textSize = 13f
            setTextColor(0xFF64748B.toInt())
        }
        card.addView(heading)
        card.addView(detail)
        card.setOnLongClickListener {
            AlertDialog.Builder(requireContext()).setMessage("Delete this item?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete") { _, _ ->
                    viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) { dao.delete(entry.id) }
                }.show()
            true
        }
        val params = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 10 }
        return card.also { it.layoutParams = params }
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
        val amount = if (section == "MONEY" || section == "TUITION") EditText(requireContext()).apply {
            hint = if (section == "MONEY") "Amount" else "Fee / payment amount"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        } else null
        box.addView(title)
        box.addView(detail)
        amount?.let { box.addView(it) }
        AlertDialog.Builder(requireContext())
            .setTitle("Add new item")
            .setView(box)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save") { _, _ ->
                val name = title.text.toString().trim()
                if (name.isNotEmpty()) {
                    val value = amount?.text?.toString()?.toDoubleOrNull() ?: 0.0
                    viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                        dao.insert(DlmEntry(section = section, title = name, details = detail.text.toString().trim(), amount = value))
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
