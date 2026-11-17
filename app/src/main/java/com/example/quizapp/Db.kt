package com.example.quizapp

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "scores")
data class ScoreEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uid: String,
    val date: Long,
    val category: String,
    val difficulty: String,
    val score: Int
)

@Dao
interface ScoreDao {
    @Insert suspend fun insert(entry: ScoreEntry)
    @Query("SELECT * FROM scores WHERE uid = :uid ORDER BY date DESC")
    fun history(uid: String): Flow<List<ScoreEntry>>
}

@Database(entities = [ScoreEntry::class], version = 1, exportSchema = false)
abstract class AppDb : RoomDatabase() {
    abstract fun dao(): ScoreDao
    companion object {
        @Volatile private var inst: AppDb? = null
        fun get(c: Context): AppDb = inst ?: synchronized(this) {
            inst ?: Room.databaseBuilder(c.applicationContext, AppDb::class.java, "quiz.db")
                .build().also { inst = it }
        }
    }
}
