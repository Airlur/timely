package com.airlur.timely.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

enum class BgType { NONE, COLOR, GRADIENT, IMAGE }
enum class TimeUnit { DAY, WEEK, MONTH, YEAR }
enum class Repeat { NONE, DAILY, WEEKLY, MONTHLY, YEARLY, CUSTOM }

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val targetDate: Long,
    val createdAt: Long,
    val displayUnit: String = TimeUnit.DAY.name,
    val isPinned: Boolean = false,
    val pinnedAt: Long? = null,
    val bgType: String = BgType.NONE.name,
    val bgColor: Int? = null,
    val gradientStart: Int? = null,
    val gradientEnd: Int? = null,
    val imagePath: String? = null,
    val isLunar: Boolean = false,
    val categoryId: Long? = null,
    val sortOrder: Int = 0,
    val iconName: String? = null,   // emoji
    val fontFamily: String? = null, // null / serif / mono
    val reminderTime: Long? = null, // 绝对时间点，与旧版一致
    val includeStartDay: Boolean = false,
    val repeat: String = Repeat.NONE.name,
    val repeatEnd: Long? = null,
    val customRule: String? = null  // "interval,unit,weekdays" 如 "2,WEEK,1,3,5"
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val icon: String? = null,       // emoji
    val color: Int = 0xFF2196F3.toInt(),
    val sortOrder: Int = 0
)

@Dao
interface EventDao {
    @Query("SELECT * FROM events ORDER BY isPinned DESC, sortOrder, createdAt")
    fun observeAll(): Flow<List<EventEntity>>

    @Query("SELECT * FROM events")
    suspend fun getAll(): List<EventEntity>

    @Query("SELECT * FROM events WHERE id = :id")
    suspend fun getById(id: Long): EventEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(event: EventEntity): Long

    @Query("DELETE FROM events WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE events SET isPinned = 0, pinnedAt = NULL WHERE isPinned = 1 AND id != :keepId")
    suspend fun clearPinnedExcept(keepId: Long)

    @Query("SELECT COUNT(*) FROM events WHERE isPinned = 1")
    suspend fun pinnedCount(): Int

    @Query("UPDATE events SET sortOrder = :order WHERE id = :id")
    suspend fun updateOrder(id: Long, order: Int)

    @Query("SELECT MAX(sortOrder) FROM events")
    suspend fun maxSortOrder(): Int?
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY sortOrder, id")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(category: CategoryEntity): Long

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun delete(id: Long)
}

@Database(entities = [EventEntity::class, CategoryEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun eventDao(): EventDao
    abstract fun categoryDao(): CategoryDao
}
