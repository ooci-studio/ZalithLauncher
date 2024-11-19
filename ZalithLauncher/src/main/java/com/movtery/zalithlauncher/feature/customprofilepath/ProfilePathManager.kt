package com.movtery.zalithlauncher.feature.customprofilepath

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.movtery.zalithlauncher.context.ContextExecutor
import com.movtery.zalithlauncher.feature.customprofilepath.ProfilePathHome.Companion.gameHome
import com.movtery.zalithlauncher.feature.log.Logging
import com.movtery.zalithlauncher.feature.version.VersionsManager
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.setting.Settings
import com.movtery.zalithlauncher.task.Task
import com.movtery.zalithlauncher.ui.subassembly.customprofilepath.ProfileItem
import com.movtery.zalithlauncher.utils.PathAndUrlManager
import com.movtery.zalithlauncher.utils.StoragePermissionsUtils
import net.kdt.pojavlaunch.Tools
import java.io.File
import java.io.FileWriter
import java.io.IOException

class ProfilePathManager {
    companion object {
        private val defaultPath: String = PathAndUrlManager.DIR_GAME_HOME

        @JvmStatic
        fun setCurrentPathId(id: String?) {
            Settings.Manager.put("launcherProfile", id).save()
            VersionsManager.refresh()
        }

        @JvmStatic
        val currentPath: String
            get() {
                if (StoragePermissionsUtils.checkPermissions()) {
                    //通过选中的id来获取当前路径
                    val id = AllSettings.launcherProfile
                    if (id == "default") {
                        checkForLauncherProfiles()
                        return defaultPath
                    }

                    PathAndUrlManager.FILE_PROFILE_PATH.apply {
                        if (exists()) {
                            runCatching {
                                val read = Tools.read(this)
                                val jsonObject = JsonParser.parseString(read).asJsonObject
                                if (jsonObject.has(id)) {
                                    val profilePathJsonObject = Tools.GLOBAL_GSON.fromJson(jsonObject[id], ProfilePathJsonObject::class.java)
                                    checkForLauncherProfiles()
                                    return profilePathJsonObject.path
                                }
                            }.getOrElse { e -> Logging.e("Read Profile", e.toString()) }
                        }
                    }
                }

                checkForLauncherProfiles()
                return defaultPath
            }

        private fun checkForLauncherProfiles() {
            Task.runTask {
                val launcherProfiles = "launcher_profiles.json"
                val launcherProfilesFile = File(gameHome, launcherProfiles)
                if (!launcherProfilesFile.exists()) {
                    //如果这个配置文件不存在，那么久复制一份，Forge安装需要这个文件
                    Tools.copyAssetFile(ContextExecutor.getApplication(), "launcher_profiles.json", gameHome, false)
                }
            }.onThrowable { e ->
                Logging.e("Unpack Launcher Profiles", Tools.printToString(e))
            }.execute()
        }

        @JvmStatic
        fun save(items: List<ProfileItem>) {
            val jsonObject = JsonObject()

            for (item in items) {
                if (item.id == "default") continue

                val profilePathJsonObject = ProfilePathJsonObject(item.title, item.path)
                jsonObject.add(item.id, Tools.GLOBAL_GSON.toJsonTree(profilePathJsonObject))
            }

            try {
                FileWriter(PathAndUrlManager.FILE_PROFILE_PATH).use { fileWriter ->
                    Tools.GLOBAL_GSON.toJson(jsonObject, fileWriter)
                }
            } catch (e: IOException) {
                Logging.e("Write Profile", e.toString())
            }
        }
    }
}
