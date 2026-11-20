package com.example.ysukampus.data.local.db

/**
 * Дублирует `CachedResponseDao` без Room/SQLite — используется JVM-тестами
 * инвалидации кэша (`ForumCache`, `NotificationCache`), которым не нужен
 * настоящий SQL, а нужен факт и аргумент вызова `deleteByPrefix`/`clear`.
 * Настоящий SQL (`LIKE :prefix || '%'`, сортировка по возрасту) проверяется
 * инструментальным `ResponseCacheTest`.
 */
class FakeCachedResponseDao : CachedResponseDao {
    private val entries = mutableMapOf<String, CachedResponseEntity>()

    val deletedPrefixes = mutableListOf<String>()
    var clearCount = 0
        private set

    override suspend fun find(key: String): CachedResponseEntity? = entries[key]

    override suspend fun put(entity: CachedResponseEntity) {
        entries[entity.key] = entity
    }

    override suspend fun deleteByPrefix(prefix: String) {
        deletedPrefixes += prefix
        entries.keys.filter { it.startsWith(prefix) }.forEach { entries.remove(it) }
    }

    override suspend fun deleteOlderThan(olderThan: Long) {
        entries.values.filter { it.updatedAt < olderThan }.forEach { entries.remove(it.key) }
    }

    override suspend fun clear() {
        clearCount++
        entries.clear()
    }
}
