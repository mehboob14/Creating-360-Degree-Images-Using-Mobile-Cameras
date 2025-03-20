package com.example.view360.communication


import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import com.example.view360.AppData
import com.example.view360.enums.ControlMsg
import com.example.view360.enums.MsgStatus
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.io.*
import java.net.*
import kotlin.collections.set


class Server {
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private var serverSocket: ServerSocket? = null
    private val clients: SnapshotStateMap<Int, Host> = mutableStateMapOf()

    private fun getKeyBySeqNo(seqNo: Int): Int? {
        return clients.entries.find { it.value.seqNo == seqNo }?.key
    }
    fun getClients(): SnapshotStateMap<Int, Host> {
        return clients
    }



    fun startServer(onResult: (String) -> Unit) {
        coroutineScope.launch {
            try {
                serverSocket = ServerSocket(AppData.port)
                onResult("Server started on port ${AppData.port}")

                while (true) {
                    val clientSocket = serverSocket?.accept()
                    clientSocket?.let { socket ->
                        val newClient = Host(
                            "Client ${clients.size + 1}",
                            clients.size + 1,
                            clientSocket
                        )
                        clients[newClient.seqNo] = newClient
                        onClientDisconnect(newClient)
                    }
                }
            } catch (e: IOException) {
                onResult("Error in starting server: ${e.toString()}")
            }
        }
    }

    private suspend fun onClientDisconnect(client: Host) {
        client.isConnected.first{ state -> !state }
        removeClient(client.seqNo)
    }


    fun removeClient(seqNo: Int){
        clients[seqNo]?.socket?.close()
        clients.remove(seqNo)
    }

    fun sendImgsTo(seqNo: Int, context: Context, imageUris: List<Uri>, onResult: (String) -> Unit,
                   onImgReceive: (Uri) -> Unit) {
        val currClient = clients[seqNo]
        if (currClient == null) {
            onResult("Client $seqNo not found.")
            return
        }

        onResult("Sending images to client $seqNo")

        coroutineScope.launch {
            try {

                currClient.sendInt(imageUris.size)
                onResult("Waiting for size ACK")
                val sizeACK = currClient.receiveControlMsg()

                if (sizeACK != ControlMsg.ACK) {
                    onResult("Error: No ACK received for image size")
                    return@launch
                }


                var sentCount = 0

                for (imageUri in imageUris) {
                    var inputStream: InputStream?
                    var hasImgSent: Boolean

                    try {
                        inputStream = context.contentResolver.openInputStream(imageUri)
                    }catch (e: Exception){
                        onResult("Failed to open input stream image ${sentCount + 1}.\n${e.toString()}")
                        continue
                    }

                    if (inputStream == null) {
                        onResult("Failed to open input stream for image $seqNo")
                        continue
                    }else{
                        hasImgSent = currClient.sendImg(inputStream)
                    }

                    if(!hasImgSent){
                        onResult("Failed to send image $seqNo")
                        continue
                    }

                    onResult("Waiting for image ${sentCount + 1} ACK")
                    val ack = currClient.receiveControlMsg()
                    if (ack != ControlMsg.ACK) {
                        onResult("Error: No ACK received for image $imageUri")
                        continue
                    }

                    sentCount++
                    onResult("Sent  image $sentCount/${imageUris.size}")
                }
                onResult("Start image receiving.")
                val file = currClient.receiveImg(context)
                if(file == null){
                    onResult("Failed to receive image")
                }else{
                    onImgReceive(file)
                }

            } catch (e: Exception) {
                onResult("Error sending image to client $seqNo \n${e.message}")
            }
        }
    }

    private suspend fun sendImages(currClient: Host, imageStreams: List<InputStream>, onResult: (String) -> Unit) {
        currClient.sendInt(imageStreams.size)
        onResult("Waiting for size ACK")
        Log.d("server","Waiting for size ACK")
        val sizeACK = currClient.receiveControlMsg()

        if (sizeACK != ControlMsg.ACK) {
            onResult("Error: No ACK received for image size")
            Log.d("server","Error: No ACK received for image size")
            return
        }

        var sentCount = 0
        for (inputStream in imageStreams) {
            if (!currClient.sendImg(inputStream)) {
                onResult("Failed to send image ${sentCount + 1}")
                Log.d("server","Failed to send image ${sentCount + 1}")
                continue
            }

            onResult("Waiting for image ${sentCount + 1} ACK")
            if (currClient.receiveControlMsg() != ControlMsg.ACK) {
                onResult("Error: No ACK received for image ${sentCount + 1}")
                continue
            }

            sentCount++
            onResult("Sent image $sentCount/${imageStreams.size}")
            Log.d("server","Sent image $sentCount/${imageStreams.size}")
        }
    }

    fun sendUriImgsTo(seqNo: Int, imageUris: List<Uri>, context: Context, onResult: (String) -> Unit, onImgReceive: (Uri) -> Unit) {
        val currClient = clients[seqNo]
        if (currClient == null) {
            onResult("Client $seqNo not found.")
            return
        }

        onResult("Sending images to client $seqNo")
        coroutineScope.launch {
            try {
                val imageStreams = imageUris.mapNotNull {
                    try { context.contentResolver.openInputStream(it) } catch (e: Exception) {
                        onResult("Failed to open input stream for image.")
                        null
                    }
                }
                sendImages(currClient, imageStreams, onResult)
                onResult("Start image receiving.")
                val file = currClient.receiveImg(context)
                if (file == null) {
                    onResult("Failed to receive image")
                } else {
                    onImgReceive(file)
                    onResult("Image received")
                }
            } catch (e: Exception) {
                onResult("Error sending image to client $seqNo \n${e.message}")
            }
        }
    }

    fun sendImgsTo(seqNo: Int, imageFiles: List<File>,context: Context, onResult: (String) -> Unit, onImgReceive: (Uri) -> Unit) {
        val currClient = clients[seqNo]
        if (currClient == null) {
            onResult("Client $seqNo not found.")
            return
        }

        onResult("Sending images to client $seqNo")
        coroutineScope.launch {
            try {
                val imageStreams = imageFiles.mapNotNull {
                    try { it.inputStream() } catch (e: Exception) {
                        onResult("Failed to open input stream for image.")
                        null
                    }
                }

                sendImages(currClient, imageStreams, onResult)

                onResult("Receiving stiched images.")
                val file = currClient.receiveImg(context)
                if (file == null) {
                    onResult("Failed to receive image")
                } else {
                    onImgReceive(file)
                    onResult("Image received")
                }
            } catch (e: Exception) {
                onResult("Error in image sharing with client $seqNo \n${e.message}")
            }
        }
    }




    fun stopServer() {
        coroutineScope.cancel()
        serverSocket?.close()
        clients.keys.forEach { seqNo ->
            removeClient(seqNo)
        }
        clients.clear()
    }


}