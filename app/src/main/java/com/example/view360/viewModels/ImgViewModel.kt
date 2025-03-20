package com.example.view360.viewModels

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import java.io.File

class ImgViewModel : ViewModel()  {
    private val _photos = mutableStateListOf<File>()
    val photos: List<File> = _photos

    fun addImg(photo: File){
        _photos.add(photo)

    }

    fun clearImgs() {
        _photos.clear()  // Clears all stored data
    }


}