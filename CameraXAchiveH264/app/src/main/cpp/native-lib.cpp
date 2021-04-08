#include <jni.h>
#include <string>
#include <pthread.h>
#include <rtmp.h>
#include "JavaCallHelper.h"
#include "VideoChannel.h"
#include "AudioChannel.h"

VideoChannel *videoChannel = nullptr;
AudioChannel *audioChannel = nullptr;
JavaVM *javaVM = 0;
JavaCallHelper *helper = 0;
pthread_t pid;
char *path = 0;
RTMP *rtmp = 0;
uint64_t startTime;

pthread_mutex_t mutex;

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    javaVM = vm;
    return JNI_VERSION_1_4;
}

void callback(RTMPPacket *packet) {
    if (rtmp) {
        packet->m_nInfoField2 = rtmp->m_stream_id;
        packet->m_nTimeStamp = RTMP_GetTime() - startTime;
        // 1: 放到队列中
        RTMP_SendPacket(rtmp, packet, 1);
    }
    RTMPPacket_Free(packet);
    delete (packet);
}


void *connect(void *args) {
    int ret;
    rtmp = RTMP_Alloc();
    RTMP_Init(rtmp);
    do {
        // 解析url地址，可能失败(地址不合法)
        ret = RTMP_SetupURL(rtmp, path);
        if (!ret) {
            //todo 通知Java 地址传的有问题。
            break;
        }
        //开启输出模式， 播放拉流不需要推流，就可以不开
        RTMP_EnableWrite(rtmp);
        ret = RTMP_Connect(rtmp, 0);
        if (!ret) {
            //todo 通知Java 服务器连接失败。
            break;
        }
        ret = RTMP_ConnectStream(rtmp, 0);
        if (!ret) {
            //todo 通知Java 未连接到流。
            break;
        }
        // 发送audio specific config(告诉播放器怎么解码我推流的音频)
        RTMPPacket *packet = audioChannel->getAudioConfig();
        callback(packet);
    } while (false);

    if (!ret) {
        RTMP_Close(rtmp);
        RTMP_Free(rtmp);
        rtmp = 0;
    }

    delete (path);
    path = 0;

    // 通知Java层可以开始推流了
    helper->onParpare(ret);
    startTime = RTMP_GetTime();
    return 0;
}


extern "C"
JNIEXPORT void JNICALL
Java_com_lecture_av_cameraxachiveh264_RtmpClient_connect(JNIEnv *env, jobject thiz, jstring url_) {

    const char *url = env->GetStringUTFChars(url_, 0);
    path = new char[strlen(url) + 1];
    strcpy(path, url);
    //启动子线程
    pthread_create(&pid, 0, connect, 0);

    env->ReleaseStringUTFChars(url_, url);


}

extern "C"
JNIEXPORT void JNICALL
Java_com_lecture_av_cameraxachiveh264_RtmpClient_nativeInit(JNIEnv *env, jobject thiz) {
    helper = new JavaCallHelper(javaVM, env, thiz);
    pthread_mutex_init(&mutex, 0);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_lecture_av_cameraxachiveh264_RtmpClient_disConnect(JNIEnv *env, jobject thiz) {
    pthread_mutex_lock(&mutex);
    if (rtmp) {
        RTMP_Close(rtmp);
        RTMP_Free(rtmp);
        rtmp = 0;
    }

    if (videoChannel) {
        videoChannel->resetPts();
    }
    pthread_mutex_unlock(&mutex);
}
extern "C"
JNIEXPORT void JNICALL
Java_com_lecture_av_cameraxachiveh264_RtmpClient_initVideoEnc(JNIEnv *env, jobject thiz, jint width,
                                                              jint height, jint fps,
                                                              jint bit_rate) {
    // 准备好视频编码器
    videoChannel = new VideoChannel;
    videoChannel->openCodec(width, height, fps, bit_rate);
    videoChannel->setCallback(callback);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_lecture_av_cameraxachiveh264_RtmpClient_releaseVideoEnc(JNIEnv *env, jobject thiz) {
    if (videoChannel) {
        delete (videoChannel);
        videoChannel = nullptr;
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_lecture_av_cameraxachiveh264_RtmpClient_nativeDeInit(JNIEnv *env, jobject thiz) {
    if (helper) {
        delete (helper);
        helper = nullptr;
    }
    pthread_mutex_destroy(&mutex);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_lecture_av_cameraxachiveh264_RtmpClient_nativeSendVideo(JNIEnv *env, jobject thiz,
                                                                 jbyteArray buffer) {
    jbyte *data = env->GetByteArrayElements(buffer, 0);
    pthread_mutex_lock(&mutex);
    //编码与推流
    videoChannel->encode(reinterpret_cast<uint8_t *>(data));
    pthread_mutex_unlock(&mutex);

    env->ReleaseByteArrayElements(buffer, data, 0);
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_lecture_av_cameraxachiveh264_RtmpClient_initAudioEnc(JNIEnv *env, jobject thiz,
                                                              jint sample_rate, jint channels) {
    audioChannel = new AudioChannel();
    audioChannel->setCallback(callback);
    audioChannel->openCodec(sample_rate, channels);
    return audioChannel->getInputByteNum();
}

extern "C"
JNIEXPORT void JNICALL
Java_com_lecture_av_cameraxachiveh264_RtmpClient_releaseAudioEnc(JNIEnv *env, jobject thiz) {
    if (audioChannel) {
        delete (audioChannel);
        audioChannel = 0;
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_lecture_av_cameraxachiveh264_RtmpClient_nativeSendAudio(JNIEnv *env, jobject thiz,
                                                                 jbyteArray buffer, jint len) {
    jbyte *data = env->GetByteArrayElements(buffer, 0);
    pthread_mutex_lock(&mutex);
    audioChannel->encode(reinterpret_cast<int32_t *>(data), len);
    pthread_mutex_unlock(&mutex);
    env->ReleaseByteArrayElements(buffer, data, 0);
}