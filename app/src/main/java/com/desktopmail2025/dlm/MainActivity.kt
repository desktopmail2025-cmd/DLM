package com.desktopmail2025.dlm

import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
    private val containerId = View.generateViewId()
    private val moneyId = 1
    private val studyId = 2
    private val tuitionId = 3

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setBackgroundColor(0xFFF6F8FC.toInt())
        }
        val header = android.widget.TextView(this).apply {
            text = "DLM"
            textSize = 24f
            setTextColor(0xFFFFFFFF.toInt())
            setPadding(24, 24, 24, 24)
            setBackgroundColor(0xFF03045E.toInt())
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        val frame = FrameLayout(this).apply { id = containerId }
        val nav = BottomNavigationView(this).apply {
            menu.add(0, moneyId, 0, "Money").setIcon(android.R.drawable.ic_menu_info_details)
            menu.add(0, studyId, 1, "Study").setIcon(android.R.drawable.ic_menu_edit)
            menu.add(0, tuitionId, 2, "Tuition").setIcon(android.R.drawable.ic_menu_my_calendar)
            setOnItemSelectedListener { item ->
                showSection(item.itemId)
                true
            }
        }
        root.addView(header)
        root.addView(frame, android.widget.LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(nav)
        setContentView(root)
        if (savedInstanceState == null) {
            nav.selectedItemId = moneyId
        }
    }

    private fun showSection(id: Int) {
        val section = when (id) {
            studyId -> "STUDY"
            tuitionId -> "TUITION"
            else -> "MONEY"
        }
        supportFragmentManager.commit {
            replace(containerId, HomeFragment.newInstance(section))
        }
    }
}
