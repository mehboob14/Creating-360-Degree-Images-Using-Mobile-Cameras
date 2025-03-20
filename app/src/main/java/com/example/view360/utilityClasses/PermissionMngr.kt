package com.example.view360.utilityClasses

import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat

class PermissionMngr(private val context: Context,
                     private val permissionLauncher: ActivityResultLauncher<String>) {

    fun hasGranted(permission: String): Boolean{
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    fun request(launcher: ActivityResultLauncher<String>,permission: String){
        launcher.launch(permission)
    }

    fun checkAndRequest(permission: String): Boolean{
        if(!hasGranted(permission)){
            request(permissionLauncher,permission)
            return false
        }
        return true
    }



}