package com.example.view360.communication

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import com.example.view360.ConnException
import com.example.view360.enums.ControlMsg
import com.example.view360.enums.ExceptionMsg
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.PrintWriter
import java.net.Socket
import kotlin.jvm.Throws


class Host(
    val name: String,
    var seqNo: Int,
    val socket: Socket
) {
    private val _isConnected = MutableStateFlow(true)
    val isConnected: StateFlow<Boolean> get() = _isConnected

    val inputStream: InputStream?
    private val reader: BufferedReader?
    private val outputStream: OutputStream?
    private val writer: PrintWriter?

    init {
        try {
            inputStream = socket.getInputStream()
            reader = BufferedReader(InputStreamReader(inputStream))
            outputStream = socket.getOutputStream()
            writer = PrintWriter(outputStream, true)

            if(inputStream == null || reader == null || outputStream == null || writer == null){
                throw RuntimeException("Stream initialization failed")
            }
        } catch (e: IOException) {
            throw RuntimeException("Stream initialization failed", e)
        }
    }

    fun disconnect(){
        _isConnected.value = false
    }

    fun sendImg(inputImgStream: InputStream): Boolean {

        if(outputStream == null) return false

        val buffer = ByteArray(4096)
        var bytesRead: Int

        while (inputImgStream.read(buffer).also { bytesRead = it } != -1) {
            outputStream.write(buffer, 0, bytesRead)
        }

        inputImgStream.close()
        outputStream.write(ControlMsg.EOF.message.toByteArray())
        outputStream.flush()

        return true
    }

    fun receiveImg(context: Context): Uri? {

        if(!socket.isConnected) throw ConnException(ExceptionMsg.ConnectionFailed.message)
        if(socket.isClosed) throw ConnException(ExceptionMsg.SocketClose.message)

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "received_img${System.currentTimeMillis()}.jpg")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
        }

        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            ?: return null

        val outputStream = context.contentResolver.openOutputStream(uri) ?: return null
        val buffer = ByteArray(4096)
        var receivedData = ByteArray(0)
        val eofBytes = ControlMsg.EOF.message.toByteArray()

        try {
            if (inputStream == null) return null

            while (true) {
                val bytesRead = inputStream.read(buffer)
                if (bytesRead == -1) {
                    return null
                }

                // Append new data to receivedData
                receivedData += buffer.copyOf(bytesRead)

                val eofIndex = receivedData.indexOfSequence(eofBytes)

                if (eofIndex != -1) {
                    outputStream.write(receivedData.copyOf(eofIndex))
                    receivedData = receivedData.copyOfRange(eofIndex + eofBytes.size, receivedData.size)
                    break
                }

                // If buffer is full, write to file and clear
                if (receivedData.size >= buffer.size) {
                    outputStream.write(receivedData)
                    receivedData = ByteArray(0)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        } finally {
            outputStream.flush()
            outputStream.close()
        }

        return uri
    }


    fun sendTxt(txt: String){
        writer?.println(txt)
    }

    fun receiveTxt(): String?{
        return reader?.readLine()
    }

    fun sendInt(num: Int){
        sendTxt(num.toString())
    }
    fun receiveInt():Int?{
        return receiveTxt()?.toIntOrNull()
    }
    fun sendControlMsg(msg: ControlMsg = ControlMsg.ACK){
        writer?.write(msg.message)
    }
    fun receiveControlMsg(): ControlMsg{
        return ControlMsg.fromMessage(receiveTxt().toString())
    }

}

fun ByteArray.indexOfSequence(sequence: ByteArray): Int {
    if (sequence.isEmpty() || this.size < sequence.size) return -1
    for (i in 0..this.size - sequence.size) {
        if (this.copyOfRange(i, i + sequence.size).contentEquals(sequence)) {
            return i
        }
    }
    return -1
}
