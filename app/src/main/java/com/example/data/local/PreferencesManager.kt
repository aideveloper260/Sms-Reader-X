package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

private val Context.dataStore by preferencesDataStore(name = "sms_reader_x_prefs")

data class ProjectConfig(
    val projectName: String = "",
    val apiUrl: String = "",
    val deviceKey: String = "",
    val merchantId: String = "",
    val selectedSim: String = "ALL", // "ALL", "SIM_1", "SIM_2"
    val enableBkash: Boolean = true,
    val enableNagad: Boolean = true,
    val enableRocket: Boolean = true,
    val isMonitoringActive: Boolean = true,
    val autoForward: Boolean = true,
    val retryFailed: Boolean = true,
    val startOnBoot: Boolean = true,
    val isConfigured: Boolean = false,
    val deviceId: String = "",
    val lastSyncTime: String = ""
)

class PreferencesManager(private val context: Context) {

    companion object {
        val KEY_PROJECT_NAME = stringPreferencesKey("project_name")
        val KEY_API_URL = stringPreferencesKey("api_url")
        val KEY_DEVICE_KEY = stringPreferencesKey("device_key")
        val KEY_MERCHANT_ID = stringPreferencesKey("merchant_id")
        val KEY_SELECTED_SIM = stringPreferencesKey("selected_sim")
        val KEY_ENABLE_BKASH = booleanPreferencesKey("enable_bkash")
        val KEY_ENABLE_NAGAD = booleanPreferencesKey("enable_nagad")
        val KEY_ENABLE_ROCKET = booleanPreferencesKey("enable_rocket")
        val KEY_MONITORING_ACTIVE = booleanPreferencesKey("monitoring_active")
        val KEY_AUTO_FORWARD = booleanPreferencesKey("auto_forward")
        val KEY_RETRY_FAILED = booleanPreferencesKey("retry_failed")
        val KEY_START_ON_BOOT = booleanPreferencesKey("start_on_boot")
        val KEY_DEVICE_ID = stringPreferencesKey("device_id")
        val KEY_LAST_SYNC_TIME = stringPreferencesKey("last_sync_time")
    }

    val projectConfigFlow: Flow<ProjectConfig> = context.dataStore.data.map { prefs ->
        val name = prefs[KEY_PROJECT_NAME] ?: ""
        val url = prefs[KEY_API_URL] ?: ""
        val key = prefs[KEY_DEVICE_KEY] ?: ""
        val merchant = prefs[KEY_MERCHANT_ID] ?: ""
        val sim = prefs[KEY_SELECTED_SIM] ?: "ALL"
        val bkash = prefs[KEY_ENABLE_BKASH] ?: true
        val nagad = prefs[KEY_ENABLE_NAGAD] ?: true
        val rocket = prefs[KEY_ENABLE_ROCKET] ?: true
        val monitoring = prefs[KEY_MONITORING_ACTIVE] ?: true
        val forward = prefs[KEY_AUTO_FORWARD] ?: true
        val retry = prefs[KEY_RETRY_FAILED] ?: true
        val boot = prefs[KEY_START_ON_BOOT] ?: true
        var devId = prefs[KEY_DEVICE_ID] ?: ""
        if (devId.isEmpty()) {
            devId = "device_" + UUID.randomUUID().toString().substring(0, 12)
        }
        val sync = prefs[KEY_LAST_SYNC_TIME] ?: ""

        ProjectConfig(
            projectName = name,
            apiUrl = url,
            deviceKey = key,
            merchantId = merchant,
            selectedSim = sim,
            enableBkash = bkash,
            enableNagad = nagad,
            enableRocket = rocket,
            isMonitoringActive = monitoring,
            autoForward = forward,
            retryFailed = retry,
            startOnBoot = boot,
            isConfigured = url.isNotBlank() && key.isNotBlank(),
            deviceId = devId,
            lastSyncTime = sync
        )
    }

    suspend fun saveProject(
        name: String,
        apiUrl: String,
        deviceKey: String,
        merchantId: String,
        sim: String,
        bkash: Boolean,
        nagad: Boolean,
        rocket: Boolean
    ) {
        context.dataStore.edit { prefs ->
            prefs[KEY_PROJECT_NAME] = name
            prefs[KEY_API_URL] = apiUrl.trimEnd('/')
            prefs[KEY_DEVICE_KEY] = deviceKey.trim()
            prefs[KEY_MERCHANT_ID] = if (merchantId.isNotBlank()) merchantId.trim() else "MERCHANT001"
            prefs[KEY_SELECTED_SIM] = sim
            prefs[KEY_ENABLE_BKASH] = bkash
            prefs[KEY_ENABLE_NAGAD] = nagad
            prefs[KEY_ENABLE_ROCKET] = rocket
            prefs[KEY_MONITORING_ACTIVE] = true
            if (!prefs.contains(KEY_DEVICE_ID)) {
                prefs[KEY_DEVICE_ID] = "dev_" + UUID.randomUUID().toString().substring(0, 10)
            }
        }
    }

    suspend fun updateMonitoringState(isActive: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_MONITORING_ACTIVE] = isActive
        }
    }

    suspend fun updateProviders(bkash: Boolean, nagad: Boolean, rocket: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ENABLE_BKASH] = bkash
            prefs[KEY_ENABLE_NAGAD] = nagad
            prefs[KEY_ENABLE_ROCKET] = rocket
        }
    }

    suspend fun updateSim(sim: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SELECTED_SIM] = sim
        }
    }

    suspend fun updateStartOnBoot(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_START_ON_BOOT] = enabled
        }
    }

    suspend fun updateLastSync(timeString: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_LAST_SYNC_TIME] = timeString
        }
    }

    suspend fun clearProject() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_PROJECT_NAME)
            prefs.remove(KEY_API_URL)
            prefs.remove(KEY_DEVICE_KEY)
            prefs.remove(KEY_MERCHANT_ID)
            prefs[KEY_MONITORING_ACTIVE] = false
        }
    }
}
