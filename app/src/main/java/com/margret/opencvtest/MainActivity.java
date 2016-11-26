package com.margret.opencvtest;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;

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
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends Activity {
    private static int RESULT_LOAD_IMG = 1;
    private static String DEFAULT_SAVE_NAME = "Censored";

    String imgDecodableString;
    private CascadeClassifier faceCascadeClassifier;
    private CascadeClassifier smileCascadeClassifier;
    private CascadeClassifier eyeCascadeClassifier;
    Bitmap currentImg = null;
    String currentImgName = null;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

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
            // must construct object and use load function as said in http://stackoverflow.com/questions/34953704/opencv-fo-android-failed-to-load-cascade-classifier-error
            faceCascadeClassifier = new CascadeClassifier(mCascadeFile.getAbsolutePath());
            faceCascadeClassifier.load(mCascadeFile.getAbsolutePath());
            if (faceCascadeClassifier.empty()) {
                Log.v("MyActivity", "--(!)Error loading A\n");
                return;
            } else {
                Log.v("MyActivity",
                        "Loaded cascade classifier from " + mCascadeFile.getAbsolutePath());
            }

            // Load smile classifier too
            is = getResources().openRawResource(R.raw.haarcascade_smile);
            cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
            mCascadeFile = new File(cascadeDir, "haarcascade_smile.xml");
            os = new FileOutputStream(mCascadeFile);
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            is.close();
            os.close();

            // Load the cascade classifier
            // must construct object and use load function as said in http://stackoverflow.com/questions/34953704/opencv-fo-android-failed-to-load-cascade-classifier-error
            smileCascadeClassifier = new CascadeClassifier(mCascadeFile.getAbsolutePath());
            smileCascadeClassifier.load(mCascadeFile.getAbsolutePath());
            if (smileCascadeClassifier.empty()) {
                Log.v("MyActivity", "--(!)Error loading A\n");
                return;
            } else {
                Log.v("MyActivity",
                        "Loaded cascade classifier from " + mCascadeFile.getAbsolutePath());
            }

            // Load eye classifier too
            is = getResources().openRawResource(R.raw.haarcascade_eye);
            cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
            mCascadeFile = new File(cascadeDir, "haarcascade_eye.xml");
            os = new FileOutputStream(mCascadeFile);
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            is.close();
            os.close();

            // Load the cascade classifier
            // must construct object and use load function as said in http://stackoverflow.com/questions/34953704/opencv-fo-android-failed-to-load-cascade-classifier-error
            eyeCascadeClassifier = new CascadeClassifier(mCascadeFile.getAbsolutePath());
            eyeCascadeClassifier.load(mCascadeFile.getAbsolutePath());
            if (eyeCascadeClassifier.empty()) {
                Log.v("MyActivity", "--(!)Error loading A\n");
                return;
            } else {
                Log.v("MyActivity",
                        "Loaded cascade classifier from " + mCascadeFile.getAbsolutePath());
            }



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
        if (!OpenCVLoader.initDebug()) {
            // Handle initialization error
            // Fixed Imgproc.rectangle not found
            // From: http://stackoverflow.com/questions/11939192/unsatisfied-link-error-opencv-for-android-non-native
            Log.v("My Activity", "Error initializing opencv");
        }
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    public void loadImagefromGallery(View view) {
        // Create intent to Open Image applications like Gallery, Google Photos
        Intent galleryIntent = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        // Start the Intent
        startActivityForResult(galleryIntent, RESULT_LOAD_IMG);
    }

    public void saveImageToGallery(View view) {
        File filepath = Environment.getExternalStorageDirectory();
        File dir = new File(filepath.getAbsolutePath() + "/Censored");
        dir.mkdirs();
        File file = new File(dir, DEFAULT_SAVE_NAME + "_" + currentImgName + ".jpg");
        try {
            FileOutputStream out = new FileOutputStream(file);
            currentImg.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();
            ;
            ContentValues vals = new ContentValues();
            vals.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());
            vals.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
            vals.put(MediaStore.MediaColumns.DATA, file.getAbsolutePath());
            MainActivity.this.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, vals);
        } catch (Exception e) {
            Log.v("SaveImageToGallery", "Failed to save image");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        try {
            // When an Image is picked from Loading
            if (requestCode == RESULT_LOAD_IMG && resultCode == RESULT_OK
                    && null != data) {
                // Get the Image from data
                Uri selectedImage = data.getData();
                String[] filePathColumn = {MediaStore.Images.Media.DATA};

                // Get the cursor
                Cursor cursor = getContentResolver().query(selectedImage,
                        filePathColumn, null, null, null);
                // Move to first row
                cursor.moveToFirst();

                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                imgDecodableString = cursor.getString(columnIndex);
                cursor.close();


                File file = new File(imgDecodableString);
                currentImgName = file.getName();

                ImageView imgView = (ImageView) findViewById(R.id.imgView);
                ProcessImageTask task = new ProcessImageTask(imgView);
                task.execute(imgDecodableString);

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

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Main Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://com.margret.opencvtest/http/host/path")
        );
        AppIndex.AppIndexApi.start(client, viewAction);
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Main Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://com.margret.opencvtest/http/host/path")
        );
        AppIndex.AppIndexApi.end(client, viewAction);
        client.disconnect();
    }

    /*
        Use this asynchronous task to process the image.
        Adapted from https://developer.android.com/training/displaying-bitmaps/process-bitmap.html
     */
    class ProcessImageTask extends AsyncTask<String, Void, Bitmap> {
        private final WeakReference<ImageView> imageViewReference;

        public ProcessImageTask(ImageView imageView) {
            // Use a WeakReference to ensure the ImageView can be garbage collected
            imageViewReference = new WeakReference<ImageView>(imageView);
        }

        // Decode image in background
        @Override
        protected Bitmap doInBackground(String... params) {
            //Covert to bitmap and mat
            Bitmap bmp32 = BitmapFactory.decodeFile(imgDecodableString)
                    .copy(Bitmap.Config.ARGB_8888, true);

            currentImg = bmp32;
            Mat imgMAT = new Mat(bmp32.getHeight(), bmp32.getWidth(), CvType.CV_8U, new Scalar(4));
            Mat grayImgMAT = new Mat(bmp32.getHeight(), bmp32.getWidth(), CvType.CV_8UC4);
            Utils.bitmapToMat(bmp32, imgMAT);

            // Create a grayscale image
            Imgproc.cvtColor(imgMAT, grayImgMAT, Imgproc.COLOR_RGB2GRAY);
            Imgproc.equalizeHist(grayImgMAT, grayImgMAT);
            MatOfRect faces = new MatOfRect();

            // The faces will be a bounded percentage of the height of the screen
            int minFaceSize = (int) (imgMAT.height() * 0.10);
            //adjust min face size until at least one face is found or limit reached
            for(double percentage = 0.10; percentage < 0.90  && faces.empty(); percentage += 0.5){
                minFaceSize = (int) (imgMAT.height() * percentage);
                // Use the classifier to detect faces
                if (faceCascadeClassifier != null && !faceCascadeClassifier.empty()) {
                    faceCascadeClassifier.detectMultiScale(imgMAT, faces, 1.1, 2, 2,
                            new Size(minFaceSize, minFaceSize), new Size());
                }

            }

            int minSmileSize = minFaceSize / 8;
            int minEyeSize = 2;

            MatOfRect smiles = new MatOfRect();
            // Use the classifier to detect smiles
            if (smileCascadeClassifier != null && !smileCascadeClassifier.empty()) {
                smileCascadeClassifier.detectMultiScale(imgMAT, smiles, 1.1, 2, 2,
                        new Size(minSmileSize, minSmileSize), new Size(minFaceSize,minFaceSize/2));
            }

            MatOfRect eyes = new MatOfRect();
            // Use the classifier to detect eyes
            if (eyeCascadeClassifier != null && !eyeCascadeClassifier.empty()) {
                eyeCascadeClassifier.detectMultiScale(imgMAT, eyes, 1.1, 2, 2,
                        new Size(minEyeSize, minEyeSize), new Size(minFaceSize,minFaceSize));
            }

            // If there are any faces found, draw a rectangle around it
            List<Rect> facesList = new ArrayList<Rect>(faces.toList());
            List<Rect> smilesList = new ArrayList<Rect>(smiles.toList());
            List<Rect> eyesList = new ArrayList<Rect>(eyes.toList());

            //filter faces out that don't contain an eye
            List<Rect> temp = new ArrayList<>();
            for(Rect face : facesList){
                boolean hasEye = false;
                for(Rect eye : eyesList){
                    if(face.contains(eye.br()) &&
                            face.contains(eye.tl())){
                        hasEye = true;
                    }
                }
                if(hasEye){
                    temp.add(face);
                }
            }
            facesList = temp;

            if(facesList.isEmpty()){
                Log.v("Looking for Faces", "No faces found");
                return bmp32;
            }

            //isolate largest face
            Collections.sort(facesList, new RectSizeComparator());
            Collections.reverse(facesList);
            Rect largest = facesList.remove(0);
            Point center = new Point(largest.x+largest.width/2,largest.y+largest.height/2);

            for (Rect face : facesList) {
                if(!face.contains(center)){

                    //blur the face until eyes cannot be recognized
                    Mat submat = imgMAT.submat(face);
                    int winsize = 10;
                    MatOfRect foundEyes = new MatOfRect();
                    do{
                        Imgproc.blur(submat,submat,new Size(winsize,winsize));
                        minEyeSize = (int) (submat.height() * 0.01);
                        int maxEyeSize = (int) (submat.height() * 0.5);
                        // Use the classifier to detect eyes
                        if (eyeCascadeClassifier != null && !eyeCascadeClassifier.empty()) {
                            eyeCascadeClassifier.detectMultiScale(submat, foundEyes, 1.1, 2, 2,
                                    new Size(minEyeSize, minEyeSize), new Size(maxEyeSize,maxEyeSize));
                        }

                    }while(!foundEyes.empty());
                }
            }

            /*
            // DEBUG: Box objects by type
            List<Rect> allObj = new ArrayList<>(facesList);
            allObj.addAll(smilesList); allObj.addAll(eyesList);
            for(Rect obj : allObj){
                Scalar color = new Scalar(255,0,0);
                if(eyesList.contains(obj)) color = new Scalar(0,255,0);
                if(smilesList.contains(obj)) color = new Scalar(0,0,255);
                Point pt1 = obj.tl();
                Point pt2 = obj.br();
                Imgproc.rectangle(imgMAT, pt1, pt2, color);
            }
            */

            Utils.matToBitmap(imgMAT, bmp32);

            //release native resources
            imgMAT.release();
            return bmp32;
        }

        // Once complete, see if ImageView is still around and set bitmap.
        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (imageViewReference != null && bitmap != null) {
                final ImageView imageView = imageViewReference.get();
                if (imageView != null) {
                    imageView.setImageBitmap(bitmap);
                }
            }
        }
    }

    class RectSizeComparator implements Comparator<Rect> {
        @Override
        public int compare(Rect a, Rect b){
            if(a.size().area() < b.size().area()) return -1;
            if(a.size().area() == b.size().area()) return 0;
            return 1;
        }
    }

}