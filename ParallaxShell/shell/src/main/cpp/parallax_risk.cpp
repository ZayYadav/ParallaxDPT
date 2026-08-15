//
// Created by parallax
//

#include "parallax_risk.h"
#include <android/api-level.h>
#include <array>
#include <climits>
#include <dirent.h>
#include <signal.h>
#include <sys/prctl.h>
#include <sys/resource.h>
#include <sys/syscall.h>
#include <time.h>
#include "mbedtls/sha256.h"
#include "mz_crypt.h"
#include "parallax.h"

extern ShellConfig g_shell_config;

PARALLAX_ENCRYPT void hardenProcessAgainstDumping() {
    // Blocks ordinary ptrace/core-dump access. A rooted or kernel-compromised device
    // remains outside the threat model, so this is defense in depth rather than DRM.
    prctl(PR_SET_DUMPABLE, 0, 0, 0, 0);
#ifdef PR_SET_PTRACER
    prctl(PR_SET_PTRACER, 0, 0, 0, 0);
#endif
    const struct rlimit no_core = {0, 0};
    setrlimit(RLIMIT_CORE, &no_core);
}

[[noreturn]] PARALLAX_ENCRYPT NO_INLINE void parallax_crash() {
#ifdef DEBUG
    abort();
#else
    const pid_t process_id = getpid();
    const pid_t thread_id = static_cast<pid_t>(syscall(__NR_gettid));
    syscall(__NR_tgkill, process_id, thread_id, SIGKILL);
    _exit(173);
#endif
}

PARALLAX_ENCRYPT void junkCodeDexProtect(JNIEnv *env) {
    const char *className = AY_OBFUSCATE(JUNK_CLASS_FULL_NAME);
    jclass klass = parallax::jni::FindClass(env, className);
    if(klass == nullptr) {
        parallax_crash();
    }
}

// Compare in-memory libc .text CRC with on-disk .text CRC; crash if mismatched.
PARALLAX_ENCRYPT NO_INLINE void verifyLibcTextCrc() {
    Dl_info info = {};
    if (dladdr(reinterpret_cast<const void *>(&fopen), &info) == 0
        || info.dli_fbase == nullptr) {
        DLOGW("dladdr libc failed, skip text crc");
        return;
    }

    std::string libc_path;
    if (info.dli_fname != nullptr) {
        if (info.dli_fname[0] == '/') {
            libc_path.assign(info.dli_fname);
        } else {
            libc_path = find_so_path(info.dli_fname);
        }
    }
    if (libc_path.empty()) {
        libc_path = find_so_path(AY_OBFUSCATE("libc.so"));
    }
    if (libc_path.empty()) {
        DLOGW("cannot resolve libc path, skip text crc");
        return;
    }

    Elf_Shdr shdr = {};
    get_elf_section(&shdr, libc_path.c_str(), AY_OBFUSCATE(".text"));
    if (shdr.sh_size == 0) {
        DLOGW("libc .text section missing or empty, skip text crc");
        return;
    }

    FILE *fp = fopen(libc_path.c_str(), "r");
    if (fp == nullptr) {
        DLOGW("cannot open libc file: %s, skip text crc", libc_path.c_str());
        return;
    }

    if (fseek(fp, static_cast<long>(shdr.sh_offset), SEEK_SET) != 0) {
        DLOGW("fseek libc .text failed, skip text crc");
        fclose(fp);
        return;
    }

    auto *file_buf = static_cast<uint8_t *>(malloc(shdr.sh_size));
    if (file_buf == nullptr) {
        DLOGW("malloc for libc .text failed, skip text crc");
        fclose(fp);
        return;
    }

    size_t nread = fread(file_buf, 1, shdr.sh_size, fp);
    fclose(fp);
    if (nread != shdr.sh_size) {
        DLOGW("fread libc .text incomplete, skip text crc");
        PARALLAX_FREE(file_buf);
        return;
    }

    const auto *mem_base = reinterpret_cast<const uint8_t *>(info.dli_fbase) + shdr.sh_addr;
    if (!isMemReadable(mem_base, shdr.sh_size)) {
        DLOGW("libc .text memory not readable, skip text crc");
        PARALLAX_FREE(file_buf);
        return;
    }

    uint32_t crc_file = 0;
    uint32_t crc_mem = 0;
    size_t remaining = shdr.sh_size;
    size_t offset = 0;
    while (remaining > 0) {
        int32_t chunk = remaining > static_cast<size_t>(INT32_MAX)
                        ? INT32_MAX
                        : static_cast<int32_t>(remaining);
        crc_file = mz_crypt_crc32_update(crc_file, file_buf + offset, chunk);
        crc_mem = mz_crypt_crc32_update(crc_mem, mem_base + offset, chunk);
        offset += static_cast<size_t>(chunk);
        remaining -= static_cast<size_t>(chunk);
    }
    PARALLAX_FREE(file_buf);

    DLOGD("libc .text crc file=%08x mem=%08x size=%u", crc_file, crc_mem,
          static_cast<unsigned>(shdr.sh_size));
    if (crc_file != crc_mem) {
        DLOGW("libc .text crc mismatch, file=%08x mem=%08x", crc_file, crc_mem);
        parallax_crash();
    }
}

