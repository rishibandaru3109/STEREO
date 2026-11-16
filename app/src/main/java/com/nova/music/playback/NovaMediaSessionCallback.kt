package com.nova.music.playback

import android.content.Intent
import android.os.Build
import android.view.KeyEvent
import androidx.media3.common.Player
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionResult

/**
 * Handles media session callbacks, hardware buttons (headset / Bluetooth),
 * and player command requests from external controllers and system notifications.
 */
class NovaMediaSessionCallback(
    private val playerController: NovaPlayerController
) : MediaSession.Callback {

    override fun onPlayerCommandRequest(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
        playerCommand: Int
    ): Int {
        when (playerCommand) {
            Player.COMMAND_SEEK_TO_NEXT,
            Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM -> {
                playerController.next()
                return SessionResult.RESULT_SUCCESS
            }
            Player.COMMAND_SEEK_TO_PREVIOUS,
            Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM -> {
                playerController.previous()
                return SessionResult.RESULT_SUCCESS
            }
            Player.COMMAND_PLAY_PAUSE -> {
                playerController.togglePlayPause()
                return SessionResult.RESULT_SUCCESS
            }
            else -> return super.onPlayerCommandRequest(session, controller, playerCommand)
        }
    }

    override fun onMediaButtonEvent(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
        intent: Intent
    ): Boolean {
        @Suppress("DEPRECATION")
        val keyEvent: KeyEvent? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT, KeyEvent::class.java)
        } else {
            intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT)
        }

        if (keyEvent != null && keyEvent.action == KeyEvent.ACTION_DOWN) {
            when (keyEvent.keyCode) {
                KeyEvent.KEYCODE_MEDIA_NEXT -> {
                    playerController.next()
                    return true
                }
                KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                    playerController.previous()
                    return true
                }
                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
                KeyEvent.KEYCODE_HEADSETHOOK -> {
                    playerController.togglePlayPause()
                    return true
                }
                KeyEvent.KEYCODE_MEDIA_PLAY -> {
                    playerController.play()
                    return true
                }
                KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                    playerController.pause()
                    return true
                }
            }
        }
        return super.onMediaButtonEvent(session, controller, intent)
    }
}
