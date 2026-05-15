package io.github.mobdev

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import io.github.mobdev.prefs.PrefsManager
import io.github.mobdev.ui.screens.ChannelsFragment
import io.github.mobdev.ui.screens.LoginFragment

class MainActivity : AppCompatActivity() {

    val isLandscape: Boolean
        get() = findViewById<View?>(R.id.containerLeft) != null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val prefs = PrefsManager(this)
        val isLoggedIn = prefs.token != null && prefs.login != null

        if (savedInstanceState == null) {
            if (isLandscape) {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.containerLeft, ChannelsFragment())
                    .commit()
            } else {
                if (isLoggedIn) {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.container, ChannelsFragment())
                        .commit()
                } else {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.container, LoginFragment())
                        .commit()
                }
            }
        } else {
            // При повороте — если ландшафт и нет фрагмента в containerLeft
            if (isLandscape) {
                val existing = supportFragmentManager.findFragmentById(R.id.containerLeft)
                if (existing == null) {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.containerLeft, ChannelsFragment())
                        .commit()
                }
            }
        }
    }

    override fun onBackPressed() {
        if (isLandscape) {
            // В ландшафте кнопка назад закрывает правую панель
            val right = supportFragmentManager.findFragmentById(R.id.containerRight)
            if (right != null) {
                supportFragmentManager.beginTransaction()
                    .remove(right)
                    .commit()
                return
            }
        }
        val count = supportFragmentManager.backStackEntryCount
        if (count > 0) {
            supportFragmentManager.popBackStack()
        } else {
            super.onBackPressed()
        }
    }
}