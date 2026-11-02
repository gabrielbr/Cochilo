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
                androidx.compose.runtime.CompositionLocalProvider(br.gabriel.cochilo.ui.kit.LocalFabric provides br.gabriel.cochilo.ui.kit.Fabric.of(fabric)) { CochiloRoot(vm) }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        vm.refreshNow()
    }
}
