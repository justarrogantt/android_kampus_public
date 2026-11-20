package com.example.ysukampus.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Локальная база приложения. Сейчас в ней только кэш ответов сервера.
 *
 * Правило, которое тут важнее остальных: всё, что лежит в этой базе, обязано
 * восстанавливаться из сети. Ничего, что пользователь создал и ещё не отправил
 * на сервер, сюда класть нельзя — см. `fallbackToDestructiveMigration` в
 * [DatabaseProvider].
 */
@Database(
    entities = [CachedResponseEntity::class],
    version = 1,
    exportSchema = true
)
abstract class KampusDatabase : RoomDatabase() {
    abstract fun cachedResponseDao(): CachedResponseDao
}

/**
 * Синглтон на процесс — по образцу `SessionManagerProvider`.
 *
 * Экземпляр обязан быть один: Room держит на файле свой пул соединений и свой
 * трекер инвалидации, и два независимых экземпляра поверх одного файла дают
 * блокировки и рассинхрон.
 */
object DatabaseProvider {

    private const val DATABASE_NAME = "kampus_cache.db"

    @Volatile
    private var database: KampusDatabase? = null

    fun get(context: Context): KampusDatabase {
        val appContext = context.applicationContext
        return database ?: synchronized(this) {
            database ?: Room.databaseBuilder(
                appContext,
                KampusDatabase::class.java,
                DATABASE_NAME
            )
                // Сознательный выбор для кэша: при смене схемы старый файл
                // сносится, а данные приедут из сети на первом же запросе.
                // Как только в базе появится что-то НЕ восстановимое из сети
                // (черновики, офлайн-очередь отправки), эту строку надо убрать
                // и писать честные Migration.
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
                .also { database = it }
        }
    }

    fun cachedResponseDao(context: Context): CachedResponseDao =
        get(context).cachedResponseDao()
}
