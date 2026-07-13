package me.magnum.melonds.ui.romdetails

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import me.magnum.melonds.R
import me.magnum.melonds.domain.model.ConsoleType
import me.magnum.melonds.domain.model.rom.Rom
import me.magnum.melonds.ui.common.rom.EmulatorLaunchValidatorDelegate
import me.magnum.melonds.ui.emulator.EmulatorActivity
import me.magnum.melonds.ui.romdetails.ui.RomDetailsScreen
import me.magnum.melonds.ui.shortcutsetup.RomShortcutFactory
import me.magnum.melonds.ui.theme.MelonTheme

@AndroidEntryPoint
class RomDetailsActivity : AppCompatActivity() {

    companion object {
        const val KEY_ROM = "rom"
    }

    private val romDetailsViewModel by viewModels<RomDetailsViewModel>()
    private val romRetroAchievementsViewModel by viewModels<RomDetailsRetroAchievementsViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val emulatorLauncherValidatorDelegate = EmulatorLaunchValidatorDelegate(this, object : EmulatorLaunchValidatorDelegate.Callback {
            override fun onRomValidated(rom: Rom) {
                val intent = EmulatorActivity.getRomEmulatorActivityIntent(this@RomDetailsActivity, rom)
                startActivity(intent)
            }

            override fun onFirmwareValidated(consoleType: ConsoleType) {
                // Do nothing (can't launch firmware fro this screen)
            }

            override fun onValidationAborted() {
                // Do nothing
            }
        })

        setContent {
            val rom by romDetailsViewModel.rom.collectAsState()
            val romConfig by romDetailsViewModel.romConfigUiState.collectAsState()

            val retroAchievementsUiState by romRetroAchievementsViewModel.uiState.collectAsState()

            LaunchedEffect(null) {
                romRetroAchievementsViewModel.viewAchievementEvent.collect {
                    launchViewAchievementIntent(it)
                }
            }

            MelonTheme {
                RomDetailsScreen(
                    rom = rom,
                    romConfigUiState = romConfig,
                    retroAchievementsUiState = retroAchievementsUiState,
                    onNavigateBack = { onNavigateUp() },
                    onLaunchRom = {
                        emulatorLauncherValidatorDelegate.validateRom(it)
                    },
                    onPinRomToHomeScreen = {
                        pinRomToHomeScreen(it)
                    },
                    onRomConfigUpdate = {
                        romDetailsViewModel.onRomConfigUpdateEvent(it)
                    },
                    onRetroAchievementsLogin = { username, password ->
                        romRetroAchievementsViewModel.login(username, password)
                    },
                    onRetroAchievementsRetryLoad = {
                        romRetroAchievementsViewModel.retryLoadAchievements()
                    },
                    onViewAchievement = {
                        romRetroAchievementsViewModel.viewAchievement(it)
                    }
                )
            }
        }
    }

    private fun pinRomToHomeScreen(rom: Rom) {
        if (!ShortcutManagerCompat.isRequestPinShortcutSupported(this)) {
            Toast.makeText(this, R.string.pin_shortcut_not_supported, Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            val romIcon = romDetailsViewModel.getRomIcon(rom)
            val shortcutInfo = RomShortcutFactory.createRomShortcutInfo(this@RomDetailsActivity, rom, romIcon)
            ShortcutManagerCompat.requestPinShortcut(this@RomDetailsActivity, shortcutInfo, null)
        }
    }

    private fun launchViewAchievementIntent(achievementUrl: String) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = achievementUrl.toUri()
        }
        startActivity(intent)
    }
}