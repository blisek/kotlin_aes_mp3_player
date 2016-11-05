package com.blisek.player

import com.blisek.filecrypto.FileCrypt
import javazoom.jl.player.advanced.AdvancedPlayer
import javazoom.jl.player.advanced.PlaybackEvent
import javazoom.jl.player.advanced.PlaybackListener
import java.io.InputStream

/**
 * Created by bartek-pc on 05.11.2016.
 */
class Player(private val fileCrypt: FileCrypt) : Runnable {

    var pausedOnFrame: Int = 0
    private var player: AdvancedPlayer? = null
    private var inited = false
    private var running = false

    override fun run() {
        play()
    }

    fun play(frames: Int = pausedOnFrame) {
        if (!inited) {
            initPlayer(fileCrypt.getFileAsBufferedInputStream())
            inited = true
        }
        pausedOnFrame = frames
        player?.play(pausedOnFrame, Int.MAX_VALUE)
    }

    fun pause() {
        player?.stop()
        inited = false
    }

    fun stop() {
        player?.stop()
        pausedOnFrame = 0
        inited = false
    }


    private fun initPlayer(inStr: InputStream) {
        player = AdvancedPlayer(fileCrypt.getFileAsBufferedInputStream())
        player?.setPlayBackListener(object : PlaybackListener() {

            override fun playbackFinished(event: PlaybackEvent) {
                pausedOnFrame = event.getFrame()
                println(pausedOnFrame)
            }

        })
    }
}