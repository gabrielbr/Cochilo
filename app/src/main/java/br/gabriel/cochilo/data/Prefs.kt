package br.gabriel.cochilo.data

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Small settings that aren't data: they are not exported. */
class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("cochilo", Context.MODE_PRIVATE)

    private val _theme = MutableStateFlow(
        runCatching { ThemeMode.valueOf(sp.getString("theme", null) ?: "") }.getOrDefault(ThemeMode.SYSTEM)
    )
    val theme: StateFlow<ThemeMode> = _theme

    fun setTheme(mode: ThemeMode) {
        sp.edit { putString("theme", mode.name) }
        _theme.value = mode
    }

    private val _live = MutableStateFlow(sp.getBoolean("live_notification", true))
    /** Show the live notification (the current sleep or wake stretch, and running timers). */
    val liveNotification: StateFlow<Boolean> = _live

    fun setLiveNotification(on: Boolean) {
        sp.edit { putBoolean("live_notification", on) }
        _live.value = on
    }

    private val _fabric = MutableStateFlow(sp.getString("fabric", null))
    /** The key of the fabric that wraps the Today card (ui.kit.Fabric), or null for plain. */
    val fabric: StateFlow<String?> = _fabric

    fun setFabric(key: String) {
        sp.edit { putString("fabric", key) }
        _fabric.value = key
    }

    /** Whether the app has asked for the notification permission once (Android 13+). */
    var askedNotificationPermission: Boolean
        get() = sp.getBoolean("asked_notifications", false)
        set(v) = sp.edit { putBoolean("asked_notifications", v) }
}
