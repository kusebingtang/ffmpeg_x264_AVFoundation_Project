package com.lecture.av.audiorecordwav.channel;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.HandlerThread;
import android.util.Log;

import com.lecture.av.audiorecordwav.util.PcmToWavUtil;

import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class AudioChannel extends Thread {

    private int sampleRate;
    private int channelConfig;
    private int minBufferSize;
    private byte[] buffer;
    private HandlerThread handlerThread;
    private AudioRecord audioRecord;
    private boolean isRecoding;
    private SimpleDateFormat sdf;

    public AudioChannel(int sampleRate, int channels) {
        this.sampleRate = sampleRate;
        //双通道应该传的值
        channelConfig = channels == 2 ? AudioFormat.CHANNEL_IN_STEREO :
                AudioFormat.CHANNEL_IN_MONO;
        minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig,
                AudioFormat.ENCODING_PCM_16BIT);
        Log.i("AudioChannel", "minBufferSize: " + minBufferSize);
        buffer = new byte[minBufferSize];

        sdf = new java.text.SimpleDateFormat(
                "yyyy-MM-dd-HH:mm:ss");
    }

    @Override
    public void run() {
        //读取麦克风的数据
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                sampleRate, channelConfig,
                AudioFormat.ENCODING_PCM_16BIT, minBufferSize);
        //开始录音
        audioRecord.startRecording();

        FileOutputStream writer = null;
        Date current = new Date();
        String time = sdf.format(current);
        try {
            // 打开一个写文件器，构造函数中的第二个参数true表示以追加形式写文件
            writer = new FileOutputStream(Environment.getExternalStorageDirectory() + "/" + time + ".pcm", true);
            while (!Thread.currentThread().isInterrupted() && isRecoding) {
                if (audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                    //len实际长度len 打印下这个值
                    int len = audioRecord.read(buffer, 0, buffer.length);
                    Log.i("AudioChannel", "len: " + len);
                    writer.write(buffer);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        new PcmToWavUtil(44100,  AudioFormat.CHANNEL_IN_STEREO,
                2, AudioFormat.ENCODING_PCM_16BIT).pcmToWav(
                        Environment.getExternalStorageDirectory() + "/" + time + ".pcm"
                , Environment.getExternalStorageDirectory() + "/" + time + ".wav");
        Log.i("AudioChannel", "AudioChannel run finish ");
    }

    public void startLive() {
        isRecoding = true;
        this.start();
    }

    public void stopLive() {
        isRecoding = false;
        try {
            join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
