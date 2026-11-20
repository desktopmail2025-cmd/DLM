package com.desktopmail2025.dlm

import android.app.DatePickerDialog
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class HomeFragment : Fragment() {
    private val section: String get() = requireArguments().getString(ARG_SECTION) ?: "MONEY"
    private lateinit var list: LinearLayout
    private lateinit var summary: TextView
    private val dao by lazy { DlmDatabase.get(requireContext()).dao() }
    private var exportBytes: ByteArray? = null
    private var exportMime = "application/pdf"

    private val createExport = registerForActivityResult(ActivityResultContracts.CreateDocument()) { uri: Uri? ->
        if (uri != null) {
            val bytes = exportBytes
            if (bytes != null) {
                runCatching {
                    requireContext().contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
                }.onSuccess {
                    MaterialAlertDialogBuilder(requireContext()).setMessage("Export saved successfully.")
                        .setPositiveButton("OK", null).show()
                }.onFailure {
                    MaterialAlertDialogBuilder(requireContext()).setMessage("Could not save the export: ${it.localizedMessage}")
                        .setPositiveButton("OK", null).show()
                }
            }
        }
        exportBytes = null
    }

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
            setPadding(0, 8, 0, 10)
        }
        list = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL }
        val scroll = android.widget.ScrollView(requireContext()).apply { addView(list) }
        val export = MaterialButton(requireContext()).apply {
            text = "Export PDF / Image"
            icon = androidx.appcompat.content.res.AppCompatResources.getDrawable(requireContext(), android.R.drawable.ic_menu_save)
            setOnClickListener { chooseExportRange() }
        }
        val add = MaterialButton(requireContext()).apply {
            text = when (section) { "STUDY" -> "Add study task"; "TUITION" -> "Add tuition record"; else -> "Add transaction" }
            setOnClickListener { showAddDialog() }
        }
        root.addView(title)
        root.addView(summary)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(export, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 6 })
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
            MaterialAlertDialogBuilder(requireContext()).setMessage("Delete this item?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete") { _, _ ->
                    viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) { dao.delete(entry.id) }
                }.show()
            true
        }
        card.layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 10 }
        return card
    }

    private fun chooseExportRange() {
        val options = arrayOf("Today", "This month", "Choose date range")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Choose export period")
            .setItems(options) { _, which ->
                val now = Calendar.getInstance()
                when (which) {
                    0 -> {
                        now.set(Calendar.HOUR_OF_DAY, 0)
                        now.set(Calendar.MINUTE, 0)
                        now.set(Calendar.SECOND, 0)
                        now.set(Calendar.MILLISECOND, 0)
                        val start = now.timeInMillis
                        exportFor(start, start + 86_399_999L)
                    }
                    1 -> {
                        now.set(Calendar.DAY_OF_MONTH, 1); now.set(Calendar.HOUR_OF_DAY, 0); now.set(Calendar.MINUTE, 0); now.set(Calendar.SECOND, 0)
                        val start = now.timeInMillis
                        now.add(Calendar.MONTH, 1); now.add(Calendar.MILLISECOND, -1)
                        exportFor(start, now.timeInMillis)
                    }
                    else -> pickStartDate()
                }
            }.show()
    }

    private fun pickStartDate() {
        val start = Calendar.getInstance()
        DatePickerDialog(requireContext(), { _, y, m, d ->
            start.set(y, m, d, 0, 0, 0); start.set(Calendar.MILLISECOND, 0)
            pickEndDate(start.timeInMillis)
        }, start.get(Calendar.YEAR), start.get(Calendar.MONTH), start.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun pickEndDate(from: Long) {
        val end = Calendar.getInstance()
        DatePickerDialog(requireContext(), { _, y, m, d ->
            end.set(y, m, d, 23, 59, 59); end.set(Calendar.MILLISECOND, 999)
            if (end.timeInMillis < from) {
                MaterialAlertDialogBuilder(requireContext()).setMessage("End date must not be before start date.")
                    .setPositiveButton("OK", null).show()
            } else exportFor(from, end.timeInMillis)
        }, end.get(Calendar.YEAR), end.get(Calendar.MONTH), end.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun exportFor(from: Long, to: Long) {
        MaterialAlertDialogBuilder(requireContext()).setTitle("Export format")
            .setItems(arrayOf("PDF document", "High-quality PNG image")) { _, choice ->
                viewLifecycleOwner.lifecycleScope.launch {
                    val rows = withContext(Dispatchers.IO) { dao.entriesBetween(section, from, to) }
                    if (rows.isEmpty()) {
                        MaterialAlertDialogBuilder(requireContext()).setMessage("No records found for this date range.")
                            .setPositiveButton("OK", null).show()
                        return@launch
                    }
                    val period = "${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(from))} - ${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(to))}"
                    val result = withContext(Dispatchers.Default) {
                        if (choice == 0) makePdf(rows, period) else makePng(rows, period)
                    }
                    exportBytes = result
                    exportMime = if (choice == 0) "application/pdf" else "image/png"
                    val extension = if (choice == 0) "pdf" else "png"
                    val safeSection = section.lowercase()
                    createExport.launch("DLM_${safeSection}_${System.currentTimeMillis()}.$extension")
                }
            }.show()
    }

    private fun exportLines(rows: List<DlmEntry>, period: String): List<String> {
        val heading = when (section) { "STUDY" -> "DLM • STUDY REPORT"; "TUITION" -> "DLM • TUITION REPORT"; else -> "DLM • MONEY REPORT" }
        return buildList {
            add(heading); add("Period: $period"); add("Generated: ${DateFormat.getDateTimeInstance().format(Date())}"); add("")
            rows.forEachIndexed { index, e ->
                val amount = when (e.type) {
                    "INCOME" -> "Income: +%.2f".format(e.amount)
                    "EXPENSE" -> "Expense: -%.2f".format(e.amount)
                    "FEE" -> "Fee: %.2f".format(e.amount)
                    "PAID" -> "Paid: %.2f".format(e.amount)
                    else -> if (section == "STUDY") if (e.completed) "Completed" else "Pending" else ""
                }
                add("${index + 1}. ${e.title}")
                if (e.details.isNotBlank()) add("   ${e.details}")
                if (amount.isNotBlank()) add("   $amount")
                add("   ${DateFormat.getDateInstance().format(Date(e.createdAt))}")
                add("")
            }
            add("Total records: ${rows.size}")
        }
    }

    private fun makePdf(rows: List<DlmEntry>, period: String): ByteArray {
        val doc = PdfDocument()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.BLACK; textSize = 12f }
        val lines = exportLines(rows, period)
        val pageHeight = 842
        val pageWidth = 595
        val lineHeight = 22
        var pageNumber = 1
        var lineIndex = 0
        while (lineIndex < lines.size) {
            val info = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            val page = doc.startPage(info)
            var y = 54f
            while (lineIndex < lines.size && y < pageHeight - 48) {
                paint.textSize = if (lineIndex == 0) 18f else 12f
                paint.typeface = if (lineIndex == 0) android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.DEFAULT
                page.canvas.drawText(lines[lineIndex].take(85), 42f, y, paint)
                y += lineHeight
                lineIndex++
            }
            doc.finishPage(page)
            pageNumber++
        }
        val out = ByteArrayOutputStream()
        doc.writeTo(out)
        doc.close()
        return out.toByteArray()
    }

    private fun makePng(rows: List<DlmEntry>, period: String): ByteArray {
        val lines = exportLines(rows, period)
        val width = 1800
        val lineHeight = 54
        val height = (lines.size * lineHeight + 180).coerceAtMost(30000)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(android.graphics.Color.WHITE)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(25, 42, 75)
            textSize = 34f
        }
        var y = 90f
        lines.take((height - 100) / lineHeight).forEachIndexed { index, line ->
            paint.textSize = if (index == 0) 48f else 30f
            paint.typeface = if (index == 0) android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.DEFAULT
            canvas.drawText(line.take(105), 72f, y, paint)
            y += lineHeight
        }
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        bitmap.recycle()
        return out.toByteArray()
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
        box.addView(title); box.addView(detail); type?.let { box.addView(it) }; amount?.let { box.addView(it) }
        MaterialAlertDialogBuilder(requireContext()).setTitle("Add new item").setView(box)
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
