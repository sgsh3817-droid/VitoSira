package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [UserEntity::class, ProjectEntity::class, TemplateEntity::class, PresetEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(DatabaseConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun projectDao(): ProjectDao
    abstract fun templateDao(): TemplateDao
    abstract fun presetDao(): PresetDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pixelcraft_database"
                )
                    .addCallback(DatabaseCallback())
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateInitialPresets(database.presetDao())
                    }
                }
            }

            private suspend fun populateInitialPresets(presetDao: PresetDao) {
                presetDao.insertPreset(PresetEntity(name = "Instagram Post", width = 1080, height = 1080, unit = "Pixels"))
                presetDao.insertPreset(PresetEntity(name = "Instagram Story", width = 1080, height = 1920, unit = "Pixels"))
                presetDao.insertPreset(PresetEntity(name = "Facebook Post", width = 1200, height = 630, unit = "Pixels"))
                presetDao.insertPreset(PresetEntity(name = "Facebook Cover", width = 820, height = 312, unit = "Pixels"))
                presetDao.insertPreset(PresetEntity(name = "YouTube Thumbnail", width = 1280, height = 720, unit = "Pixels"))
                presetDao.insertPreset(PresetEntity(name = "YouTube Banner", width = 2560, height = 1440, unit = "Pixels"))
                presetDao.insertPreset(PresetEntity(name = "Twitter Post", width = 1600, height = 900, unit = "Pixels"))
            }
        }
    }
}
