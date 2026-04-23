package com.example.urbanfix

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupWithNavController
import com.example.urbanfix.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration
    private var statusBarTopInsetPx: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        navController = findNavController(R.id.nav_host_fragment_activity_main)
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home,
                R.id.navigation_dashboard,
                R.id.navigation_profile,
            ),
        )
        setSupportActionBar(binding.toolbarMain)
        binding.toolbarMain.setupWithNavController(navController, appBarConfiguration)
        binding.navView.setupWithNavController(navController)

        installToolbarStatusBarInset()

        navController.addOnDestinationChangedListener { _, destination, _ ->
            applyDestinationChrome(destination.id)
        }
        applyDestinationChrome(navController.currentDestination?.id)
    }

    private fun installToolbarStatusBarInset() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.toolbarMain) { toolbar, windowInsets ->
            statusBarTopInsetPx = windowInsets.getInsets(
                WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.displayCutout(),
            ).top
            val tlp = toolbar.layoutParams as ConstraintLayout.LayoutParams
            tlp.topMargin = statusBarTopInsetPx
            toolbar.layoutParams = tlp
            val onAuth = navController.currentDestination?.id == R.id.navigation_auth
            anchorNavHostBelowToolbar(!onAuth)
            windowInsets
        }
        ViewCompat.requestApplyInsets(binding.root)
    }

    private fun applyDestinationChrome(destinationId: Int?) {
        val onAuth = destinationId == R.id.navigation_auth
        binding.navView.visibility = if (onAuth) View.GONE else View.VISIBLE
        binding.toolbarMain.visibility = if (onAuth) View.GONE else View.VISIBLE
        anchorNavHostBelowToolbar(!onAuth)
        applyToolbarText(destinationId)
        invalidateOptionsMenu()
    }

    private fun applyToolbarText(destinationId: Int?) {
        when (destinationId) {
            R.id.navigation_home -> {
                supportActionBar?.title = getString(R.string.toolbar_home_title)
                supportActionBar?.subtitle = getString(R.string.toolbar_city_subtitle)
            }
            R.id.navigation_profile -> {
                supportActionBar?.title = getString(R.string.toolbar_profile_title)
                supportActionBar?.subtitle = getString(R.string.toolbar_city_subtitle)
            }
            else -> {
                supportActionBar?.subtitle = null
            }
        }
    }

    /** Na auth: NavHost od góry okna + inset status bara; inaczej od dołu toolbara (toolbar ma już topMargin = inset). */
    private fun anchorNavHostBelowToolbar(belowToolbar: Boolean) {
        val host = binding.root.findViewById<View>(R.id.nav_host_fragment_activity_main)
        val lp = host.layoutParams as ConstraintLayout.LayoutParams
        if (belowToolbar) {
            lp.topToTop = ConstraintLayout.LayoutParams.UNSET
            lp.topToBottom = R.id.toolbar_main
            lp.topMargin = 0
        } else {
            lp.topToTop = ConstraintLayout.LayoutParams.PARENT_ID
            lp.topToBottom = ConstraintLayout.LayoutParams.UNSET
            lp.topMargin = statusBarTopInsetPx
        }
        host.layoutParams = lp
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_toolbar_menu, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val onAuth = navController.currentDestination?.id == R.id.navigation_auth
        menu.findItem(R.id.action_notifications)?.isVisible = !onAuth
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_notifications -> {
                Toast.makeText(this, R.string.notifications_placeholder_toast, Toast.LENGTH_SHORT).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean =
        navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
}
