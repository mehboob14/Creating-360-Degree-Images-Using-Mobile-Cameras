package com.example.view360.viewModels

import android.os.FileObserver
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import java.io.File

class PhotoesViewModel : ViewModel()  {
    private val _photoes = mutableStateListOf<String>()
    val photoes: List<String> = _photoes


    fun initializePaths(folder: String) {
        val files = File(folder).listFiles()?.map { it.name } ?: emptyList()
        _photoes.clear()
        _photoes.addAll(files)
    }

    fun addPhoto(folder: String, photo: String){
        _photoes.add(photo)
    }


}