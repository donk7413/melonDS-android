package me.magnum.melonds.ui.emulator.autoaction.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Slider
import androidx.compose.material.Surface
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import me.magnum.melonds.R
import me.magnum.melonds.domain.model.Input
import me.magnum.melonds.domain.model.Rect
import me.magnum.melonds.domain.model.autoaction.AutoActionStep
import me.magnum.melonds.domain.model.autoaction.AutoActionTrigger
import me.magnum.melonds.domain.model.autoaction.AutoActionTriggerMode
import me.magnum.melonds.domain.model.autoaction.RomAutoAction
import me.magnum.melonds.ui.common.melonOutlinedTextFieldColors
import me.magnum.melonds.ui.common.melonTextButtonColors
import java.util.UUID
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private const val SCREENSHOT_WIDTH = 256
private const val SCREENSHOT_HEIGHT = 384

// Regular DS button presses plus frontend-only actions that can be scripted into a sequence
private val SELECTABLE_STEP_INPUTS = Input.SYSTEM_BUTTONS + listOf(Input.TOGGLE_FULLSCREEN, Input.SWAP_SCREENS)

@Composable
private fun stepInputLabel(input: Input): String {
    val resource = when (input) {
        Input.A -> R.string.input_a
        Input.B -> R.string.input_b
        Input.X -> R.string.input_x
        Input.Y -> R.string.input_y
        Input.LEFT -> R.string.input_left
        Input.RIGHT -> R.string.input_right
        Input.UP -> R.string.input_up
        Input.DOWN -> R.string.input_down
        Input.L -> R.string.input_l
        Input.R -> R.string.input_r
        Input.START -> R.string.input_start
        Input.SELECT -> R.string.input_select
        Input.TOGGLE_FULLSCREEN -> R.string.auto_action_input_toggle_fullscreen
        Input.SWAP_SCREENS -> R.string.input_swap_screens
        else -> return input.name
    }
    return stringResource(resource)
}

private data class TriggerDraft(
    val referencedActionId: UUID?,
    val mode: AutoActionTriggerMode,
)

