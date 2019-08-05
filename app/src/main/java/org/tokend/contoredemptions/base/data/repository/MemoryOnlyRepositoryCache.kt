package org.tokend.contoredemptions.base.data.repository

/**
 * Repository cache without persistence
 */
open class MemoryOnlyRepositoryCache<T> : RepositoryCache<T>() {
    override fun isContentSame(first: T, second: T): Boolean = false

    override fun getAllFromDb(): List<T> = emptyList()

    override fun addToDb(items: List<T>) {}

    override fun updateInDb(items: List<T>) {}

    override fun deleteFromDb(items: List<T>) {}

    override fun clearDb() {}
}