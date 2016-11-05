package com.blisek.filecrypto

import org.apache.commons.io.IOUtils
import java.io.*
import java.security.Key
import java.security.SecureRandom
import java.security.spec.AlgorithmParameterSpec
import java.util.*
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Created by bartek on 05.11.16.
 */
private val BUFFER_SIZE = 2048

class FileCrypt(private val file: File, private val key: Key, algorithm: CipherType,
                mode: CipherMode, padding: PaddingType) {
    private val cipherEngine: Cipher
    private var loadedFile: ByteArray = byteArrayOf()
    private var fileEncrypted: Boolean = false
    private var iv: ByteArray? = null
    private var fileLoaded = false
    private val algorithm: String
    private val mode: String
    private val padding: String

    init {
        this.algorithm = algorithm.name
        this.mode = mode.name
        this.padding = padding.name

        cipherEngine = getCipherEngine(this.algorithm, this.mode, this.padding)

        FileInputStream(file).use {
            fileEncrypted = isFileHeaderSameAs(it, EncryptedFileHeader)
        }
    }

    val isFileLoaded: Boolean
        get() = fileLoaded

    fun getFileAsBufferedInputStream(): InputStream {
        if (fileEncrypted)
            return getFileAsBufferedInputStreamWithEncryption()
        else
            return getFileAsBufferedInputStreamWithoutEncryption()
    }

    fun getFileAsByteArray(): ByteArray {
        if(!fileLoaded)
            loadFileToMemory()
        return loadedFile
    }

    fun writeToFileEncrypted(outFile: File) {
        if(fileLoaded) {
            val iv = generateIV(16)
            initCipherEngineEncrypt(cipherEngine, iv)
            FileOutputStream(file).use {
                it.write(EncryptedFileHeader)
                it.write(iv.size)
                it.write(iv)

                CipherOutputStream(it, cipherEngine).use { cIt ->
                    cIt.write(loadedFile)
                    cIt.flush()
                }
            }
        } else {
            initCipherEngineEncrypt(cipherEngine, iv)
            val cEng = getCipherEngine(algorithm, mode, padding)
            val iv = generateIV(16)
            initCipherEngineEncrypt(cEng, iv)

            FileOutputStream(outFile).use {
                it.write(EncryptedFileHeader)
                it.write(iv.size)
                it.write(iv)

                CipherOutputStream(it, cEng).use { cIt ->
                    IOUtils.copy(getFileAsBufferedInputStream(), cIt)
                    cIt.flush()
                }
            }
        }
    }

    fun writeToFileDecrypted(outFile: File) {
        if(fileLoaded) {
            FileOutputStream(outFile).use { IOUtils.write(loadedFile, it) }
        } else {
            FileOutputStream(outFile).use {
                IOUtils.copy(getFileAsBufferedInputStream(), it)
            }
        }
    }

    fun loadFileToMemory() {
        if (fileEncrypted) loadFileToMemoryEncrypted() else loadFileToMemoryNoEncrypted()
        fileLoaded = true
    }

    fun generateIV(blockSize: Int): ByteArray {
        val randomSecureRandom = SecureRandom.getInstance("SHA1PRNG")
        val iv = ByteArray(blockSize)
        randomSecureRandom.nextBytes(iv)
        return iv
    }

    private fun getFileAsBufferedInputStreamWithEncryption() : InputStream {
        val bufIS = BufferedInputStream(FileInputStream(file))
//        val bufIS = FileInputStream(file)
        bufIS.skip(EncryptedFileHeader.size.toLong())
        val iv: ByteArray = ByteArray(bufIS.read())
        bufIS.read(iv)

        initCipherEngineDecrypt(cipherEngine, iv)
        return CipherInputStream(bufIS, cipherEngine)
    }

    private fun getFileAsBufferedInputStreamWithoutEncryption() : InputStream =
            BufferedInputStream(FileInputStream(file))
//              FileInputStream(file)

    private fun loadFileToMemoryEncrypted() {
        var buffer: ByteArray? = null
        BufferedInputStream(FileInputStream(file)).use {
            readIv(it)
            buffer = IOUtils.toByteArray(it)
        }

        initCipherEngineDecrypt(cipherEngine, iv)
        cipherEngine.update(buffer)
        loadedFile = cipherEngine.doFinal()
    }

    private fun loadFileToMemoryNoEncrypted() {
        FileInputStream(file).use {
            loadedFile = IOUtils.toByteArray(it)
        }
    }

    private fun initCipherEngine(cipher: Cipher, mode: Int, iv: ByteArray?) {
        if(iv != null && iv.size > 0)
            cipher.init(mode, key, IvParameterSpec(iv))
        else
            cipher.init(mode, key)
    }

    private fun initCipherEngineDecrypt(cipher: Cipher, iv: ByteArray? = null) {
        initCipherEngine(cipher, Cipher.DECRYPT_MODE, iv)
    }

    private fun initCipherEngineEncrypt(cipher: Cipher, iv: ByteArray? = null) {
        initCipherEngine(cipher, Cipher.ENCRYPT_MODE, iv)
    }

    private fun readIv(inStr: InputStream) {
        inStr.skip(EncryptedFileHeader.size.toLong())
        iv = ByteArray(inStr.read())
        inStr.read(iv)
    }
}