namespace {

constexpr size_t kIntegrityWindow = 96;
constexpr size_t kIntegrityTargetCount = 3;
std::array<std::array<uint8_t, 32>, kIntegrityTargetCount> g_integrity_baseline{};
bool g_integrity_initialized = false;

bool constantTimeEqual(const uint8_t *left, const uint8_t *right, size_t size) {
    uint8_t difference = 0;
    for (size_t i = 0; i < size; ++i) {
        difference |= static_cast<uint8_t>(left[i] ^ right[i]);
    }
    return difference == 0;
}

bool containsCaseInsensitive(const char *text, const char *needle) {
    if (text == nullptr || needle == nullptr) {
        return false;
    }
    const size_t needle_length = strlen(needle);
    if (needle_length == 0) {
        return false;
    }
    for (const char *cursor = text; *cursor != '\0'; ++cursor) {
        if (strncasecmp(cursor, needle, needle_length) == 0) {
            return true;
        }
    }
    return false;
}

bool containsInstrumentationMarker(const char *text) {
    return containsCaseInsensitive(text, AY_OBFUSCATE("frida"))
           || containsCaseInsensitive(text, AY_OBFUSCATE("gum-js"))
           || containsCaseInsensitive(text, AY_OBFUSCATE("linjector"))
           || containsCaseInsensitive(text, AY_OBFUSCATE("libsubstrate"))
           || containsCaseInsensitive(text, AY_OBFUSCATE("xposed"))
           || containsCaseInsensitive(text, AY_OBFUSCATE("lsposed"))
           || containsCaseInsensitive(text, AY_OBFUSCATE("edxp"))
           || containsCaseInsensitive(text, AY_OBFUSCATE("sandhook"))
           || containsCaseInsensitive(text, AY_OBFUSCATE("riru"))
           || containsCaseInsensitive(text, AY_OBFUSCATE("zygisk"));
}

bool suspiciousMappingLoaded() {
    FILE *fp = fopen(AY_OBFUSCATE("/proc/self/maps"), "r");
    if (fp == nullptr) {
        return false;
    }
    char line[1024] = {0};
    bool suspicious = false;
    while (fgets(line, sizeof(line), fp) != nullptr) {
        if (containsInstrumentationMarker(line)) {
            suspicious = true;
            break;
        }
    }
    fclose(fp);
    return suspicious;
}

bool suspiciousFileDescriptorOpen() {
    DIR *directory = opendir(AY_OBFUSCATE("/proc/self/fd"));
    if (directory == nullptr) {
        return false;
    }

    bool suspicious = false;
    struct dirent *entry = nullptr;
    while ((entry = readdir(directory)) != nullptr) {
        if (entry->d_name[0] == '.') {
            continue;
        }
        char link_path[128] = {0};
        char target[512] = {0};
        snprintf(link_path, sizeof(link_path), AY_OBFUSCATE("/proc/self/fd/%s"), entry->d_name);
        const ssize_t length = readlink(link_path, target, sizeof(target) - 1);
        if (length <= 0) {
            continue;
        }
        target[length] = '\0';
        if (containsInstrumentationMarker(target)) {
            suspicious = true;
            break;
        }
    }
    closedir(directory);
    return suspicious;
}

bool suspiciousLoaderEnvironment() {
    const char *preload = getenv(AY_OBFUSCATE("LD_PRELOAD"));
    const char *audit = getenv(AY_OBFUSCATE("LD_AUDIT"));
    return (preload != nullptr && preload[0] != '\0')
           || (audit != nullptr && audit[0] != '\0');
}

} // namespace

