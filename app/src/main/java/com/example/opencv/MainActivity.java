package com.example.opencv;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;


public class MainActivity extends AppCompatActivity {

    ImageView view;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        if (OpenCVLoader.initLocal()) {
            Log.i("OpenCV", "OpenCV successfully loaded.");
        }
        ImageView img = findViewById(R.id.resizedImage);
        ImageView originalImage= findViewById(R.id.originalImage);
        originalImage.setImageResource(R.drawable.sample);


        // An example: Resize an image using OpenCV
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(),R.drawable.sample);  // load the image

        if (bitmap != null) {
            Bitmap resizedBitmap = getResizedBitmapCV(bitmap, 400, 500);  // image resized using OpenCV

            img.setImageBitmap(resizedBitmap);
            // Other code goes here ...
        }
    }

    private Bitmap getResizedBitmapCV(Bitmap inputBitmap, int newWidth, int newHeight) {

        Mat inputMat = new Mat();
        Utils.bitmapToMat(inputBitmap, inputMat);

        Mat resizedMat = new Mat();
        Imgproc.resize(inputMat, resizedMat, new Size(newWidth, newHeight));


        Bitmap resizedBitmap = Bitmap.createBitmap(newWidth, newHeight, Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(resizedMat, resizedBitmap);

        inputMat.release();
        resizedMat.release();

        return resizedBitmap;
    }

}
