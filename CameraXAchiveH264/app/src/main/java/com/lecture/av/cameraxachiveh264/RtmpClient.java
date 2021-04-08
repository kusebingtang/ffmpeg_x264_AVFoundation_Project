package com.lecture.av.cameraxachiveh264;

import android.util.Log;
import android.view.TextureView;

import androidx.lifecycle.LifecycleOwner;

import com.lecture.av.cameraxachiveh264.CameraX.VideoChanel;
import com.lecture.av.cameraxachiveh264.audio.AudioChannel;

public class RtmpClient {

    private static final String TAG = "---->RtmpClient<----";

    static {
        System.loadLibrary("native-lib");
    }

    private final LifecycleOwner lifecycleOwner;
    private int width;
    private int height;
    private boolean isConnectd;
    private VideoChanel videoChanel;
    private AudioChannel audioChannel;

    public RtmpClient(LifecycleOwner lifecycleOwner) {
        this.lifecycleOwner = lifecycleOwner;
        nativeInit();
    }

    public void initVideo(TextureView displayer, int width, int height, int fps, int bitRate) {
        this.width = width;
        this.height = height;
        videoChanel = new VideoChanel(lifecycleOwner, displayer, this);
        initVideoEnc(width, height, fps, bitRate);
    }

    public void initAudio(int sampleRate, int channels) {
        audioChannel = new AudioChannel(sampleRate, channels, this);
        int inputByteNum = initAudioEnc(sampleRate, channels);
        audioChannel.setInputByteNum(inputByteNum);
    }

    public void toggleCamera() {
        videoChanel.toggleCamera();
    }


    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public boolean isConnectd() {
        return isConnectd;
    }

    public void startLive(String url) {
        connect(url);
    }

    /**
     * JNICall 回调
     * @param isConnect
     */
    private void onPrepare(boolean isConnect) {
        this.isConnectd = isConnect;
        audioChannel.start();
        Log.e(TAG, "开始直播==================");

    }

    public void stopLive() {
        isConnectd = false;
        audioChannel.stop();
        disConnect();
        Log.e(TAG, "停止直播==================");
    }

    public void sendVideo(byte[] buffer) {
        nativeSendVideo(buffer);
    }

    public void sendAudio(byte[] buffer, int len) {
        nativeSendAudio(buffer, len);
    }


    public void release() {
        videoChanel.release();
        audioChannel.release();
        releaseVideoEnc();
        releaseAudioEnc();
        nativeDeInit();
    }


    private native void connect(String url);

    private native void disConnect();

    private native void nativeInit();

    private native void initVideoEnc(int width, int height, int fps, int bitRate);

    private native void releaseVideoEnc();

    private native void nativeDeInit();

    private native void nativeSendVideo(byte[] buffer);

    private native int initAudioEnc(int sampleRate, int channels);

    private native void releaseAudioEnc();

    private native void nativeSendAudio(byte[] buffer, int len);


}
