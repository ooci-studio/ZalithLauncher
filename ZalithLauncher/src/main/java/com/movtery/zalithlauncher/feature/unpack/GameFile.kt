package com.movtery.zalithlauncher.feature.unpack

import com.movtery.zalithlauncher.R

enum class GameFile(val gameFile: String, val displayName: String, val summary: Int?, val privateDirectory: Boolean) {
    MINECRAFT("minecraft", "Minecraft", R.string.splash_screen_instance, false),
}