PARALLAX_ENCRYPT NO_INLINE void verifyRuntimeIntegrity() {
    const std::array<uintptr_t, kIntegrityTargetCount> targets = {
            reinterpret_cast<uintptr_t>(&detectFrida),
            reinterpret_cast<uintptr_t>(&detectDebugger),
            reinterpret_cast<uintptr_t>(&verifyLibcTextCrc)
    };

    std::array<std::array<uint8_t, 32>, kIntegrityTargetCount> current{};
    for (size_t i = 0; i < targets.size(); ++i) {
        const auto *code = reinterpret_cast<const unsigned char *>(targets[i]);
        if (!isMemReadable(code, kIntegrityWindow)) {
            DLOGW("runtime integrity target is unreadable");
            parallax_crash();
            return;
        }
        mbedtls_sha256(code, kIntegrityWindow, current[i].data(), 0);
    }

    if (!g_integrity_initialized) {
        g_integrity_baseline = current;
        g_integrity_initialized = true;
        return;
    }

    for (size_t i = 0; i < current.size(); ++i) {
        if (!constantTimeEqual(current[i].data(), g_integrity_baseline[i].data(),
                               current[i].size())) {
            DLOGW("runtime code integrity mismatch");
            parallax_crash();
            return;
        }
    }
}

PARALLAX_ENCRYPT void detectFrida() {
    const char *frida_agent = AY_OBFUSCATE("frida-agent");
    const char *pool_frida = AY_OBFUSCATE("pool-frida");
    const char *gmain = AY_OBFUSCATE("gmain");
    const char *gbus = AY_OBFUSCATE("gdbus");
    const char *gum_js_loop = AY_OBFUSCATE("gum-js-loop");

    int frida_so_count = find_in_maps(1, frida_agent);
    if (frida_so_count > 0) {
        DLOGD("found frida so");
        parallax_crash();
    }
    int frida_thread_count = find_in_threads_list(4
            , pool_frida
            , gmain
            , gbus
            , gum_js_loop);

    if (frida_thread_count >= 2) {
        DLOGD("found frida threads");
        parallax_crash();
    }

    if (suspiciousMappingLoaded() || suspiciousFileDescriptorOpen()
        || suspiciousLoaderEnvironment()) {
        DLOGD("found runtime instrumentation marker");
        parallax_crash();
    }
}

PARALLAX_ENCRYPT void detectDebugger() {
    if (prctl(PR_GET_DUMPABLE, 0, 0, 0, 0) != 0) {
        DLOGD("process unexpectedly dumpable");
        parallax_crash();
    }

    const char *status_path = AY_OBFUSCATE("/proc/self/status");
    FILE *fp = fopen(status_path, "r");
    if (fp == nullptr) {
        DLOGW("cannot open /proc/self/status, skip tracer pid check");
        return;
    }

    const char *tracer_key = AY_OBFUSCATE("TracerPid:");
    char line[256];
    while (fgets(line, sizeof(line), fp) != nullptr) {
        if (strncmp(line, tracer_key, strlen(tracer_key)) == 0) {
            int tracer_pid = 0;
            sscanf(line + strlen(tracer_key), "%d", &tracer_pid);
            if (tracer_pid != 0) {
                DLOGD("found tracer pid: %d", tracer_pid);
                fclose(fp);
                parallax_crash();
            }
            break;
        }
    }
    fclose(fp);
}

