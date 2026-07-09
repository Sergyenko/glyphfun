package com.sergy.glyphfun

import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

/**
 * Quick Settings tile: tap to start a 25-minute focus session shown as
 * a draining hourglass on the Glyph Matrix; tap again to stop. The
 * running service refreshes the subtitle once a minute.
 */
class PomodoroTileService : TileService() {

    override fun onStartListening() {
        updateTile()
    }

    override fun onClick() {
        val action =
            if (PomodoroService.running) PomodoroService.ACTION_STOP
            else PomodoroService.ACTION_START
        startForegroundService(
            Intent(this, PomodoroService::class.java).setAction(action))
        Handler(Looper.getMainLooper()).postDelayed({ updateTile() }, 400)
    }

    private fun updateTile() {
        qsTile?.apply {
            label = getString(R.string.pomodoro_tile_label)
            if (PomodoroService.running) {
                state = Tile.STATE_ACTIVE
                subtitle = "${PomodoroService.phase.label} · ${PomodoroService.remainingMinutes()} min"
            } else {
                state = Tile.STATE_INACTIVE
                subtitle = "25 + 5"
            }
            updateTile()
        }
    }
}
