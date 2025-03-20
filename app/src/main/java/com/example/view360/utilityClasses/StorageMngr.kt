package com.example.view360.utilityClasses
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import java.io.File
import java.io.FileOutputStream


class StorageMngr(private val context: Context) {
    fun saveImg(bitmap: Bitmap, fileName: String, saveInPrivate: Boolean = true): File? {
        return if (saveInPrivate) {
            val dir = context.filesDir
            val file = File(dir, "$fileName.jpg")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
            }
            file
        } else {
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "$fileName.jpg")
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
            }

            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            uri?.let {
                context.contentResolver.openOutputStream(it)?.use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
                }
            }
            null
        }
    }


    fun getSavedImgs(): List<File> {
//        val directory = context.getExternalFilesDir
        val directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        return  directory?.listFiles()?.toList() ?: emptyList()
    }

    fun deleteImgs() {
        for (file in getSavedImgs()) {
            if (!file.delete()) {
                Log.e("StorageMngr", "Failed to delete file: ${file.absolutePath}")
            }
        }
    }
}
