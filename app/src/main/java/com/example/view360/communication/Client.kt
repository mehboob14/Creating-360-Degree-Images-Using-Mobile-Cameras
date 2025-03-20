package com.example.view360.communication

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import com.example.view360.enums.ControlMsg
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.net.Socket

class Client(private val context: Context) {
    private var server: Host? = null
    private val connection: Connection = Connection(context)
    private val coroutineScope = CoroutineScope(Dispatchers.IO)


    suspend fun connToServer(serverIp: String, serverPort: Int) {
        if (!connection.isWifiEnabled()) {
            throw RuntimeException("Wifi is closed.")
        }
        try {
            withContext(Dispatchers.IO) { // Runs on IO thread, but propagates exceptions
                server = Host("Server", 0, Socket(serverIp, serverPort))
            }
        } catch (e: Exception) {
            throw RuntimeException("Couldn't connect to server.")
        }
    }


    private suspend fun sendImages(host: Host, imageStreams: List<InputStream>, onResult: (String) -> Unit) {
        host.sendInt(imageStreams.size)
        onResult("Waiting for size ACK")
        Log.d("server","Waiting for size ACK")
        val sizeACK = host.receiveControlMsg()

        if (sizeACK != ControlMsg.ACK) {
            onResult("Error: No ACK received for image size")
            Log.d("server","Error: No ACK received for image size")
            return
        }

        var sentCount = 0
        for (inputStream in imageStreams) {
            if (!host.sendImg(inputStream)) {
                onResult("Failed to send image ${sentCount + 1}")
                Log.d("server","Failed to send image ${sentCount + 1}")
                continue
            }

            onResult("Waiting for image ${sentCount + 1} ACK")
            if (host.receiveControlMsg() != ControlMsg.ACK) {
                onResult("Error: No ACK received for image ${sentCount + 1}")
                continue
            }

            sentCount++
            onResult("Sent image $sentCount/${imageStreams.size}")
            Log.d("server","Sent image $sentCount/${imageStreams.size}")
        }
    }


    fun sendImgsToServer(imageFiles: List<File>,onResult: (String) -> Unit, onImgReceive: (Uri) -> Unit) {
        if(server == null){
            onResult("Server not connected")
            return
        }

        onResult("Sending images to server")
        coroutineScope.launch {
            try {
                val imageStreams = imageFiles.mapNotNull {
                    try { it.inputStream() } catch (e: Exception) {
                        onResult("Failed to open input stream for image.")
                        null
                    }
                }

                sendImages(server!!, imageStreams, onResult)

                onResult("Receiving stiched images.")
                val file = server!!.receiveImg(context)
                if (file == null) {
                    onResult("Failed to receive image")
                } else {
                    onImgReceive(file)
                    onResult("Image received")
                }
            } catch (e: Exception) {
                onResult("Error in image sharing with server. \n${e.message}")
            }
        }
    }

}