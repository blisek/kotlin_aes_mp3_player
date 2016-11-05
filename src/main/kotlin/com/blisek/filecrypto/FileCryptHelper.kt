package com.blisek.filecrypto

import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.security.Key
import java.security.KeyStore
import java.util.*

/**
 * Created by bartek on 13.11.16.
 */

internal val EncryptedFileHeader: ByteArray = "ENCMP3".toByteArray(Charsets.US_ASCII)

fun getCipherEngineName(alg: String, mode: String, padding: String?) : String =
        String.format("%s/%s/%s", alg, mode, padding ?: "NoPadding")

fun getKeyFromKeystore(keystoreFile: File, keystorePass: String, alias: String, keyPass: String) : Key {
    FileInputStream(keystoreFile).use {
        val keystore = KeyStore.getInstance("JCEKS")
        keystore.load(it, keystorePass.toCharArray())
        return keystore.getKey(alias, keyPass.toCharArray())
    }
}

fun isFileHeaderSameAs(inStr: InputStream, expected: ByteArray) : Boolean {
    val bArr = ByteArray(expected.size)
    inStr.read(bArr)
    return Arrays.equals(expected, bArr)
}