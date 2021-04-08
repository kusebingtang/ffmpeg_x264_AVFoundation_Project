package com.lecture.av.cameraxachiveh264;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.TextureView;
import android.view.View;


import androidx.appcompat.app.AppCompatActivity;

import com.lecture.av.cameraxachiveh264.CameraX.VideoChanel;
import com.lecture.av.cameraxachiveh264.audio.AudioChannel;


public class MainActivity extends AppCompatActivity {

    private TextureView textureView;
    private VideoChanel videoChanel;
    private AudioChannel audioChannel;

    private RtmpClient rtmpClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkPermission();
        textureView = findViewById(R.id.textureView);

        rtmpClient = new RtmpClient(this);
        //初始化摄像头， 同时 创建编码器
        rtmpClient.initVideo(textureView, 480, 640, 10, 640_000);
        rtmpClient.initAudio(44100, 2);


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        rtmpClient.release();
    }

    public boolean checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO
            }, 1);

        }
        return false;
    }

    public void toggleCamera(View view) {
        rtmpClient.toggleCamera();
    }

    public void startLive(View view) {
        rtmpClient.startLive("rtmp://192.168.10.224/live/livestream");
    }

    public void stopLive(View view) {
        rtmpClient.stopLive();
    }


}