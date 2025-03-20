package com.example.view360.ui.composables

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.view360.utilityClasses.ImgProcessing
import com.example.view360.viewModels.ImgViewModel
import org.opencv.android.Utils
import org.opencv.core.Mat
import java.io.IOException
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.view360.MainActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavHostController
import com.example.view360.App
import com.example.view360.AppBottomBar
import com.example.view360.AppData
import com.example.view360.AppFAB
import com.example.view360.AppTopBar
import com.example.view360.MainContent
import com.example.view360.enums.NestedScreen
import com.example.view360.enums.Screen
import com.example.view360.utilityClasses.Accelarometer
import com.example.view360.utilityClasses.CameraMngr
import com.example.view360.utilityClasses.DeviceRotationMngr
import com.example.view360.utilityClasses.MovementDetector
import com.example.view360.utilityClasses.StorageMngr
import kotlinx.coroutines.delay
import java.io.File
import kotlin.math.abs


@Composable
fun HomeScreen() {

    AppData.showScaffold.value = true
    val imgViewModel: ImgViewModel = viewModel()
    val navController = rememberNavController()
    val navigateTo: (String) -> Unit = { navController.navigate(it) }

    NavHost(navController = navController, startDestination = NestedScreen.Search.route){
        composable(NestedScreen.Camera.route){ CameraScreen({
            navController.navigate(NestedScreen.ThreeDView.route)},
            imgViewModel
        )}
        composable(NestedScreen.ThreeDView.route){
            StitchingResultScreen({
                navController.popBackStack()},
                imgViewModel
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
fun SearchScreen(navigateTo: (String) -> Unit){
    AppData.showScaffold.value = true
    Column {
        SearchBar()
        SearchResult()
        Button(onClick = {
            AppData.showScaffold.value = false
            navigateTo(NestedScreen.Camera.route)
        }) {
            Text(text = "Open camera")
        }
    }
}

@Composable
fun SearchBar(){
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically){
        Text(text = "Search")
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = "Filter")
    }
}

@Composable
fun SearchResult(){
    Text(text = "Search Result")
}



@Composable
fun CameraScreen(navHndlr:() -> Unit,imgVM: ImgViewModel) {

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
            navHndlr()
        }
    }
    else {
        Column(modifier = Modifier.fillMaxSize()) {
            var imgs = imgVM.photos
            CapturedImgsResutl(imgs, Modifier.fillMaxWidth().weight(0.1f))

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
fun StitchingResultScreen(navHndlr:() -> Unit, imgVm: ImgViewModel) {

    // Ensuring back navigation clears images

    Log.d("recomposition","stitching screen")

    BackHandler {
        imgVm.clearImgs()
        navHndlr()
    }


    if (imgVm.photos.size < 2) {
        Text(text = "Not enough images to stitch.")
        return
    }

    val img1Path = imgVm.photos[0].absolutePath
    val img2Path = imgVm.photos[1].absolutePath

    // Ensuring imgProcessing is remembered correctly
    val imgProcessing = remember(img1Path, img2Path) { ImgProcessing(img1Path, img2Path) }

    var errorMsg by remember { mutableStateOf("stitched") } // ✅ Store errorMsg in state

    // Ensure stitchedImage is properly remembered
    val stitchedImage = remember {
        try {
            imgProcessing.stitchImgs()
        } catch (e: Exception) {
            errorMsg = e.toString() // ✅ Now errorMsg is a state variable, preventing unexpected recompositions
            null
        }
    }

    if (stitchedImage != null) {
        val bitmap = MatToBitmap(stitchedImage)

        // Using LaunchedEffect to ensure saveImageToStorage runs only once
        val context = LocalContext.current
        LaunchedEffect(Unit) {
            var storageMngr = StorageMngr(context)
            storageMngr.saveImg(bitmap,"stitched_${System.currentTimeMillis()}.jpg",false)
        }

        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Stitched Image",
            modifier = Modifier.fillMaxSize()
        )

    } else {
        Text(text = errorMsg)
    }
}

@Composable
fun CapturedImgsResutl(imgs: List<File>,modifier: Modifier){

    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(imgs, key = {it.name}) { img ->
            RenderImg(img.absolutePath)
            Log.d("imgPath",img.absolutePath)
        }
    }

}


fun MatToBitmap(mat: Mat): Bitmap {
    val bitmap = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888)
    Utils.matToBitmap(mat, bitmap)
    return bitmap
}

@Composable
fun RenderImg(imgPath: String) {
    val bitmap = BitmapFactory.decodeFile(imgPath)

    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Image",
            modifier = Modifier
                .size(100.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, Color.Gray, shape = RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop,

        )
    } else {
        ImgLoadError("Error loading image")
    }
}

@Composable
fun ImgLoadError(message: String){
    Text(
        text = message,
        color = Color.Red,
        modifier = Modifier
            .size(100.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.LightGray),
        textAlign = TextAlign.Center
    )
}

@Composable
fun Guide(modifier: Modifier = Modifier) {

    val angleDiff = AppData.angleDiff.value
    val offsetX = angleDiff[1]
    val offsetY = angleDiff[2]


    val circleColor = if (abs(offsetX) < 2 && abs(offsetY) < 2){
        Color.Green.copy(alpha = 0.5f)
    }
    else Color.Red.copy(alpha = 0.5f)

    Box(
        modifier = modifier
    ) {
        // Large Circle
        Canvas(modifier = Modifier.size(150.dp)) {

            val centerX = size.width / 2
            val centerY = size.height / 2

            drawCircle(
                color = Color.Gray.copy(alpha = 0.5f),
                radius = size.minDimension / 2
                )

            drawCircle(
                color = Color.Green, // Border color
                radius = (size.minDimension / 8) + 3, // Full size of the canvas
                style = Stroke(width = 2.dp.toPx()) // Only draw the outline
            )

            drawCircle(color = circleColor,
                radius = size.minDimension / 8,
                center = Offset(centerX + offsetY,centerY + offsetX)
                )
        }
    }
}

