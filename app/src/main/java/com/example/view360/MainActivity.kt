package com.example.view360

import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.p2p.WifiP2pManager
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.view360.ui.theme.View360Theme
import androidx.activity.result.ActivityResultLauncher
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.view360.ui.composables.App
import android.Manifest
import androidx.activity.viewModels
import com.example.view360.viewModels.PhotoesViewModel


class MainActivity : ComponentActivity() {

    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>
    private val viewModel : PhotoesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        requestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()){ isGranted ->

                if(isGranted){
                    Toast.makeText(this, "Permission granted.", Toast.LENGTH_SHORT).show()
                }
                else{
                    Toast.makeText(this, "Permission was denied.", Toast.LENGTH_SHORT).show()
                    Toast.makeText(this, "Go to app setting to grand the permission.", Toast.LENGTH_SHORT).show()
                }
            }



        setContent {
            View360Theme {
                App(requestPermissionLauncher, ::startCamera)
            }
        }
    }

}



@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    View360Theme {
        
    }
}
