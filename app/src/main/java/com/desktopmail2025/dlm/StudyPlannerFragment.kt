package com.desktopmail2025.dlm

import android.os.Bundle
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Date
import java.text.DateFormat

class StudyPlannerFragment : Fragment() {
    private val dao by lazy { DlmDatabase.get(requireContext()).dao() }
    private lateinit var root: LinearLayout
    private var subjects = emptyList<StudySubject>()
    private var selected: StudySubject? = null
    private var topics = emptyList<StudyTopic>()
    private var notes = emptyList<StudyNote>()
    private var exportBytes:ByteArray?=null
    private val saveReport=registerForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")){uri:Uri?->if(uri!=null)exportBytes?.let{b->requireContext().contentResolver.openOutputStream(uri)?.use{it.write(b)}};exportBytes=null}

    override fun onCreateView(i: android.view.LayoutInflater, p: ViewGroup?, s: Bundle?): View {
        root = LinearLayout(requireContext()).apply { orientation=LinearLayout.VERTICAL; setPadding(18,18,18,12); setBackgroundColor(0xFFF4F7FC.toInt()) }
        return ScrollView(requireContext()).apply { isFillViewport=true; addView(root) }
    }
    override fun onViewCreated(v: View, s: Bundle?) {
        viewLifecycleOwner.lifecycleScope.launch { dao.subjects().collect { subjects=it; render() } }
    }
    private fun render() {
        if (!isAdded) return
        root.removeAllViews()
        root.addView(label(if(selected==null) "Study planner" else selected!!.name,23,true))
        root.addView(label("Subjects • Topics • Completion • Revision notes",13,false))
        if(selected==null) {
            root.addView(button("＋ Add subject"){ editSubject() })
            if(subjects.isEmpty()) root.addView(label("No subjects yet. Add your first subject.",14,false))
            subjects.forEach { s ->
                val c=card(); c.addView(label(s.name,18,true))
                c.addView(label("Tap Open to manage topics and revision notes.",13,false))
                c.addView(button("Open"){ selected=s; observe(s.id); render() })
                c.addView(button("Edit"){ editSubject(s) })
                c.addView(button("Delete"){ MaterialAlertDialogBuilder(requireContext()).setMessage("Delete this subject and all its study data?").setNegativeButton("Cancel",null).setPositiveButton("Delete"){_,_-> viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO){ dao.deleteSubjectTopics(s.id); dao.deleteSubjectNotes(s.id); dao.deleteSubject(s.id) } }.show() })
                root.addView(c)
            }
        } else {
            root.addView(button("← All subjects"){ selected=null; render() })
            root.addView(button("＋ Add topic"){ editTopic() })
            root.addView(button("Export study report (PDF / PNG)"){exportReport()})
            val total=topics.size; val done=topics.count{it.completed}
            root.addView(label(done.toString()+" of "+total+" topics completed",14,false))
            if(topics.isEmpty()) root.addView(label("No topics yet. Add topics and mark them complete.",14,false))
            topics.filter{it.parentId==null}.forEach { t ->
                val c=card()
                val row=LinearLayout(requireContext()).apply{orientation=LinearLayout.HORIZONTAL;gravity=android.view.Gravity.CENTER_VERTICAL}
                val cb=CheckBox(requireContext()).apply{text=t.name;isChecked=t.completed;setOnCheckedChangeListener{_,v->viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO){dao.updateTopic(t.copy(completed=v))}}}
                row.addView(cb,LinearLayout.LayoutParams(0,-2,1f)); c.addView(row)
                if(t.note.isNotBlank()) c.addView(label(t.note,13,false))
                topics.filter{it.parentId==t.id}.forEach { child ->
                    val cr=LinearLayout(requireContext()).apply{orientation=LinearLayout.HORIZONTAL;gravity=android.view.Gravity.CENTER_VERTICAL}
                    cr.addView(CheckBox(requireContext()).apply{text=child.name;isChecked=child.completed;setOnCheckedChangeListener{_,v->viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO){dao.updateTopic(child.copy(completed=v))}}},LinearLayout.LayoutParams(0,-2,1f))
                    cr.addView(button("Edit"){editTopic(child)})
                    c.addView(cr)
                }
                c.addView(button("＋ Add subtopic"){editTopic(parentId=t.id)})
                c.addView(button("Edit topic"){editTopic(t)})
                c.addView(button("Delete topic"){MaterialAlertDialogBuilder(requireContext()).setMessage("Delete topic and its subtopics?").setNegativeButton("Cancel",null).setPositiveButton("Delete"){_,_->viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO){topics.filter{it.parentId==t.id}.forEach{dao.deleteTopic(it.id)};dao.deleteTopic(t.id)}}.show()})
                root.addView(c)
            }
            root.addView(label("Revision notes",18,true))
            root.addView(button("＋ Add note"){editNote()})
            notes.forEach { n ->
                val c=card(); c.addView(label(n.note,14,false))
                c.addView(label(DateFormat.getDateInstance().format(Date(n.noteDate)),12,false))
                c.addView(button("Delete note"){MaterialAlertDialogBuilder(requireContext()).setMessage("Delete this note?").setNegativeButton("Cancel",null).setPositiveButton("Delete"){_,_->viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO){dao.deleteStudyNote(n.id)}}.show()})
                root.addView(c)
            }
        }
    }
    private fun observe(id:Long) {
        viewLifecycleOwner.lifecycleScope.launch { dao.topics(id).collect{topics=it;render()} }
        viewLifecycleOwner.lifecycleScope.launch { dao.studyNotes(id).collect{notes=it;render()} }
    }
    private fun editSubject(old:StudySubject?=null) {
        val e=EditText(requireContext()).apply{hint="Subject name";setText(old?.name.orEmpty())}
        MaterialAlertDialogBuilder(requireContext()).setTitle(if(old==null)"New subject" else "Edit subject").setView(e).setNegativeButton("Cancel",null).setPositiveButton("Save"){_,_->val n=e.text.toString().trim();if(n.isNotEmpty())viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO){if(old==null)dao.addSubject(StudySubject(name=n))else dao.updateSubject(old.copy(name=n))}}.show()
    }
    private fun editTopic(old:StudyTopic?=null,parentId:Long?=null) {
        val box=LinearLayout(requireContext()).apply{orientation=LinearLayout.VERTICAL;setPadding(20,4,20,0)}
        val n=EditText(requireContext()).apply{hint="Topic name";setText(old?.name.orEmpty())}
        val d=EditText(requireContext()).apply{hint="Note (optional)";setText(old?.note.orEmpty())}
        box.addView(n);box.addView(d)
        MaterialAlertDialogBuilder(requireContext()).setTitle(if(old==null)"New topic" else "Edit topic").setView(box).setNegativeButton("Cancel",null).setPositiveButton("Save"){_,_->val name=n.text.toString().trim();if(name.isNotEmpty())viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO){if(old==null)dao.addTopic(StudyTopic(subjectId=selected!!.id,parentId=parentId,name=name,note=d.text.toString().trim()))else dao.updateTopic(old.copy(name=name,note=d.text.toString().trim()))}}.show()
    }
    private fun editNote() {
        val e=EditText(requireContext()).apply{hint="Revision note";minLines=4}
        MaterialAlertDialogBuilder(requireContext()).setTitle("New revision note").setView(e).setNegativeButton("Cancel",null).setPositiveButton("Save"){_,_->val n=e.text.toString().trim();if(n.isNotEmpty())viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO){dao.addStudyNote(StudyNote(subjectId=selected!!.id,note=n))}}.show()
    }
    private fun exportReport(){
        val subject=selected?:return
        val lines=buildList{
            add("DLM STUDY REPORT");add(subject.name);add("Generated: "+DateFormat.getDateTimeInstance().format(Date()));add("")
            topics.filter{it.parentId==null}.forEach{t->add((if(t.completed)"[DONE] " else "[PENDING] ")+t.name);if(t.note.isNotBlank())add("  "+t.note);topics.filter{it.parentId==t.id}.forEach{add("  "+(if(it.completed)"[DONE] " else "[PENDING] ")+it.name)}}
            add("");add("REVISION NOTES");notes.forEach{add(DateFormat.getDateInstance().format(Date(it.noteDate)));add(it.note);add("")}
            add("Generated by DLM")
        }
        MaterialAlertDialogBuilder(requireContext()).setTitle("Export format").setItems(arrayOf("PDF document","High-quality PNG image")){_,choice->
            exportBytes=if(choice==0)DlmExport.pdf(lines)else DlmExport.png(lines)
            saveReport.launch("DLM_Study_"+System.currentTimeMillis()+"."+(if(choice==0)"pdf" else "png"))
        }.show()
    }
    private fun card()=LinearLayout(requireContext()).apply{orientation=LinearLayout.VERTICAL;setPadding(14,12,14,10);background=android.graphics.drawable.GradientDrawable().apply{setColor(-1);cornerRadius=22f};elevation=2f;layoutParams=LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=12}}
    private fun label(s:String,size:Float,bold:Boolean)=TextView(requireContext()).apply{text=s;textSize=size;setTextColor(0xFF102A43.toInt());if(bold)typeface=android.graphics.Typeface.DEFAULT_BOLD;setPadding(4,6,4,6)}
    private fun button(s:String,run:()->Unit)=MaterialButton(requireContext()).apply{text=s;setOnClickListener{run()}}
}
