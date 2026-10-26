package com.elishaazaria.sayboard.data

import java.util.Locale

/**
 * Catalog of every speech recognition model offered by the built-in downloader.
 *
 * GENERATED FILE - do not edit by hand.
 * Edit `catalog/vosk_models.json` and run `./gradlew :desktop:generateCatalog` instead.
 */
enum class ModelLink(
    val link: String,
    val locale: Locale,
    val sizeMb: Int,
    val license: String,
    val notes: String,
) {

    // English (US)
    VOSK_MODEL_SMALL_EN_US_0_15(
        "https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip",
        Locale.forLanguageTag("en-US"),
        40,
        "Apache-2.0",
        "Lightweight wideband model for Android and RPi"
    ),
    VOSK_MODEL_EN_US_0_22_LGRAPH(
        "https://alphacephei.com/vosk/models/vosk-model-en-us-0.22-lgraph.zip",
        Locale.forLanguageTag("en-US"),
        128,
        "Apache-2.0",
        "Big US English model with dynamic graph"
    ),
    VOSK_MODEL_EN_US_LIBRISPEECH_0_2(
        "https://alphacephei.com/vosk/models/vosk-model-en-us-librispeech-0.2.zip",
        Locale.forLanguageTag("en-US"),
        845,
        "Apache-2.0",
        "Repackaged Librispeech model, not very accurate"
    ),
    VOSK_MODEL_EN_US_DAANZU_20200905_LGRAPH(
        "https://alphacephei.com/vosk/models/vosk-model-en-us-daanzu-20200905-lgraph.zip",
        Locale.forLanguageTag("en-US"),
        129,
        "AGPL-3.0",
        "Dictation model from kaldi-active-grammar, dynamic graph"
    ),
    VOSK_MODEL_EN_US_0_21(
        "https://alphacephei.com/vosk/models/vosk-model-en-us-0.21.zip",
        Locale.forLanguageTag("en-US"),
        1600,
        "Apache-2.0",
        "Wideband model, previous generation"
    ),
    VOSK_MODEL_EN_US_0_22(
        "https://alphacephei.com/vosk/models/vosk-model-en-us-0.22.zip",
        Locale.forLanguageTag("en-US"),
        1800,
        "Apache-2.0",
        "Accurate generic US English model"
    ),
    VOSK_MODEL_EN_US_0_42_GIGASPEECH(
        "https://alphacephei.com/vosk/models/vosk-model-en-us-0.42-gigaspeech.zip",
        Locale.forLanguageTag("en-US"),
        2300,
        "Apache-2.0",
        "Accurate model trained on Gigaspeech, best for podcasts"
    ),
    VOSK_MODEL_EN_US_DAANZU_20200905(
        "https://alphacephei.com/vosk/models/vosk-model-en-us-daanzu-20200905.zip",
        Locale.forLanguageTag("en-US"),
        1000,
        "AGPL-3.0",
        "Wideband dictation model from kaldi-active-grammar"
    ),
    VOSK_MODEL_EN_US_ASPIRE_0_2(
        "https://alphacephei.com/vosk/models/vosk-model-en-us-aspire-0.2.zip",
        Locale.forLanguageTag("en-US"),
        1400,
        "Apache-2.0",
        "Kaldi original ASPIRE model, not very accurate"
    ),
    VOSK_MODEL_SMALL_EN_US_ZAMIA_0_5(
        "https://alphacephei.com/vosk/models/vosk-model-small-en-us-zamia-0.5.zip",
        Locale.forLanguageTag("en-US"),
        49,
        "LGPL-3.0",
        "Repackaged Zamia model, mainly for research"
    ),

    // English (India)
    VOSK_MODEL_SMALL_EN_IN_0_4(
        "https://alphacephei.com/vosk/models/vosk-model-small-en-in-0.4.zip",
        Locale.forLanguageTag("en-IN"),
        36,
        "Apache-2.0",
        "Lightweight Indian English model for mobile applications"
    ),
    VOSK_MODEL_EN_IN_0_5(
        "https://alphacephei.com/vosk/models/vosk-model-en-in-0.5.zip",
        Locale.forLanguageTag("en-IN"),
        1000,
        "Apache-2.0",
        "Generic Indian English model for telecom and broadcast"
    ),

    // Chinese
    VOSK_MODEL_SMALL_CN_0_22(
        "https://alphacephei.com/vosk/models/vosk-model-small-cn-0.22.zip",
        Locale.forLanguageTag("zh"),
        42,
        "Apache-2.0",
        "Lightweight model for Android and RPi"
    ),
    VOSK_MODEL_CN_0_22(
        "https://alphacephei.com/vosk/models/vosk-model-cn-0.22.zip",
        Locale.forLanguageTag("zh"),
        1300,
        "Apache-2.0",
        "Big generic Chinese model for server processing"
    ),
    VOSK_MODEL_CN_KALDI_MULTICN_0_15(
        "https://alphacephei.com/vosk/models/vosk-model-cn-kaldi-multicn-0.15.zip",
        Locale.forLanguageTag("zh"),
        1500,
        "Apache-2.0",
        "Original wideband Kaldi multi-cn model"
    ),

    // Russian
    VOSK_MODEL_SMALL_RU_0_22(
        "https://alphacephei.com/vosk/models/vosk-model-small-ru-0.22.zip",
        Locale.forLanguageTag("ru"),
        45,
        "Apache-2.0",
        "Lightweight wideband model for Android/iOS and RPi"
    ),
    VOSK_MODEL_RU_0_22(
        "https://alphacephei.com/vosk/models/vosk-model-ru-0.22.zip",
        Locale.forLanguageTag("ru"),
        1500,
        "Apache-2.0",
        "Big mixed band Russian model for servers"
    ),
    VOSK_MODEL_RU_0_42(
        "https://alphacephei.com/vosk/models/vosk-model-ru-0.42.zip",
        Locale.forLanguageTag("ru"),
        1800,
        "Apache-2.0",
        "Big mixed band Russian model for servers"
    ),
    VOSK_MODEL_RU_0_10(
        "https://alphacephei.com/vosk/models/vosk-model-ru-0.10.zip",
        Locale.forLanguageTag("ru"),
        2500,
        "Apache-2.0",
        "Big narrowband Russian model for servers"
    ),

    // French
    VOSK_MODEL_SMALL_FR_0_22(
        "https://alphacephei.com/vosk/models/vosk-model-small-fr-0.22.zip",
        Locale.forLanguageTag("fr"),
        41,
        "Apache-2.0",
        "Lightweight wideband model for Android/iOS and RPi"
    ),
    VOSK_MODEL_SMALL_FR_PGUYOT_0_3(
        "https://alphacephei.com/vosk/models/vosk-model-small-fr-pguyot-0.3.zip",
        Locale.forLanguageTag("fr"),
        39,
        "CC-BY-NC-SA-4.0",
        "Lightweight model by Paul Guyot"
    ),
    VOSK_MODEL_FR_0_22(
        "https://alphacephei.com/vosk/models/vosk-model-fr-0.22.zip",
        Locale.forLanguageTag("fr"),
        1400,
        "Apache-2.0",
        "Big accurate model for servers"
    ),
    VOSK_MODEL_FR_0_6_LINTO_2_2_0(
        "https://alphacephei.com/vosk/models/vosk-model-fr-0.6-linto-2.2.0.zip",
        Locale.forLanguageTag("fr"),
        1500,
        "AGPL-3.0",
        "Model from the LINTO project"
    ),

    // German
    VOSK_MODEL_SMALL_DE_0_15(
        "https://alphacephei.com/vosk/models/vosk-model-small-de-0.15.zip",
        Locale.forLanguageTag("de"),
        45,
        "Apache-2.0",
        "Lightweight wideband model for Android and RPi"
    ),
    VOSK_MODEL_SMALL_DE_ZAMIA_0_3(
        "https://alphacephei.com/vosk/models/vosk-model-small-de-zamia-0.3.zip",
        Locale.forLanguageTag("de"),
        49,
        "LGPL-3.0",
        "Zamia small model repackaged (not recommended)"
    ),
    VOSK_MODEL_DE_0_21(
        "https://alphacephei.com/vosk/models/vosk-model-de-0.21.zip",
        Locale.forLanguageTag("de"),
        1900,
        "Apache-2.0",
        "Big German model for telephony and server"
    ),
    VOSK_MODEL_DE_TUDA_0_6_900K(
        "https://alphacephei.com/vosk/models/vosk-model-de-tuda-0.6-900k.zip",
        Locale.forLanguageTag("de"),
        4400,
        "Apache-2.0",
        "Big wideband model from the Tuda-DE project"
    ),

    // Spanish
    VOSK_MODEL_SMALL_ES_0_42(
        "https://alphacephei.com/vosk/models/vosk-model-small-es-0.42.zip",
        Locale.forLanguageTag("es"),
        39,
        "Apache-2.0",
        "Lightweight wideband model for Android and RPi"
    ),
    VOSK_MODEL_ES_0_42(
        "https://alphacephei.com/vosk/models/vosk-model-es-0.42.zip",
        Locale.forLanguageTag("es"),
        1400,
        "Apache-2.0",
        "Big model for Spanish"
    ),

    // Portuguese
    VOSK_MODEL_SMALL_PT_0_3(
        "https://alphacephei.com/vosk/models/vosk-model-small-pt-0.3.zip",
        Locale.forLanguageTag("pt"),
        31,
        "Apache-2.0",
        "Lightweight wideband model for Android and RPi"
    ),
    VOSK_MODEL_PT_FB_V0_1_1_20220516_2113(
        "https://alphacephei.com/vosk/models/vosk-model-pt-fb-v0.1.1-20220516_2113.zip",
        Locale.forLanguageTag("pt"),
        1600,
        "GPL-3.0",
        "Big Brazilian Portuguese model from FalaBrazil"
    ),

    // Greek
    VOSK_MODEL_EL_GR_0_7(
        "https://alphacephei.com/vosk/models/vosk-model-el-gr-0.7.zip",
        Locale.forLanguageTag("el"),
        1100,
        "Apache-2.0",
        "Big narrowband Greek model for servers"
    ),

    // Turkish
    VOSK_MODEL_SMALL_TR_0_3(
        "https://alphacephei.com/vosk/models/vosk-model-small-tr-0.3.zip",
        Locale.forLanguageTag("tr"),
        35,
        "Apache-2.0",
        "Lightweight wideband model for Android and RPi"
    ),

    // Vietnamese
    VOSK_MODEL_SMALL_VN_0_4(
        "https://alphacephei.com/vosk/models/vosk-model-small-vn-0.4.zip",
        Locale.forLanguageTag("vi"),
        32,
        "Apache-2.0",
        "Lightweight Vietnamese model"
    ),
    VOSK_MODEL_VN_0_4(
        "https://alphacephei.com/vosk/models/vosk-model-vn-0.4.zip",
        Locale.forLanguageTag("vi"),
        78,
        "Apache-2.0",
        "Bigger Vietnamese model for server"
    ),

    // Italian
    VOSK_MODEL_SMALL_IT_0_22(
        "https://alphacephei.com/vosk/models/vosk-model-small-it-0.22.zip",
        Locale.forLanguageTag("it"),
        48,
        "Apache-2.0",
        "Lightweight model for Android and RPi"
    ),
    VOSK_MODEL_IT_0_22(
        "https://alphacephei.com/vosk/models/vosk-model-it-0.22.zip",
        Locale.forLanguageTag("it"),
        1200,
        "Apache-2.0",
        "Big generic Italian model for servers"
    ),

    // Dutch
    VOSK_MODEL_SMALL_NL_0_22(
        "https://alphacephei.com/vosk/models/vosk-model-small-nl-0.22.zip",
        Locale.forLanguageTag("nl"),
        39,
        "Apache-2.0",
        "Lightweight model for Dutch"
    ),
    VOSK_MODEL_NL_SPRAAKHERKENNING_0_6_LGRAPH(
        "https://alphacephei.com/vosk/models/vosk-model-nl-spraakherkenning-0.6-lgraph.zip",
        Locale.forLanguageTag("nl"),
        100,
        "CC-BY-NC-SA-4.0",
        "Dutch model with dynamic graph"
    ),
    VOSK_MODEL_NL_SPRAAKHERKENNING_0_6(
        "https://alphacephei.com/vosk/models/vosk-model-nl-spraakherkenning-0.6.zip",
        Locale.forLanguageTag("nl"),
        860,
        "CC-BY-NC-SA-4.0",
        "Medium Dutch model from Kaldi_NL"
    ),

    // Catalan
    VOSK_MODEL_SMALL_CA_0_4(
        "https://alphacephei.com/vosk/models/vosk-model-small-ca-0.4.zip",
        Locale.forLanguageTag("ca"),
        42,
        "Apache-2.0",
        "Lightweight wideband model for Android and RPi"
    ),

    // Arabic
    VOSK_MODEL_AR_MGB2_0_4(
        "https://alphacephei.com/vosk/models/vosk-model-ar-mgb2-0.4.zip",
        Locale.forLanguageTag("ar"),
        318,
        "Apache-2.0",
        "Arabic model trained on the MGB-2 dataset"
    ),
    VOSK_MODEL_AR_0_22_LINTO_1_1_0(
        "https://alphacephei.com/vosk/models/vosk-model-ar-0.22-linto-1.1.0.zip",
        Locale.forLanguageTag("ar"),
        1300,
        "AGPL-3.0",
        "Big model from the LINTO project"
    ),

    // Arabic (Tunisia)
    VOSK_MODEL_SMALL_AR_TN_0_1_LINTO(
        "https://alphacephei.com/vosk/models/vosk-model-small-ar-tn-0.1-linto.zip",
        Locale.forLanguageTag("ar-TN"),
        158,
        "Apache-2.0",
        "Small Tunisian Arabic model from Linagora"
    ),
    VOSK_MODEL_AR_TN_0_1_LINTO(
        "https://alphacephei.com/vosk/models/vosk-model-ar-tn-0.1-linto.zip",
        Locale.forLanguageTag("ar-TN"),
        517,
        "Apache-2.0",
        "Tunisian Arabic model from Linagora"
    ),

    // Persian
    VOSK_MODEL_SMALL_FA_0_42(
        "https://alphacephei.com/vosk/models/vosk-model-small-fa-0.42.zip",
        Locale.forLanguageTag("fa"),
        53,
        "Apache-2.0",
        "Small model for desktop and mobile applications"
    ),
    VOSK_MODEL_SMALL_FA_0_5(
        "https://alphacephei.com/vosk/models/vosk-model-small-fa-0.5.zip",
        Locale.forLanguageTag("fa"),
        60,
        "Apache-2.0",
        "Bigger small model for desktop applications"
    ),
    VOSK_MODEL_FA_0_5(
        "https://alphacephei.com/vosk/models/vosk-model-fa-0.5.zip",
        Locale.forLanguageTag("fa"),
        1000,
        "Apache-2.0",
        "Large vocabulary Persian model"
    ),
    VOSK_MODEL_FA_0_42(
        "https://alphacephei.com/vosk/models/vosk-model-fa-0.42.zip",
        Locale.forLanguageTag("fa"),
        1600,
        "Apache-2.0",
        "Large vocabulary Persian model, best accuracy"
    ),

    // Filipino
    VOSK_MODEL_TL_PH_GENERIC_0_6(
        "https://alphacephei.com/vosk/models/vosk-model-tl-ph-generic-0.6.zip",
        Locale.forLanguageTag("fil"),
        320,
        "CC-BY-NC-SA-4.0",
        "Medium wideband model for Filipino (Tagalog)"
    ),

    // Ukrainian
    VOSK_MODEL_SMALL_UK_V3_NANO(
        "https://alphacephei.com/vosk/models/vosk-model-small-uk-v3-nano.zip",
        Locale.forLanguageTag("uk"),
        73,
        "Apache-2.0",
        "Nano model for Ukrainian"
    ),
    VOSK_MODEL_SMALL_UK_V3_SMALL(
        "https://alphacephei.com/vosk/models/vosk-model-small-uk-v3-small.zip",
        Locale.forLanguageTag("uk"),
        133,
        "Apache-2.0",
        "Small model for Ukrainian"
    ),
    VOSK_MODEL_UK_V3_LGRAPH(
        "https://alphacephei.com/vosk/models/vosk-model-uk-v3-lgraph.zip",
        Locale.forLanguageTag("uk"),
        325,
        "Apache-2.0",
        "Ukrainian model with dynamic graph"
    ),
    VOSK_MODEL_UK_V3(
        "https://alphacephei.com/vosk/models/vosk-model-uk-v3.zip",
        Locale.forLanguageTag("uk"),
        343,
        "Apache-2.0",
        "Bigger Ukrainian model"
    ),

    // Kazakh
    VOSK_MODEL_SMALL_KZ_0_42(
        "https://alphacephei.com/vosk/models/vosk-model-small-kz-0.42.zip",
        Locale.forLanguageTag("kk"),
        58,
        "Apache-2.0",
        "Small mobile model for Kazakh"
    ),
    VOSK_MODEL_KZ_0_42(
        "https://alphacephei.com/vosk/models/vosk-model-kz-0.42.zip",
        Locale.forLanguageTag("kk"),
        1300,
        "Apache-2.0",
        "Bigger model for Kazakh"
    ),

    // Swedish
    VOSK_MODEL_SMALL_SV_RHASSPY_0_15(
        "https://alphacephei.com/vosk/models/vosk-model-small-sv-rhasspy-0.15.zip",
        Locale.forLanguageTag("sv"),
        289,
        "MIT",
        "Repackaged model from the Rhasspy project"
    ),

    // Japanese
    VOSK_MODEL_SMALL_JA_0_22(
        "https://alphacephei.com/vosk/models/vosk-model-small-ja-0.22.zip",
        Locale.forLanguageTag("ja"),
        48,
        "Apache-2.0",
        "Lightweight wideband model for Japanese"
    ),
    VOSK_MODEL_JA_0_22(
        "https://alphacephei.com/vosk/models/vosk-model-ja-0.22.zip",
        Locale.forLanguageTag("ja"),
        1000,
        "Apache-2.0",
        "Big model for Japanese"
    ),

    // Esperanto
    VOSK_MODEL_SMALL_EO_0_42(
        "https://alphacephei.com/vosk/models/vosk-model-small-eo-0.42.zip",
        Locale.forLanguageTag("eo"),
        42,
        "Apache-2.0",
        "Lightweight model for Esperanto"
    ),

    // Hindi
    VOSK_MODEL_SMALL_HI_0_22(
        "https://alphacephei.com/vosk/models/vosk-model-small-hi-0.22.zip",
        Locale.forLanguageTag("hi"),
        42,
        "Apache-2.0",
        "Lightweight model for Hindi"
    ),
    VOSK_MODEL_HI_0_22(
        "https://alphacephei.com/vosk/models/vosk-model-hi-0.22.zip",
        Locale.forLanguageTag("hi"),
        1500,
        "Apache-2.0",
        "Big accurate model for servers"
    ),

    // Czech
    VOSK_MODEL_SMALL_CS_0_4_RHASSPY(
        "https://alphacephei.com/vosk/models/vosk-model-small-cs-0.4-rhasspy.zip",
        Locale.forLanguageTag("cs"),
        44,
        "MIT",
        "Lightweight model for Czech from Rhasspy"
    ),

    // Polish
    VOSK_MODEL_SMALL_PL_0_22(
        "https://alphacephei.com/vosk/models/vosk-model-small-pl-0.22.zip",
        Locale.forLanguageTag("pl"),
        50,
        "Apache-2.0",
        "Lightweight model for Polish"
    ),

    // Uzbek
    VOSK_MODEL_SMALL_UZ_0_22(
        "https://alphacephei.com/vosk/models/vosk-model-small-uz-0.22.zip",
        Locale.forLanguageTag("uz"),
        49,
        "Apache-2.0",
        "Lightweight model for Uzbek"
    ),

    // Korean
    VOSK_MODEL_SMALL_KO_0_22(
        "https://alphacephei.com/vosk/models/vosk-model-small-ko-0.22.zip",
        Locale.forLanguageTag("ko"),
        82,
        "Apache-2.0",
        "Lightweight model for Korean"
    ),

    // Breton
    VOSK_MODEL_BR_0_8(
        "https://alphacephei.com/vosk/models/vosk-model-br-0.8.zip",
        Locale.forLanguageTag("br"),
        70,
        "MIT",
        "Breton model from the vosk-br project"
    ),

    // Gujarati
    VOSK_MODEL_SMALL_GU_0_42(
        "https://alphacephei.com/vosk/models/vosk-model-small-gu-0.42.zip",
        Locale.forLanguageTag("gu"),
        100,
        "Apache-2.0",
        "Lightweight model for Gujarati"
    ),
    VOSK_MODEL_GU_0_42(
        "https://alphacephei.com/vosk/models/vosk-model-gu-0.42.zip",
        Locale.forLanguageTag("gu"),
        700,
        "Apache-2.0",
        "Big Gujarati model"
    ),

    // Tajik
    VOSK_MODEL_SMALL_TG_0_22(
        "https://alphacephei.com/vosk/models/vosk-model-small-tg-0.22.zip",
        Locale.forLanguageTag("tg"),
        50,
        "Apache-2.0",
        "Lightweight model for Tajik"
    ),
    VOSK_MODEL_TG_0_22(
        "https://alphacephei.com/vosk/models/vosk-model-tg-0.22.zip",
        Locale.forLanguageTag("tg"),
        327,
        "Apache-2.0",
        "Big Tajik model"
    ),

    // Telugu
    VOSK_MODEL_SMALL_TE_0_42(
        "https://alphacephei.com/vosk/models/vosk-model-small-te-0.42.zip",
        Locale.forLanguageTag("te"),
        58,
        "Apache-2.0",
        "Lightweight model for Telugu"
    ),

    // Kyrgyz
    VOSK_MODEL_SMALL_KY_0_42(
        "https://alphacephei.com/vosk/models/vosk-model-small-ky-0.42.zip",
        Locale.forLanguageTag("ky"),
        49,
        "Apache-2.0",
        "Small mobile model for Kyrgyz"
    ),
    VOSK_MODEL_KY_0_42(
        "https://alphacephei.com/vosk/models/vosk-model-ky-0.42.zip",
        Locale.forLanguageTag("ky"),
        1100,
        "Apache-2.0",
        "Bigger model for Kyrgyz"
    ),

    // Georgian
    VOSK_MODEL_SMALL_KA_0_42(
        "https://alphacephei.com/vosk/models/vosk-model-small-ka-0.42.zip",
        Locale.forLanguageTag("ka"),
        45,
        "Apache-2.0",
        "Small mobile model for Georgian"
    ),
    VOSK_MODEL_KA_0_42(
        "https://alphacephei.com/vosk/models/vosk-model-ka-0.42.zip",
        Locale.forLanguageTag("ka"),
        700,
        "Apache-2.0",
        "Bigger model for Georgian"
    )
    ;

    val filename: String
        get() = link.substring(link.lastIndexOf('/') + 1, link.lastIndexOf('.'))

    /**
     * The folder the model is unzipped into. Official Vosk archives contain a
     * single top level folder with the same name as the archive itself.
     */
    val folderName: String
        get() = filename

    val sizeTier: ModelSizeTier
        get() = ModelSizeTier.forSizeMb(sizeMb)

    companion object {
        fun forFilename(filename: String): ModelLink? = values().firstOrNull { it.filename == filename }
    }
}
