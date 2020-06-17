package com.team8020.opencvapplication1;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.Range;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2, View.OnTouchListener {


    private CameraBridgeViewBase mOpenCvCameraView;
    private Mat mRgba, mHsv, hsvBase;
    private Scalar mBlobColorHsv;
    private Scalar mBlobColorRgba;
    private boolean permissions = false;
    private static final int GALLERY_REQUEST_CODE = 123;
    List<Cell> allFilesPaths;
    Button btnPickImage;

    Scalar mColorRgb;


    double x = -1;
    double y = -1;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    mOpenCvCameraView.enableView();
                    mOpenCvCameraView.setOnTouchListener(MainActivity.this);
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        btnPickImage = findViewById(R.id.btnPickImage);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.opencv_tutorial_activity_surface_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);

        btnPickImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent, "Pick an Imgae"), GALLERY_REQUEST_CODE);
            }
        });

        checkPermissions();
        showImages();




    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(requestCode == GALLERY_REQUEST_CODE && resultCode == RESULT_OK && data != null){
            Uri imageUri = data.getData();


            String picPath = getPath(imageUri);



            List<Distance> distances = new ArrayList<>();

            for (int i=0; i < allFilesPaths.size(); i++){
                Mat srcBase = Imgcodecs.imread(allFilesPaths.get(i).getPath());
                Mat pic = Imgcodecs.imread(picPath);

                Mat hsvBase = new Mat();
                Mat hsvBase2 = new Mat();


                Imgproc.cvtColor(pic, hsvBase, Imgproc.COLOR_RGB2HSV);
                Imgproc.cvtColor( srcBase, hsvBase2, Imgproc.COLOR_BGR2HSV );

                int hBins = 50, sBins = 60;
                int[] histSize = { hBins, sBins };
                // hue varies from 0 to 179, saturation from 0 to 255
                float[] ranges = { 0, 180, 0, 256 };
                // Use the 0-th and 1-st channels
                int[] channels = { 0, 1 };


                Mat histBase = new Mat();
                Mat histBase2 = new Mat();

                List<Mat> hsvBaseList = Arrays.asList(hsvBase);
                Imgproc.calcHist(hsvBaseList, new MatOfInt(channels), new Mat(), histBase, new MatOfInt(histSize), new MatOfFloat(ranges), false);
                Core.normalize(histBase, histBase, 0, 1, Core.NORM_MINMAX);

                List<Mat> hsvTest1List = Arrays.asList(hsvBase2);
                Imgproc.calcHist(hsvTest1List, new MatOfInt(channels), new Mat(), histBase2, new MatOfInt(histSize), new MatOfFloat(ranges), false);
                Core.normalize(histBase2, histBase2, 0, 1, Core.NORM_MINMAX);

                double distance = Imgproc.compareHist( histBase, histBase2, 1 );

                distances.add(new Distance(allFilesPaths.get(i), distance));

            }

            Collections.sort(distances, new Comparator<Distance>() {
                @Override
                public int compare(Distance o1, Distance o2) {
                    return o1.getDistance().compareTo(o2.getDistance());
                }
            });

            List<Cell> cells = new ArrayList<>();

            for (int i=0; i < distances.size(); i++){
                cells.add(distances.get(i).getPic());
            }

            Intent intent = new Intent(MainActivity.this, AddImage.class);
            intent.putExtra("DISTANCES", (Serializable) cells);
            startActivity(intent);
        }
    }

    public String getPath(Uri uri){
        if (uri == null){
            return null;
        }else {
            String[] projection = { MediaStore.Images.Media.DATA };
            Cursor cursor = getContentResolver().query(uri, projection, null, null, null);

            if (cursor != null){
                int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                cursor.moveToFirst();

                return cursor.getString(column_index);
            }
        }

        return uri.getPath();
    }

    public void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions();
        } else {
            permissions = true;
        }
    }

    private final int MY_PERMISSIONS_REQUEST_READ_CONTACTS = 1;

    public void requestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_CONTACTS)) {
                requestPermissions();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_REQUEST_READ_CONTACTS);
            }
        } else {
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_CONTACTS: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    this.permissions = true;
                } else {
                    this.permissions = false;
                    Toast.makeText(this, "The App needs permissions in order to run properly!", Toast.LENGTH_SHORT).show();
                    ;
                    System.exit(0);
                }
                return;
                // other 'case' lines to check for other
                // permissions this app might request.
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0, this, mLoaderCallback);
        } else {
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat();
        mHsv = new Mat();
        mBlobColorRgba = new Scalar(255);
        mBlobColorHsv = new Scalar(255);
    }

    @Override
    public void onCameraViewStopped() {
        mRgba.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();


        return mRgba;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {

        List<Distance> distances = new ArrayList<>();

        for (int i=0; i < allFilesPaths.size(); i++){
            Mat srcBase = Imgcodecs.imread(allFilesPaths.get(i).getPath());

            Mat hsvBase = new Mat();
            Mat hsvBase2 = new Mat();


            Imgproc.cvtColor(mRgba, hsvBase, Imgproc.COLOR_RGB2HSV);
            Imgproc.cvtColor( srcBase, hsvBase2, Imgproc.COLOR_BGR2HSV );

            int hBins = 50, sBins = 60;
            int[] histSize = { hBins, sBins };
            // hue varies from 0 to 179, saturation from 0 to 255
            float[] ranges = { 0, 180, 0, 256 };
            // Use the 0-th and 1-st channels
            int[] channels = { 0, 1 };


            Mat histBase = new Mat();
            Mat histBase2 = new Mat();

            List<Mat> hsvBaseList = Arrays.asList(hsvBase);
            Imgproc.calcHist(hsvBaseList, new MatOfInt(channels), new Mat(), histBase, new MatOfInt(histSize), new MatOfFloat(ranges), false);
            Core.normalize(histBase, histBase, 0, 1, Core.NORM_MINMAX);

            List<Mat> hsvTest1List = Arrays.asList(hsvBase2);
            Imgproc.calcHist(hsvTest1List, new MatOfInt(channels), new Mat(), histBase2, new MatOfInt(histSize), new MatOfFloat(ranges), false);
            Core.normalize(histBase2, histBase2, 0, 1, Core.NORM_MINMAX);

            double distance = Imgproc.compareHist( histBase, histBase2, 1 );

            distances.add(new Distance(allFilesPaths.get(i), distance));

        }

        Collections.sort(distances, new Comparator<Distance>() {
            @Override
            public int compare(Distance o1, Distance o2) {
                return o1.getDistance().compareTo(o2.getDistance());
            }
        });

        List<Cell> cells = new ArrayList<>();

        for (int i=0; i < distances.size(); i++){
            cells.add(distances.get(i).getPic());
        }

        Intent intent = new Intent(MainActivity.this, AddImage.class);
        intent.putExtra("DISTANCES", (Serializable) cells);
        startActivity(intent);




        return false;
    }

    private Scalar convertScalarHsv2Rgba(Scalar hsvColor) {
        Mat pointMatRgba = new Mat();
        Mat pointMatHsv = new Mat(1, 1, CvType.CV_8UC3, hsvColor);
        Imgproc.cvtColor(pointMatHsv, pointMatRgba, Imgproc.COLOR_HSV2RGB_FULL, 4);

        return new Scalar(pointMatRgba.get(0, 0));
    }

    //show the images on the screen
    private void showImages()
    {
        //this is the folder with all images
        String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/ImagesDB/";
        allFilesPaths = new ArrayList<>();
        allFilesPaths = listAllFiles(path);
    }

    //prepare the images for the list
    private ArrayList<Cell> prepareData(){
        ArrayList<Cell> allImages = new ArrayList<>();
        for (Cell c : allFilesPaths){
            Cell cell = new Cell();
            cell.setTitle(c.getTitle());
            cell.setPath(c.getPath());
            allImages.add(cell);
        }
        return allImages;
    }

    //Load all pics from the folder
    private List<Cell> listAllFiles(String pathName){
        List<Cell> allFiles = new ArrayList<>();
        File file = new File(pathName);
        File[] files = file.listFiles();
        if (files != null){
            for (File f : files) {
                Cell cell = new Cell();
                cell.setTitle(f.getName());
                cell.setPath(f.getAbsolutePath());
                allFiles.add(cell);
            }
        }
        return allFiles;
    }

}


