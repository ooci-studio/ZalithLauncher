package com.movtery.zalithlauncher.feature.unpack

import android.content.Context
import com.movtery.zalithlauncher.feature.log.Logging.e
import com.movtery.zalithlauncher.utils.CopyDefaultFromAssets.Companion.copyFromAssets
import com.movtery.zalithlauncher.utils.PathAndUrlManager
import net.kdt.pojavlaunch.Tools

class UnpackSingleFilesTask(val context: Context) : AbstractUnpackTask() {
    override fun isNeedUnpack(): Boolean = true

    override fun run() {
        runCatching {
            copyFromAssets(context)
            Tools.copyAssetFile(context, "servers.json", PathAndUrlManager.DIR_GAME_HOME, true)
            Tools.copyAssetFile(context, "resolv.conf", PathAndUrlManager.DIR_DATA, false)
        }.getOrElse { e("AsyncAssetManager", "Failed to unpack critical components !") }
    }
}