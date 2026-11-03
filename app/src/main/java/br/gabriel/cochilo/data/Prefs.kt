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

    private val _nightLight = MutableStateFlow(sp.getBoolean("night_light", true))
    /** The night light: amber on warm black from 19:00 to 07:00 (docs/night-light.md). */
    val nightLight: StateFlow<Boolean> = _nightLight

    fun setNightLight(on: Boolean) {
        sp.edit { putBoolean("night_light", on) }
        _nightLight.value = on
    }

    private val _celebrate = MutableStateFlow(sp.getBoolean("celebrations", true))
    /** Little celebrations when what she was trying for happens (docs/celebrations.md). */
    val celebrations: StateFlow<Boolean> = _celebrate

    fun setCelebrations(on: Boolean) {
        sp.edit { putBoolean("celebrations", on) }
        _celebrate.value = on
    }

    /** When each celebration was last shown ("FULL_NAP=2026-11-02;…"), for the once-a-day caps. */
    var celebrationsShown: Map<br.gabriel.cochilo.domain.Moment, java.time.LocalDate>
        get() = (sp.getString("celebrations_shown", null) ?: "").split(';').mapNotNull { part ->
            val (k, v) = part.split('=').takeIf { it.size == 2 } ?: return@mapNotNull null
            val m = br.gabriel.cochilo.domain.Moment.entries.firstOrNull { it.name == k } ?: return@mapNotNull null
            runCatching { m to java.time.LocalDate.parse(v) }.getOrNull()
        }.toMap()
        set(v) = sp.edit { putString("celebrations_shown", v.entries.joinToString(";") { "${it.key.name}=${it.value}" }) }

    /** The routine steps the parent wrote (docs/rituals.md), one per line, per routine kind; null = the defaults. */
    private val _rituals = MutableStateFlow(
        mapOf("NAP" to sp.getString("ritual_NAP", null), "BEDTIME" to sp.getString("ritual_BEDTIME", null))
    )
    val rituals: StateFlow<Map<String, String?>> = _rituals

    fun setRitual(kind: String, text: String) {
        sp.edit { putString("ritual_$kind", text) }
        _rituals.value = _rituals.value + (kind to text)
    }

    private val _passphrase = MutableStateFlow(sp.getString("backup_passphrase", null))
    /**
     * The backup passphrase (docs/backup.md), or null: exports are then plain JSON. Kept in the
     * app's private storage, which never leaves the phone (allowBackup is off); only the exported
     * file is sealed with it.
     */
    val backupPassphrase: StateFlow<String?> = _passphrase

    fun setBackupPassphrase(p: String?) {
        sp.edit { if (p == null) remove("backup_passphrase") else putString("backup_passphrase", p) }
        _passphrase.value = p
    }

    /** Entries already asked about after they ended (details after), newest last, at most 50. */
    var detailsAsked: List<String>
        get() = (sp.getString("details_asked", null) ?: "").split(',').filter { it.isNotEmpty() }
        set(v) = sp.edit { putString("details_asked", v.takeLast(50).joinToString(",")) }

    // Tatá's suggestions (docs/tata-slings/in-app-marketing.md): kept on this phone, not in backups.
    private val _suggestions = MutableStateFlow(sp.getBoolean("tata_suggestions", true))
    /** The one switch for all of Tatá's suggestions (Settings, once any moment is approved). */
    val tataSuggestions: StateFlow<Boolean> = _suggestions

    fun setTataSuggestions(on: Boolean) {
        sp.edit { putBoolean("tata_suggestions", on) }
        _suggestions.value = on
    }

    /** When the app was first opened; no suggestion in the first week. */
    val firstSeen: Long
        get() = sp.getLong("first_seen", 0L).takeIf { it > 0 } ?: System.currentTimeMillis().also { now -> sp.edit { putLong("first_seen", now) } }

    /** Age notes the parent closed ("Got it"), by band. */
    var ageNotesClosed: Set<String>
        get() = sp.getStringSet("age_notes_closed", emptySet())!!.toSet()
        set(v) = sp.edit { putStringSet("age_notes_closed", v) }

    /** Moments dismissed for good. */
    var suggestionsDismissed: Set<String>
        get() = sp.getStringSet("suggestions_dismissed", emptySet())!!.toSet()
        set(v) = sp.edit { putStringSet("suggestions_dismissed", v) }

    /** When each moment first showed ("MOMENT=epochMillis;…"). */
    var suggestionsShown: Map<String, Long>
        get() = (sp.getString("suggestions_shown", null) ?: "").split(';').mapNotNull { part ->
            val (k, v) = part.split('=').takeIf { it.size == 2 } ?: return@mapNotNull null
            v.toLongOrNull()?.let { k to it }
        }.toMap()
        set(v) = sp.edit { putString("suggestions_shown", v.entries.joinToString(";") { "${it.key}=${it.value}" }) }

    // Window reminders (docs/notifications.md).
    private val _reminders = MutableStateFlow(sp.getBoolean("reminders", true))
    val reminders: StateFlow<Boolean> = _reminders

    fun setReminders(on: Boolean) {
        sp.edit { putBoolean("reminders", on) }
        _reminders.value = on
    }

    private val _leads = MutableStateFlow(
        sp.getString("reminder_leads", null)?.split(',')?.mapNotNull { it.toIntOrNull() }?.toSet() ?: br.gabriel.cochilo.domain.Reminders.LEADS.toSet()
    )
    /** Minutes before the window: 30 and 15 by default. */
    val reminderLeads: StateFlow<Set<Int>> = _leads

    fun setReminderLeads(leads: Set<Int>) {
        sp.edit { putString("reminder_leads", leads.joinToString(",")) }
        _leads.value = leads
    }

    /** Reminders already sent, so a window gets each lead once ("lead@windowStart;…"). */
    var remindersSent: List<br.gabriel.cochilo.domain.Reminders.Sent>
        get() = (sp.getString("reminders_sent", null) ?: "").split(';').mapNotNull { part ->
            val (l, w) = part.split('@').takeIf { it.size == 2 } ?: return@mapNotNull null
            val lead = l.toIntOrNull() ?: return@mapNotNull null
            w.toLongOrNull()?.let { br.gabriel.cochilo.domain.Reminders.Sent(lead, it) }
        }
        set(v) = sp.edit { putString("reminders_sent", v.joinToString(";") { "${it.lead}@${it.windowStart}" }) }

    /** Whether the safe-babywearing check (T.I.C.K.S.) has been shown once. */
    var ticksShown: Boolean
        get() = sp.getBoolean("ticks_shown", false)
        set(v) = sp.edit { putBoolean("ticks_shown", v) }

    /** Whether the app has asked for the notification permission once (Android 13+). */
    var askedNotificationPermission: Boolean
        get() = sp.getBoolean("asked_notifications", false)
        set(v) = sp.edit { putBoolean("asked_notifications", v) }
}
