package com.houvven.guise.log

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [RuntimeLog::class], version = 1)
abstract class RuntimeLogDatabase : RoomDatabase() {
    abstract fun runtimeLogDao(): RuntimeLogDao

    companion object {
        private const val DATABASE_NAME = "runtime_log.db"
        private const val LEGACY_DATABASE_NAME = "module_log.db"

        fun create(context: Context): RuntimeLogDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                RuntimeLogDatabase::class.java,
                DATABASE_NAME,
            ).build()
        }

        fun deleteLegacyDatabase(context: Context) {
            context.deleteDatabase(LEGACY_DATABASE_NAME)
        }
    }
}
