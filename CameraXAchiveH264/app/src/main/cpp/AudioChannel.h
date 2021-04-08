
#ifndef PUSHER_AUDIOCHANNEL_H
#define PUSHER_AUDIOCHANNEL_H

#include <faac.h>
#include "Callback.h"


class AudioChannel {
public:
    AudioChannel();

    ~AudioChannel();

    void encode(int32_t *data, int len);

public:
    void openCodec(int sampleRate, int channels);

    int getInputByteNum() {
        return inputByteNum;
    }

    void setCallback(Callback callback) {
        this->callback = callback;
    }

    RTMPPacket *getAudioConfig();

private:
    void closeCodec();

private:
    Callback callback;
    faacEncHandle codec = 0;
    unsigned long inputByteNum;
    unsigned char *outputBuffer = 0;
    unsigned long maxOutputBytes;
};


#endif //PUSHER_AUDIOCHANNEL_H
