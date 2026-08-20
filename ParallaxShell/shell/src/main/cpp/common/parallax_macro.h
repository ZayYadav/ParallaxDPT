//
// Created by parallax
//

#ifndef PARALLAX_PARALLAX_MACRO_H
#define PARALLAX_PARALLAX_MACRO_H

#include <stdint.h>
#include <stdlib.h>
#include <sys/mman.h>

#include "common/obfuscate.h"

#define PARALLAX_DATA_SECTION __attribute__((section (".parallax_data")))
#define PARALLAX_ENCRYPT __attribute__((section (".bitcode")))
#define KEEP_SYMBOL __attribute__((used))

#define ARRAY_LENGTH(array) (sizeof(array) / sizeof(array[0]))

#define PAGE_START(addr) (~(getpagesize() - 1) & (addr))
#define PAGE_END(addr) PAGE_START(addr + getpagesize() - 1)
#define PAGE_COVER(addr) (PAGE_END(addr) - PAGE_START(addr))

#define PARALLAX_FREE(ptr) do { if ((ptr) != nullptr) { free(ptr); (ptr) = nullptr; } } while (0)
#define PARALLAX_MUNMAP(ptr, len) do { if ((ptr) != nullptr && (ptr) != MAP_FAILED) { munmap((ptr), (len)); (ptr) = nullptr; } } while (0)

#define SHELL_CONFIG_IN_ZIP "assets/ItsParallaxBaby"
#define CODE_ITEM_NAME_IN_ZIP "assets/Parallax.love"

#define FLAG_DISABLE_FRIDA_DETECT 1
#define FLAG_DISABLE_CRC_DETECT (1 << 1)
#define FLAG_DISABLE_ANTI_DEBUG (1 << 2)

#endif //PARALLAX_PARALLAX_MACRO_H
