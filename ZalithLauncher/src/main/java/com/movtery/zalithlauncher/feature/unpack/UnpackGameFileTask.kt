package com.movtery.zalithlauncher.feature.unpack

import android.content.Context
import android.content.res.AssetManager
import com.movtery.zalithlauncher.feature.customprofilepath.ProfilePathHome.Companion.gameHome
import com.movtery.zalithlauncher.feature.log.Logging.i
import net.kdt.pojavlaunch.Tools
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

class UnpackGameFileTask(val context: Context, val gameFile: GameFile) : AbstractUnpackTask() {
    private lateinit var am: AssetManager
    private lateinit var rootDir: String
    private lateinit var versionFile: File
    private lateinit var input: InputStream
    private var isCheckFailed: Boolean = false

    init {
        runCatching {
            am = context.assets
            rootDir = gameHome
            versionFile = File("$rootDir/version")
            input = am.open("components/${gameFile.gameFile}/version")
        }.getOrElse {
            isCheckFailed = true
        }
    }

    fun isCheckFailed() = isCheckFailed

    override fun isNeedUnpack(): Boolean {
        if (isCheckFailed) return false

        if (!versionFile.exists()) {
            requestEmptyParentDir(versionFile)
            i("Unpack GameFile", "${gameFile.gameFile}: Pack was installed manually, or does not exist...")
            return true
        } else {
            val fis = FileInputStream(versionFile)
            val release1 = Tools.read(input)
            val release2 = Tools.read(fis)
            if (release1 != release2) {
                requestEmptyParentDir(versionFile)
                return true
            } else {
                i("UnpackPrep", "${gameFile.gameFile}: Pack is up-to-date with the launcher, continuing...")
                return false
            }
        }
    }

    override fun run() {
        listener?.onTaskStart()
        Tools.copyAssetFolder(
            context,
            "components/${gameFile.gameFile}",
            rootDir,
            true
        )
        listener?.onTaskEnd()
    }

    private fun requestEmptyParentDir(file: File) {
        file.parentFile!!.apply {
            if (exists() and isDirectory) {
                FileUtils.deleteDirectory(this)
            }
            mkdirs()
        }
    }
}