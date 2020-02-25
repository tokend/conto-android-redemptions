package org.tokend.contoredemptions.base.data.repository

interface ObjectPersistence<T: Any> {
    fun loadItem(): T?
    fun saveItem(item: T)
    fun hasItem(): Boolean
    fun clear()
}