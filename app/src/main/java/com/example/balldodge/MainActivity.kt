package com.example.balldodge

import android.os.Bundle
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var gameView: GameView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        gameView = GameView(this)
        setContentView(gameView)
    }

    override fun onResume() {
        super.onResume()
        gameView.resumeGame()
    }

    override fun onPause() {
        super.onPause()
        gameView.pauseGame()
    }

    // استقبال حركة العصا التناظرية اليسرى من يد PS4 عبر البلوتوث
    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        val isJoystick = (event.source and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK
        if (isJoystick && event.action == MotionEvent.ACTION_MOVE) {
            val x = event.getAxisValue(MotionEvent.AXIS_X)
            gameView.setStickX(x)
            return true
        }
        return super.onGenericMotionEvent(event)
    }

    // دعم أزرار D-Pad كبديل، وزر X لإعادة اللعب بعد الخسارة
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                gameView.setStickX(-1f)
                return true
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                gameView.setStickX(1f)
                return true
            }
            // زر X على يد PS4 يصل عادة كـ KEYCODE_BUTTON_A
            KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_BUTTON_START -> {
                gameView.restartIfGameOver()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT -> {
                gameView.setStickX(0f)
                return true
            }
        }
        return super.onKeyUp(keyCode, event)
    }
}
