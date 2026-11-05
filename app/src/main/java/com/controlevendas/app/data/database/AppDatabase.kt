package com.controlevendas.app.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.controlevendas.app.data.dao.VendaDao
import com.controlevendas.app.data.model.Venda

@Database(entities = [Venda::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun vendaDao(): VendaDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "controle_vendas.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
