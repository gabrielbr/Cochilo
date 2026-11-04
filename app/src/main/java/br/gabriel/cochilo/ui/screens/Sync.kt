package br.gabriel.cochilo.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import br.gabriel.cochilo.R
import br.gabriel.cochilo.sync.PartnerSync
import br.gabriel.cochilo.sync.Peer
import br.gabriel.cochilo.ui.Format
import br.gabriel.cochilo.ui.kit.Button
import br.gabriel.cochilo.ui.kit.ButtonKind
import br.gabriel.cochilo.ui.kit.Caption
import br.gabriel.cochilo.ui.kit.GIcon
import br.gabriel.cochilo.ui.kit.Hairline
import br.gabriel.cochilo.ui.kit.ListRow
import br.gabriel.cochilo.ui.kit.RowGroup
import br.gabriel.cochilo.ui.theme.CTokens.Spacing
import br.gabriel.cochilo.ui.theme.CochiloTheme
import br.gabriel.cochilo.ui.theme.Type

/**
 * Settings › Sync (0.47.0, docs/sync.md). Not paired: one row, "Pair a phone". Paired: who, when it
 * last synced (or that the partner isn't on this Wi-Fi), "Sync now" and "Unpair".
 */
@Composable
fun SyncSection(state: PartnerSync.State, f: Format, onPair: () -> Unit, onSyncNow: () -> Unit, onUnpair: () -> Unit) {
    val c = CochiloTheme.colors
    val chevron: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = { GIcon(R.drawable.gg_ic_chevron, null, c.muted) }
    Caption(stringResource(R.string.caption_sync))
    RowGroup {
        val partner = state.partner
        if (partner == null) {
            ListRow(stringResource(R.string.sync_pair), meta = stringResource(R.string.sync_pair_meta), onClick = onPair, trailing = chevron)
        } else {
            val meta = when {
                state.syncing -> stringResource(R.string.sync_syncing)
                state.unreachable -> stringResource(R.string.sync_unreachable, partner)
                state.lastSyncedAt != null -> stringResource(R.string.sync_last, f.dayTime(state.lastSyncedAt))
                else -> stringResource(R.string.sync_never)
            }
            ListRow(stringResource(R.string.sync_with, partner), meta = meta)
            Hairline()
            ListRow(stringResource(R.string.sync_now), onClick = onSyncNow, trailing = chevron)
            Hairline()
            ListRow(stringResource(R.string.sync_unpair), meta = stringResource(R.string.sync_unpair_meta), onClick = onUnpair)
        }
    }
}

/**
 * Pairing (docs/sync.md): the phones on this Wi-Fi that also opened "Pair a phone"; tap one. Then
 * both phones show the same six digits, and the parent says if the other phone shows them too.
 */
@Composable
fun PairSheet(
    pairing: PartnerSync.Pairing,
    onPick: (Peer) -> Unit,
    onAnswer: (Boolean) -> Unit,
    onRetry: () -> Unit,
    onClose: () -> Unit,
) {
    Sheet(onClose) { PairBody(pairing, onPick, onAnswer, onRetry, onClose) }
}

/** The pairing sheet's content, on its own for screenshots. */
@Composable
fun PairBody(
    pairing: PartnerSync.Pairing,
    onPick: (Peer) -> Unit,
    onAnswer: (Boolean) -> Unit,
    onRetry: () -> Unit,
    onClose: () -> Unit,
) {
    val c = CochiloTheme.colors
    androidx.compose.foundation.layout.Column {
        SheetTitle(stringResource(R.string.pair_title))
        when (pairing) {
            is PartnerSync.Pairing.Looking, PartnerSync.Pairing.Idle -> {
                val phones = (pairing as? PartnerSync.Pairing.Looking)?.phones.orEmpty()
                Text(
                    stringResource(R.string.pair_looking), style = Type.bodySmall, color = c.ink,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
                if (phones.isNotEmpty()) {
                    Spacer(Modifier.height(Spacing.space4))
                    RowGroup {
                        phones.forEachIndexed { i, p ->
                            if (i > 0) Hairline()
                            ListRow(
                                p.name, meta = stringResource(R.string.pair_pick_meta), onClick = { onPick(p) },
                                trailing = { GIcon(R.drawable.gg_ic_chevron, null, c.muted) },
                            )
                        }
                    }
                }
                Spacer(Modifier.height(Spacing.space6))
                Button(stringResource(R.string.cancel), onClose, ButtonKind.SECONDARY, Modifier.fillMaxWidth())
            }
            is PartnerSync.Pairing.Compare -> {
                // The number, big, grouped in threes so it can be read across the room.
                val n = pairing.number
                Text(
                    n.take(3) + " " + n.drop(3), style = Type.timer, color = c.ink,
                    modifier = Modifier.semantics { contentDescription = n.toList().joinToString(" ") },
                )
                Spacer(Modifier.height(Spacing.space3))
                if (pairing.waitingForPartner) {
                    Text(
                        stringResource(R.string.pair_waiting, pairing.partner), style = Type.bodySmall, color = c.muted,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                    Spacer(Modifier.height(Spacing.space6))
                    Button(stringResource(R.string.cancel), { onAnswer(false); onClose() }, ButtonKind.SECONDARY, Modifier.fillMaxWidth())
                } else {
                    Text(stringResource(R.string.pair_compare, pairing.partner), style = Type.body, color = c.ink)
                    Spacer(Modifier.height(Spacing.space6))
                    Button(stringResource(R.string.pair_same), { onAnswer(true) }, ButtonKind.PRIMARY, Modifier.fillMaxWidth())
                    Spacer(Modifier.height(Spacing.space2))
                    Button(stringResource(R.string.pair_different), { onAnswer(false) }, ButtonKind.SECONDARY, Modifier.fillMaxWidth())
                }
            }
            is PartnerSync.Pairing.Done -> {
                Text(stringResource(R.string.pair_done, pairing.partner), style = Type.body, color = c.ink)
                Spacer(Modifier.height(Spacing.space6))
                Button(stringResource(R.string.close), onClose, ButtonKind.PRIMARY, Modifier.fillMaxWidth())
            }
            PartnerSync.Pairing.Failed -> {
                Text(stringResource(R.string.pair_failed), style = Type.body, color = c.ink)
                Spacer(Modifier.height(Spacing.space6))
                Button(stringResource(R.string.pair_try_again), onRetry, ButtonKind.PRIMARY, Modifier.fillMaxWidth())
                Spacer(Modifier.height(Spacing.space2))
                Button(stringResource(R.string.close), onClose, ButtonKind.SECONDARY, Modifier.fillMaxWidth())
            }
        }
    }
}
