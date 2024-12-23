package com.example.view360

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.example.view360.ui.composables.CameraPreview
import com.example.view360.utilityClasses.Permission
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale





fun MainActivity.capturePhoto(imageCapture: ImageCapture, photoPaths: MutableList<String>) {

    val fileName = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.US).format(System.currentTimeMillis()) + ".jpg"
    var path = createPrivateDir()

    if(path == null){
        Toast.makeText(this, "Error getting path", Toast.LENGTH_SHORT).show()
        return
    }




    val photoFile = File(
        path,
        fileName
    )


    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

    imageCapture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(this),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                photoPaths.add(photoFile.absolutePath)
            }

            override fun onError(exception: ImageCaptureException) {
                Toast.makeText(baseContext, "Photo capture failed: ${exception.message}", Toast.LENGTH_SHORT).show()
                Log.e("CameraX", "Photo capture failed: ${exception.message}", exception)
            }
        }
    )
}




fun MainActivity.startCamera(requestPermissionLauncher: ActivityResultLauncher<String>){
    if(!Permission.granted(this,Manifest.permission.CAMERA)){
        requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        return
    }

    val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

    cameraProviderFuture.addListener({
        val cameraProvider = cameraProviderFuture.get()
        val imageCapture = ImageCapture.Builder().build()
        val preview = androidx.camera.core.Preview.Builder().build()
        val previewView = PreviewView(this)
        preview.surfaceProvider = previewView.surfaceProvider

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                this,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageCapture
            )

            setContent {
                CameraPreview(previewView,imageCapture, ::capturePhoto)
            }

        } catch (exc: Exception) {
            Log.e("Camera5", "Use case binding failed", exc)
        }
    }, ContextCompat.getMainExecutor(this))
}

fun MainActivity.createPrivateDir(): File?{
    var dir = File(this.getExternalFilesDir(null), "currentSessionPhotos")

    if(dir == null){
        return null
    }

    if (!dir.exists()) {
        if (!dir.mkdirs()) {
            return null
        }
    }

    return dir
}



