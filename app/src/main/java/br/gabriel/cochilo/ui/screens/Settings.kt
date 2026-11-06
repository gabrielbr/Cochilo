package br.gabriel.cochilo.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import br.gabriel.cochilo.R
import br.gabriel.cochilo.data.Baby
import br.gabriel.cochilo.data.ThemeMode
import br.gabriel.cochilo.domain.localDate
import br.gabriel.cochilo.ui.Format
import br.gabriel.cochilo.ui.kit.Caption
import br.gabriel.cochilo.ui.kit.FilterChips
import br.gabriel.cochilo.ui.kit.GIcon
import br.gabriel.cochilo.ui.kit.Hairline
import br.gabriel.cochilo.ui.kit.ListRow
import br.gabriel.cochilo.ui.kit.RowGroup
import br.gabriel.cochilo.ui.theme.CTokens.Spacing
import br.gabriel.cochilo.ui.theme.CochiloTheme
import java.time.ZoneId

@Composable
fun SettingsScreen(
    baby: Baby?,
    theme: ThemeMode,
    zone: ZoneId,
    onEditBaby: () -> Unit,
    onTheme: (ThemeMode) -> Unit,
    onExportJson: () -> Unit,
    onExportCsv: () -> Unit,
    onImport: () -> Unit,
    onImportNapper: () -> Unit,
    liveNotification: Boolean = true,
    onLiveNotification: (Boolean) -> Unit = {},
    fabric: br.gabriel.cochilo.ui.kit.Fabric = br.gabriel.cochilo.ui.kit.Fabric.PLAIN,
    onFabric: (br.gabriel.cochilo.ui.kit.Fabric) -> Unit = {},
    /** Extra About rows from the build (the github build's "Check for updates"). */
    about: @Composable () -> Unit = {},
    /** Settings › Baby › Appearance: the look picker. */
    onEditLook: () -> Unit = {},
    celebrations: Boolean = true,
    onCelebrations: (Boolean) -> Unit = {},
    nightLight: Boolean = true,
    onNightLight: (Boolean) -> Unit = {},
    /** Settings › Baby › Routine steps (docs/rituals.md). */
    onEditRitual: () -> Unit = {},
    onEditMusic: () -> Unit = {},
    musicCount: Int = 0,
    /** Settings › Sync (0.47.0, docs/sync.md): only in the build that can sync. */
    sync: (@Composable () -> Unit)? = null,
    /** Settings › Data › Report (docs/report.md). */
    onExportReport: () -> Unit = {},
    /** Settings › Data › Protect backups (docs/backup.md). */
    passphraseSet: Boolean = false,
    onPassphrase: () -> Unit = {},
    /** Settings › Ask Cochilo › On-device AI (docs/assistant.md); null hides the row. */
    modelStatus: String? = null,
    onModel: () -> Unit = {},
    /** The one switch for Tatá's suggestions; null hides it (no moment approved yet). */
    tataSuggestions: Boolean? = null,
    onTataSuggestions: (Boolean) -> Unit = {},
    /** Window reminders (docs/notifications.md). */
    reminders: Boolean = true,
    onReminders: (Boolean) -> Unit = {},
    reminderLeads: Set<Int> = br.gabriel.cochilo.domain.Reminders.LEADS.toSet(),
    onReminderLeads: (Set<Int>) -> Unit = {},
) {
    val context = LocalContext.current
    val f = Format(context, zone)
    val chevron: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {
        GIcon(R.drawable.gg_ic_chevron, null, CochiloTheme.colors.muted)
    }
    ScrollPage {
        Caption(stringResource(R.string.caption_baby), top = Spacing.space1)
        RowGroup {
            ListRow(stringResource(R.string.baby_name), value = baby?.name ?: stringResource(R.string.not_set), onClick = onEditBaby, trailing = chevron)
            if (baby != null) {
                Hairline()
                // The avatar: how the baby looks (the look picker in a sheet).
                ListRow(
                    stringResource(R.string.look_title, baby.name),
                    leading = { br.gabriel.cochilo.ui.kit.BabyAvatar(br.gabriel.cochilo.domain.BabyLook.decode(baby.look), size = br.gabriel.cochilo.ui.theme.CTokens.Size.kindBadgeLead) },
                    onClick = onEditLook, trailing = chevron,
                )
                Hairline()
                ListRow(stringResource(R.string.ritual_title), onClick = onEditRitual, trailing = chevron)
                Hairline()
                ListRow(
                    stringResource(R.string.music_title),
                    value = if (musicCount > 0) stringResource(R.string.music_count, musicCount) else stringResource(R.string.not_set),
                    onClick = onEditMusic, trailing = chevron,
                )
            }
            Hairline()
            ListRow(stringResource(R.string.birth_date), value = baby?.let { f.day(localDate(it.birthDate, zone)) } ?: stringResource(R.string.not_set), onClick = onEditBaby, trailing = chevron)
            Hairline()
            ListRow(
                stringResource(R.string.due_date), meta = stringResource(R.string.due_date_meta),
                value = baby?.dueDate?.let { f.day(localDate(it, zone)) } ?: stringResource(R.string.not_set),
                onClick = onEditBaby, trailing = chevron,
            )
        }

        Caption(stringResource(R.string.caption_appearance))
        RowGroup {
            Column(Modifier.padding(horizontal = Spacing.space4, vertical = Spacing.space3)) {
                androidx.compose.material3.Text(stringResource(R.string.theme), style = br.gabriel.cochilo.ui.theme.Type.body, color = CochiloTheme.colors.ink)
                val labels = mapOf(
                    ThemeMode.SYSTEM to stringResource(R.string.theme_system),
                    ThemeMode.LIGHT to stringResource(R.string.theme_light),
                    ThemeMode.DARK to stringResource(R.string.theme_dark),
                )
                FilterChips(ThemeMode.entries, theme, { labels.getValue(it) }, onSelect = onTheme)
            }
            Hairline()
            br.gabriel.cochilo.ui.kit.SwitchRow(stringResource(R.string.night_light), stringResource(R.string.night_light_meta), nightLight, onNightLight)
            Hairline()
            Column(Modifier.padding(horizontal = Spacing.space4, vertical = Spacing.space3)) {
                androidx.compose.material3.Text(stringResource(R.string.fabric), style = br.gabriel.cochilo.ui.theme.Type.body, color = CochiloTheme.colors.ink)
                androidx.compose.material3.Text(stringResource(R.string.fabric_meta), style = br.gabriel.cochilo.ui.theme.Type.meta, color = CochiloTheme.colors.muted)
                br.gabriel.cochilo.ui.kit.FabricPicker(fabric, onFabric)
                // The picked fabric exists as a sling: one quiet line to the shop, here only,
                // behind her own choice (docs/tata-slings/in-app-marketing.md §3.3).
                if (fabric.texture != null) {
                    val name = stringResource(fabric.label)
                    br.gabriel.cochilo.ui.kit.QuietLink(
                        stringResource(R.string.fabric_in_shop_line), stringResource(R.string.fabric_in_shop_action),
                        stringResource(R.string.fabric_in_shop_spoken, name),
                        onClick = { openShop(context, br.gabriel.cochilo.store.Store.fabric(name, fabric.key)) },
                    )
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Hairline()
                ListRow(stringResource(R.string.language), trailing = chevron, onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_APP_LOCALE_SETTINGS, Uri.fromParts("package", context.packageName, null))
                    )
                })
            }
        }

        Caption(stringResource(R.string.caption_notifications))
        RowGroup {
            br.gabriel.cochilo.ui.kit.SwitchRow(
                stringResource(R.string.live_notification), stringResource(R.string.live_notification_meta),
                liveNotification, onLiveNotification,
            )
            Hairline()
            ReminderRows(reminders, onReminders, reminderLeads, onReminderLeads)
            Hairline()
            br.gabriel.cochilo.ui.kit.SwitchRow(
                stringResource(R.string.celebrations), stringResource(R.string.celebrations_meta),
                celebrations, onCelebrations,
            )
            if (tataSuggestions != null) {
                Hairline()
                br.gabriel.cochilo.ui.kit.SwitchRow(
                    stringResource(R.string.tata_suggestions), stringResource(R.string.tata_suggestions_meta),
                    tataSuggestions, onTataSuggestions,
                )
            }
        }

        if (modelStatus != null) {
            Caption(stringResource(R.string.caption_ask))
            RowGroup {
                ListRow(stringResource(R.string.model_row), meta = modelStatus, onClick = onModel, trailing = chevron)
            }
        }

        sync?.invoke()

        Caption(stringResource(R.string.caption_data))
        RowGroup {
            ListRow(stringResource(R.string.export_json), onClick = onExportJson, trailing = chevron)
            Hairline()
            ListRow(stringResource(R.string.export_csv), onClick = onExportCsv, trailing = chevron)
            Hairline()
            ListRow(stringResource(R.string.export_report), onClick = onExportReport, trailing = chevron)
            Hairline()
            ListRow(
                stringResource(R.string.passphrase_row),
                meta = stringResource(if (passphraseSet) R.string.passphrase_row_on else R.string.passphrase_row_off),
                onClick = onPassphrase, trailing = chevron,
            )
            Hairline()
            ListRow(stringResource(R.string.import_json), meta = stringResource(R.string.import_json_meta), onClick = onImport, trailing = chevron)
            Hairline()
            ListRow(stringResource(R.string.import_napper), meta = stringResource(R.string.import_napper_meta), onClick = onImportNapper, trailing = chevron)
        }

        Caption(stringResource(R.string.caption_about))
        RowGroup {
            ListRow(stringResource(R.string.safe_sleep), meta = stringResource(R.string.safe_sleep_meta))
            Hairline()
            ListRow(stringResource(R.string.privacy), meta = stringResource(if (sync != null) R.string.privacy_meta_sync else R.string.privacy_meta))
            Hairline()
            val version = runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: ""
            ListRow(stringResource(R.string.version), value = version)
            Hairline()
            // The brand's home. Opens the browser (an intent: the app itself needs no internet);
            // the UTM tags let the shop count visits from the app (docs/tata-slings/monetization.md).
            ListRow(stringResource(R.string.brand_line), meta = stringResource(R.string.brand_meta), trailing = chevron, onClick = {
                openShop(context, br.gabriel.cochilo.store.Store.about)
            })
            about()
        }
    }
}

/** Opens a shop link in the browser: an intent, so the app itself needs no internet. */
fun openShop(context: android.content.Context, url: String) {
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

/** Settings › Notifications › Window reminders: the switch, and when (both leads or one), one tap each. */
@Composable
internal fun ReminderRows(reminders: Boolean, onReminders: (Boolean) -> Unit, leads: Set<Int>, onLeads: (Set<Int>) -> Unit) {
    val context = LocalContext.current
    br.gabriel.cochilo.ui.kit.SwitchRow(stringResource(R.string.reminders), stringResource(R.string.reminders_meta), reminders, onReminders)
    if (reminders) {
        val both = br.gabriel.cochilo.domain.Reminders.LEADS.toSet()
        val options = listOf(both, setOf(30), setOf(15))
        Column(Modifier.padding(start = Spacing.space4, end = Spacing.space4, bottom = Spacing.space3)) {
            FilterChips(options, leads, { o ->
                when (o) { both -> context.getString(R.string.reminders_both); setOf(30) -> context.getString(R.string.reminders_30); else -> context.getString(R.string.reminders_15) }
            }, onSelect = onLeads)
        }
    }
}
