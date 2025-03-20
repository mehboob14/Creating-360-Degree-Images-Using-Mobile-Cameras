package com.example.view360

import android.Manifest
import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.view360.enums.NestedScreen
import com.example.view360.enums.Screen
import com.example.view360.ui.composables.CameraScreen
import com.example.view360.ui.composables.HomeScreen
import com.example.view360.ui.composables.MultiDeviceScreen
import com.example.view360.ui.theme.View360Theme
import com.example.view360.utilityClasses.PermissionMngr
import org.opencv.android.OpenCVLoader


class MainActivity : ComponentActivity() {

    private lateinit var permissionMngr: PermissionMngr


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if(!OpenCVLoader.initLocal()){
            Toast.makeText(this, "OpenCV not loaded", Toast.LENGTH_SHORT).show()
            return
        }
        Log.d("image","Main activity")

        val launcher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()){ isGranted ->

                if(isGranted){
                    Toast.makeText(this, "Permission granted.", Toast.LENGTH_SHORT).show()
                }
                else{
                    Toast.makeText(this, "Permission was denied.", Toast.LENGTH_SHORT).show()
                    Toast.makeText(this, "Go to app setting to grand the permission.", Toast.LENGTH_SHORT).show()
                }
            }

        permissionMngr = PermissionMngr(this,launcher)


        setContent {
            View360Theme {
                App()
            }
        }
    }

}






@Composable
fun App() {
    var selectedTab by remember { mutableStateOf(Screen.Home) }
    val context = LocalContext.current
    AppData.showScaffold.value = true

    BackHandler {
        if (selectedTab == Screen.Home) {
            (context as? Activity)?.finish()
        } else {
            selectedTab = Screen.Home
        }
    }


    Scaffold(
        topBar = { if(AppData.showScaffold.value) AppTopBar() },
        bottomBar = { if(AppData.showScaffold.value)  AppBottomBar(selectedTab) { selectedTab = it } }
    ) { paddingValues ->
        MainContent(paddingValues, selectedTab)
        }

}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar() {
    TopAppBar(
        title = { Text("View 360") },
    )
}

@Composable
fun AppBottomBar(selectedTab: Screen, onTabSelected: (Screen) -> Unit) {
    NavigationBar {

        NavigationBar {
            Screen.entries.forEach { screen ->
                NavigationBarItem(
                    icon = { Icon(screen.icon, contentDescription = screen.route) },
                    label = { Text(screen.route) },
                    selected = selectedTab == screen,
                    onClick = { onTabSelected(screen) }
                )
            }
        }

    }
}

@Composable
fun AppFAB(onClick: () -> Unit) {
    FloatingActionButton(onClick = {onClick()}) {
        Icon(Icons.Default.Add, contentDescription = "Toggle Bars")
    }
}


@Composable
fun MainContent(paddingValues: PaddingValues, selectedTab: Screen) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .background(Color.LightGray),
        contentAlignment = Alignment.Center
    ) {
        when (selectedTab) {
            Screen.Home -> HomeScreen()
            Screen.MultiDevice -> MultiDeviceScreen()
            Screen.Profile -> Profile()
            Screen.Guide -> Guide()
        }
    }
}


@Composable
fun MultiDevice(){
    Text("Multidevice Content")
}
@Composable
fun Guide(){
    Text("Guide Content")
}

@Composable
fun Profile(){
    Text("Profile Content")
}
