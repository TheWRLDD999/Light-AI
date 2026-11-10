#include <jni.h>
#include <string>

extern "C"
JNIEXPORT jstring JNICALL
Java_com_thewrldd999_lightai_MainActivity_getEngineStatus(
        JNIEnv* env,
        jobject /* this */) {

    return env->NewStringUTF(
        "Light AI native bridge connected!"
    );
}
