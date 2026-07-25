package me.magnum.melonds.ui.emulator.input

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.MotionEvent.PointerCoords
import android.view.View
import me.magnum.melonds.MelonEmulator.onScreenRelease
import me.magnum.melonds.domain.model.Input
import me.magnum.melonds.domain.model.Point
import me.magnum.melonds.domain.model.Rect

/**
 * Translates touches on a view into DS touchscreen input. By default the whole view maps to the
 * touchscreen. If [touchRectProvider] is provided, the returned rect (in view coordinates) is
 * used as the touchscreen area instead.
 */
class TouchscreenInputHandler(inputListener: IInputListener, private val touchRectProvider: (() -> Rect)? = null) : BaseInputHandler(inputListener) {
    private val touchPoint: Point = Point()

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View, event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                inputListener.onKeyPress(Input.TOUCHSCREEN)
                inputListener.onTouch(normalizeTouchCoordinates(event, v.width, v.height))
            }
            MotionEvent.ACTION_MOVE -> {
                inputListener.onTouch(normalizeTouchCoordinates(event, v.width, v.height))
            }
            MotionEvent.ACTION_UP -> {
                inputListener.onKeyReleased(Input.TOUCHSCREEN)
                onScreenRelease()
            }
        }
        return true
    }

    private fun normalizeTouchCoordinates(event: MotionEvent, viewWidth: Int, viewHeight: Int): Point {
        var averageTouchX = 0f
        var averageTouchY = 0f
        val pointerCoordinates = PointerCoords()

        // Average out touch positions. Even though the DS has a resistive touch screen, some games rely on the nuances
        // of this technology for some mechanics. Averaging out the coordinates of the touch position allows us to
        // simulate those nuances to some degree
        for (i in 0 until event.pointerCount) {
            event.getPointerCoords(i, pointerCoordinates)
            averageTouchX += pointerCoordinates.x
            averageTouchY += pointerCoordinates.y
        }
        averageTouchX /= event.pointerCount
        averageTouchY /= event.pointerCount

        val touchRect = touchRectProvider?.invoke()
        if (touchRect != null && touchRect.width > 0 && touchRect.height > 0) {
            touchPoint.x = ((averageTouchX - touchRect.x) / touchRect.width * 256).toInt().coerceIn(0, 255)
            touchPoint.y = ((averageTouchY - touchRect.y) / touchRect.height * 192).toInt().coerceIn(0, 191)
        } else {
            touchPoint.x = (averageTouchX / viewWidth * 256).toInt().coerceIn(0, 255)
            touchPoint.y = (averageTouchY / viewHeight * 192).toInt().coerceIn(0, 191)
        }
        return touchPoint
    }
}