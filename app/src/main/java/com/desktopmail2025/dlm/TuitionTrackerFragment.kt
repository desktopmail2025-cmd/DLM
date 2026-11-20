package com.desktopmail2025.dlm

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import java.text.DateFormat

class TuitionTrackerFragment : Fragment() {
    private val dao by lazy { DlmDatabase.get(requireContext()).dao() }
    private lateinit var root: LinearLayout
    private var profiles=emptyList<TuitionProfile>()
    private var selected:TuitionProfile?=null
    private var days=emptyList<TuitionDay>()
    private var month=Calendar.getInstance().apply{set(Calendar.DAY_OF_MONTH,1);set(Calendar.HOUR_OF_DAY,0);set(Calendar.MINUTE,0);set(Calendar.SECOND,0);set(Calendar.MILLISECOND,0)}
    override fun onCreateView(i:android.view.LayoutInflater,p:ViewGroup?,s:Bundle?):View{
        root=LinearLayout(requireContext()).apply{orientation=LinearLayout.VERTICAL;setPadding(16,16,16,12);setBackgroundColor(0xFFF4F7FC.toInt())}
        return ScrollView(requireContext()).apply{isFillViewport=true;addView(root)}
    }
    override fun onViewCreated(v:View,s:Bundle?){viewLifecycleOwner.lifecycleScope.launch{dao.tuitionProfiles().collect{profiles=it;render()}}}
    private fun bounds():Pair<Long,Long>{val a=(month.clone() as Calendar);val b=(month.clone() as Calendar).apply{set(Calendar.DAY_OF_MONTH,getActualMaximum(Calendar.DAY_OF_MONTH));set(Calendar.HOUR_OF_DAY,23);set(Calendar.MINUTE,59);set(Calendar.SECOND,59);set(Calendar.MILLISECOND,999)};return a.timeInMillis to b.timeInMillis}
    private fun render(){
        if(!isAdded)return
        root.removeAllViews()
        root.addView(label(if(selected==null)"Tuition tracker" else selected!!.name,23,true))
        root.addView(label("Manage students, monthly fees, attendance and daily notes.",13,false))
        if(selected==null){
            root.addView(button("＋ Add tuition / student"){editProfile()})
            if(profiles.isEmpty())root.addView(label("No tuition records yet. Add a student or tuition class.",14,false))
            profiles.forEach{p->
                val c=card();c.addView(label(p.name,18,true));if(p.teacher.isNotBlank())c.addView(label("Teacher: "+p.teacher,13,false));c.addView(label("Monthly fee: "+money(p.monthlyFee),14,false))
                c.addView(button("Open attendance"){selected=p;observeMonth();render()})
                c.addView(button("Edit"){editProfile(p)})
                c.addView(button("Delete"){MaterialAlertDialogBuilder(requireContext()).setMessage("Delete this tuition and all attendance history?").setNegativeButton("Cancel",null).setPositiveButton("Delete"){_,_->viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO){dao.deleteTuitionDays(p.id);dao.deleteTuition(p.id)}}.show()})
                root.addView(c)
            }
        }else{
            val p=selected!!
            root.addView(button("← All tuition"){selected=null;render()})
            val nav=LinearLayout(requireContext()).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL}
            nav.addView(button("‹"){month.add(Calendar.MONTH,-1);observeMonth()})
            nav.addView(label(month.getDisplayName(Calendar.MONTH,Calendar.LONG,java.util.Locale.getDefault())+" "+month.get(Calendar.YEAR),16,true),LinearLayout.LayoutParams(0,-2,1f))
            nav.addView(button("›"){month.add(Calendar.MONTH,1);observeMonth()})
            root.addView(nav)
            val attended=days.count{it.attendance==1};val missed=days.count{it.attendance==2};val paid=days.count{it.paid}
            root.addView(label("Attended: $attended  •  Missed: $missed",14,false))
            root.addView(label("Monthly fee: "+money(p.monthlyFee)+"  •  Fee status: "+if(paid>0)"Paid" else "Due",14,true))
            root.addView(button(if(paid>0)"Mark fee unpaid" else "Mark monthly fee paid"){togglePaid(p)})
            val grid=GridLayout(requireContext()).apply{columnCount=7}
            listOf("S","M","T","W","T","F","S").forEach{grid.addView(TextView(requireContext()).apply{text=it;gravity=Gravity.CENTER;setTextColor(0xFF64748B.toInt())},GridLayout.LayoutParams().apply{width=0;columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f)})}
            val cal=month.clone() as Calendar
            val offset=cal.get(Calendar.DAY_OF_WEEK)-1
            repeat(offset){grid.addView(TextView(requireContext()),GridLayout.LayoutParams().apply{width=0;columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f)})}
            val max=cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            for(d in 1..max){
                cal.set(Calendar.DAY_OF_MONTH,d);val stamp=cal.timeInMillis
                val entry=days.firstOrNull{sameDay(it.day,stamp)}
                val dayButton=MaterialButton(requireContext()).apply{
                    text=d.toString()+(when(entry?.attendance){1->" ✓";2->" •";else->""})
                    textSize=11f;insetTop=2;insetBottom=2;minHeight=42
                    setOnClickListener{editDay(p,stamp,entry)}
                }
                grid.addView(dayButton,GridLayout.LayoutParams().apply{width=0;height=48;columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f);setMargins(2,2,2,2)})
            }
            root.addView(grid)
            root.addView(label("Daily notes & attendance",18,true))
            days.sortedByDescending{it.day}.forEach{d->
                val c=card();c.addView(label(DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(d.day))+" • "+when(d.attendance){1->"Attended";2->"Missed";else->"Not marked"},14,true))
                if(d.note.isNotBlank())c.addView(label(d.note,13,false))
                c.addView(button("Edit day"){editDay(p,d.day,d)})
                root.addView(c)
            }
        }
    }
    private fun observeMonth(){val p=selected?:return;val (from,to)=bounds();viewLifecycleOwner.lifecycleScope.launch{dao.tuitionDays(p.id,from,to).collect{days=it;render()}}}
    private fun editProfile(old:TuitionProfile?=null){
        val box=LinearLayout(requireContext()).apply{orientation=LinearLayout.VERTICAL;setPadding(20,4,20,0)}
        val n=EditText(requireContext()).apply{hint="Student / tuition name";setText(old?.name.orEmpty())}
        val teacher=EditText(requireContext()).apply{hint="Teacher (optional)";setText(old?.teacher.orEmpty())}
        val fee=EditText(requireContext()).apply{hint="Monthly fee";inputType=android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL;setText(if(old==null)"" else old.monthlyFee.toString())}
        box.addView(n);box.addView(teacher);box.addView(fee)
        MaterialAlertDialogBuilder(requireContext()).setTitle(if(old==null)"New tuition" else "Edit tuition").setView(box).setNegativeButton("Cancel",null).setPositiveButton("Save"){_,_->val name=n.text.toString().trim();val amount=fee.text.toString().toDoubleOrNull()?:0.0;if(name.isNotEmpty()&&amount>=0)viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO){if(old==null)dao.addTuition(TuitionProfile(name=name,teacher=teacher.text.toString().trim(),monthlyFee=amount))else dao.updateTuition(old.copy(name=name,teacher=teacher.text.toString().trim(),monthlyFee=amount))}}.show()
    }
    private fun editDay(p:TuitionProfile,stamp:Long,old:TuitionDay?){
        val box=LinearLayout(requireContext()).apply{orientation=LinearLayout.VERTICAL;setPadding(20,4,20,0)}
        val status=Spinner(requireContext()).apply{adapter=ArrayAdapter(requireContext(),android.R.layout.simple_spinner_dropdown_item,listOf("Not marked","Attended","Missed"));setSelection(old?.attendance?:0)}
        val note=EditText(requireContext()).apply{hint="What was taught / homework / reminder";minLines=3;setText(old?.note.orEmpty())}
        val paid=CheckBox(requireContext()).apply{text="Monthly fee paid";isChecked=old?.paid?:false}
        box.addView(status);box.addView(note);box.addView(paid)
        MaterialAlertDialogBuilder(requireContext()).setTitle(DateFormat.getDateInstance(DateFormat.FULL).format(Date(stamp))).setView(box).setNegativeButton("Cancel",null).setPositiveButton("Save"){_,_->viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO){dao.saveTuitionDay((old?:TuitionDay(tuitionId=p.id,day=stamp)).copy(attendance=status.selectedItemPosition,note=note.text.toString().trim(),paid=paid.isChecked))}}.setNeutralButton("Clear"){_,_->if(old!=null)viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO){dao.deleteTuitionDay(old.id)}}.show()
    }
    private fun togglePaid(p:TuitionProfile){val (from,to)=bounds();viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO){val existing=dao.tuitionDay(p.id,from);dao.saveTuitionDay((existing?:TuitionDay(tuitionId=p.id,day=from)).copy(paid=existing?.paid!=true))}}
    private fun sameDay(a:Long,b:Long):Boolean{val x=Calendar.getInstance().apply{timeInMillis=a};val y=Calendar.getInstance().apply{timeInMillis=b};return x.get(Calendar.YEAR)==y.get(Calendar.YEAR)&&x.get(Calendar.DAY_OF_YEAR)==y.get(Calendar.DAY_OF_YEAR)}
    private fun money(v:Double)=String.format(java.util.Locale.getDefault(),"%.2f",v)
    private fun card()=LinearLayout(requireContext()).apply{orientation=LinearLayout.VERTICAL;setPadding(14,12,14,10);background=android.graphics.drawable.GradientDrawable().apply{setColor(-1);cornerRadius=20f};elevation=2f;layoutParams=LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=12}}
    private fun label(s:String,size:Float,bold:Boolean)=TextView(requireContext()).apply{text=s;textSize=size;setTextColor(0xFF102A43.toInt());if(bold)typeface=android.graphics.Typeface.DEFAULT_BOLD;setPadding(4,5,4,5)}
    private fun button(s:String,run:()->Unit)=MaterialButton(requireContext()).apply{text=s;setOnClickListener{run()}}
}
