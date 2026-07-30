package me.magnum.melonds.ui.emulator.autoaction.ui

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.launch
import me.magnum.melonds.R
import me.magnum.melonds.ui.emulator.EmulatorViewModel

@Composable
fun AutoActionsDialog(
    viewModel: EmulatorViewModel,
    onDismiss: () -> Unit,
) {
    var editorScreenshot by remember { mutableStateOf<Bitmap?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val actions by remember { viewModel.getCurrentRomAutoActions() }.collectAsState(initial = emptyList())

    val screenshot = editorScreenshot
    if (screenshot == null) {
        Dialog(onDismissRequest = onDismiss) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colors.surface,
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.auto_actions),
                        style = MaterialTheme.typography.h6,
                    )

                    LazyColumn(Modifier.padding(top = 8.dp).weight(1f, fill = false)) {
                        if (actions.isEmpty()) {
                            item {
                                Text(
                                    modifier = Modifier.padding(vertical = 16.dp),
                                    text = stringResource(R.string.auto_actions_empty),
                                    style = MaterialTheme.typography.body2,
                                )
                            }
                        }

                        items(actions, key = { it.id }) { action ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        text = action.name,
                                        style = MaterialTheme.typography.body1,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    val modeText = if (action.repeatWhileVisible) {
                                        stringResource(R.string.auto_action_mode_repeat)
                                    } else {
                                        stringResource(R.string.auto_action_mode_once)
                                    }
                                    Text(
                                        text = "${action.similarityThreshold}% • $modeText",
                                        style = MaterialTheme.typography.caption,
                                    )
                                }

                                Switch(
                                    checked = action.enabled,
                                    onCheckedChange = { viewModel.setAutoActionEnabled(action, it) },
                                )

                                IconButton(onClick = { viewModel.deleteAutoAction(action) }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = stringResource(R.string.delete),
                                    )
                                }
                            }
                        }
                    }

                    Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                        TextButton(onClick = onDismiss) {
                            Text(stringResource(R.string.cancel).uppercase())
                        }

                        androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))

                        TextButton(
                            onClick = {
                                coroutineScope.launch {
                                    viewModel.captureScreenshotForAutoAction()?.let {
                                        editorScreenshot = it
                                    }
                                }
                            },
                        ) {
                            Text(stringResource(R.string.auto_action_new).uppercase())
                        }
                    }
                }
            }
        }
    } else {
        AutoActionEditorDialog(
            screenshot = screenshot,
            onSave = { name, region, threshold, repeatWhileVisible, steps ->
                viewModel.createAutoAction(name, region, threshold, repeatWhileVisible, steps, screenshot)
                editorScreenshot = null
            },
            onCancel = {
                editorScreenshot = null
            },
        )
    }
}
