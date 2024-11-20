package com.movtery.zalithlauncher.feature.version

import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.movtery.zalithlauncher.R
import org.apache.commons.io.FileUtils

/**
 * 用于自动设置版本的图标，或者重置对应版本的自定义图标
 */
class VersionIconUtils(
    private val version: Version
) {
    private val iconFile = VersionsManager.getVersionIconFile(version)

    /**
     * 通过版本来识别其默认的图标，比如原版、模组加载器封面图，如果有自定义图标，那么会优先设置自定义图标
     * @return 返回是否设置为了自定义图标，便于使用重置图标的操作
     */
    fun start(imageView: ImageView): Boolean {
        val context = imageView.context

        var isIconSet = false
        var isCustomIcon = false

        iconFile.let { icon ->
            if (icon.exists()) {
                Glide.with(imageView)
                    .load(icon)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .into(imageView)
                isIconSet = true
                isCustomIcon = true
            }
        }

        version.getVersionInfo()?.let { versionInfo ->
            versionInfo.loaderInfo.forEach { loaderInfo ->
                if (!isIconSet) {
                    getLoaderIcon(loaderInfo.name)?.let { icon ->
                        imageView.setImageDrawable(ContextCompat.getDrawable(context, icon))
                        isIconSet = true
                    }
                } else return@forEach
            }
        }

        if (!isIconSet) imageView.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_minecraft))

        return isCustomIcon
    }

    /**
     * 通过删除自定义图标文件，来达成重置的目的
     * **这个操作不可逆**
     */
    fun resetIcon() {
        FileUtils.deleteQuietly(iconFile)
    }

    private fun getLoaderIcon(name: String): Int? {
        return if (name.equals("fabric", true)) R.drawable.ic_fabric
        else if (name.equals("forge", true)) R.drawable.ic_anvil
        else if (name.equals("quilt", true)) R.drawable.ic_quilt
        else if (name.equals("neoforge", true)) R.drawable.ic_neoforge
        else if (name.equals("optifine", true)) R.drawable.ic_optifine
        else null
    }
}