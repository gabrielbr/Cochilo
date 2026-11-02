package br.gabriel.cochilo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import br.gabriel.cochilo.domain.BabyGender
import br.gabriel.cochilo.domain.BabyLook
import br.gabriel.cochilo.domain.Hair
import br.gabriel.cochilo.domain.HairColor
import br.gabriel.cochilo.ui.kit.BabyAvatar
import br.gabriel.cochilo.ui.kit.BabyMood
import br.gabriel.cochilo.ui.screens.Onboarding
import br.gabriel.cochilo.ui.theme.CTokens
import br.gabriel.cochilo.ui.theme.CochiloTheme
import com.android.resources.NightMode
import org.junit.Rule
import org.junit.Test
import java.time.ZoneId

/** The baby avatar in every skin tone, hair and mood, and the onboarding that picks it (docs/onboarding.md). */
class AvatarSnapshotTest {
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5, maxPercentDifference = 0.1)

    @Test fun avatar_matrix_light() {
        paparazzi.snapshot {
            CochiloTheme(false) {
                Column(Modifier.fillMaxSize().background(CochiloTheme.colors.page).padding(CTokens.Spacing.space4), verticalArrangement = Arrangement.spacedBy(CTokens.Spacing.space2)) {
                    // Rows: skin tones; columns: hair styles (with a colour and gender each), then the moods.
                    for (skin in 1..BabyLook.SKIN_TONES) {
                        Row(horizontalArrangement = Arrangement.spacedBy(CTokens.Spacing.space1)) {
                            Hair.entries.forEachIndexed { i, h ->
                                val look = BabyLook(skin, h, HairColor.entries[(skin + i) % HairColor.entries.size], BabyGender.entries[i % 3])
                                BabyAvatar(look, size = CTokens.Size.kindBadgeLead + CTokens.Spacing.space2)
                            }
                            BabyAvatar(BabyLook(skin), size = CTokens.Size.kindBadgeLead + CTokens.Spacing.space2, mood = BabyMood.ASLEEP)
                            BabyAvatar(BabyLook(skin), size = CTokens.Size.kindBadgeLead + CTokens.Spacing.space2, mood = BabyMood.HAPPY, lift = 1f)
                        }
                    }
                }
            }
        }
    }

    @Test fun celebration_full_nap_light() {
        paparazzi.snapshot {
            CochiloTheme(false) {
                Column(Modifier.fillMaxSize().background(CochiloTheme.colors.page).padding(CTokens.Spacing.space4)) {
                    br.gabriel.cochilo.ui.kit.CelebrationCard("A full nap. Well done, both of you.", BabyLook(4, Hair.CURLY, HairColor.BLACK, BabyGender.GIRL), quiet = false, onDone = {}, preview = true)
                }
            }
        }
    }

    @Test fun celebration_quiet_ptBR_dark() {
        paparazzi.unsafeUpdateConfig(DeviceConfig.PIXEL_5.copy(nightMode = NightMode.NIGHT, locale = "pt-rBR"))
        paparazzi.snapshot {
            CochiloTheme(true) {
                Column(Modifier.fillMaxSize().background(CochiloTheme.colors.page).padding(CTokens.Spacing.space4)) {
                    br.gabriel.cochilo.ui.kit.CelebrationCard("Rotina feita. Boa noite.", BabyLook(), quiet = true, onDone = {}, preview = true)
                }
            }
        }
    }

    @Test fun ritual_checklist_ptBR_light() {
        paparazzi.unsafeUpdateConfig(DeviceConfig.PIXEL_5.copy(nightMode = NightMode.NOTNIGHT, locale = "pt-rBR"))
        paparazzi.snapshot {
            CochiloTheme(false) {
                Column(Modifier.fillMaxSize().background(CochiloTheme.colors.page).padding(CTokens.Spacing.space6)) {
                    br.gabriel.cochilo.ui.kit.Checklist(listOf("Banho", "Fralda e pijama", "Luz baixa", "Mamada", "Uma música no colo"), setOf(0, 1, 2)) {}
                }
            }
        }
    }

    @Test fun onboarding_meet_light() {
        paparazzi.unsafeUpdateConfig(DeviceConfig.PIXEL_5.copy(nightMode = NightMode.NOTNIGHT))
        paparazzi.snapshot { CochiloTheme(false) { br.gabriel.cochilo.ui.kit.Frame { Onboarding(ZoneId.systemDefault(), {}, previewName = "Dante") } } }
    }

    @Test fun onboarding_look_ptBR_dark() {
        paparazzi.unsafeUpdateConfig(DeviceConfig.PIXEL_5.copy(nightMode = NightMode.NIGHT, locale = "pt-rBR"))
        paparazzi.snapshot { CochiloTheme(true) { br.gabriel.cochilo.ui.kit.Frame { Onboarding(ZoneId.systemDefault(), {}, startStep = 1, previewName = "Dante") } } }
    }
}
