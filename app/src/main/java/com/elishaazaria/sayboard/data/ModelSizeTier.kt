package com.elishaazaria.sayboard.data

/**
 * How heavy a model is, used to warn the user before downloading.
 *
 * GENERATED FILE - do not edit by hand.
 * Edit `catalog/vosk_models.json` and run `./gradlew :desktop:generateCatalog` instead.
 */
enum class ModelSizeTier(val maxSizeMb: Int, val recommendedRamMb: Int) {
    SMALL(150, 500),
    MEDIUM(600, 1500),
    LARGE(1600, 4000),
    HUGE(100000, 8000),
    ;

    companion object {
        fun forSizeMb(sizeMb: Int): ModelSizeTier =
            values().first { sizeMb <= it.maxSizeMb }
    }
}
