#include <jni.h>
#include <sys/system_properties.h>
#include <dirent.h>
#include <unistd.h>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <cctype>

static bool parallax_contains_ci(const char* text, const char* marker) {
    if (text == nullptr || marker == nullptr) return false;
    const size_t marker_len = strlen(marker);
    if (marker_len == 0) return false;
    for (const char* p = text; *p != '\0'; ++p) {
        size_t i = 0;
        while (i < marker_len && p[i] != '\0'
               && std::tolower(static_cast<unsigned char>(p[i])) == std::tolower(static_cast<unsigned char>(marker[i]))) {
            ++i;
        }
        if (i == marker_len) return true;
    }
    return false;
}

static int parallax_read_tracer_pid() {
    FILE* fp = fopen("/proc/self/status", "r");
    if (fp == nullptr) return 0;
    char line[256] = {};
    int tracer_pid = 0;
    while (fgets(line, sizeof(line), fp) != nullptr) {
        if (strncmp(line, "TracerPid:", 10) == 0) {
            tracer_pid = atoi(line + 10);
            break;
        }
    }
    fclose(fp);
    return tracer_pid;
}

static bool parallax_suspicious_maps() {
    FILE* fp = fopen("/proc/self/maps", "r");
    if (fp == nullptr) return false;
    const char* markers[] = {
            "frida", "gum-js-loop", "frida-gadget", "xposed", "lsposed",
            "edxp", "riru", "zygisk", "substrate", "sandhook", "linjector"
    };
    char line[1024] = {};
    bool suspicious = false;
    while (!suspicious && fgets(line, sizeof(line), fp) != nullptr) {
        for (const char* marker : markers) {
            if (parallax_contains_ci(line, marker)) {
                suspicious = true;
                break;
            }
        }
    }
    fclose(fp);
    return suspicious;
}

static bool parallax_suspicious_threads() {
    DIR* dir = opendir("/proc/self/task");
    if (dir == nullptr) return false;
    const char* markers[] = {"frida", "gum-js-loop", "linjector", "xposed", "lsposed"};
    bool suspicious = false;
    struct dirent* entry;
    while (!suspicious && (entry = readdir(dir)) != nullptr) {
        if (entry->d_name[0] == '.') continue;
        char path[256] = {};
        snprintf(path, sizeof(path), "/proc/self/task/%s/comm", entry->d_name);
        FILE* fp = fopen(path, "r");
        if (fp == nullptr) continue;
        char name[128] = {};
        if (fgets(name, sizeof(name), fp) != nullptr) {
            for (const char* marker : markers) {
                if (parallax_contains_ci(name, marker)) {
                    suspicious = true;
                    break;
                }
            }
        }
        fclose(fp);
    }
    closedir(dir);
    return suspicious;
}

static bool parallax_property_equals(const char* key, const char* expected) {
    char value[PROP_VALUE_MAX] = {};
    int len = __system_property_get(key, value);
    return len > 0 && strcmp(value, expected) == 0;
}

static bool parallax_property_has_marker(const char* key, const char* const* markers, size_t count) {
    char value[PROP_VALUE_MAX] = {};
    int len = __system_property_get(key, value);
    if (len <= 0) return false;
    for (size_t i = 0; i < count; ++i) {
        if (parallax_contains_ci(value, markers[i])) return true;
    }
    return false;
}

static bool parallax_virtual_environment() {
    if (parallax_property_equals("ro.kernel.qemu", "1") || parallax_property_equals("ro.boot.qemu", "1")) return true;

    const char* hardware_markers[] = {"goldfish", "ranchu", "qemu", "vbox86"};
    const char* model_markers[] = {"google_sdk", "emulator", "android sdk built for", "sdk_gphone", "genymotion"};
    const char* product_markers[] = {"sdk_gphone", "sdk_google", "sdk_x86", "vbox86", "emulator", "simulator"};
    const char* fingerprint_markers[] = {"generic/sdk", "generic_x86", "sdk_gphone", "emulator"};

    if (parallax_property_has_marker("ro.hardware", hardware_markers, sizeof(hardware_markers) / sizeof(hardware_markers[0]))) return true;
    if (parallax_property_has_marker("ro.product.model", model_markers, sizeof(model_markers) / sizeof(model_markers[0]))) return true;
    if (parallax_property_has_marker("ro.product.name", product_markers, sizeof(product_markers) / sizeof(product_markers[0]))) return true;
    if (parallax_property_has_marker("ro.build.fingerprint", fingerprint_markers, sizeof(fingerprint_markers) / sizeof(fingerprint_markers[0]))) return true;

    const char* files[] = {"/dev/qemu_pipe", "/dev/socket/qemud", "/sys/qemu_trace", "/system/bin/qemu-props"};
    for (const char* path : files) {
        if (access(path, F_OK) == 0) return true;
    }
    return false;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_parallax_shell_ParallaxBhaiya_nativeGate(JNIEnv*, jclass) {
    if (parallax_read_tracer_pid() > 0) return JNI_FALSE;
    if (parallax_suspicious_maps()) return JNI_FALSE;
    if (parallax_suspicious_threads()) return JNI_FALSE;
    return JNI_TRUE;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_parallax_shell_ParallaxBhaiya_nativeVirtualGate(JNIEnv*, jclass) {
    return parallax_virtual_environment() ? JNI_FALSE : JNI_TRUE;
}
