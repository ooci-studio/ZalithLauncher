package com.movtery.zalithlauncher

import android.content.Context

class InfoCenter {
    companion object {
        const val LAUNCHER_NAME: String = "OociLauncher"
        const val APP_NAME: String = "Ooci Launcher"

        @JvmStatic
        fun replaceName(context: Context, resString: Int): String = context.getString(resString, APP_NAME)
    }
}
