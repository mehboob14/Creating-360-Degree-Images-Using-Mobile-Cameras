package com.example.view360.ui.composables

import android.graphics.BitmapFactory
import androidx.activity.result.ActivityResultLauncher
import androidx.camera.core.ImageCapture
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.view360.viewModels.PhotoesViewModel
import androidx.lifecycle.viewmodel.compose.viewModel



@Composable
fun App(launcher: ActivityResultLauncher<String>,startCamera: (ActivityResultLauncher<String>) -> Unit)
{


    Button(onClick = {startCamera(launcher)}) {
        Text(text = "Start camera")
    }


}


@Composable
fun CameraPreview(previewView: PreviewView, imageCapture: ImageCapture, capturePhoto: (imageCapture: ImageCapture, photoPaths: MutableList<String>) -> Unit) {

    val photoPaths = remember { mutableStateListOf<String>() }

    val viewModel : PhotoesViewModel = viewModel()


    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.1f)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            photoPaths.forEach { path ->
                RenderPhoto(path)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.9f)
                .clickable(onClick = { capturePhoto(imageCapture,photoPaths) }),
            contentAlignment = Alignment.BottomCenter
        ) {
            AndroidView(
                factory = { previewView },
                modifier = Modifier
                    .fillMaxSize()
            )

            Button(
                onClick = { capturePhoto(imageCapture,photoPaths) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                Text("Capture")
            }
        }
    }
}



@Composable
fun RenderPhoto(photoPath: String) {
    val bitmap = remember(photoPath) {
        BitmapFactory.decodeFile(photoPath)
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Photo",
            modifier = Modifier
                .size(100.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, Color.Gray, shape = RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop,

        )
    } else {
        Text(
            text = "Error loading image",
            color = Color.Red,
            modifier = Modifier
                .size(100.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.LightGray),
            textAlign = TextAlign.Center
        )
    }
}
