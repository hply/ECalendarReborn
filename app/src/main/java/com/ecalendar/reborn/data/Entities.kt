package com.ecalendar.reborn.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val note: String = "",
    /** 开始时间，epoch 毫秒 */
    val startAt: Long,
    val allDay: Boolean = false,
    /** 提前提醒分钟数，-1 表示不提醒 */
    val remindBefore: Int = 10,
    /** none / daily / weekly / monthly / yearly / lunarYearly */
    val repeatRule: String = "none",
    /** schedule（日程）/ todo（待办） */
    val kind: String = "schedule",
    val done: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "note_groups")
data class NoteGroupEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val sort: Int = 0,
)

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val groupId: Long = 0,
    val title: String,
    val content: String = "",
    val locked: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "birthdays")
data class BirthdayEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val relation: String = "家人",
    val month: Int,
    val day: Int,
    /** 是否农历生日 */
    val isLunar: Boolean = false,
    val birthYear: Int = 0,
    /** 提前几天提醒 */
    val remindDays: Int = 1,
    val note: String = "",
)

@Entity(tableName = "cities")
data class CityEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val isDefault: Boolean = false,
)

@Entity(tableName = "day_marks")
data class DayMarkEntity(
    @PrimaryKey val epochDay: Long,
    val mark: String,
    val color: Int = 0,
)
