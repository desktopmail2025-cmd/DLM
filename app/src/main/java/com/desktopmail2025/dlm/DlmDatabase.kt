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
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "entries")
data class DlmEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val section: String,
    val title: String,
    val details: String = "",
    val amount: Double = 0.0,
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

    @Query("SELECT COALESCE(SUM(amount), 0) FROM entries WHERE section = 'MONEY'")
    fun totalMoney(): Flow<Double>
}

@Database(entities = [DlmEntry::class], version = 1, exportSchema = false)
abstract class DlmDatabase : RoomDatabase() {
    abstract fun dao(): DlmDao

    companion object {
        @Volatile private var INSTANCE: DlmDatabase? = null
        fun get(context: Context): DlmDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    DlmDatabase::class.java,
                    "dlm_native.db"
                ).build().also { INSTANCE = it }
            }
    }
}