@Composable
fun AutoActionEditorDialog(
    screenshot: Bitmap,
    existingActions: List<RomAutoAction>,
    onSave: (name: String, region: Rect, similarityThreshold: Int, repeatWhileVisible: Boolean, steps: List<AutoActionStep>, triggers: List<AutoActionTrigger>) -> Unit,
    onCancel: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var threshold by remember { mutableStateOf(90f) }
    var repeatWhileVisible by remember { mutableStateOf(false) }
    var selectionStart by remember { mutableStateOf<Offset?>(null) }
    var selectionEnd by remember { mutableStateOf<Offset?>(null) }
    var imageSize by remember { mutableStateOf(IntSize.Zero) }
    val steps = remember { mutableStateListOf<AutoActionStep>() }
    val triggers = remember { mutableStateListOf<TriggerDraft>() }

    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colors.surface,
        ) {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(R.string.auto_action_new),
                    style = MaterialTheme.typography.h6,
                )

                Text(
                    text = stringResource(R.string.auto_action_select_zone),
                    style = MaterialTheme.typography.body2,
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(SCREENSHOT_WIDTH.toFloat() / SCREENSHOT_HEIGHT.toFloat())
                        .onSizeChanged { imageSize = it }
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = {
                                    selectionStart = it
                                    selectionEnd = it
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    selectionEnd = change.position
                                },
                            )
                        },
                ) {
                    Image(
                        bitmap = screenshot.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        filterQuality = FilterQuality.None,
                    )

                    Canvas(Modifier.fillMaxSize()) {
                        val start = selectionStart
                        val end = selectionEnd
                        if (start != null && end != null) {
                            val topLeft = Offset(min(start.x, end.x), min(start.y, end.y))
                            val selectionSize = Size(abs(end.x - start.x), abs(end.y - start.y))
                            drawRect(Color(0x40FFFFFF), topLeft = topLeft, size = selectionSize)
                            drawRect(Color.White, topLeft = topLeft, size = selectionSize, style = Stroke(2.dp.toPx()))
                        }
                    }
                }

                Text(
                    text = stringResource(R.string.auto_action_threshold, threshold.roundToInt()),
                    style = MaterialTheme.typography.body1,
                )
                Slider(
                    value = threshold,
                    onValueChange = { threshold = it },
                    valueRange = 50f..100f,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = stringResource(R.string.auto_action_repeat),
                        style = MaterialTheme.typography.body1,
                    )
                    Switch(
                        checked = repeatWhileVisible,
                        onCheckedChange = { repeatWhileVisible = it },
                    )
                }

                Text(
                    text = stringResource(R.string.auto_action_triggers),
                    style = MaterialTheme.typography.subtitle1,
                )

                if (existingActions.isEmpty()) {
                    Text(
                        text = stringResource(R.string.auto_action_no_other_actions),
                        style = MaterialTheme.typography.body2,
                    )
                } else {
                    triggers.forEachIndexed { index, trigger ->
                        TriggerConditionRow(
                            trigger = trigger,
                            existingActions = existingActions,
                            onTriggerChanged = { triggers[index] = it },
                            onDelete = { triggers.removeAt(index) },
                        )
                    }

                    TextButton(
                        colors = melonTextButtonColors(),
                        onClick = { triggers.add(TriggerDraft(existingActions.first().id, AutoActionTriggerMode.WAS_LAST_ACTION)) },
                    ) {
                        Text(stringResource(R.string.auto_action_add_trigger).uppercase())
                    }
                }

                Text(
                    text = stringResource(R.string.auto_action_sequence),
                    style = MaterialTheme.typography.subtitle1,
                )

                steps.forEachIndexed { index, step ->
                    AutoActionStepRow(
                        step = step,
                        onStepChanged = { steps[index] = it },
                        onDelete = { steps.removeAt(index) },
                    )
                }

                TextButton(colors = melonTextButtonColors(), onClick = { steps.add(AutoActionStep(Input.A, 100, 200)) }) {
                    Text(stringResource(R.string.auto_action_add_step).uppercase())
                }

                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.auto_action_name)) },
                    singleLine = true,
                    colors = melonOutlinedTextFieldColors(),
                )

                Row(Modifier.fillMaxWidth()) {
                    TextButton(onClick = onCancel, colors = melonTextButtonColors()) {
                        Text(stringResource(R.string.cancel).uppercase())
                    }

                    Spacer(Modifier.weight(1f))

                    val selectedRegion = computeSelectedRegion(selectionStart, selectionEnd, imageSize)
                    TextButton(
                        enabled = name.isNotBlank() && selectedRegion != null && steps.isNotEmpty(),
                        colors = melonTextButtonColors(),
                        onClick = {
                            selectedRegion?.let {
                                val resolvedTriggers = triggers.mapNotNull { draft ->
                                    draft.referencedActionId?.let { id -> AutoActionTrigger(id, draft.mode) }
                                }
                                onSave(name.trim(), it, threshold.roundToInt(), repeatWhileVisible, steps.toList(), resolvedTriggers)
                            }
                        },
                    ) {
                        Text(stringResource(R.string.auto_action_save).uppercase())
                    }
                }
            }
        }
    }
}

