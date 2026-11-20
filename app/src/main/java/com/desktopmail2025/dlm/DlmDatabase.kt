package com.desktopmail2025.dlm

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "entries", indices = [Index("section"), Index("createdAt")])
data class DlmEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val section: String,
    val title: String,
    val details: String = "",
    val amount: Double = 0.0,
    val type: String = "GENERAL",
    val completed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val category: String = "Other",
    val pinned: Boolean = false
)

@Entity(tableName = "study_subjects")
data class StudySubject(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val color: Int = 0xFF0077B6.toInt(),
    val createdAt: Long = System.currentTimeMillis(),
    val pinned: Boolean = false
)

@Entity(tableName = "study_topics", indices = [Index("subjectId")])
data class StudyTopic(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subjectId: Long,
    val parentId: Long? = null,
    val name: String,
    val note: String = "",
    val completed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "study_notes", indices = [Index("subjectId")])
data class StudyNote(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subjectId: Long,
    val note: String,
    val noteDate: Long = System.currentTimeMillis()
)

@Entity(tableName = "tuition_profiles")
data class TuitionProfile(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val teacher: String = "",
    val monthlyFee: Double = 0.0,
    val startDate: Long = System.currentTimeMillis(),
    val color: Int = 0xFF52796F.toInt(),
    val pinned: Boolean = false
)

@Entity(tableName = "tuition_days", indices = [Index("tuitionId"), Index("day")])
data class TuitionDay(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tuitionId: Long,
    val day: Long,
    val attendance: Int = 0,
    val note: String = "",
    val paid: Boolean = false
)

@Dao
interface DlmDao {
    @Query("SELECT * FROM entries WHERE section = :section ORDER BY pinned DESC, createdAt DESC")
    fun entries(section: String): Flow<List<DlmEntry>>

    @Query("SELECT * FROM entries WHERE section = :section AND createdAt BETWEEN :from AND :to ORDER BY createdAt ASC")
    suspend fun entriesBetween(section: String, from: Long, to: Long): List<DlmEntry>

    @Insert suspend fun insert(entry: DlmEntry)
    @Update suspend fun update(entry: DlmEntry)
    @Query("DELETE FROM entries WHERE id = :id") suspend fun delete(id: Long)
    @Query("UPDATE entries SET completed = :completed WHERE id = :id") suspend fun setCompleted(id: Long, completed: Boolean)
    @Query("UPDATE entries SET pinned = :pinned WHERE id = :id") suspend fun setPinned(id: Long, pinned: Boolean)

    @Query("SELECT COALESCE(SUM(CASE WHEN type = 'INCOME' THEN amount ELSE -amount END), 0) FROM entries WHERE section = 'MONEY'")
    fun moneyBalance(): Flow<Double>
    @Query("SELECT COALESCE(SUM(amount), 0) FROM entries WHERE section = 'MONEY' AND type = 'INCOME'") fun totalIncome(): Flow<Double>
    @Query("SELECT COALESCE(SUM(amount), 0) FROM entries WHERE section = 'MONEY' AND type = 'EXPENSE'") fun totalExpense(): Flow<Double>
    @Query("SELECT COALESCE(SUM(amount), 0) FROM entries WHERE section = 'TUITION' AND type = 'PAID'") fun tuitionPaid(): Flow<Double>
    @Query("SELECT COALESCE(SUM(amount), 0) FROM entries WHERE section = 'TUITION' AND type = 'FEE'") fun tuitionFees(): Flow<Double>

    @Query("SELECT * FROM study_subjects ORDER BY pinned DESC, name COLLATE NOCASE") fun subjects(): Flow<List<StudySubject>>
    @Insert suspend fun addSubject(subject: StudySubject): Long
    @Update suspend fun updateSubject(subject: StudySubject)
    @Query("DELETE FROM study_subjects WHERE id = :id") suspend fun deleteSubject(id: Long)
    @Query("DELETE FROM study_topics WHERE subjectId = :subjectId") suspend fun deleteSubjectTopics(subjectId: Long)
    @Query("DELETE FROM study_notes WHERE subjectId = :subjectId") suspend fun deleteSubjectNotes(subjectId: Long)
    @Query("SELECT * FROM study_topics WHERE subjectId = :subjectId ORDER BY createdAt") fun topics(subjectId: Long): Flow<List<StudyTopic>>
    @Insert suspend fun addTopic(topic: StudyTopic)
    @Update suspend fun updateTopic(topic: StudyTopic)
    @Query("DELETE FROM study_topics WHERE id = :id") suspend fun deleteTopic(id: Long)
    @Query("SELECT * FROM study_notes WHERE subjectId = :subjectId ORDER BY noteDate DESC") fun studyNotes(subjectId: Long): Flow<List<StudyNote>>
    @Insert suspend fun addStudyNote(note: StudyNote)
    @Query("DELETE FROM study_notes WHERE id = :id") suspend fun deleteStudyNote(id: Long)

