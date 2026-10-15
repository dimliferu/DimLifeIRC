package com.boxlabs.hexdroid.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.boxlabs.hexdroid.CapPrefs
import com.boxlabs.hexdroid.ChatFontStyle
import com.boxlabs.hexdroid.SaslConfig
import com.boxlabs.hexdroid.SaslMechanism
import com.boxlabs.hexdroid.UiSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

private fun parseFontChoice(raw: String?, fallback: com.boxlabs.hexdroid.FontChoice): com.boxlabs.hexdroid.FontChoice {
    val normalized = when (raw) {
        null -> fallback.name
        "DEFAULT", "SERIF", "OPEN_SANS", "OPENSANS" -> com.boxlabs.hexdroid.FontChoice.OPEN_SANS.name
        "SANS_SERIF", "INTER" -> com.boxlabs.hexdroid.FontChoice.INTER.name
        "MONOSPACE" -> com.boxlabs.hexdroid.FontChoice.MONOSPACE.name
        else -> raw
    }
    return runCatching { com.boxlabs.hexdroid.FontChoice.valueOf(normalized) }.getOrDefault(fallback)
}


private val Context.dataStore by preferencesDataStore(name = "hexdroid_prefs")

/**
 * Update older defaulted quit messages
 */
private val LEGACY_DEFAULT_QUIT_MESSAGES = setOf(
    "HexDroid IRC - https://hexdroid.boxlabs.uk",
    "HexDroid IRC - https://hexdroid.boxlabs.uk/",
    "An IRC client for Android - https://hexdroid.org",
)

class SettingsRepository(private val ctx: Context) {

    val secretStore: SecretStore = SecretStore(ctx.applicationContext)
    private object Keys {
        val SETTINGS_JSON = stringPreferencesKey("settings_json")
        val NETWORKS_JSON = stringPreferencesKey("networks_json")
        val LAST_NETWORK_ID = stringPreferencesKey("last_network_id")
        val DESIRED_NETWORK_IDS_JSON = stringPreferencesKey("desired_network_ids_json")
        val SECRETS_MIGRATED_V1 = booleanPreferencesKey("secrets_migrated_v1")
    
        val SECRETS_MIGRATED_V2 = booleanPreferencesKey("secrets_migrated_v2")
        val QUIT_MSG_MIGRATED_V1 = booleanPreferencesKey("quit_msg_migrated_v1")
        val QUIT_MSG_MIGRATED_V2 = booleanPreferencesKey("quit_msg_migrated_v2")
        val QUIT_MSG_MIGRATED_V3 = booleanPreferencesKey("quit_msg_migrated_v3")
        val QUIT_MSG_MIGRATED_V4 = booleanPreferencesKey("quit_msg_migrated_v4")
    }

    val settingsFlow: Flow<UiSettings> = ctx.dataStore.data.map { prefs ->
        parseSettings(prefs[Keys.SETTINGS_JSON])
    }

    val networksFlow: Flow<List<NetworkProfile>> = ctx.dataStore.data.map { prefs ->
        parseNetworks(prefs[Keys.NETWORKS_JSON])
    }

    val lastNetworkIdFlow: Flow<String?> = ctx.dataStore.data.map { prefs ->
        prefs[Keys.LAST_NETWORK_ID]
    }


