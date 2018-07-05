package com.example.antonio.fotosinstant;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.wearable.activity.WearableActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
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
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.common.api.GoogleApiClient;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;



public class MainActivity extends WearableActivity {

    GoogleApiClient mGoogleApiClient;

    private static final String TAG = "MainActivity";
    private static final boolean D = true;

    int tiempo = 0;

    private int flashX = 0;

    private Node mPhoneNode = null;

    private TextView mTextView;
    private static final String
            RECIBIR_MENSAJES_CAPABILITY_NAME = "recibir_mensajes";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final SeekBar sb = findViewById(R.id.temporizador);
        final TextView temporizador = findViewById(R.id.t2);
        temporizador.setText("0 segundos");

        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                if(progress==0){
                    temporizador.setText("0 segundos");
                    tiempo = 0;
                }else if(progress==1){
                    temporizador.setText("3 segundos");
                    tiempo = 3000;
                }
                else if(progress==2){
                    temporizador.setText("5 segundos");
                    tiempo = 5000;
                }
                else if(progress==3){
                    temporizador.setText("10 segundos");
                    tiempo = 10000;
                }
                String tiempo = temporizador.getText().toString();

                sendToPhone(tiempo, null, null);
            }

            @Override
            public void onStartTrackingTouch(SeekBar sb) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar sb) {

            }
        });



        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();
        mGoogleApiClient.connect();

        mTextView = (TextView) findViewById(R.id.text);
        final Button button = findViewById(R.id.boton);

        final ImageButton ib = findViewById(R.id.flashButton);

        Log.d("Debug", "HOLA");
        findPhoneNode();
        Log.d("Debug", "HOLA2");
        ib.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String tipoFlash = "flashOff";
                flashX++;
                if(flashX == 3){
                    flashX = 0;
                }

                if(flashX == 0){
                    ib.setImageResource(R.drawable.flash_off);
                    tipoFlash = "flashOff";
                }else if(flashX == 1){
                    ib.setImageResource(R.drawable.flash_on);
                    tipoFlash = "flashOn";
                }else if(flashX == 2){
                    ib.setImageResource(R.drawable.flash_auto);
                    tipoFlash = "flashAuto";
                }

                sendToPhone(tipoFlash, null, null);

            }

        });
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                sendToPhone("foto", null, null);
                button.setEnabled(false);
                button.setText("" + tiempo/1000);
                new CountDownTimer(tiempo+1000, 1000) {


                    public void onTick(long millisUntilFinished) {
                        int seconds = (int) (millisUntilFinished / 1000);
                        button.setText(String.format("%02d", seconds));
                    }

                    public void onFinish() {
                        button.setEnabled(true);
                        button.setText("Hacer foto");
                    }
                }.start();


            }
        });


        // Enables Always-on
        setAmbientEnabled();
    }


    void findPhoneNode() {
        PendingResult<NodeApi.GetConnectedNodesResult> pending = Wearable.NodeApi.getConnectedNodes(mGoogleApiClient);
        pending.setResultCallback(new ResultCallback<NodeApi.GetConnectedNodesResult>() {
            @Override
            public void onResult(NodeApi.GetConnectedNodesResult result) {
                if(result.getNodes().size()>0) {
                    mPhoneNode = result.getNodes().get(0);
                    if(D) Log.d(TAG, "Found wearable: name=" + mPhoneNode.getDisplayName() + ", id=" + mPhoneNode.getId());
                    sendToPhone("start", null, null);
                } else {
                    mPhoneNode = null;
                }
            }
        });
    }


    private void sendToPhone(String path, byte[] data, final ResultCallback<MessageApi.SendMessageResult> callback) {
        if (mPhoneNode != null) {
            PendingResult<MessageApi.SendMessageResult> pending = Wearable.MessageApi.sendMessage(mGoogleApiClient, mPhoneNode.getId(), path, data);
            pending.setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
                @Override
                public void onResult(MessageApi.SendMessageResult result) {
                    if (callback != null) {
                        callback.onResult(result);
                    }
                    if (!result.getStatus().isSuccess()) {
                        if(D) Log.d(TAG, "ERROR: failed to send Message: " + result.getStatus());
                    }
                }
            });
        } else {
            if(D){
                Log.d(TAG, "ERROR: tried to send message before device was found");
                findPhoneNode();
            }
        }
    }


}
