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
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MainActivity : AppCompatActivity() {
    private val containerId=View.generateViewId()
    private val moneyId=1;private val studyId=2;private val tuitionId=3
    private var currentSectionId=moneyId
    private lateinit var navigation:BottomNavigationView
    private val prefs by lazy{getSharedPreferences("dlm_ui",MODE_PRIVATE)}
    override fun onCreate(savedInstanceState:Bundle?){
        installSplashScreen()
        window.statusBarColor=0xFF03045E.toInt()
        window.navigationBarColor=0xFFF4F7FC.toInt()
        super.onCreate(savedInstanceState)
        onBackPressedDispatcher.addCallback(this,object:OnBackPressedCallback(true){override fun handleOnBackPressed()=showExitConfirmation()})
        val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setBackgroundColor(0xFFF4F7FC.toInt())}
        val header=TextView(this).apply{text="DLM";textSize=24f;setTextColor(-1);setPadding(24,20,24,20);setBackgroundColor(0xFF03045E.toInt());typeface=android.graphics.Typeface.DEFAULT_BOLD}
        val frame=FrameLayout(this).apply{id=containerId;setBackgroundColor(0xFFF4F7FC.toInt())}
        navigation=BottomNavigationView(this).apply{
            menu.add(0,moneyId,0,"Money").setIcon(android.R.drawable.ic_menu_info_details)
            menu.add(0,studyId,1,"Study").setIcon(android.R.drawable.ic_menu_edit)
            menu.add(0,tuitionId,2,"Tuition").setIcon(android.R.drawable.ic_menu_my_calendar)
            setOnItemSelectedListener{item->if(item.itemId==currentSectionId)return@setOnItemSelectedListener true;confirmSectionChange(item.itemId);false}
        }
        root.addView(header);root.addView(frame,LinearLayout.LayoutParams(-1,0,1f));root.addView(navigation);setContentView(root)
        currentSectionId=savedInstanceState?.getInt("current_section")?:prefs.getInt("current_section",moneyId)
        if(currentSectionId !in listOf(moneyId,studyId,tuitionId))currentSectionId=moneyId
        navigation.selectedItemId=currentSectionId;showSection(currentSectionId)
    }
    override fun onSaveInstanceState(outState:Bundle){outState.putInt("current_section",currentSectionId);super.onSaveInstanceState(outState)}
    private fun confirmSectionChange(targetId:Int){
        val name=when(targetId){studyId->"Study";tuitionId->"Tuition";else->"Money"}
        val card=dialogCard("Open $name section?","Your saved records remain safe. Continue to this section?")
        MaterialAlertDialogBuilder(this).setView(card).setNegativeButton("Cancel"){_,_->navigation.menu.findItem(currentSectionId)?.isChecked=true}
            .setPositiveButton("Continue"){_,_->currentSectionId=targetId;prefs.edit().putInt("current_section",targetId).apply();navigation.menu.findItem(targetId)?.isChecked=true;showSection(targetId)}
            .setOnCancelListener{navigation.menu.findItem(currentSectionId)?.isChecked=true}.show()
    }
    private fun dialogCard(title:String,message:String)=MaterialCardView(this).apply{
        radius=24f;cardElevation=0f;setCardBackgroundColor(0xFFF1F6FF.toInt());setContentPadding(24,22,24,22)
        addView(LinearLayout(this@MainActivity).apply{orientation=LinearLayout.VERTICAL
            addView(TextView(this@MainActivity).apply{text=title;textSize=19f;typeface=android.graphics.Typeface.DEFAULT_BOLD;setTextColor(0xFF102A43.toInt())})
            addView(TextView(this@MainActivity).apply{text=message;textSize=14f;setTextColor(0xFF526078.toInt());setPadding(0,8,0,0)})
        })
    }
    private fun showExitConfirmation(){
        MaterialAlertDialogBuilder(this).setView(dialogCard("Are you sure you want to exit?","Your DLM records are saved on this device."))
            .setNegativeButton("Stay",null).setPositiveButton("Exit"){_,_->finish()}.show()
    }
    private fun showSection(id:Int){
        val fragment=when(id){studyId->StudyPlannerFragment();tuitionId->TuitionTrackerFragment();else->HomeFragment.newInstance("MONEY")}
        supportFragmentManager.commit{setReorderingAllowed(true);replace(containerId,fragment)}
    }
}
