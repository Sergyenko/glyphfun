package com.sergy.glyphfun

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

/**
 * Quick Settings tile: one tap commits a random pixel to today's
 * tracker, buzzes, and flashes the constellation on the matrix.
 * Works from the lock screen too — nothing sensitive is exposed.
 */
class CommitTileService : TileService() {

    override fun onStartListening() {
        updateTile()
    }

    override fun onClick() {
        DayTracker.commit(this)
        MatrixFlasher.vibrate(this)
        MatrixFlasher.flash(this)
        updateTile()
    }

    private fun updateTile() {
        qsTile?.apply {
            state = Tile.STATE_ACTIVE
            label = getString(R.string.tile_label)
            subtitle = "${DayTracker.pixels(this@CommitTileService).size} ✦ today"
            updateTile()
        }
    }
}