PARALLAX_ENCRYPT void detectJavaDebugger(JNIEnv *env) {
    if (env == nullptr) {
        return;
    }

    jclass debug_class = env->FindClass(AY_OBFUSCATE("android/os/Debug"));
    if (debug_class != nullptr) {
        jmethodID connected_method = env->GetStaticMethodID(debug_class,
                AY_OBFUSCATE("isDebuggerConnected"), AY_OBFUSCATE("()Z"));
        jmethodID waiting_method = env->GetStaticMethodID(debug_class,
                AY_OBFUSCATE("waitingForDebugger"), AY_OBFUSCATE("()Z"));
        const bool connected = connected_method != nullptr
                               && env->CallStaticBooleanMethod(debug_class, connected_method);
        const bool waiting = waiting_method != nullptr
                             && env->CallStaticBooleanMethod(debug_class, waiting_method);
        env->DeleteLocalRef(debug_class);
        if (connected || waiting) {
            DLOGD("Java debugger detected");
            parallax_crash();
            return;
        }
    } else if (env->ExceptionCheck()) {
        env->ExceptionClear();
    }

    jclass activity_thread_class = env->FindClass(AY_OBFUSCATE("android/app/ActivityThread"));
    if (activity_thread_class == nullptr) {
        if (env->ExceptionCheck()) {
            env->ExceptionClear();
        }
        return;
    }
    jmethodID current_application_method = env->GetStaticMethodID(activity_thread_class,
            AY_OBFUSCATE("currentApplication"),
            AY_OBFUSCATE("()Landroid/app/Application;"));
    jobject application = current_application_method == nullptr ? nullptr
            : env->CallStaticObjectMethod(activity_thread_class, current_application_method);
    env->DeleteLocalRef(activity_thread_class);
    if (application == nullptr) {
        if (env->ExceptionCheck()) {
            env->ExceptionClear();
        }
        return;
    }

    jclass application_class = env->GetObjectClass(application);
    jmethodID get_application_info = env->GetMethodID(application_class,
            AY_OBFUSCATE("getApplicationInfo"),
            AY_OBFUSCATE("()Landroid/content/pm/ApplicationInfo;"));
    jobject application_info = get_application_info == nullptr ? nullptr
            : env->CallObjectMethod(application, get_application_info);
    env->DeleteLocalRef(application_class);
    env->DeleteLocalRef(application);
    if (application_info == nullptr) {
        if (env->ExceptionCheck()) {
            env->ExceptionClear();
        }
        return;
    }

    jclass application_info_class = env->GetObjectClass(application_info);
    jfieldID flags_field = env->GetFieldID(application_info_class,
            AY_OBFUSCATE("flags"), AY_OBFUSCATE("I"));
    const jint flags = flags_field == nullptr ? 0
            : env->GetIntField(application_info, flags_field);
    env->DeleteLocalRef(application_info_class);
    env->DeleteLocalRef(application_info);
    constexpr jint kFlagDebuggable = 0x2;
    if ((flags & kFlagDebuggable) != 0) {
        DLOGD("debuggable application detected");
        parallax_crash();
    }
}

[[noreturn]] PARALLAX_ENCRYPT void *detectRiskOnThread(__unused void *args) {
    while (true) {
        if ((g_shell_config.risk_check_flags & FLAG_DISABLE_FRIDA_DETECT) == 0) {
            detectFrida();
        }
        if ((g_shell_config.risk_check_flags & FLAG_DISABLE_CRC_DETECT) == 0) {
            verifyRuntimeIntegrity();
            verifyLibcTextCrc();
        }
        if ((g_shell_config.risk_check_flags & FLAG_DISABLE_ANTI_DEBUG) == 0) {
            detectDebugger();
        }
        struct timespec now = {};
        clock_gettime(CLOCK_MONOTONIC, &now);
        const uint64_t jitter = static_cast<uint64_t>(now.tv_nsec)
                                ^ static_cast<uint64_t>(getpid());
        sleep(3u + static_cast<unsigned int>(jitter % 5u));
    }
}

PARALLAX_ENCRYPT void detectRisk() {
    if ((g_shell_config.risk_check_flags & FLAG_DISABLE_CRC_DETECT) == 0) {
        verifyRuntimeIntegrity();
    }
    pthread_t t;
    if (pthread_create(&t, nullptr, detectRiskOnThread, nullptr) != 0) {
        DLOGW("cannot start runtime protection thread");
        parallax_crash();
        return;
    }
    pthread_detach(t);
}

