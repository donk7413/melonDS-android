package me.magnum.melonds.ui.shortcutsetup

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.graphics.applyCanvas
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.IconCompat
import me.magnum.melonds.R
import me.magnum.melonds.domain.model.RomIconFiltering
import me.magnum.melonds.domain.model.rom.Rom
import me.magnum.melonds.ui.emulator.EmulatorActivity
import me.magnum.melonds.ui.romlist.RomIcon

/**
 * Builds launcher shortcuts that boot a ROM directly in the emulator.
 */
object RomShortcutFactory {

    fun createRomShortcutInfo(context: Context, rom: Rom, romIcon: RomIcon): ShortcutInfoCompat {
        val intent = Intent("${context.packageName}.LAUNCH_ROM").apply {
            putExtra(EmulatorActivity.KEY_URI, rom.uri.toString())
        }

        return ShortcutInfoCompat.Builder(context, rom.uri.toString())
            .setShortLabel(rom.config.customName ?: rom.name)
            .setIcon(IconCompat.createWithAdaptiveBitmap(buildShortcutBitmap(context, romIcon)))
            .setIntent(intent)
            .build()
    }

    private fun buildShortcutBitmap(context: Context, romIcon: RomIcon): Bitmap {
        val iconBitmap = romIcon.bitmap ?: BitmapFactory.decodeResource(context.resources, R.drawable.logo_splash)
        val shortcutBitmap = createBitmap(256, 256)

        return shortcutBitmap.applyCanvas {
            drawRect(Rect(0, 0, width, height), Paint().apply { color = Color.WHITE })
            val iconRect = Rect(77, 77, shortcutBitmap.width - 77, shortcutBitmap.height - 77)
            drawBitmap(iconBitmap, null, iconRect, Paint().apply { isFilterBitmap = romIcon.filtering == RomIconFiltering.LINEAR })
        }
    }
}
