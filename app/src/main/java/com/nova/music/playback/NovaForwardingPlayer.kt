package com.nova.music.playback

import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Player

/**
 * ForwardingPlayer wrapper that ensures system media controls (notification, lock screen,
 * Bluetooth headsets, Android Auto) always see active playback commands (Play/Pause, Next, Previous)
 * and dispatches them to NovaPlayerController.
 */
class NovaForwardingPlayer(
    player: Player,
    private val playerController: NovaPlayerController
) : ForwardingPlayer(player) {

    override fun getAvailableCommands(): Player.Commands {
        val baseCommands = super.getAvailableCommands()
        val builder = baseCommands.buildUpon()
        builder.addAll(
            Player.COMMAND_PLAY_PAUSE,
            Player.COMMAND_SEEK_TO_NEXT,
            Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
            Player.COMMAND_SEEK_TO_PREVIOUS,
            Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
            Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM
        )
        return builder.build()
    }

    override fun isCommandAvailable(command: Int): Boolean {
        return when (command) {
            Player.COMMAND_PLAY_PAUSE,
            Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM -> true
            Player.COMMAND_SEEK_TO_NEXT,
            Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM -> {
                val state = playerController.playbackState.value
                state.queue.isNotEmpty()
            }
            Player.COMMAND_SEEK_TO_PREVIOUS,
            Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM -> {
                val state = playerController.playbackState.value
                state.queue.isNotEmpty()
            }
            else -> super.isCommandAvailable(command)
        }
    }

    override fun hasNextMediaItem(): Boolean {
        val state = playerController.playbackState.value
        return state.queue.size > 1
    }

    override fun hasPreviousMediaItem(): Boolean {
        val state = playerController.playbackState.value
        return state.queue.isNotEmpty()
    }

    override fun seekToNext() {
        playerController.next()
    }

    override fun seekToNextMediaItem() {
        playerController.next()
    }

    override fun seekToPrevious() {
        playerController.previous()
    }

    override fun seekToPreviousMediaItem() {
        playerController.previous()
    }

    override fun play() {
        playerController.play()
    }

    override fun pause() {
        playerController.pause()
    }
}
