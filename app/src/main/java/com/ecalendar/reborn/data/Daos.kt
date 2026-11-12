package com.ecalendar.reborn.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {
    @Query("SELECT * FROM events ORDER BY startAt ASC")
    fun observeAll(): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE startAt BETWEEN :from AND :to ORDER BY startAt ASC")
    fun observeBetween(from: Long, to: Long): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE startAt BETWEEN :from AND :to ORDER BY startAt ASC")
    suspend fun between(from: Long, to: Long): List<EventEntity>

    @Query("SELECT * FROM events WHERE id = :id")
    suspend fun find(id: Long): EventEntity?

    @Query("SELECT * FROM events WHERE kind = 'todo' AND done = 0 ORDER BY startAt ASC")
    fun observeOpenTodos(): Flow<List<EventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(event: EventEntity): Long

    @Update
    suspend fun update(event: EventEntity)

    @Query("UPDATE events SET done = :done WHERE id = :id")
    suspend fun setDone(id: Long, done: Boolean)

    @Query("DELETE FROM events WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface NoteDao {
    @Query("SELECT * FROM note_groups ORDER BY sort ASC, id ASC")
    fun observeGroups(): Flow<List<NoteGroupEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGroup(group: NoteGroupEntity): Long

    @Query("DELETE FROM note_groups WHERE id = :id")
    suspend fun deleteGroup(id: Long)

    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE groupId = :groupId ORDER BY updatedAt DESC")
    fun observeByGroup(groupId: Long): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun find(id: Long): NoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(note: NoteEntity): Long

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface BirthdayDao {
    @Query("SELECT * FROM birthdays ORDER BY month ASC, day ASC")
    fun observeAll(): Flow<List<BirthdayEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: BirthdayEntity): Long

    @Query("DELETE FROM birthdays WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface CityDao {
    @Query("SELECT * FROM cities ORDER BY id ASC")
    fun observeAll(): Flow<List<CityEntity>>

    @Query("SELECT * FROM cities WHERE isDefault = 1 LIMIT 1")
    suspend fun defaultCity(): CityEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(city: CityEntity): Long

    @Query("UPDATE cities SET isDefault = 0")
    suspend fun clearDefault()

    @Query("DELETE FROM cities WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface DayMarkDao {
    @Query("SELECT * FROM day_marks")
    fun observeAll(): Flow<List<DayMarkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(mark: DayMarkEntity)

    @Query("DELETE FROM day_marks WHERE epochDay = :epochDay")
    suspend fun delete(epochDay: Long)
}
