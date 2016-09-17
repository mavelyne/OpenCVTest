package com.margret.opencvtest;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.*;
import org.opencv.imgproc.*;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class MainActivity extends Activity {
    private static int RESULT_LOAD_IMG = 1;
    String imgDecodableString;
    private CascadeClassifier cascadeClassifier;
    private int absoluteFaceSize;

    private void initializeOpenCVDependencies() {
        try {
            // Copy the resource into a temp file so OpenCV can load it
            InputStream is = getResources().openRawResource(R.raw.lbpcascade_frontalface);
            File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
            File mCascadeFile = new File(cascadeDir, "lbpcascade_frontalface.xml");
            FileOutputStream os = new FileOutputStream(mCascadeFile);


            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            is.close();
            os.close();


            // Load the cascade classifier
            cascadeClassifier = new CascadeClassifier(mCascadeFile.getAbsolutePath());
        } catch (Exception e) {
            Log.e("OpenCVActivity", "Error loading cascade", e);
        }

    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    initializeOpenCVDependencies();
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void loadImagefromGallery(View view) {
        // Create intent to Open Image applications like Gallery, Google Photos
        Intent galleryIntent = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        // Start the Intent
        startActivityForResult(galleryIntent, RESULT_LOAD_IMG);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        try {
            // When an Image is picked
            if (requestCode == RESULT_LOAD_IMG && resultCode == RESULT_OK
                    && null != data) {
                // Get the Image from data

                Uri selectedImage = data.getData();
                String[] filePathColumn = { MediaStore.Images.Media.DATA };

                // Get the cursor
                Cursor cursor = getContentResolver().query(selectedImage,
                        filePathColumn, null, null, null);
                // Move to first row
                cursor.moveToFirst();

                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                imgDecodableString = cursor.getString(columnIndex);
                cursor.close();
                ImageView imgView = (ImageView) findViewById(R.id.imgView);

                Bitmap bmp32 = BitmapFactory.decodeFile(imgDecodableString)
                        .copy(Bitmap.Config.ARGB_8888, true);
                Mat imgMAT = new Mat(bmp32.getHeight(), bmp32.getWidth(), CvType.CV_8U, new Scalar(4));
                Mat grayImgMAT = new Mat(bmp32.getHeight(), bmp32.getWidth(), CvType.CV_8UC4);
                Utils.bitmapToMat(bmp32, imgMAT);

                // Create a grayscale image
                Imgproc.cvtColor(imgMAT, grayImgMAT, Imgproc.COLOR_RGB2GRAY);

                MatOfRect faces = new MatOfRect();

                // The faces will be a 20% of the height of the screen
                absoluteFaceSize = (int) (imgMAT.height() * 1);

                // Use the classifier to detect faces
                if (cascadeClassifier != null) {
                    cascadeClassifier.detectMultiScale(grayImgMAT, faces, 1.1, 2, 2,
                            new Size(absoluteFaceSize, absoluteFaceSize), new Size());
                }

                // If there are any faces found, draw a rectangle around it
                Rect[] facesArray = faces.toArray();
                for (int i = 0; i <facesArray.length; i++)
                    Imgproc.rectangle(imgMAT, facesArray[i].tl(), facesArray[i].br(), new Scalar(0, 255, 0, 255), 3);

                Utils.matToBitmap(grayImgMAT,bmp32);
                // Set the Image in ImageView after decoding the String
                imgView.setImageBitmap(bmp32);

            } else {
                Toast.makeText(this, "You haven't picked Image",
                        Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Something went wrong", Toast.LENGTH_LONG)
                    .show();
        }

    }

    @Override
    public void onResume() {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_6, this, mLoaderCallback);
    }

}