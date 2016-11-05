package com.blisek.main

import com.blisek.filecrypto.*
import com.blisek.player.Player
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.io.File
import java.security.Security
import java.util.concurrent.Executors
import javax.crypto.Cipher
import kotlin.system.exitProcess


fun getGreeting(): String {
    val words = mutableListOf<String>()
    words.add("Hello,")
    words.add("world!")

    return words.joinToString(separator = " ")
}

fun writeHelpMessage() {
    println("Usage: <keystore path> <key alias> <cipher engine> <cipher mode> <padding type> <input> <output> <work mode>")

    println("<cipher engine>:")
    CipherType.values().forEach { println("\t${it.toString()},") }

    println("\n<cipher mode>:")
    CipherMode.values().forEach { println("\t${it.toString()},") }

    println("\n<padding type>:")
    PaddingType.values().forEach { println("\t${it.toString()},") }

    println("\n<work mode>:")
    WorkMode.values().forEach { println("\t${it.toString()},") }
}

private val keystorePass = "mystorepass"
private val keyAliasPassword = "mystorepass"
private var keyAlias: String = ""
private var keyStorePath: File? = null
private var cipherEngine: CipherType? = null
private var cipherMode: CipherMode? = null
private var paddingType: PaddingType? = null
private var inputPath: File? = null
private var outputPath: File? = null
private var workMode: WorkMode? = null

fun main(args: Array<String>) {
//    println(getGreeting())
    Security.addProvider(BouncyCastleProvider())
    parseArguments(args)

    val loadedKey = getKeyFromKeystore(keyStorePath ?: throw RuntimeException("keyStorePath"),
            keystorePass,
            keyAlias,
            keyAliasPassword)

    val fileCrypt = FileCrypt(inputPath ?: throw RuntimeException("inputPath"),
            loadedKey,
            cipherEngine ?: throw RuntimeException("cipherEngine"),
            cipherMode ?: throw RuntimeException("cipherMode"),
            paddingType ?: throw RuntimeException("paddingType"))

    if(workMode != null) {

        when (workMode) {
            WorkMode.DECRYPT -> decrypt(fileCrypt)
            WorkMode.ENCRYPT -> encrypt(fileCrypt)
            WorkMode.PLAY -> play(fileCrypt)
        }

    }

}

fun decrypt(fileCrypt: FileCrypt) {
    fileCrypt.writeToFileDecrypted(outputPath ?: throw RuntimeException("outputPath"))
}

fun encrypt(fileCrypt: FileCrypt) {
    fileCrypt.writeToFileEncrypted(outputPath ?: throw RuntimeException("outputPath"))
}

fun play(fileCrypt: FileCrypt) {
//    fileCrypt.getFileAsBufferedInputStream().use {
//        val mp3Player: Player = Player(it)
//        mp3Player.play()
//    }

    val player = Player(fileCrypt)
    showControls(player)
}

private fun showControls(player: Player) {
    var playerThread: Thread? = null
    var running = true

    while (running) {

        println("[P]lay, P[A]use, [S]top, [J]ump. Choice: ")
        val choice = readLine()

        if(choice != null) {
            when (choice.toUpperCase()) {
                "P" -> {
                    if (playerThread == null) {
                        playerThread = Thread(player);
                        playerThread?.start()
                    }
                }
                "A" -> {
                    player.pause();
                    playerThread = null
                }
                "S" -> {
                    player.stop();
                    playerThread = null;
                    running = false
                }
                "J" -> {
                    println("Jump to: ")
                    val frameNum = Integer.parseInt(readLine())
                    player.stop()
                    player.pausedOnFrame = frameNum
                    playerThread = Thread(player)
                    playerThread?.start()
                }
            }
        }
    }
}

private fun parseArguments(args: Array<String>) {
    if (args.size != 8) {
        writeHelpMessage()
        throw RuntimeException("Arguments missing")
    }

    keyStorePath = File(args[0])
    keyStorePath?.exists() ?: throw RuntimeException("Keystore file don't exists")

    keyAlias = args[1]

    try {
        cipherEngine = CipherType.valueOf(args[2].toUpperCase())
    } catch (e: IllegalArgumentException) {
        throw RuntimeException("Unknown/unsupported cipher engine: ${args[1]}")
    }

    try {
        cipherMode = CipherMode.valueOf(args[3].toUpperCase())
    } catch (e: IllegalArgumentException) {
        throw RuntimeException("Unknown/unsupported cipher mode: ${args[3]}")
    }

    try {
        paddingType = PaddingType.valueOf(args[4])
    } catch (e: IllegalArgumentException) {
        throw RuntimeException("Unknown/unsupported padding type: ${args[4]}")
    }

    inputPath = File(args[5])
    inputPath?.exists() ?: throw RuntimeException("File ${args[5]} don't exists.")

    outputPath = File(args[6])

    try {
        workMode = WorkMode.valueOf(args[7].toUpperCase())
    } catch (e: IllegalArgumentException) {
        throw RuntimeException("Unknown work mode: ${args[7]}.", e)
    }
}