    @Query("SELECT * FROM tuition_profiles ORDER BY pinned DESC, name COLLATE NOCASE") fun tuitionProfiles(): Flow<List<TuitionProfile>>
    @Insert suspend fun addTuition(profile: TuitionProfile): Long
    @Update suspend fun updateTuition(profile: TuitionProfile)
    @Query("DELETE FROM tuition_profiles WHERE id = :id") suspend fun deleteTuition(id: Long)
    @Query("SELECT * FROM tuition_days WHERE tuitionId = :tuitionId AND day BETWEEN :from AND :to ORDER BY day") fun tuitionDays(tuitionId: Long, from: Long, to: Long): Flow<List<TuitionDay>>
    @Query("DELETE FROM tuition_days WHERE tuitionId = :tuitionId") suspend fun deleteTuitionDays(tuitionId: Long)
    @Query("SELECT * FROM tuition_days WHERE tuitionId = :tuitionId AND day = :day LIMIT 1") suspend fun tuitionDay(tuitionId: Long, day: Long): TuitionDay?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveTuitionDay(day: TuitionDay)
    @Query("DELETE FROM tuition_days WHERE id = :id") suspend fun deleteTuitionDay(id: Long)
}

@Database(
    entities = [DlmEntry::class, StudySubject::class, StudyTopic::class, StudyNote::class, TuitionProfile::class, TuitionDay::class],
    version = 3,
    exportSchema = false
)
abstract class DlmDatabase : RoomDatabase() {
    abstract fun dao(): DlmDao

    companion object {
        @Volatile private var INSTANCE: DlmDatabase? = null
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE entries ADD COLUMN type TEXT NOT NULL DEFAULT 'GENERAL'")
                db.execSQL("ALTER TABLE entries ADD COLUMN completed INTEGER NOT NULL DEFAULT 0")
            }
        }
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE entries ADD COLUMN category TEXT NOT NULL DEFAULT 'Other'")
                db.execSQL("ALTER TABLE entries ADD COLUMN pinned INTEGER NOT NULL DEFAULT 0")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_entries_section ON entries(section)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_entries_createdAt ON entries(createdAt)")
                db.execSQL("CREATE TABLE IF NOT EXISTS study_subjects (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, color INTEGER NOT NULL, createdAt INTEGER NOT NULL, pinned INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS study_topics (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, subjectId INTEGER NOT NULL, parentId INTEGER, name TEXT NOT NULL, note TEXT NOT NULL, completed INTEGER NOT NULL, createdAt INTEGER NOT NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_study_topics_subjectId ON study_topics(subjectId)")
                db.execSQL("CREATE TABLE IF NOT EXISTS study_notes (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, subjectId INTEGER NOT NULL, note TEXT NOT NULL, noteDate INTEGER NOT NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_study_notes_subjectId ON study_notes(subjectId)")
                db.execSQL("CREATE TABLE IF NOT EXISTS tuition_profiles (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, teacher TEXT NOT NULL, monthlyFee REAL NOT NULL, startDate INTEGER NOT NULL, color INTEGER NOT NULL, pinned INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS tuition_days (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, tuitionId INTEGER NOT NULL, day INTEGER NOT NULL, attendance INTEGER NOT NULL, note TEXT NOT NULL, paid INTEGER NOT NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_tuition_days_tuitionId ON tuition_days(tuitionId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_tuition_days_day ON tuition_days(day)")
            }
        }

        fun get(context: Context): DlmDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(context.applicationContext, DlmDatabase::class.java, "dlm_native.db")
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build().also { INSTANCE = it }
            }
    }
}
