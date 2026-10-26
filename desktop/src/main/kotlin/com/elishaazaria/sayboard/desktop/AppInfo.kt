package com.elishaazaria.sayboard.desktop

/**
 * Everything the "About" tab shows. Edit the two marked lines with your own
 * name and link when you publish your fork.
 */
object AppInfo {
    val VERSION: String = Version.NAME

    const val APP_NAME = "sayTypist"

    /** This project (your fork). Change it when you rename the repository. */
    const val PROJECT_URL = "https://github.com/ssmns/Sayboard"
    val ISSUES_URL: String get() = "$PROJECT_URL/issues"

    /** The project sayTypist was forked from. */
    const val UPSTREAM = "https://github.com/ElishaAz/Sayboard"
    const val MODELS_SITE = "https://alphacephei.com/vosk/models"
    const val VOSK_SITE = "https://alphacephei.com/vosk"
    const val LICENSE = "GNU GPL v3.0"
    const val LICENSE_URL = "https://www.gnu.org/licenses/gpl-3.0.html"

    // ------------------------------------------------------------------------
    // put your own details here
    const val MAINTAINER = "sayTypist desktop fork"
    const val MAINTAINER_URL = ""
    // ------------------------------------------------------------------------
}
