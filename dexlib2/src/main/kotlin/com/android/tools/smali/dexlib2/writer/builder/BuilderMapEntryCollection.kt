package com.android.tools.smali.dexlib2.writer.builder

import java.util.AbstractCollection

abstract class BuilderMapEntryCollection<Key>(private val keys: Collection<Key>) :
    AbstractCollection<MutableMap.MutableEntry<Key, Int>>() {

    private inner class MapEntry(override val key: Key) : MutableMap.MutableEntry<Key, Int> {
        override val value: Int
            get() = this@BuilderMapEntryCollection.getValue(key)

        override fun setValue(newValue: Int): Int {
            return this@BuilderMapEntryCollection.setValue(key, newValue)
        }
    }

    override fun iterator(): MutableIterator<MutableMap.MutableEntry<Key, Int>> {
        val iter = keys.iterator()

        return object : MutableIterator<MutableMap.MutableEntry<Key, Int>> {
            override fun hasNext(): Boolean {
                return iter.hasNext()
            }

            override fun next(): MutableMap.MutableEntry<Key, Int> {
                return MapEntry(iter.next())
            }

            override fun remove() {
                throw UnsupportedOperationException()
            }
        }
    }

    override val size: Int
        get() = keys.size

    protected abstract fun getValue(key: Key): Int
    protected abstract fun setValue(key: Key, value: Int): Int
}
