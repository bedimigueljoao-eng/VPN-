package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.CarrierSelectionMode
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.vaultDataStore: DataStore<Preferences> by preferencesDataStore(name = "internet_vault_prefs")

data class VaultPreferences(
    val carrierSelectionMode: CarrierSelectionMode = CarrierSelectionMode.AUTO,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val autoPrepareOnWifi: Boolean = true,
    val dnsLeakGuardEnabled: Boolean = true,
    val bypassLanEnabled: Boolean = true,
    val lastWifiPrepTimestamp: Long = 0L
)

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val CARRIER_MODE = stringPreferencesKey("carrier_selection_mode")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val AUTO_PREPARE_WIFI = booleanPreferencesKey("auto_prepare_wifi")
        val DNS_LEAK_GUARD = booleanPreferencesKey("dns_leak_guard")
        val BYPASS_LAN = booleanPreferencesKey("bypass_lan")
        val LAST_WIFI_PREP = longPreferencesKey("last_wifi_prep")
    }

    val preferencesFlow: Flow<VaultPreferences> = context.vaultDataStore.data.map { prefs ->
        val carrierMode = prefs[Keys.CARRIER_MODE]?.let {
            runCatching { CarrierSelectionMode.valueOf(it) }.getOrNull()
        } ?: CarrierSelectionMode.AUTO
        val themeMode = prefs[Keys.THEME_MODE]?.let {
            runCatching { ThemeMode.valueOf(it) }.getOrNull()
        } ?: ThemeMode.SYSTEM

        VaultPreferences(
            carrierSelectionMode = carrierMode,
            themeMode = themeMode,
            autoPrepareOnWifi = prefs[Keys.AUTO_PREPARE_WIFI] ?: true,
            dnsLeakGuardEnabled = prefs[Keys.DNS_LEAK_GUARD] ?: true,
            bypassLanEnabled = prefs[Keys.BYPASS_LAN] ?: true,
            lastWifiPrepTimestamp = prefs[Keys.LAST_WIFI_PREP] ?: 0L
        )
    }

    suspend fun setCarrierSelectionMode(mode: CarrierSelectionMode) {
        context.vaultDataStore.edit { it[Keys.CARRIER_MODE] = mode.name }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.vaultDataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setAutoPrepareOnWifi(enabled: Boolean) {
        context.vaultDataStore.edit { it[Keys.AUTO_PREPARE_WIFI] = enabled }
    }

    suspend fun setDnsLeakGuard(enabled: Boolean) {
        context.vaultDataStore.edit { it[Keys.DNS_LEAK_GUARD] = enabled }
    }

    suspend fun setBypassLan(enabled: Boolean) {
        context.vaultDataStore.edit { it[Keys.BYPASS_LAN] = enabled }
    }

    suspend fun updateLastWifiPrep(timestamp: Long) {
        context.vaultDataStore.edit { it[Keys.LAST_WIFI_PREP] = timestamp }
    }
}
