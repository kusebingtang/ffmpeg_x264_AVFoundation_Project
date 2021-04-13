package com.lecture.av.audiorecordwav;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.lecture.av.audiorecordwav.channel.AudioChannel;

public class MainActivity extends AppCompatActivity {

    private AudioChannel audioChannel = null;
    private Button bthStartRecord;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bthStartRecord = findViewById(R.id.bthStartRecord);
        checkPermission();


    }

    public boolean checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.RECORD_AUDIO
            }, 1);
        }
        return false;
    }


    public void startRecord(View view) {
        if (audioChannel == null) {
            audioChannel = new AudioChannel(44100, 2);
            audioChannel.startLive();
            bthStartRecord.setText("停止录音");
        } else {
            audioChannel.stopLive();
            audioChannel = null;
            bthStartRecord.setText("开始录音");
        }


    }
}