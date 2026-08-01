package com.houvven.ktx_xposed.logger

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ModuleLog::class], version = 1)
abstract class ModuleLogDBHelper : RoomDatabase() {

    abstract fun moduleLogDao(): ModuleLogDao

    companion object {
        @Volatile
        private var applicationContext: Context? = null

        private val db: ModuleLogDBHelper by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            Room.databaseBuilder(
                checkNotNull(applicationContext) { "ModuleLogDBHelper is not initialized" },
                ModuleLogDBHelper::class.java,
                "module_log.db",
            ).build()
        }

        val moduleLogDao: ModuleLogDao by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            db.moduleLogDao()
        }

        fun init(context: Context) {
            applicationContext = context.applicationContext
        }
    }
}
