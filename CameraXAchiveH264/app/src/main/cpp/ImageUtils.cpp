
#include <jni.h>
#include <libyuv.h>

extern "C"
JNIEXPORT void JNICALL
Java_com_lecture_av_cameraxachiveh264_ImageUtils_rotation(JNIEnv *env, jclass thiz, jbyteArray data_, jint width,
                                          jint height, jint degress) {

    jbyte *data = env->GetByteArrayElements(data_, 0);
    uint8_t *src = reinterpret_cast<uint8_t *>(data);
    int ySize = width * height;
    int uSize = (width >> 1) * (height >> 1);
    int size = (ySize * 3) >> 1;
    uint8_t dst[size];

    uint8_t *src_y = src;
    uint8_t *src_u = src + ySize;
    uint8_t *src_v = src + ySize + uSize;

    uint8_t *dst_y = dst;
    uint8_t *dst_u = dst + ySize;
    uint8_t *dst_v = dst + ySize + uSize;

    libyuv::I420Rotate(src_y, width, src_u, width >> 1, src_v, width >> 1,
                       dst_y, height, dst_u, height >> 1, dst_v, height >> 1,
                       width, height, static_cast<libyuv::RotationMode>(degress));

    jbyteArray result = env->NewByteArray(size);
    env->SetByteArrayRegion(result, 0, size, reinterpret_cast<const jbyte *>(dst));

    env->ReleaseByteArrayElements(data_, data, 0);

    env->SetByteArrayRegion(data_, 0, size, reinterpret_cast<const jbyte *>(dst));
}

extern "C"
JNIEXPORT void JNICALL
Java_com_lecture_av_cameraxachiveh264_ImageUtils_scale(JNIEnv *env, jclass clazz, jbyteArray src_, jbyteArray dst_,
                                       jint srcWidth,
                                       jint srcHeight, jint dstWidth, jint dstHeight) {
    jbyte *data = env->GetByteArrayElements(src_, 0);
    uint8_t *src = reinterpret_cast<uint8_t *>(data);

    int64_t size = (dstWidth * dstHeight * 3) >> 1;
    uint8_t dst[size];

    uint8_t *src_y;
    uint8_t *src_u;
    uint8_t *src_v;
    int src_stride_y;
    int src_stride_u;
    int src_stride_v;

    uint8_t *dst_y;
    uint8_t *dst_u;
    uint8_t *dst_v;
    int dst_stride_y;
    int dst_stride_u;
    int dst_stride_v;


    src_stride_y = srcWidth;
    src_stride_u = srcWidth >> 1;
    src_stride_v = src_stride_u;

    dst_stride_y = dstWidth;
    dst_stride_u = dstWidth >> 1;
    dst_stride_v = dst_stride_u;

    int src_y_size = srcWidth * srcHeight;
    int src_u_size = src_stride_u * (srcHeight >> 1);
    src_y = src;
    src_u = src + src_y_size;
    src_v = src + src_y_size + src_u_size;

    int dst_y_size = dstWidth * dstHeight;
    int dst_u_size = dst_stride_u * (dstHeight >> 1);
    dst_y = dst;
    dst_u = dst + dst_y_size;
    dst_v = dst + dst_y_size + dst_u_size;

    libyuv::I420Scale(src_y, src_stride_y,
                      src_u, src_stride_u,
                      src_v, src_stride_v,
                      srcWidth, srcHeight,
                      dst_y, dst_stride_y,
                      dst_u, dst_stride_u,
                      dst_v, dst_stride_v,
                      dstWidth, dstHeight,
                      libyuv::FilterMode::kFilterNone);
    env->ReleaseByteArrayElements(src_, data, 0);

    env->SetByteArrayRegion(dst_, 0, size, reinterpret_cast<const jbyte *>(dst));
}
