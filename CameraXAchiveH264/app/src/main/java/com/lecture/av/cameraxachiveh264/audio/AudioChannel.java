package com.lecture.av.cameraxachiveh264.audio;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.HandlerThread;

import com.lecture.av.cameraxachiveh264.RtmpClient;

public class AudioChannel {

    private int sampleRate;
    private int minBufferSize;
    private int channelConfig;
    private Handler handler;
    private RtmpClient rtmpClient;
    private HandlerThread handlerThread;
    private AudioRecord audioRecord;
    private byte[] buffer;

    public AudioChannel(int sampleRate, int channels, RtmpClient rtmpClient) {
        this.rtmpClient = rtmpClient;
        this.sampleRate = sampleRate;
        channelConfig = channels == 2 ? AudioFormat.CHANNEL_IN_STEREO : AudioFormat.CHANNEL_IN_MONO;
        minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig,
                AudioFormat.ENCODING_PCM_16BIT);

        handlerThread = new HandlerThread("Audio-Record");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
    }

    public void start() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                        sampleRate, channelConfig,
                        AudioFormat.ENCODING_PCM_16BIT, minBufferSize);

                audioRecord.startRecording();
                while (audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                    int len = audioRecord.read(buffer, 0, buffer.length);
                    if (len > 0) {
                        // 样本数 = 字节数/2字节（16位）
                        rtmpClient.sendAudio(buffer, len >> 1);
                    }
                }
            }
        });
    }

    public void stop() {
        audioRecord.stop();
    }

    public void setInputByteNum(int inputByteNum) {
        buffer = new byte[inputByteNum];
        // 假设 最小缓存区是：1024，inputByteNum：2048
        minBufferSize = inputByteNum > minBufferSize ? inputByteNum : minBufferSize;
    }

    public void release() {
        handlerThread.quitSafely();
        handler.removeCallbacksAndMessages(null);
        audioRecord.release();
    }
}