package com.example.view360

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.view360.communication.Server

object AppData {
    var showScaffold = mutableStateOf(true)
    var angleDiff = mutableStateOf(FloatArray(3))
    var processingClient: Int? = null
    val port = 8888
}