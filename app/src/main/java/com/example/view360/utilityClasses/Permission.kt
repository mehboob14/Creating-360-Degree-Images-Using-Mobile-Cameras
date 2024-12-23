package com.example.view360.utilityClasses

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat

object Permission {

    fun granted(context: Context,permission: String): Boolean{
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    fun request(launcher: ActivityResultLauncher<String>,permission: String){
        launcher.launch(permission)
    }

    fun checkAndRequest(context: Context,launcher: ActivityResultLauncher<String>,permission: String){
        if(!granted(context,permission)){
            Log.d("permission","$permission not granted")
            request(launcher,permission)
        }
        else{
            Log.d("permission","$permission has granted")
        }

    }



}