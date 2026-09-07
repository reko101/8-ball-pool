package com.university.poolseclab

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.university.poolseclab.core.GameHolder
import com.university.poolseclab.databinding.ActivityMainBinding
import com.university.poolseclab.game.GameFragment
import com.university.poolseclab.security.MemoryMapFragment
import com.university.poolseclab.security.ReportFragment
import com.university.poolseclab.security.SecurityLabFragment
import com.university.poolseclab.security.TamperLabFragment

/**
 * Hosts the five laboratory screens.
 *
 * Android 15 note: applications targeting SDK 35 are laid out edge to edge by
 * default, so the window insets are applied here as padding. Without this the
 * content would sit under the status bar and the gesture bar.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        GameHolder.ensureInitialised()

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            WindowInsetsCompat.CONSUMED
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            val fragment: Fragment = when (item.itemId) {
                R.id.nav_game -> GameFragment()
                R.id.nav_state -> SecurityLabFragment()
                R.id.nav_memory -> MemoryMapFragment()
                R.id.nav_tamper -> TamperLabFragment()
                R.id.nav_report -> ReportFragment()
                else -> GameFragment()
            }
            show(fragment)
            true
        }

        if (savedInstanceState == null) {
            binding.bottomNav.selectedItemId = R.id.nav_game
        }
    }

    private fun show(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, fragment)
            .commit()
    }
}
