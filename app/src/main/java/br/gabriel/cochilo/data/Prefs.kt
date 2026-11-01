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
}
