package com.example.antonio.fotosinstant;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;


import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;


import android.media.Image;
import android.media.ImageReader;
import android.widget.Toast;



import com.google.android.cameraview.AspectRatio;
import com.google.android.cameraview.CameraView;



public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CAMERA_PERMISSION = 1;

    public static final int REQUEST_STORAGE = 101;

    private static final String FRAGMENT_DIALOG = "dialog";

    private static final int[] FLASH_OPTIONS = {
            CameraView.FLASH_AUTO,
            CameraView.FLASH_OFF,
            CameraView.FLASH_ON,
    };

    private static final int[] FLASH_ICONS = {
            R.drawable.ic_flash_auto,
            R.drawable.ic_flash_off,
            R.drawable.ic_flash_on,
    };

    private static final int[] FLASH_TITLES = {
            R.string.flash_auto,
            R.string.flash_off,
            R.string.flash_on,
    };

    private int mCurrentFlash;

    private CameraView mCameraView;

    private Handler mBackgroundHandler;



    private static final String TAG = "MainActivity";


    private boolean hasCameraPermission;

    private View capture;


    static final int REQUEST_IMAGE_CAPTURE = 1;


    private static final boolean D = true;

    private long lastMessageTime = 0;
    private TextView texto, tiempo, flash;
    private ImageView imagen;

    private android.hardware.Camera mCamera;
    public int mCameraOrientation;

    private GoogleApiClient mGoogleApiClient;
    private Node mWearableNode = null;

    private int displayFrameLag = 0;
    private long displayTimeLag = 0;

    private SurfaceHolder mSurfaceHolder;
    private SurfaceView mSurfaceView;
    private ImageView mImageView;
    public boolean mPreviewRunning=false;

    private static int currentCamera = android.hardware.Camera.CameraInfo.CAMERA_FACING_BACK;
    private static String currentFlashMode = android.hardware.Camera.Parameters.FLASH_MODE_OFF;

    int temporizador = 0;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



        if (Build.VERSION.SDK_INT >= 23) {
            String[] PERMISSIONS = {android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.CAMERA};
            if (!hasPermissions(this, PERMISSIONS)) {
                ActivityCompat.requestPermissions((Activity) this, PERMISSIONS, REQUEST_STORAGE);
            } else {

            }
        }

        mCameraView = (CameraView) findViewById(R.id.camera);
        if (mCameraView != null) {
            mCameraView.addCallback(mCallback);
        }
        mCameraView.start();

        //texto = findViewById(R.id.foto);

        //texto = findViewById(R.id.foto);
        //tiempo = findViewById(R.id.tiempo);
        //flash = findViewById(R.id.flash);

        //imagen = findViewById(R.id.imagen);

        //focusview = findViewById(R.id.focusView);






        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle connectionHint) {
                        if(D) Log.d(TAG, "onConnected: " + connectionHint);
                        findWearableNode();
                        Wearable.MessageApi.addListener(mGoogleApiClient, mMessageListener);
                    }
                    @Override
                    public void onConnectionSuspended(int cause) {
                        if(D) Log.d(TAG, "onConnectionSuspended: " + cause);
                    }
                })
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult result) {
                        if(D) Log.d(TAG, "onConnectionFailed: " + result);
                    }
                })
                .addApi(Wearable.API)
                .build();
        mGoogleApiClient.connect();


        lastMessageTime = System.currentTimeMillis();

        Timer mTimer = new Timer();
        mTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if(displayFrameLag>1) { displayFrameLag--; }
                if(displayTimeLag>1000) { displayTimeLag-=1000; }
            }
        }, 0, 1000);


    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private static boolean hasPermissions(Context context, String... permissions) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    /*get Permissions Result*/
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_STORAGE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("TAG", "PERMISSION_GRANTED");
                } else {
                    Toast.makeText(this, "The app was not allowed to write to your storage", Toast.LENGTH_LONG).show();
                }
            }

        }
    }


    void findWearableNode() {
        PendingResult<NodeApi.GetConnectedNodesResult> nodes = Wearable.NodeApi.getConnectedNodes(mGoogleApiClient);
        nodes.setResultCallback(new ResultCallback<NodeApi.GetConnectedNodesResult>() {
            @Override
            public void onResult(NodeApi.GetConnectedNodesResult result) {
                if(result.getNodes().size()>0) {
                    mWearableNode = result.getNodes().get(0);
                    if(D) Log.d(TAG, "Found wearable: name=" + mWearableNode.getDisplayName() + ", id=" + mWearableNode.getId());
                } else {
                    mWearableNode = null;
                }
            }
        });
    }

    private MessageApi.MessageListener mMessageListener = new MessageApi.MessageListener() {
        @Override
        public void onMessageReceived (MessageEvent m){
            if(D) Log.d(TAG, "onMessageReceived: " + m.getPath());
            lastMessageTime = System.currentTimeMillis();
            Log.d("Debug", "path vale"+m.getPath());
            String accion = String.valueOf(m.getPath());


            if(m.getPath().equals("foto")){
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mCameraView.takePicture();

                    }
                }, temporizador);

            }else if(m.getPath().equals("0 segundos")){
                temporizador = 0;
            }else if(m.getPath().equals("3 segundos")){
                temporizador = 3000;
            }else if(m.getPath().equals("5 segundos")){
                temporizador = 5000;
            }else if(m.getPath().equals("10 segundos")){
                temporizador = 10000;
            }else if(m.getPath().equals("flashOn")){
                if (mCameraView != null) {
                    mCameraView.setFlash(1);
                }

            }else if(m.getPath().equals("flashOff")){
                if (mCameraView != null) {
                    mCameraView.setFlash(0);
                }
            }else if(m.getPath().equals("flashAuto")){
                if (mCameraView != null) {
                    mCameraView.setFlash(3);
                }
            }
        }
    };


    private void takeScreenshot() {
        Date now = new Date();
        android.text.format.DateFormat.format("yyyy-MM-dd_hh:mm:ss", now);

        try {
            // image naming and path  to include sd card  appending name you choose for file
            String mPath = Environment.getExternalStorageDirectory().toString() + "/" + now + ".jpg";

            // create bitmap screen capture
            View v1 = getWindow().getDecorView().getRootView();
            v1.setDrawingCacheEnabled(true);
            Bitmap bitmap = Bitmap.createBitmap(v1.getDrawingCache());
            v1.setDrawingCacheEnabled(false);

            File imageFile = new File(mPath);

            FileOutputStream outputStream = new FileOutputStream(imageFile);
            int quality = 100;
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);
            outputStream.flush();
            outputStream.close();
            Log.d("Debug", "La foto se ha tomado");

        } catch (Throwable e) {
            // Several error may come out with file handling or OOM
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        mCameraView.start();
        super.onResume();

    }

    @Override
    protected void onPause() {
        mCameraView.stop();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBackgroundHandler != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                mBackgroundHandler.getLooper().quitSafely();
            } else {
                mBackgroundHandler.getLooper().quit();
            }
            mBackgroundHandler = null;
        }
    }

    private Handler getBackgroundHandler() {
        if (mBackgroundHandler == null) {
            HandlerThread thread = new HandlerThread("background");
            thread.start();
            mBackgroundHandler = new Handler(thread.getLooper());
        }
        return mBackgroundHandler;
    }


    private CameraView.Callback mCallback
            = new CameraView.Callback() {

        @Override
        public void onCameraOpened(CameraView cameraView) {
            Log.d(TAG, "onCameraOpened");
        }

        @Override
        public void onCameraClosed(CameraView cameraView) {
            Log.d(TAG, "onCameraClosed");
        }

        @Override
        public void onPictureTaken(CameraView cameraView, final byte[] data) {
            Log.d(TAG, "onPictureTaken " + data.length);
            Toast.makeText(cameraView.getContext(), R.string.picture_taken, Toast.LENGTH_SHORT)
                    .show();
            getBackgroundHandler().post(new Runnable() {
                @Override
                public void run() {

                    Date now = new Date();
                    android.text.format.DateFormat.format("yyyy-MM-dd_hh:mm:ss", now);
                    String mPath = Environment.getExternalStorageDirectory().toString() + "/" + now + ".jpg";

                    File imageFile = new File(mPath);

                    OutputStream os = null;
                    try {
                        os = new FileOutputStream(imageFile);
                        os.write(data);
                        os.close();
                    } catch (IOException e) {
                        Log.w(TAG, "Cannot write to " + imageFile, e);
                    } finally {
                        if (os != null) {
                            try {
                                os.close();
                            } catch (IOException e) {
                                // Ignore
                            }
                        }
                    }
                }
            });
        }

    };

}
