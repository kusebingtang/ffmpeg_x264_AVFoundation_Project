package com.lecture.av.cameraxachiveh264;


import android.graphics.ImageFormat;

import androidx.camera.core.ImageProxy;

import java.nio.ByteBuffer;

public class ImageUtils {

    static {
        System.loadLibrary("ImageUtils");
    }

    static ByteBuffer yuv420;
    static byte[] scaleBytes;

    public static byte[] getBytes(ImageProxy image, int rotationDegrees, int width, int height) {
        if (image.getFormat() != ImageFormat.YUV_420_888) {
            // https://developer.android.google.cn/training/camerax/analyze
            throw new IllegalStateException("根据文档，Camerax图像分析返回的就是YUV420!");
        }
        ImageProxy.PlaneProxy[] planes = image.getPlanes();
        // todo 避免内存抖动.
        int size = image.getWidth() * image.getHeight() * 3 / 2;
        if (yuv420 == null || yuv420.capacity() < size) {
            yuv420 = ByteBuffer.allocate(size);
        }
        yuv420.position(0);

        /**
         * Y数据
         */
        ImageProxy.PlaneProxy plane = planes[0];
        //pixelStride = 1 : 取值无间隔
        //pixelStride = 2 : 间隔1个字节取值
        // y的此数据应该都是1
        int pixelStride = plane.getPixelStride();
        //大于等于宽， 表示连续的两行数据的间隔
        //  如：640x480的数据，
        //  可能得到640
        //  可能得到650，表示每行最后10个字节为补位的数据
        int rowStride = plane.getRowStride();
        ByteBuffer buffer = plane.getBuffer();
        byte[] row = new byte[image.getWidth()];
        // 每行要排除的无效数据，但是需要注意：实际测试中 最后一行没有这个补位数据
        byte[] skipRow = new byte[rowStride - image.getWidth()];
        for (int i = 0; i < image.getHeight(); i++) {
            buffer.get(row);
            yuv420.put(row);
            // 不是最后一行
            if (i < image.getHeight() - 1) {
                buffer.get(skipRow);
            }
        }
        /**
         * U V 数据
         */
        for (int i = 1; i < 3; i++) {
            plane = planes[i];
            pixelStride = plane.getPixelStride();
            // uv数据的rowStride可能是
            // 如：640的宽
            // 可能得到320， pixelStride 为1
            // 可能大于320同时小于640，有为了补位的无效数据  pixelStride 为1
            // 可能得到640 uv数据在一起，pixelStride为2
            // 可能大于640，有为了补位的无效数据 pixelStride为2
            rowStride = plane.getRowStride();
            buffer = plane.getBuffer();
            int uvWidth = image.getWidth() / 2;
            int uvHeight = image.getHeight() / 2;

            for (int j = 0; j < uvHeight; j++) {
                for (int k = 0; k < rowStride; k++) {
                    // 最后一行，是没有补位数据的
                    if (j == uvHeight - 1) {
                        //只有自己(U/V)的数据
                        if (pixelStride == 1) {
                            // 结合外层if 则表示：
                            //  如果是最后一行，我们就不管结尾的占位数据了
                            if (k >= uvWidth) {
                                break;
                            }
                        } else if (pixelStride == 2) {
                            //与同级if相同意思
                            // todo uv混合，
                            //  planes[2]:uvu
                            //  planes[3]:vuv
                            if (k >= image.getWidth() - 1) {
                                break;
                            }
                        }
                    }
                    byte b = buffer.get();
                    if (pixelStride == 2) {
                        //打包格式 uv在一起,偶数位取出来是U数据： 0 2 4 6
                        //1、偶数位下标的数据是我们本次要获得的U/V数据
                        //2、占位无效数据要丢弃，不保存
                        if (k < image.getWidth() && k % 2 == 0) {
                            yuv420.put(b);
                        }
                    } else if (pixelStride == 1) {
                        // uv没有混合在一起
                        if (k < uvWidth) {
                            yuv420.put(b);
                        }
                    }
                }
            }
        }
        int srcWidth = image.getWidth();
        int srcHeight = image.getHeight();
        byte[] result = yuv420.array();
        //注意旋转后 宽高变了
        if (rotationDegrees == 90 || rotationDegrees == 270) {
            //todo jni对result修改值，避免内存抖动
            rotation(result, image.getWidth(), image.getHeight(), rotationDegrees);
            srcWidth = image.getHeight();
            srcHeight = image.getWidth();
        }
        if (srcWidth != width || srcHeight != height) {
            //todo jni对scaleBytes修改值，避免内存抖动
            int scaleSize = width * height * 3 / 2;
            if (scaleBytes == null || scaleBytes.length < scaleSize) {
                scaleBytes = new byte[scaleSize];
            }
            scale(result, scaleBytes, srcWidth, srcHeight, width, height);
            return scaleBytes;
        }
        return result;
    }


    private native static void rotation(byte[] data, int width, int height, int rotationDegrees);

    private native static void scale(byte[] src, byte[] dst, int srcWidth, int srcHeight, int dstWidth, int dstHeight);

    //    nv21ToNV12
    public static void yuvToNv21(byte[] y, byte[] u, byte[] v, byte[] nv21, int stride, int height) {
        System.arraycopy(y, 0, nv21, 0, y.length);
        // 注意，若length值为 y.length * 3 / 2 会有数组越界的风险，需使用真实数据长度计算
        int length = y.length + u.length / 2 + v.length / 2;
        int uIndex = 0, vIndex = 0;
        for (int i = stride * height; i < length; i += 2) {
            nv21[i] = v[vIndex];
            nv21[i + 1] = u[uIndex];
            vIndex += 2;
            uIndex += 2;
        }
    }

    public static byte[] nv21_rotate_to_90(byte[] nv21_data, byte[] nv21_rotated, int width, int height) {
        int y_size = width * height;
        int buffser_size = y_size * 3 / 2;

        // Rotate the Y luma
        int i = 0;
        int startPos = (height - 1) * width;
        for (int x = 0; x < width; x++) {
            int offset = startPos;
            for (int y = height - 1; y >= 0; y--) {
                nv21_rotated[i] = nv21_data[offset + x];
                i++;
                offset -= width;
            }
        }
        // Rotate the U and V color components
        i = buffser_size - 1;
        for (int x = width - 1; x > 0; x = x - 2) {
            int offset = y_size;
            for (int y = 0; y < height / 2; y++) {
                nv21_rotated[i] = nv21_data[offset + x];
                i--;
                nv21_rotated[i] = nv21_data[offset + (x - 1)];
                i--;
                offset += width;
            }
        }
        return nv21_rotated;
    }

    public static byte[] nv21toNV12(byte[] nv21, byte[] nv12) {
        int size = nv21.length;
        nv12 = new byte[size];
        int len = size * 2 / 3;
        System.arraycopy(nv21, 0, nv12, 0, len);

        int i = len;
        while (i < size - 1) {
            nv12[i] = nv21[i + 1];
            nv12[i + 1] = nv21[i];
            i += 2;
        }
        return nv12;
    }


}
