package com.example.view360.ui.composables

import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.view360.AppData
import com.example.view360.communication.Server
import com.example.view360.enums.NestedScreen
import com.example.view360.utilityClasses.CameraMngr
import com.example.view360.utilityClasses.DeviceRotationMngr
import com.example.view360.utilityClasses.StorageMngr
import com.example.view360.viewModels.ImgViewModel
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.example.view360.communication.Client
import com.example.view360.communication.Connection
import com.example.view360.communication.Host
import java.io.File
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog


@Composable
fun MultiDeviceScreen(){
    AppData.showScaffold.value = true
    val context = LocalContext.current
    val imgViewModel: ImgViewModel = viewModel()
    val navController = rememberNavController()
    val client = remember { Client(context) }
    val navigateTo: (String) -> Unit = { navController.navigate(it) {
        popUpTo(NestedScreen.ModeSelection.route){inclusive = false}}
        AppData.showScaffold.value = false
    }

    NavHost(navController = navController, startDestination = NestedScreen.ModeSelection.route){

        composable(NestedScreen.ModeSelection.route){ ModeSelectionDialog(
            navigateTo
            )}


        composable(NestedScreen.Server.route){ ServerScreen(
            navigateTo,
            client
        )}
        composable(NestedScreen.Camera.route){ CameraScreen2(
            navigateTo,
            imgViewModel
        )}
        composable(NestedScreen.ThreeDView.route){
            ThreeDViewScreen(
                imgViewModel,
                client
            )}
        composable(NestedScreen.Search.route){ SearchScreen(navigateTo)}
    }

    DisposableEffect(Unit) {
        onDispose {
            imgViewModel.clearImgs()
        }
    }
}

@Composable
fun ModeSelectionDialog(
    onModeSelected: (String) -> Unit
) {
    Card(
        modifier = Modifier.padding(16.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Select a Mode", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(onClick = { onModeSelected(NestedScreen.Server.route) }) {
                    Text("Server")
                }
                Button(onClick = { onModeSelected(NestedScreen.Server.route)}) {
                    Text("Client")
                }
            }
        }
    }
}





@Composable
fun CameraScreen2(navigateTo:(String) -> Unit, imgVM: ImgViewModel) {

    Log.d("main","camera screen")
    val lifecycleOwner = LocalLifecycleOwner.current
    val context  = LocalContext.current


    var previewView = remember { PreviewView(context) }
    var cameraManager = remember { CameraMngr(context) }
    var storageMngr = remember {StorageMngr(context) }
    var cameraStarted by remember { mutableStateOf(false) }

    val deviceRotationMngr = remember { DeviceRotationMngr(context){ AppData.angleDiff.value = it.clone()} }

    LaunchedEffect(Unit) {
        cameraManager.startCamera(lifecycleOwner,previewView)
        cameraStarted = true
        deviceRotationMngr.startListening()
    }


    if(!cameraStarted){
        Text(text = "Camera initialization ...")
        return
    }



    if(imgVM.photos.size == 2){
        LaunchedEffect(Unit){
//            AppData.server?.sendImgs(AppData.processingClient,imgVM.photos,{Log.d("server",it)})
            navigateTo(NestedScreen.ThreeDView.route)
        }
    }
    else {
        Column(modifier = Modifier.fillMaxSize()) {
            var imgs = imgVM.photos
            CapturedImgsResutl(
                imgs,
                Modifier.fillMaxWidth().
                weight(0.1f).
                clickable{navigateTo(NestedScreen.ThreeDView.route)}
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.9f),
                contentAlignment = Alignment.BottomCenter
            ) {
                AndroidView(
                    factory = { previewView as View },
                    modifier = Modifier
                        .fillMaxSize()
                )

                if(imgs.isNotEmpty()){
                    Guide(modifier = Modifier.align(Alignment.Center).size(200.dp))
                }

                Button(
                    onClick = {
                        if(imgs.isEmpty()) deviceRotationMngr.setReferenceAngles()
                        cameraManager.captureImg { img: File ->
                            imgVM.addImg(img)
                            storageMngr.saveImg(BitmapFactory.decodeFile(img.absolutePath),
                                img.name,
                                false)

                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
//                    enabled = ((abs(angleDiff[1]) < 2 && abs(angleDiff[2]) < 2) || imgs.isEmpty())
                ) {
                    Text("Capture")
                }
            }
        }
    }
}


@Composable
fun ServerScreen(navigateTo: (String) -> Unit,client: Client) {

    AppData.showScaffold.value = true
    val context = LocalContext.current
    val connection = remember { Connection(context) }
    val serverIp = connection.getGatewayIp()
    var clicked by remember { mutableStateOf(false) }

//    var clients = remember { mutableStateMapOf<Int, Host>() }
//    clients = server.getClients()

    var serverStatus by remember { mutableStateOf("Server not started") }
    var connStatus by remember { mutableStateOf("Not started.") }

    if(serverIp != null && clicked){
        LaunchedEffect(Unit) {
            try {
                client.connToServer(serverIp, AppData.port) // Now exceptions will be caught
                navigateTo(NestedScreen.Camera.route)
            } catch (e: Exception) {
                Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
            }
        }
    }else{
        Toast.makeText(context, "Server IP was not set.", Toast.LENGTH_SHORT).show()
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "server :  $serverIp", fontSize = 18.sp, modifier = Modifier.padding(8.dp))

        Button(onClick = {clicked = true}) {
            Text("Connect")
        }

    }
}


@Composable
fun ClientItem(client: Host, onProceed: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text = client.name, fontSize = 16.sp, modifier = Modifier.weight(1f))
        Button(onClick = onProceed) {
            Text("Proceed")
        }
    }
}


@Composable
fun ThreeDViewScreen(imgVM: ImgViewModel, client: Client) {
    val context = LocalContext.current
    val imageFiles = imgVM.photos
    var outputImg by remember { mutableStateOf<Uri?>(null) }
    var sendStatus by remember { mutableStateOf("Ready to send images") }

    if (client == null) {
        sendStatus = "No client selected."
        return
    }

    if (imageFiles.isEmpty()) {
        sendStatus = "No images to send."
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = sendStatus,
            fontSize = 18.sp,
            modifier = Modifier.padding(8.dp)
        )

        outputImg?.let {
            DisplayResultedImg(it) // Display image if available
        }
    }


    LaunchedEffect(Unit) { // Ensures this runs only once after composition
        sendStatus = "Sending images..."

        client.sendImgsToServer(imageFiles, { sendStatus = it }, { outputImg = it })
    }
}


@Composable
fun DisplayResultedImg(imageUri: Uri?) {
    if (imageUri != null) {
        AndroidView(
            factory = { context ->
                ImageView(context).apply {
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    setImageURI(imageUri)
                }
            },
            update = { it.setImageURI(imageUri) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        )
    } else {
        Text(text = "Image is null", fontSize = 18.sp, modifier = Modifier.padding(8.dp))
    }
}

