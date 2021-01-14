package edu.uw.ysuzuki1.photogram

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.LinearLayout
import androidx.annotation.ContentView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.Navigation
import androidx.navigation.findNavController
import androidx.navigation.ui.onNavDestinationSelected
import androidx.preference.PreferenceManager
import androidx.preference.SwitchPreferenceCompat
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.AuthUI.IdpConfig.*
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.collection.LLRBNode

/**
 * Main activity, implements the settings and authentication
 */
class MainActivity : AppCompatActivity() {

    private val RC_SIGN_IN = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Sets the app to light/dark mode depending on whether the setting is toggled or not. The toggle will stay regardless of lifecycle
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this /* Activity context */)

        val darkmode = sharedPreferences.getBoolean("darkmode", false)

        if(darkmode) {
            val currentLayout = findViewById<LinearLayout>(R.id.MainActivity)
            currentLayout.setBackgroundColor(applicationContext.getColor(R.color.dark))
        }
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu, menu)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        // Authentication logic with firebase. Implemented email log in.
        when (item.itemId) {
            R.id.log_in_menu -> {
                val auth = FirebaseAuth.getInstance()
                if (auth.currentUser != null) {
                    AuthUI.getInstance()
                        .signOut(this)
                        .addOnCompleteListener {
                            invalidateOptionsMenu()
                        }

                } else {
                    startActivityForResult(
                        AuthUI.getInstance()
                            .createSignInIntentBuilder()
                            .setAvailableProviders(
                                listOf(
                                    EmailBuilder().build()
                                )
                            )
                            .build(),
                        RC_SIGN_IN
                    )
                }
            }

            R.id.fragment_settings -> {
                val navController = findNavController(R.id.nav_host_fragment)
                return item.onNavDestinationSelected(navController) || super.onOptionsItemSelected(item)
            }
        }

        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {

        // The navigation bar setting display
        if (FirebaseAuth.getInstance().currentUser != null) {
            menu?.findItem(R.id.log_in_menu)?.title = "Log Out"
        } else {
            menu?.findItem(R.id.log_in_menu)?.title = "Log In"
        }
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        // Display toast if log in failed for any reason.
        if (requestCode == RC_SIGN_IN) {
            val response = IdpResponse.fromResultIntent(data)
            if (resultCode == Activity.RESULT_OK) {
                invalidateOptionsMenu()
            } else {
               Log.v("MainActivity", "Log in Failed: $response")
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }
}