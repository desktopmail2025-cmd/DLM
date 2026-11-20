package com.desktopmail2025.dlm

import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.card.MaterialCardView

class MainActivity : AppCompatActivity() {
    private val containerId = View.generateViewId()
    private val moneyId = 1
    private val studyId = 2
    private val tuitionId = 3

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                showExitConfirmation()
            }
        })

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFFF4F7FC.toInt())
        }
        val header = TextView(this).apply {
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
        root.addView(frame, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(nav)
        setContentView(root)
        if (savedInstanceState == null) nav.selectedItemId = moneyId
    }

    private fun showExitConfirmation() {
        val card = MaterialCardView(this).apply {
            radius = 24f
            cardElevation = 0f
            setCardBackgroundColor(0xFFF1F6FF.toInt())
            setContentPadding(24, 22, 24, 22)
        }
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        content.addView(TextView(this).apply {
            text = "Are you sure you want to exit?"
            textSize = 19f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(0xFF102A43.toInt())
        })
        content.addView(TextView(this).apply {
            text = "Your saved DLM data will remain safe on this device."
            textSize = 14f
            setTextColor(0xFF526078.toInt())
            setPadding(0, 8, 0, 0)
        })
        card.addView(content)
        MaterialAlertDialogBuilder(this)
            .setView(card)
            .setNegativeButton("Stay", null)
            .setPositiveButton("Exit") { _, _ -> finish() }
            .show()
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
