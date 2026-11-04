package br.gabriel.cochilo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import br.gabriel.cochilo.data.ThemeMode
import br.gabriel.cochilo.ui.CochiloRoot
import br.gabriel.cochilo.ui.MainViewModel
import br.gabriel.cochilo.ui.theme.CochiloTheme

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // A file shared to Cochilo (0.41.0): imported once, not again when the activity is recreated.
        if (savedInstanceState == null) importShared(intent)
        setContent {
            val mode by vm.theme.collectAsState()
            val dark = when (mode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            // The night light (docs/night-light.md): from 19:00 to 07:00, checked each minute.
            val nightLight by vm.nightLight.collectAsState()
            var nightNow by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(br.gabriel.cochilo.domain.isNightStart(System.currentTimeMillis(), java.time.ZoneId.systemDefault())) }
            LaunchedEffect(Unit) {
                while (true) {
                    nightNow = br.gabriel.cochilo.domain.isNightStart(System.currentTimeMillis(), java.time.ZoneId.systemDefault())
                    kotlinx.coroutines.delay(60_000)
                }
            }
            val night = nightLight && nightNow
            LaunchedEffect(dark, night) {
                val style = if (dark || night) SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                else SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }
            CochiloTheme(dark, night) {
                // The fabric she picked wraps the Today card (Settings › Fabric).
                val fabric by vm.fabric.collectAsState()
                val sync by vm.syncState.collectAsState()
                // Who logged it (0.52.0): only while paired; "" stands for "no initial of my own".
                val me = if (sync.partner != null) sync.initial ?: "" else null
                androidx.compose.runtime.CompositionLocalProvider(
                    br.gabriel.cochilo.ui.kit.LocalFabric provides br.gabriel.cochilo.ui.kit.Fabric.of(fabric),
                    br.gabriel.cochilo.ui.screens.LocalSyncMe provides me,
                ) { CochiloRoot(vm) }
            }
        }
    }

    // Partner sync answers while Cochilo is on screen, and catches up when it comes back (docs/sync.md).
    override fun onStart() {
        super.onStart()
        (application as CochiloApp).sync.start(visible = true)
        // Keep in sync when closed (0.52.0): started from here, while Cochilo is on screen, as
        // Android requires of a foreground service.
        br.gabriel.cochilo.sync.SyncBackground.update(this)
    }

    override fun onStop() {
        (application as CochiloApp).sync.stop()
        super.onStop()
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        importShared(intent)
    }

    /** A share or an "open with": the file (or the shared text) goes through the same import as Settings. */
    private fun importShared(intent: android.content.Intent?) {
        val shared = br.gabriel.cochilo.backup.ShareImport.from(intent) ?: return
        when (shared) {
            is br.gabriel.cochilo.backup.ShareImport.Shared.File -> vm.importFile(shared.uri)
            is br.gabriel.cochilo.backup.ShareImport.Shared.Text -> vm.importText(shared.text)
        }
        // Handled: a later rotation or return mustn't import it again.
        intent?.action = null
    }

    override fun onResume() {
        super.onResume()
        vm.refreshNow()
    }
}
