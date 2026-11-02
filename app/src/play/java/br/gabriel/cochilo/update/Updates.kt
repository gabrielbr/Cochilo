package br.gabriel.cochilo.update

import androidx.compose.runtime.Composable

// The Play build has no INTERNET permission and never updates itself: Google Play does that, and
// its policy forbids apps on Play from installing their own updates.

/** This build is updated by Google Play. */
const val SELF_UPDATE = false

@Composable
fun UpdatePrompt() = Unit

@Composable
fun AboutUpdates() = Unit
