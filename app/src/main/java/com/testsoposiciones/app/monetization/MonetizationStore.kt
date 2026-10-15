package com.testsoposiciones.app.monetization

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.monetizationDataStore: DataStore<Preferences> by preferencesDataStore(name = "monetization")

data class StoredMonetization(val premium: Boolean = false, val rewardedUntilMs: Long = 0)

/** Caché local: Premium (la tienda manda al arrancar), desbloqueo por anuncio y último anuncio a pantalla completa. */
class MonetizationStore(private val store: DataStore<Preferences>) {
    val data: Flow<StoredMonetization> = store.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { StoredMonetization(it[PREMIUM] ?: false, it[REWARDED_UNTIL] ?: 0) }

    suspend fun setPremium(premium: Boolean) {
        store.edit { it[PREMIUM] = premium }
    }

    suspend fun setRewardedUntil(epochMs: Long) {
        store.edit { it[REWARDED_UNTIL] = epochMs }
    }

    suspend fun lastFullScreenAt(): Long = store.data.map { it[LAST_FULL_SCREEN] ?: 0 }.catch { emit(0) }.first()

    suspend fun setLastFullScreenAt(epochMs: Long) {
        store.edit { it[LAST_FULL_SCREEN] = epochMs }
    }

    private companion object {
        val PREMIUM = booleanPreferencesKey("premium")
        val REWARDED_UNTIL = longPreferencesKey("rewarded_until")
        val LAST_FULL_SCREEN = longPreferencesKey("last_full_screen")
    }
}