PARALLAX_ENCRYPT void verifyAppSignature(JNIEnv *env, jobject context, const char *expectedSha256) {
    static std::string actual = {};
    if (context == nullptr || expectedSha256 == nullptr || strlen(expectedSha256) == 0) {
        DLOGW("signature check not configured, skip");
        return;
    }

    if(!actual.empty()) {
        if (parallax_strncasecmp(actual.c_str(), expectedSha256, 64) != 0) {
            DLOGW("signature cache verification failed, expected: %s actual: %s", expectedSha256, actual.c_str());
            parallax_crash();
        }
        return;
    }

    jobject pm = parallax::jni::CallObjectMethod(env, context,
            AY_OBFUSCATE("getPackageManager"),
            AY_OBFUSCATE("()Landroid/content/pm/PackageManager;"));
    if (pm == nullptr) {
        DLOGW("getPackageManager failed");
        parallax_crash();
        return;
    }

    jstring packageName = (jstring) parallax::jni::CallObjectMethod(env, context,
            AY_OBFUSCATE("getPackageName"),
            AY_OBFUSCATE("()Ljava/lang/String;"));
    if (packageName == nullptr) {
        DLOGW("getPackageName failed");
        parallax_crash();
        return;
    }

    int api = android_get_device_api_level();
    jint flags = (api >= 28) ? (jint)0x08000000 : (jint)0x40;

    jobject packageInfo = parallax::jni::CallObjectMethod(env, pm,
            AY_OBFUSCATE("getPackageInfo"),
            AY_OBFUSCATE("(Ljava/lang/String;I)Landroid/content/pm/PackageInfo;"),
            packageName, flags);
    if (packageInfo == nullptr) {
        DLOGW("getPackageInfo failed");
        parallax_crash();
        return;
    }

    jbyteArray certBytes = nullptr;
    if (api >= 28) {
        jobject signingInfo = parallax::jni::GetObjectField(env, packageInfo,
                AY_OBFUSCATE("signingInfo"),
                AY_OBFUSCATE("Landroid/content/pm/SigningInfo;"));
        if (signingInfo == nullptr) {
            DLOGW("signingInfo is null");
            parallax_crash();
            return;
        }
        jobjectArray signaturesArr = (jobjectArray) parallax::jni::CallObjectMethod(env, signingInfo,
                AY_OBFUSCATE("getApkContentsSigners"),
                AY_OBFUSCATE("()[Landroid/content/pm/Signature;"));
        if (signaturesArr == nullptr || env->GetArrayLength(signaturesArr) == 0) {
            DLOGW("getApkContentsSigners returned empty");
            parallax_crash();
            return;
        }
        jobject signature = env->GetObjectArrayElement(signaturesArr, 0);
        certBytes = (jbyteArray) parallax::jni::CallObjectMethod(env, signature,
                AY_OBFUSCATE("toByteArray"), AY_OBFUSCATE("()[B"));
    } else {
        jobjectArray signaturesArr = (jobjectArray) parallax::jni::GetObjectField(env, packageInfo,
                AY_OBFUSCATE("signatures"),
                AY_OBFUSCATE("[Landroid/content/pm/Signature;"));
        if (signaturesArr == nullptr || env->GetArrayLength(signaturesArr) == 0) {
            DLOGW("signatures field is empty");
            parallax_crash();
            return;
        }
        jobject signature = env->GetObjectArrayElement(signaturesArr, 0);
        certBytes = (jbyteArray) parallax::jni::CallObjectMethod(env, signature,
                AY_OBFUSCATE("toByteArray"), AY_OBFUSCATE("()[B"));
    }

    if (certBytes == nullptr) {
        DLOGW("certBytes is null");
        parallax_crash();
        return;
    }

    jsize certLen = env->GetArrayLength(certBytes);
    jbyte *certData = env->GetByteArrayElements(certBytes, nullptr);

    uint8_t sha256Output[32];
    mbedtls_sha256(reinterpret_cast<const unsigned char *>(certData),
                   static_cast<size_t>(certLen), sha256Output, 0);

    env->ReleaseByteArrayElements(certBytes, certData, JNI_ABORT);

    char sha256Hex[65] = {0};
    for (int i = 0; i < 32; i++) {
        snprintf(sha256Hex + i * 2, 3, "%02x", sha256Output[i]);
    }

    actual.assign(sha256Hex);

    if (parallax_strncasecmp(sha256Hex, expectedSha256, 64) != 0) {
        DLOGW("signature verification failed, expected: %s actual: %s", expectedSha256, sha256Hex);
        parallax_crash();
    }
}
