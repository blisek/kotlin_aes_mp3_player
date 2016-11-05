package com.blisek.filecrypto

import org.apache.commons.io.IOUtils
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.security.Key
import java.security.spec.AlgorithmParameterSpec
import java.util.*
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Created by bartek on 13.11.16.
 */

class FileCrypt(private val file: File, private val key: Key, algorithm: String, mode: String, padding: String) {
    private val cipherEngine: Cipher
    private var loadedFile: ByteArray = byteArrayOf()
    private var fileEncrypted: Boolean = false
    private var iv: ByteArray? = null

    init {
        val fullCipherEngineName = getCipherEngineName(algorithm, mode, padding)
        cipherEngine = Cipher.getInstance(algorithm)

        FileInputStream(file).use {
            fileEncrypted = isFileHeaderSameAs(it, EncryptedFileHeader)
        }
    }

    fun getFileAsBufferedInputStream(): InputStream {
        if (fileEncrypted)
            return getFileAsBufferedInputStreamWithoutEncryption()
        else
            return getFileAsBufferedInputStreamWithEncryption()
    }

    private fun getFileAsBufferedInputStreamWithEncryption() : InputStream {
        val bufIS = BufferedInputStream(FileInputStream(file))
        bufIS.skip(EncryptedFileHeader.size.toLong())
        val iv: ByteArray = ByteArray(bufIS.read())
        bufIS.read(iv)

        initCipherEngineDecrypt(iv)
        return CipherInputStream(bufIS, cipherEngine)
    }

    private fun getFileAsBufferedInputStreamWithoutEncryption() : InputStream =
            BufferedInputStream(FileInputStream(file))

    fun getFileAsByteArray(): ByteArray {
        if(loadedFile == null)
            loadFileToMemory()
        return loadedFile
    }

    fun loadFileToMemory() {
        if (fileEncrypted) loadFileToMemoryEncrypted() else loadFileToMemoryNoEncrypted()
    }

    private fun loadFileToMemoryEncrypted() {

    }

    private fun loadFileToMemoryNoEncrypted() {
        FileInputStream(file).use {
            loadedFile = IOUtils.toByteArray(it)
        }
    }

    private fun initCipherEngineDecrypt(iv: ByteArray?) {
        if(iv != null && iv.size > 0)
            cipherEngine.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(iv))
        else
            cipherEngine.init(Cipher.DECRYPT_MODE, key)
    }
}
