package com.ecalendar.reborn.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        EventEntity::class,
        NoteGroupEntity::class,
        NoteEntity::class,
        BirthdayEntity::class,
        CityEntity::class,
        DayMarkEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun eventDao(): EventDao
    abstract fun noteDao(): NoteDao
    abstract fun birthdayDao(): BirthdayDao
    abstract fun cityDao(): CityDao
    abstract fun dayMarkDao(): DayMarkDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "ecalendar-reborn.db",
            ).fallbackToDestructiveMigration().build().also { instance = it }
        }
    }
}
