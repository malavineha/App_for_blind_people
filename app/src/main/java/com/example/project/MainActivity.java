package com.example.project;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.wonderkiln.camerakit.CameraKitError;
import com.wonderkiln.camerakit.CameraKitEvent;
import com.wonderkiln.camerakit.CameraKitEventListener;
import com.wonderkiln.camerakit.CameraKitImage;
import com.wonderkiln.camerakit.CameraKitVideo;
import com.wonderkiln.camerakit.CameraView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    String MODEL_PATH = "mobilenet_quant_v1_224.tflite";
    String LABEL_PATH = "labels.txt";



    private static final int INPUT_SIZE = 224;
    private Classifier classifier;
    private Executor executor = Executors.newSingleThreadExecutor();
    private CameraView cameraView;

    Timer timer1;
    MyTimerTaskobject myTimerTaskobject;
    private TextToSpeech tts;
    String oldaddr="";
    String oldobject="";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cameraView = findViewById(R.id.cameraView);

        cameraView.addCameraKitListener(new CameraKitEventListener() {
            @Override
            public void onEvent(CameraKitEvent cameraKitEvent) {

            }

            @Override
            public void onError(CameraKitError cameraKitError) {

            }

            @Override
            public void onImage(CameraKitImage cameraKitImage) {

                Bitmap bitmap = cameraKitImage.getBitmap();
                bitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, false);
                //imageViewResult.setImageBitmap(bitmap);
                final List<Classifier.Recognition> results = classifier.recognizeImage(bitmap);
                //textViewResult.setText(results.toString());
                //Toast.makeText(getApplicationContext(),results.toString(),Toast.LENGTH_LONG).show();

                String objectresults="";
                Classifier.Recognition[] arr = new Classifier.Recognition[results.size()];
                for (int i = 0; i < results.size(); i++)
                    arr[i] = results.get(i);

                for (Classifier.Recognition x : arr) {
                    //if(x.getConfidence()>=40) {
                    objectresults += x.getTitle()+",";
                    //}
                }

                String objects=objectresults;

                Toast.makeText(getApplicationContext(), objects, Toast.LENGTH_SHORT).show();

                if (!objects.isEmpty() && oldobject.equals(objects)==false) {
                    oldobject=objects;

                    tts = new TextToSpeech(MainActivity.this, new TextToSpeech.OnInitListener()
                    {
                        @Override
                        public void onInit(int status) {
                            // TODO Auto-generated method stub
                            if(status == TextToSpeech.SUCCESS){
                                int result=tts.setLanguage(Locale.US);
                                if(result==TextToSpeech.LANG_MISSING_DATA ||
                                        result==TextToSpeech.LANG_NOT_SUPPORTED){
                                    Log.e("error", "This Language is not supported");
                                }
                                else{
                                    tts.speak("Objects List "+objects, TextToSpeech.QUEUE_FLUSH, null);
                                }
                            }
                            else
                                Log.e("error", "Initilization Failed!");
                        }
                    });

                }
            }

            @Override
            public void onVideo(CameraKitVideo cameraKitVideo) {

            }
        });


        initTensorFlowAndLoadModel();


        timer1 = new Timer();
        myTimerTaskobject = new MyTimerTaskobject();
        timer1.schedule(myTimerTaskobject, 5000, 5000);


        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                //code for delay
            }
        }, 4000);


    }

    @Override
    protected void onResume() {
        super.onResume();
        cameraView.start();
    }

    @Override
    protected void onPause() {
        cameraView.stop();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                classifier.close();
            }
        });
    }

    private void initTensorFlowAndLoadModel() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    classifier = TensorFlowImageClassifier.create(
                            getAssets(),
                            MODEL_PATH,
                            LABEL_PATH,
                            INPUT_SIZE);
                } catch (final Exception e) {
                    throw new RuntimeException("Error initializing TensorFlow!", e);
                }
            }
        });
    }


    public void geObject() {
        cameraView.captureImage();
    }


    class MyTimerTaskobject extends TimerTask {

        @Override
        public void run() {

            runOnUiThread(new Runnable(){

                @Override
                public void run() {
                    geObject();

                }});
        }

    }





}