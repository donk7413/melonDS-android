package me.magnum.melonds.domain.model.autoaction

import android.net.Uri
import me.magnum.melonds.domain.model.Input
import me.magnum.melonds.domain.model.Rect
import java.util.UUID

/**
 * An automatic action bound to a ROM. When the [region] of the DS composite screen (256x384, top
 * screen above bottom screen) matches the stored reference image with a similarity of at least
 * [similarityThreshold] percent, the [steps] are executed automatically.
 */
data class RomAutoAction(
    val id: UUID,
    val romUri: Uri,
    val name: String,
    val enabled: Boolean,
    val region: Rect,
    val similarityThreshold: Int,
    val repeatWhileVisible: Boolean,
    val steps: List<AutoActionStep>,
)

data class AutoActionStep(
    val input: Input,
    val pressDurationMs: Long,
    val delayAfterMs: Long,
)
