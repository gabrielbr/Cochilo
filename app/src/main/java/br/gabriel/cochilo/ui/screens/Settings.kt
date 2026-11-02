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
    /** Extra About rows from the build (the github build's "Check for updates"). */
    about: @Composable () -> Unit = {},
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
                FilterChips(ThemeMode.entries, theme, { labels.getValue(it) }, onTheme)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Hairline()
                ListRow(stringResource(R.string.language), meta = stringResource(R.string.language_meta), trailing = chevron, onClick = {
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
        }

        Caption(stringResource(R.string.caption_data))
        RowGroup {
            ListRow(stringResource(R.string.export_json), meta = stringResource(R.string.export_json_meta), onClick = onExportJson, trailing = chevron)
            Hairline()
            ListRow(stringResource(R.string.export_csv), meta = stringResource(R.string.export_csv_meta), onClick = onExportCsv, trailing = chevron)
            Hairline()
            ListRow(stringResource(R.string.import_json), meta = stringResource(R.string.import_json_meta), onClick = onImport, trailing = chevron)
            Hairline()
            ListRow(stringResource(R.string.import_napper), meta = stringResource(R.string.import_napper_meta), onClick = onImportNapper, trailing = chevron)
        }

        Caption(stringResource(R.string.caption_about))
        RowGroup {
            ListRow(stringResource(R.string.safe_sleep), meta = stringResource(R.string.safe_sleep_meta))
            Hairline()
            ListRow(stringResource(R.string.privacy), meta = stringResource(R.string.privacy_meta))
            Hairline()
            val version = runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: ""
            ListRow(stringResource(R.string.version), value = version)
            Hairline()
            // The brand's home. Opens the browser (an intent: the app itself needs no internet);
            // the UTM tags let the shop count visits from the app (docs/tata-slings/monetization.md).
            ListRow(stringResource(R.string.brand_line), meta = stringResource(R.string.brand_meta), trailing = chevron, onClick = {
                runCatching {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(TATA_SLINGS_URL)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
            })
            about()
        }
    }
}

/** tataslings.com.br, tagged so the shop's analytics can tell visits from the app. */
const val TATA_SLINGS_URL = "https://tataslings.com.br/?utm_source=cochilo&utm_medium=app&utm_campaign=about"
