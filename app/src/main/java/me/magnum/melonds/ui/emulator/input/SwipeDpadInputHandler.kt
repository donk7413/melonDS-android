package me.magnum.melonds.ui.emulator.input

import android.view.MotionEvent
import android.view.View
import me.magnum.melonds.common.vibration.TouchVibrator
import me.magnum.melonds.domain.model.Input
import kotlin.math.atan2
import kotlin.math.pow

/**
 * Input handler that turns the view into a floating d-pad. The point where the finger first
 * touches the view becomes the origin and, once the finger is dragged beyond a small dead zone,
 * the direction of the drag is held as a d-pad press (8-way, so diagonals press two inputs at
 * once). Releasing the finger releases all directions.
 */
class SwipeDpadInputHandler(inputListener: IInputListener, enableHapticFeedback: Boolean, touchVibrator: TouchVibrator) : FeedbackInputHandler(inputListener, enableHapticFeedback, touchVibrator) {

    companion object {
        private const val DEAD_ZONE_DP = 24f

        // Inputs pressed on each 45º sector, starting at the right sector (0º) and rotating in
        // the direction of positive angles (downwards in view coordinates)
        private val SECTOR_INPUTS = listOf(
            listOf(Input.RIGHT),
            listOf(Input.RIGHT, Input.DOWN),
            listOf(Input.DOWN),
            listOf(Input.DOWN, Input.LEFT),
            listOf(Input.LEFT),
            listOf(Input.LEFT, Input.UP),
            listOf(Input.UP),
            listOf(Input.UP, Input.RIGHT),
        )
    }

    private var originX = 0f
    private var originY = 0f
    private val pressedInputs = mutableListOf<Input>()
    private val newPressedInputs = mutableListOf<Input>()
    // Reusable input list to avoid memory allocations
    private val tempInputList = mutableListOf<Input>()

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        newPressedInputs.clear()

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                originX = event.x
                originY = event.y
            }
            MotionEvent.ACTION_MOVE -> {
                val deltaX = event.x - originX
                val deltaY = event.y - originY
                val deadZoneSquared = (DEAD_ZONE_DP * v.resources.displayMetrics.density).pow(2)
                if (deltaX.pow(2) + deltaY.pow(2) >= deadZoneSquared) {
                    val angle = Math.toDegrees(atan2(deltaY, deltaX).toDouble())
                    // Round to the nearest sector so that each sector is centered on its direction
                    val sector = Math.floorMod(Math.round(angle / 45.0).toInt(), SECTOR_INPUTS.size)
                    newPressedInputs.addAll(SECTOR_INPUTS[sector])
                }
            }
        }

        tempInputList.clear()
        pressedInputs.filterNotTo(tempInputList) {
            it in newPressedInputs
        }.forEach {
            inputListener.onKeyReleased(it)
        }

        if (tempInputList.isNotEmpty()) {
            performHapticFeedback(v, HapticFeedbackType.KEY_RELEASE)
        }

        tempInputList.clear()
        newPressedInputs.filterNotTo(tempInputList) {
            it in pressedInputs
        }.forEach {
            inputListener.onKeyPress(it)
        }

        if (tempInputList.isNotEmpty()) {
            performHapticFeedback(v, HapticFeedbackType.KEY_PRESS)
        }

        pressedInputs.clear()
        pressedInputs.addAll(newPressedInputs)

        return true
    }
}
