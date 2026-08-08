package com.bitiplay.world

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import com.bitiplay.world.audio.Sfx

class MainActivity : Activity() {

    private lateinit var view: GameView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        view = GameView(this)
        setContentView(view)
        com.bitiplay.world.audio.Loops.init(this)
        // Decoding clips and synthesising fallbacks takes a moment; keep it off
        // the UI thread.
        val ctx = applicationContext
        Thread({ Sfx.init(ctx) }, "bitiplay-sfx").start()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) goImmersive()
    }

    private fun goImmersive() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.let { ic ->
                ic.hide(WindowInsets.Type.systemBars())
                ic.systemBarsBehavior =
                    android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        }
    }

    override fun onResume() {
        super.onResume()
        view.resume()
        goImmersive()
    }

    override fun onPause() {
        view.pause()
        super.onPause()
    }

    override fun onDestroy() {
        com.bitiplay.world.audio.Music.release()
        com.bitiplay.world.audio.Loops.release()
        Sfx.release()
        super.onDestroy()
    }

    @Deprecated("Plain Activity still routes back through here.")
    override fun onBackPressed() {
        if (!view.onBackPressedHandled()) {
            @Suppress("DEPRECATION")
            super.onBackPressed()
        }
    }
}
