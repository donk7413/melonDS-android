package me.magnum.melonds.ui.shortcutsetup

import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import me.magnum.melonds.databinding.ActivityShortcutSetupBinding
import me.magnum.melonds.domain.model.rom.Rom
import me.magnum.melonds.ui.romlist.RomListFragment
import me.magnum.melonds.ui.romlist.RomListViewModel

@AndroidEntryPoint
class ShortcutSetupActivity : AppCompatActivity() {
    companion object {
        private const val FRAGMENT_ROM_LIST = "rom_list"
    }

    private val viewModel: RomListViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT))
        super.onCreate(savedInstanceState)
        val binding = ActivityShortcutSetupBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        var defaultContentInsetLeft = -1
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            if (defaultContentInsetLeft == -1) {
                defaultContentInsetLeft = binding.toolbar.contentInsetLeft
            }

            binding.toolbar.setContentInsetsAbsolute(defaultContentInsetLeft + insets.left, binding.toolbar.contentInsetRight)
            binding.toolbar.updatePadding(
                left = insets.left,
                right = insets.right,
            )
            binding.viewStatusBarBackground.updateLayoutParams {
                height = insets.top
            }
            binding.layoutRoot.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                leftMargin = insets.left
                rightMargin = insets.right
            }

            windowInsets.inset(insets.left, insets.top, insets.right, 0)
        }

        val fragment = if (savedInstanceState == null) {
            RomListFragment.newInstance(false, RomListFragment.RomEnableCriteria.ENABLE_ALL).also {
                supportFragmentManager.commit {
                    replace(binding.layoutRoot.id, it, FRAGMENT_ROM_LIST)
                }
            }
        } else {
            supportFragmentManager.findFragmentByTag(FRAGMENT_ROM_LIST) as RomListFragment
        }

        fragment.setRomSelectedListener { onRomSelected(it) }
    }

    private fun onRomSelected(rom: Rom) {
        lifecycleScope.launch {
            val romIcon = viewModel.getRomIcon(rom)
            val shortcutInfo = RomShortcutFactory.createRomShortcutInfo(this@ShortcutSetupActivity, rom, romIcon)
            val shortcutIntent = ShortcutManagerCompat.createShortcutResultIntent(this@ShortcutSetupActivity, shortcutInfo)

            setResult(RESULT_OK, shortcutIntent)
            finish()
        }
    }
}