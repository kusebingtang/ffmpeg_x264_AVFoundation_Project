package com.lecture.av.cameraxachiveh264.CameraX;

import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.TextureView;
import android.util.Size;
import android.view.View;
import android.view.ViewGroup;

import androidx.camera.core.CameraX;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageAnalysisConfig;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;

import androidx.camera.core.PreviewConfig;
import androidx.lifecycle.LifecycleOwner;

import com.lecture.av.cameraxachiveh264.ImageUtils;
import com.lecture.av.cameraxachiveh264.RtmpClient;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.locks.ReentrantLock;

public class VideoChanel implements Preview.OnPreviewOutputUpdateListener, ImageAnalysis.Analyzer {
    private static final String TAG = "---->VideoChanel<----";
    int width = 480;
    int height = 640;
    private HandlerThread handlerThread;
    private CameraX.LensFacing currentFacing = CameraX.LensFacing.BACK;

    private RtmpClient rtmpClient;

    private TextureView textureView;
    private LifecycleOwner lifecycleOwner;
    private FileOutputStream fos;

    public VideoChanel(LifecycleOwner lifecycleOwner, TextureView textureView,RtmpClient rtmpClient) {
        this.textureView = textureView;
        this.lifecycleOwner = lifecycleOwner;
        this.rtmpClient = rtmpClient;
        //子线程中回调
        handlerThread = new HandlerThread("Analyze-thread");
        handlerThread.start();
        CameraX.bindToLifecycle(lifecycleOwner, getPreView(), getAnalysis());

    }

    private Preview getPreView() {
        // 分辨率并不是最终的分辨率，CameraX会自动根据设备的支持情况，结合你的参数，设置一个最为接近的分辨率
        PreviewConfig previewConfig = new PreviewConfig.Builder().setTargetResolution(new Size(width, height)).setLensFacing(currentFacing).build();
        Preview preview = new Preview(previewConfig);
        preview.setOnPreviewOutputUpdateListener(this);
        return preview;
    }

    private ImageAnalysis getAnalysis() {
        ImageAnalysisConfig imageAnalysisConfig = new ImageAnalysisConfig.Builder()
                .setCallbackHandler(new Handler(handlerThread.getLooper()))
                .setLensFacing(currentFacing)
                .setImageReaderMode(ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE)
                .setTargetResolution(new Size(width, height))
                .build();

        ImageAnalysis imageAnalysis = new ImageAnalysis(imageAnalysisConfig);
        imageAnalysis.setAnalyzer(this);
        return imageAnalysis;
    }


    @Override
    public void analyze(ImageProxy image, int rotationDegrees) {
        // 开启直播并且已经成功连接服务器才获取i420数据
        if (rtmpClient.isConnectd()) {
            byte[] bytes = ImageUtils.getBytes(image, rotationDegrees, rtmpClient.getWidth(), rtmpClient.getHeight());
            rtmpClient.sendVideo(bytes);
        }
    }

    @Override
    public void onUpdated(Preview.PreviewOutput output) {
        SurfaceTexture surfaceTexture = output.getSurfaceTexture();
        if (textureView.getSurfaceTexture() != surfaceTexture) {
            if (textureView.isAvailable()) {
                // 当切换摄像头时，会报错
                ViewGroup parent = (ViewGroup) textureView.getParent();
                parent.removeView(textureView);
                parent.addView(textureView, 0);
                parent.requestLayout();
            }
            textureView.setSurfaceTexture(surfaceTexture);
        }
    }


    public void release() {
        handlerThread.quitSafely();
    }



    public void toggleCamera() {
        CameraX.unbindAll();
        if (currentFacing == CameraX.LensFacing.BACK) {
            currentFacing = CameraX.LensFacing.FRONT;
        } else {
            currentFacing = CameraX.LensFacing.BACK;
        }
        CameraX.bindToLifecycle(lifecycleOwner, getPreView(), getAnalysis());
    }
}