    /**
     * Networks the user wants to stay connected to ("Always connected").
     *
     * This is deliberately separate from NetworkProfile.autoConnect (startup behavior) so that
     * once a user hits "Connect", we can restore and keep trying across process death / service restarts.
     */
    val desiredNetworkIdsFlow: Flow<Set<String>> = ctx.dataStore.data.map { prefs ->
        parseDesiredNetworkIds(prefs[Keys.DESIRED_NETWORK_IDS_JSON])
    }

    
    /**
     * One-time migration:
     * - Move any legacy plaintext SASL passwords from networks_json into [secretStore]
     * - Remove saslPassword from persisted JSON so it is not backed up / copied in cleartext.
     */
    suspend fun migrateLegacySecretsIfNeeded() {
    ctx.dataStore.edit { prefs ->
        val v1Done = prefs[Keys.SECRETS_MIGRATED_V1] == true
        val v2Done = prefs[Keys.SECRETS_MIGRATED_V2] == true
        if (v1Done && v2Done) return@edit

        val raw = prefs[Keys.NETWORKS_JSON]
        if (!raw.isNullOrBlank()) {
            // Migration runs inside runCatching, so a failure is logged and the migration flags are
            // still set below.
            runCatching {
                val arr = JSONArray(raw)
                var changed = false
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    val id = o.optString("id", "")
                    if (id.isBlank()) continue

                    if (!v1Done) {
                        val legacySasl = o.optString("saslPassword", "")
                        if (legacySasl.isNotBlank()) {
                            secretStore.setSaslPassword(id, legacySasl)
                            o.remove("saslPassword")
                            changed = true
                        }
                    }

                    if (!v2Done) {
                        val legacyServerPass = o.optString("serverPassword", "")
                        if (legacyServerPass.isNotBlank()) {
                            secretStore.setServerPassword(id, legacyServerPass)
                            o.remove("serverPassword")
                            changed = true
                        }
                    }
                }

                if (changed) {
                    prefs[Keys.NETWORKS_JSON] = arr.toString()
                }
            }.onFailure { e ->
                android.util.Log.e("SettingsRepo", "Secret migration failed — marking done to prevent infinite retry", e)
                // We intentionally mark migration as done even on failure: corrupt/unreadable data
                // won't be fixed by retrying, and retrying on every launch creates startup lag
                // without benefit. Users who lost passwords can re-enter them manually.
            }
        }

        // Always mark migration flags; the runCatching above ensures we reach this line even
        // if the migration body threw an exception.
        if (!v1Done) prefs[Keys.SECRETS_MIGRATED_V1] = true
        if (!v2Done) prefs[Keys.SECRETS_MIGRATED_V2] = true
    }
}

    /**
     * rewrite a defaulted quit message so new versions are updated to use hexdroid.org
     */
    suspend fun migrateLegacyQuitMessageIfNeeded() {
        ctx.dataStore.edit { prefs ->
            if (prefs[Keys.QUIT_MSG_MIGRATED_V4] == true) return@edit

            val raw = prefs[Keys.SETTINGS_JSON]
            if (!raw.isNullOrBlank()) {
                runCatching {
                    val o = JSONObject(raw)
                    val storedQuit = if (o.has("quitMessage")) o.optString("quitMessage", "") else null
                    val oldDefault = "\\u00033.\\u00038.\\u00034.\\u00033Dim\\u00034Life \\u0002\\u000311S\\u001fc\\u001fript\\u00033.\\u00038.\\u00034.\\u000f \\u001f\\u00037,0https://dimlife.ru.ru"
                    val newDefault = UiSettings().quitMessage
                    if (storedQuit != null && (storedQuit in LEGACY_DEFAULT_QUIT_MESSAGES ||
                            storedQuit == "\\u000303DimLife \\u000309IRC\\u0003 • irc.dimlife.ru" ||
                            storedQuit == oldDefault)) {
                        o.put("quitMessage", newDefault)
                    }
                    val storedPart = o.optString("partMessage", "Leaving")
                    if (storedPart == "Leaving" || storedPart == oldDefault) {
                        o.put("partMessage", UiSettings().partMessage)
                    }
                    prefs[Keys.SETTINGS_JSON] = o.toString()
                }.onFailure { e ->
                    android.util.Log.e("SettingsRepo", "Quit message migration failed, marking done", e)
                }
            }

            prefs[Keys.QUIT_MSG_MIGRATED_V1] = true
            prefs[Keys.QUIT_MSG_MIGRATED_V2] = true
            prefs[Keys.QUIT_MSG_MIGRATED_V3] = true
            prefs[Keys.QUIT_MSG_MIGRATED_V4] = true
        }
    }

