package com.example.urbanfix

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.urbanfix.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView

        navController = findNavController(R.id.nav_host_fragment_activity_main)
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home,
                R.id.navigation_dashboard,
                R.id.navigation_profile,
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            val onAuth = destination.id == R.id.navigation_auth
            navView.visibility = if (onAuth) View.GONE else View.VISIBLE
            applyAuthScreenChrome(onAuth)
            invalidateOptionsMenu()
        }
    }

    private fun actionBarSizePx(): Int {
        val a = theme.obtainStyledAttributes(intArrayOf(androidx.appcompat.R.attr.actionBarSize))
        val size = a.getDimensionPixelSize(0, 0)
        a.recycle()
        return size
    }

    private fun applyAuthScreenChrome(onAuth: Boolean) {
        if (onAuth) {
            supportActionBar?.hide()
        } else {
            supportActionBar?.show()
        }
        val host = binding.root.findViewById<View>(R.id.nav_host_fragment_activity_main)
        val lp = host.layoutParams as ConstraintLayout.LayoutParams
        lp.topMargin = if (onAuth) 0 else actionBarSizePx()
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
