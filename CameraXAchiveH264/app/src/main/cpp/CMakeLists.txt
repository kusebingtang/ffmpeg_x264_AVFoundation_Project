cmake_minimum_required(VERSION 3.4.1)

include_directories(${CMAKE_SOURCE_DIR}/librtmp)
# 加入子文件夹
add_subdirectory(librtmp)

#x264 lib
include_directories(${CMAKE_SOURCE_DIR}/x264/${ANDROID_ABI}/include)
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -L${CMAKE_SOURCE_DIR}/x264/${ANDROID_ABI}/lib")

include_directories(${CMAKE_SOURCE_DIR}/faac/${ANDROID_ABI}/include)
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -L${CMAKE_SOURCE_DIR}/faac/${ANDROID_ABI}/lib")

message(CMAKE_CXX_FLAGS,${CMAKE_CXX_FLAGS})



add_library(
        native-lib
        SHARED
        native-lib.cpp
        JavaCallHelper.cpp
        VideoChannel.cpp
        AudioChannel.cpp
)
#推流
target_link_libraries(
        native-lib
        rtmp
        x264
        faac
        log)


# 图像处理操作
include_directories(${CMAKE_SOURCE_DIR}/libyuv/include)
add_subdirectory(libyuv)


add_library(
        ImageUtils
        SHARED
        ImageUtils.cpp)

target_link_libraries(
        ImageUtils
        yuv)
