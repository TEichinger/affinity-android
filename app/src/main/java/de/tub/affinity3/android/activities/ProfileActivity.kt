package de.tub.affinity3.android.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import de.tub.affinity3.android.R
import kotlinx.android.synthetic.main.activity_profile.toolbar

class ProfileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun setTitle(titleId: Int) {
        supportActionBar?.setTitle(titleId)
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        if (navController.currentDestination?.id == R.id.profileFragment) {
            onBackPressed()
            return true
        }
        return navController.navigateUp()
    }
}
