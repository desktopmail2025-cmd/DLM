package com.desktopmail2025.dlm

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "entries")
data class DlmEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val section: String,
    val title: String,
    val details: String = "",
    val amount: Double = 0.0,
    val type: String = "GENERAL",
    val completed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface DlmDao {
    @Query("SELECT * FROM entries WHERE section = :section ORDER BY createdAt DESC")
    fun entries(section: String): Flow<List<DlmEntry>>

    @Insert
    suspend fun insert(entry: DlmEntry)

    @Query("DELETE FROM entries WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE entries SET completed = :completed WHERE id = :id")
    suspend fun setCompleted(id: Long, completed: Boolean)

    @Query("SELECT COALESCE(SUM(CASE WHEN type = 'INCOME' THEN amount ELSE -amount END), 0) FROM entries WHERE section = 'MONEY'")
    fun moneyBalance(): Flow<Double>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM entries WHERE section = 'TUITION' AND type = 'PAID'")
    fun tuitionPaid(): Flow<Double>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM entries WHERE section = 'TUITION' AND type = 'FEE'")
    fun tuitionFees(): Flow<Double>
}

@Database(entities = [DlmEntry::class], version = 2, exportSchema = false)
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

        fun get(context: Context): DlmDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    DlmDatabase::class.java,
                    "dlm_native.db"
                ).addMigrations(MIGRATION_1_2).build().also { INSTANCE = it }
            }
    }
}
