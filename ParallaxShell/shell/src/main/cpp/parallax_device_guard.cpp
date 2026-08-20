#include <jni.h>
#include <sys/prctl.h>
#include <cstdio>
#include <cstdlib>
#include <cstring>

static int parallax_read_tracer_pid() {
    FILE* fp = fopen("/proc/self/status", "r");
    if (fp == nullptr) {
        return -1;
    }

    char line[256] = {};
    int tracer_pid = -1;
    while (fgets(line, sizeof(line), fp) != nullptr) {
        if (strncmp(line, "TracerPid:", 10) == 0) {
            tracer_pid = atoi(line + 10);
            break;
        }
    }

    fclose(fp);
    return tracer_pid;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_parallax_shell_ParallaxBhaiya_nativeGate(JNIEnv*, jclass) {
#ifdef DEBUG
    return JNI_TRUE;
#else
    const int tracer_pid = parallax_read_tracer_pid();
    const int dumpable = prctl(PR_GET_DUMPABLE, 0, 0, 0, 0);

    if (tracer_pid > 0) {
        return JNI_FALSE;
    }
    if (dumpable != 0) {
        return JNI_FALSE;
    }
    return JNI_TRUE;
#endif
}
