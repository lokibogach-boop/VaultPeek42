#include <jni.h>
#include <stdint.h>
#include <stdio.h>
#include <string.h>

#include "biomes.h"
#include "rng.h"
#include "loot/loot_tables.h"
#include "loot/loot_table_context.h"
#include "loot/loot_functions.h"
#include "loot/mc_loot.h"

static uint64_t mix_stafford13(uint64_t value) {
    value = (value ^ (value >> 30)) * UINT64_C(0xbf58476d1ce4e5b9);
    value = (value ^ (value >> 27)) * UINT64_C(0x94d049bb133111eb);
    return value ^ (value >> 31);
}

static Xoroshiro ominous_sequence_seed(int64_t world_seed) {
    const uint64_t seed_xor = UINT64_C(0x6a09e667f3bcc909);
    const uint64_t golden_gamma = UINT64_C(0x9e3779b97f4a7c15);
    uint64_t lo = ((uint64_t)world_seed) ^ seed_xor;
    uint64_t hi = lo + golden_gamma;

    /* MD5 of minecraft:chests/trial_chambers/reward_ominous */
    lo ^= UINT64_C(0x05a13d5ce5edaab3);
    hi ^= UINT64_C(0x1a3950a30a86bc23);

    Xoroshiro state;
    state.lo = mix_stafford13(lo);
    state.hi = mix_stafford13(hi);
    return state;
}


/* Keep special loot outcomes generic in the public sequence viewer. */
static int is_special_or_weapon_related_reward(const char *name) {
    return strcmp(name, "minecraft:heavy_core") == 0
        || strcmp(name, "minecraft:enchanted_golden_apple") == 0
        || strcmp(name, "minecraft:flow_armor_trim_smithing_template") == 0
        || strcmp(name, "minecraft:flow_banner_pattern") == 0
        || strcmp(name, "minecraft:music_disc_creator") == 0
        || strcmp(name, "minecraft:crossbow") == 0
        || strcmp(name, "minecraft:diamond_axe") == 0
        || strcmp(name, "minecraft:book") == 0
        || strstr(name, "_sword") != NULL
        || strstr(name, "_axe") != NULL
        || strstr(name, "_bow") != NULL
        || strstr(name, "trident") != NULL
        || strstr(name, "mace") != NULL;
}

static const char *safe_display_name(const char *name) {
    return is_special_or_weapon_related_reward(name)
            ? "vaultpeek:special_reward"
            : name;
}

JNIEXPORT jobjectArray JNICALL
Java_dev_vaultpeek_NativeLootPredictor_generateOminousNative(
        JNIEnv *env, jclass clazz, jlong world_seed, jint offset, jint amount) {
    (void)clazz;
    if (offset < 0 || amount < 1 || amount > 5000 || offset > 1000000) {
        return NULL;
    }

    LootTableContext *context = NULL;
    if (!init_reward_ominous(&context, MC_1_21_4) || context == NULL) {
        return NULL;
    }

    set_loot_prng_type(context, XOROSHIRO);
    RandomState initial_state;
    memset(&initial_state, 0, sizeof(initial_state));
    initial_state.xr = ominous_sequence_seed((int64_t)world_seed);
    set_internal_loot_seed(context, initial_state);

    for (jint i = 0; i < offset; i++) {
        generate_loot(context);
    }

    jclass string_class = (*env)->FindClass(env, "java/lang/String");
    if (string_class == NULL) {
        free_loot_table(context);
        return NULL;
    }
    jobjectArray result = (*env)->NewObjectArray(env, amount, string_class, NULL);
    if (result == NULL) {
        free_loot_table(context);
        return NULL;
    }

    for (jint roll = 0; roll < amount; roll++) {
        generate_loot(context);
        char buffer[8192];
        size_t used = 0;
        buffer[0] = '\0';

        for (int i = 0; i < context->generated_item_count; i++) {
            ItemStack *stack = &context->generated_items[i];
            const char *name = get_item_name(context, stack->item);
            if (name == NULL || stack->count <= 0) {
                continue;
            }
            name = safe_display_name(name);

            int written = snprintf(
                    buffer + used,
                    sizeof(buffer) - used,
                    "%s%s*%d",
                    used == 0 ? "" : "|",
                    name,
                    stack->count
            );
            if (written < 0 || (size_t)written >= sizeof(buffer) - used) {
                used = sizeof(buffer) - 1;
                break;
            }
            used += (size_t)written;
        }

        jstring entry = (*env)->NewStringUTF(env, buffer);
        if (entry == NULL) {
            free_loot_table(context);
            return NULL;
        }
        (*env)->SetObjectArrayElement(env, result, roll, entry);
        (*env)->DeleteLocalRef(env, entry);
    }

    free_loot_table(context);
    return result;
}
