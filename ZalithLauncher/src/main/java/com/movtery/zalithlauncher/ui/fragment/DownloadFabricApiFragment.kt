package com.movtery.zalithlauncher.ui.fragment

import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.feature.version.Addon

class DownloadFabricApiFragment: DownloadFabricLikeApiModFragment(
    Addon.FABRIC_API,
    "P7dR8mSH",
    "https://modrinth.com/mod/fabric-api",
    "https://www.mcmod.cn/class/3124.html",
    R.drawable.ic_fabric
) {
    companion object {
        const val TAG: String = "DownloadFabricApiFragment"
    }
}