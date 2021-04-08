
#include <cstdlib>
#include <rtmp.h>
#include <cstring>
#include <malloc.h>
#include "AudioChannel.h"


AudioChannel::AudioChannel() {

}

AudioChannel::~AudioChannel() {
    closeCodec();
    if (outputBuffer) {
        free(outputBuffer);
        outputBuffer = 0;
    }
}


void AudioChannel::openCodec(int sampleRate, int channels) {
    //输入样本： 要送给编码器编码的样本数
    unsigned long inputSamples;
    codec = faacEncOpen(sampleRate, channels, &inputSamples, &maxOutputBytes);
    // 样本是 16位的，那么一个样本就是2个字节
    inputByteNum = inputSamples * 2;
    outputBuffer = static_cast<unsigned char *>(malloc(maxOutputBytes));
    //得到当前编码器的各种参数配置
    faacEncConfigurationPtr configurationPtr = faacEncGetCurrentConfiguration(codec);
    configurationPtr->mpegVersion = MPEG4;
    configurationPtr->aacObjectType = LOW;
    // 1： 每一帧音频编码的结果数据 都会携带ADTS（包含了采样、声道等信息的一个数据头）
    // 0： 编码出aac裸数据
    configurationPtr->outputFormat = 0;

    configurationPtr->inputFormat = FAAC_INPUT_16BIT;

    faacEncSetConfiguration(codec, configurationPtr);
}


void AudioChannel::closeCodec() {
    if (codec) {
        faacEncClose(codec);
        codec = 0;
    }
}

void AudioChannel::encode(int32_t *data, int len) {
    //3、输入的样本数
    //4、输出，编码之后的结果
    //5、编码结果缓存区能接收数据的个数
    int bytelen = faacEncEncode(codec, data, len, outputBuffer, maxOutputBytes);
    if (bytelen > 0) {

        RTMPPacket *packet = new RTMPPacket;
        RTMPPacket_Alloc(packet, bytelen + 2);
        packet->m_body[0] = 0xAF;
        packet->m_body[1] = 0x01;

        memcpy(&packet->m_body[2], outputBuffer, bytelen);

        packet->m_hasAbsTimestamp = 0;
        packet->m_nBodySize = bytelen + 2;
        packet->m_packetType = RTMP_PACKET_TYPE_AUDIO;
        packet->m_nChannel = 0x11;
        packet->m_headerType = RTMP_PACKET_SIZE_LARGE;
        callback(packet);
    }
}

//相当于视频的sps与pps,但是与视频不同。
// 视频的sps与pps需要在每一个I帧之前发送一次
// 音频的这个信息只需要在发送声音数据包之前发送一个就可以了
RTMPPacket *AudioChannel::getAudioConfig() {
    u_char *buf;
    u_long len;
    //2，3参数：数据与数据长度
    faacEncGetDecoderSpecificInfo(codec, &buf, &len);
    RTMPPacket *packet = new RTMPPacket;
    RTMPPacket_Alloc(packet, len + 2);
    packet->m_body[0] = 0xAF;
    packet->m_body[1] = 0x00;

    memcpy(&packet->m_body[2], buf, len);

    packet->m_hasAbsTimestamp = 0;
    packet->m_nBodySize = len + 2;
    packet->m_packetType = RTMP_PACKET_TYPE_AUDIO;
    packet->m_nChannel = 0x11;
    packet->m_headerType = RTMP_PACKET_SIZE_LARGE;
    return packet;
}
