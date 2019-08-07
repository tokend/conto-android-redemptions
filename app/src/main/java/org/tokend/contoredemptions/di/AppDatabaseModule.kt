package org.tokend.contoredemptions.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import org.tokend.contoredemptions.db.AppDatabase
import javax.inject.Singleton

@Module
class AppDatabaseModule(private val dbName: String) {
    @Provides
    @Singleton
    fun database(context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            dbName
        )
            .build()
    }
}