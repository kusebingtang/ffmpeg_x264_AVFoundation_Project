
#ifndef PLAYER_JAVACALLHELPER_H
#define PLAYER_JAVACALLHELPER_H

#include <jni.h>

//标记线程 因为子线程需要attach
#define THREAD_MAIN 1
#define THREAD_CHILD 2

class JavaCallHelper {
public:
    JavaCallHelper(JavaVM *_javaVM, JNIEnv *_env, jobject &_jobj);

    ~JavaCallHelper();


    void onParpare(jboolean isConnect, int thread = THREAD_CHILD);

public:
    JavaVM *javaVM;
    JNIEnv *env;
    jobject jobj;
    jmethodID jmid_prepare;


};

#endif //PLAYER_JAVACALLHELPER_H