@Composable
private fun AutoActionStepRow(
    step: AutoActionStep,
    onStepChanged: (AutoActionStep) -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box {
            var isMenuOpen by remember { mutableStateOf(false) }
            TextButton(onClick = { isMenuOpen = true }, colors = melonTextButtonColors()) {
                Text(stepInputLabel(step.input))
            }
            DropdownMenu(
                expanded = isMenuOpen,
                onDismissRequest = { isMenuOpen = false },
            ) {
                SELECTABLE_STEP_INPUTS.forEach { input ->
                    DropdownMenuItem(
                        onClick = {
                            onStepChanged(step.copy(input = input))
                            isMenuOpen = false
                        },
                    ) {
                        Text(stepInputLabel(input))
                    }
                }
            }
        }

        var pressDurationText by remember { mutableStateOf(step.pressDurationMs.toString()) }
        OutlinedTextField(
            modifier = Modifier.weight(1f),
            value = pressDurationText,
            onValueChange = { text ->
                pressDurationText = text
                text.toLongOrNull()?.let {
                    onStepChanged(step.copy(pressDurationMs = it.coerceIn(16, 10000)))
                }
            },
            label = { Text(stringResource(R.string.auto_action_press_duration)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = melonOutlinedTextFieldColors(),
        )

        var delayAfterText by remember { mutableStateOf(step.delayAfterMs.toString()) }
        OutlinedTextField(
            modifier = Modifier.weight(1f),
            value = delayAfterText,
            onValueChange = { text ->
                delayAfterText = text
                text.toLongOrNull()?.let {
                    onStepChanged(step.copy(delayAfterMs = it.coerceIn(0, 60000)))
                }
            },
            label = { Text(stringResource(R.string.auto_action_delay_after)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = melonOutlinedTextFieldColors(),
        )

        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = stringResource(R.string.delete),
            )
        }
    }
}

@Composable
private fun TriggerConditionRow(
    trigger: TriggerDraft,
    existingActions: List<RomAutoAction>,
    onTriggerChanged: (TriggerDraft) -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box {
            var isMenuOpen by remember { mutableStateOf(false) }
            val selectedName = existingActions.firstOrNull { it.id == trigger.referencedActionId }?.name
                ?: stringResource(R.string.auto_action_trigger_select_action)
            TextButton(onClick = { isMenuOpen = true }, colors = melonTextButtonColors()) {
                Text(selectedName)
            }
            DropdownMenu(
                expanded = isMenuOpen,
                onDismissRequest = { isMenuOpen = false },
            ) {
                existingActions.forEach { action ->
                    DropdownMenuItem(
                        onClick = {
                            onTriggerChanged(trigger.copy(referencedActionId = action.id))
                            isMenuOpen = false
                        },
                    ) {
                        Text(action.name)
                    }
                }
            }
        }

        Box {
            var isMenuOpen by remember { mutableStateOf(false) }
            val modeLabel = if (trigger.mode == AutoActionTriggerMode.WAS_LAST_ACTION) {
                stringResource(R.string.auto_action_trigger_was)
            } else {
                stringResource(R.string.auto_action_trigger_was_not)
            }
            TextButton(onClick = { isMenuOpen = true }, colors = melonTextButtonColors()) {
                Text(modeLabel)
            }
            DropdownMenu(
                expanded = isMenuOpen,
                onDismissRequest = { isMenuOpen = false },
            ) {
                DropdownMenuItem(
                    onClick = {
                        onTriggerChanged(trigger.copy(mode = AutoActionTriggerMode.WAS_LAST_ACTION))
                        isMenuOpen = false
                    },
                ) {
                    Text(stringResource(R.string.auto_action_trigger_was))
                }
                DropdownMenuItem(
                    onClick = {
                        onTriggerChanged(trigger.copy(mode = AutoActionTriggerMode.WAS_NOT_LAST_ACTION))
                        isMenuOpen = false
                    },
                ) {
                    Text(stringResource(R.string.auto_action_trigger_was_not))
                }
            }
        }

        Spacer(Modifier.weight(1f))

        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = stringResource(R.string.delete),
            )
        }
    }
}

private fun computeSelectedRegion(selectionStart: Offset?, selectionEnd: Offset?, imageSize: IntSize): Rect? {
    if (selectionStart == null || selectionEnd == null || imageSize.width == 0 || imageSize.height == 0) {
        return null
    }

    val scaleX = SCREENSHOT_WIDTH.toFloat() / imageSize.width
    val scaleY = SCREENSHOT_HEIGHT.toFloat() / imageSize.height
    val left = (min(selectionStart.x, selectionEnd.x) * scaleX).roundToInt().coerceIn(0, SCREENSHOT_WIDTH - 1)
    val top = (min(selectionStart.y, selectionEnd.y) * scaleY).roundToInt().coerceIn(0, SCREENSHOT_HEIGHT - 1)
    val right = (max(selectionStart.x, selectionEnd.x) * scaleX).roundToInt().coerceIn(left + 1, SCREENSHOT_WIDTH)
    val bottom = (max(selectionStart.y, selectionEnd.y) * scaleY).roundToInt().coerceIn(top + 1, SCREENSHOT_HEIGHT)

    val width = right - left
    val height = bottom - top
    // Require a minimum area to avoid degenerate selections
    if (width < 8 || height < 8) {
        return null
    }

    return Rect(left, top, width, height)
}
