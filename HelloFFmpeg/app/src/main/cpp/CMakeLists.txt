cmake_minimum_required(VERSION 3.10.2)
project("hellpffmpeg")
include_directories(include)

#添加一个系统查找目录
#添加系统环境变量
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -L${CMAKE_SOURCE_DIR}/libs/${ANDROID_ABI}")

set(JNI_LIBS_DIR ${CMAKE_SOURCE_DIR}/libs)



add_library( # Sets the name of the library.
             native-lib

             SHARED
             native-lib.cpp )


find_library(
              log-lib
              log )


target_link_libraries( # Specifies the target library.
                       native-lib
                        avcodec
                        avdevice
                        avfilter
                        avformat
                        avutil
                        swresample
                        swscale
                       # Links the target library to the log library
                       # included in the NDK.
                        android
                       ${log-lib} )