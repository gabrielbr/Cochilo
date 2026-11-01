package br.gabriel.cochilo

import android.app.Application
import br.gabriel.cochilo.data.CochiloDb
import br.gabriel.cochilo.data.Prefs

class CochiloApp : Application() {
    val db: CochiloDb by lazy { CochiloDb.create(this) }
    val prefs: Prefs by lazy { Prefs(this) }
}
