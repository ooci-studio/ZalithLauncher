package com.movtery.zalithlauncher.utils

import android.content.Context
import android.os.Environment
import com.movtery.zalithlauncher.BuildConfig
import net.kdt.pojavlaunch.Tools
import okhttp3.Request
import okhttp3.RequestBody
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLConnection

class PathAndUrlManager {
    companion object {
        private const val TIME_OUT = 8000
        private const val URL_USER_AGENT: String = "ZalithLauncher/${BuildConfig.VERSION_NAME}"
        const val URL_GITHUB_UPDATE: String = "https://api.github.com/repos/MovTery/ZalithLauncher/contents/versions.json"
        const val URL_GITHUB_HOME: String = "https://api.github.com/repos/MovTery/Zalith-Info/contents/"
        const val URL_MCMOD: String = "https://www.mcmod.cn/"
        const val URL_MINECRAFT: String = "https://www.minecraft.net/"
        const val URL_SUPPORT: String = "https://afdian.com/a/MovTery"
        const val URL_HOME: String = "https://github.com/MovTery/ZalithLauncher"

        lateinit var DIR_NATIVE_LIB: String
        lateinit var DIR_FILE: File
        lateinit var DIR_DATA: String //Initialized later to get context
        lateinit var DIR_CACHE: File
        lateinit var DIR_MULTIRT_HOME: String
        @JvmField var DIR_GAME_HOME: String = Environment.getExternalStorageDirectory().absolutePath + "/games/ZalithLauncher"
        lateinit var DIR_LAUNCHER_LOG: String
        lateinit var DIR_CTRLMAP_PATH: String
        lateinit var DIR_ACCOUNT_NEW: String
        lateinit var DIR_CACHE_STRING: String

        lateinit var DIR_CUSTOM_MOUSE: String
        lateinit var DIR_BACKGROUND: File
        lateinit var DIR_APP_CACHE: File
        lateinit var DIR_USER_SKIN: File

        lateinit var FILE_SETTINGS: File
        lateinit var FILE_PROFILE_PATH: File
        lateinit var FILE_CTRLDEF_FILE: String
        lateinit var FILE_VERSION_LIST: String
        lateinit var FILE_NEWBIE_GUIDE: File

        @JvmStatic
        fun initContextConstants(context: Context) {
            DIR_NATIVE_LIB = context.applicationInfo.nativeLibraryDir
            DIR_FILE = context.filesDir
            DIR_DATA = DIR_FILE.getParent()!!
            DIR_CACHE = context.cacheDir
            DIR_MULTIRT_HOME = "$DIR_DATA/runtimes"
            DIR_GAME_HOME = Tools.getPojavStorageRoot(context).absolutePath
            DIR_LAUNCHER_LOG = "$DIR_GAME_HOME/launcher_log"
            DIR_CTRLMAP_PATH = "$DIR_GAME_HOME/controlmap"
            DIR_ACCOUNT_NEW = "$DIR_FILE/accounts"
            DIR_CACHE_STRING = "$DIR_CACHE/string_cache"

            FILE_PROFILE_PATH = File(DIR_DATA, "/profile_path.json")
            FILE_CTRLDEF_FILE = "$DIR_GAME_HOME/controlmap/default.json"
            FILE_VERSION_LIST = "$DIR_DATA/version_list.json"
            FILE_NEWBIE_GUIDE = File(DIR_DATA, "/newbie_guide.json")

            FILE_SETTINGS = File(DIR_FILE, "/launcher_settings.json")
            DIR_CUSTOM_MOUSE = "$DIR_GAME_HOME/mouse"
            DIR_BACKGROUND = File("$DIR_GAME_HOME/background")
            DIR_APP_CACHE = context.externalCacheDir!!
            DIR_USER_SKIN = File(DIR_FILE, "/user_skin")

            runCatching {
                //此处的账号文件已不再使用，需要检查并清除
                FileUtils.deleteQuietly(File("$DIR_DATA/accounts"))
                FileUtils.deleteQuietly(File(DIR_DATA, "/user_skin"))
            }
        }

        @JvmStatic
        fun createConnection(url: URL): URLConnection {
            val connection = url.openConnection()
            connection.setRequestProperty("User-Agent", URL_USER_AGENT)
            connection.setConnectTimeout(TIME_OUT)
            connection.setReadTimeout(TIME_OUT)

            return connection
        }

        @JvmStatic
        @Throws(IOException::class)
        fun createHttpConnection(url: URL): HttpURLConnection {
            return createConnection(url) as HttpURLConnection
        }

        @JvmStatic
        fun createRequestBuilder(url: String): Request.Builder {
            return createRequestBuilder(url, null)
        }

        @JvmStatic
        fun createRequestBuilder(url: String, body: RequestBody?): Request.Builder {
            val request = Request.Builder().url(url).header("User-Agent", URL_USER_AGENT)
            body?.let{ request.post(it) }

            return request
        }
    }
}