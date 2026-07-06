package com.example.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "hostpanel_settings")

class SettingsDataStore(private val context: Context) {

    companion object {
        val SERVER_URL = stringPreferencesKey("server_url")
        val API_KEY = stringPreferencesKey("api_key")
        val REMOTE_MODE = booleanPreferencesKey("remote_mode")
        val TUNNEL_TOKEN = stringPreferencesKey("tunnel_token")
        val TUNNEL_ENABLED = booleanPreferencesKey("tunnel_enabled")
        val SERVER_DOMAIN = stringPreferencesKey("server_domain")
        val AUTO_REFRESH = booleanPreferencesKey("auto_refresh")
        val TUNNEL_URL = stringPreferencesKey("tunnel_url")
        val THEME_MODE = stringPreferencesKey("theme_mode")
    }

    val serverUrl: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[SERVER_URL] ?: "http://localhost:3001"
    }

    val apiKey: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[API_KEY] ?: "hostpanel-local"
    }

    val isRemoteMode: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[REMOTE_MODE] ?: false
    }

    val tunnelToken: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[TUNNEL_TOKEN] ?: ""
    }

    val isTunnelEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[TUNNEL_ENABLED] ?: false
    }

    val serverDomain: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[SERVER_DOMAIN] ?: ""
    }

    val autoRefresh: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[AUTO_REFRESH] ?: true
    }

    val tunnelUrl: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[TUNNEL_URL] ?: ""
    }

    val themeMode: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[THEME_MODE] ?: "dark"
    }

    suspend fun saveServerUrl(url: String) {
        context.dataStore.edit { prefs -> prefs[SERVER_URL] = url }
    }

    suspend fun saveApiKey(key: String) {
        context.dataStore.edit { prefs -> prefs[API_KEY] = key }
    }

    suspend fun setRemoteMode(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[REMOTE_MODE] = enabled }
    }

    suspend fun saveTunnelToken(token: String) {
        context.dataStore.edit { prefs -> prefs[TUNNEL_TOKEN] = token }
    }

    suspend fun setTunnelEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[TUNNEL_ENABLED] = enabled }
    }

    suspend fun saveServerDomain(domain: String) {
        context.dataStore.edit { prefs -> prefs[SERVER_DOMAIN] = domain }
    }

    suspend fun setAutoRefresh(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[AUTO_REFRESH] = enabled }
    }

    suspend fun saveTunnelUrl(url: String) {
        context.dataStore.edit { prefs -> prefs[TUNNEL_URL] = url }
    }

    suspend fun saveThemeMode(mode: String) {
        context.dataStore.edit { prefs -> prefs[THEME_MODE] = mode }
    }
}

