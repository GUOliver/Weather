package com.weather.weather.Backend

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.weather.weather.MainActivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlin.system.exitProcess

// Extension property to create a DataStore for user preferences
private val Context.dataStore by preferencesDataStore("user_preferences")

// Class to handle data operations in a DataStore
class DataWorker {
    // Access the DataStore from the MainActivity's application context
    private val dataStore: DataStore<Preferences> = MainActivity.applicationContext().dataStore

    // Generic function to create a preference key based on the type T
    private inline fun <reified T : Any> _getPrefKey(key: String): Preferences.Key<T> {
        val prefKey: Preferences.Key<T> = when (T::class) {
            Int::class -> intPreferencesKey(key)
            String::class -> stringPreferencesKey(key)
            Float::class -> floatPreferencesKey(key)
            Boolean::class -> booleanPreferencesKey(key)
            Long::class -> longPreferencesKey(key)
            else -> {
                exitProcess(-1) // Terminate if type is not supported
            }
        } as Preferences.Key<T>
        return prefKey
    }

    // Generic function to set data in the DataStore
    private inline fun <reified T : Any> _setData(key: String, value: T) {
        runBlocking {
            dataStore.edit { settings -> settings[_getPrefKey<T>(key)] = value }
        }
    }

    // Generic function to retrieve data from the DataStore
    private inline fun <reified T : Any> _getData(key: String): T? {
        return runBlocking {
            (dataStore.data.map { preferences -> preferences[_getPrefKey<T>(key)] }).first()
        }
    }

    // Generic function to remove data from the DataStore
    private inline fun <reified T : Any> _removeData(key: String) {
        runBlocking {
            dataStore.edit { data -> data.remove(_getPrefKey<T>(key)) }
        }
    }

    // Type-specific functions to set data
    fun setData(key: String, value: Int) = _setData(key, value)
    fun setData(key: String, value: String) = _setData(key, value)
    fun setData(key: String, value: Float) = _setData(key, value)
    fun setData(key: String, value: Boolean) = _setData(key, value)
    fun setData(key: String, value: Long) = _setData(key, value)

    // Type-specific functions to get data
    fun getDataInt(key: String): Int? = _getData(key)
    fun getDataString(key: String): String? = _getData(key)
    fun getDataFloat(key: String): Float? = _getData(key)
    fun getDataBoolean(key: String): Boolean? = _getData(key)
    fun getDataLong(key: String): Long? = _getData(key)

    // Type-specific functions to remove data
    fun removeDataLong(key: String) = _removeData<Long>(key)
    fun removeDataFloat(key: String) = _removeData<Float>(key)
    fun removeDataString(key: String) = _removeData<String>(key)
    fun removeDataInt(key: String) = _removeData<Int>(key)
    fun removeDataBoolean(key: String) = _removeData<Boolean>(key)

    // Function to clear all data from the DataStore
    fun removeData() {
        runBlocking {
            dataStore.edit { it.clear() }
        }
    }
}
