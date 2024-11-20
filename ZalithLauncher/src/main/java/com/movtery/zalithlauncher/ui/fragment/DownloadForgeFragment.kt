package com.movtery.zalithlauncher.ui.fragment

import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.event.sticky.SelectInstallTaskEvent
import com.movtery.zalithlauncher.feature.log.Logging
import com.movtery.zalithlauncher.feature.mod.modloader.ModVersionListAdapter
import com.movtery.zalithlauncher.task.TaskExecutors
import com.movtery.zalithlauncher.ui.subassembly.modlist.ModListFragment
import com.movtery.zalithlauncher.utils.ZHTools
import net.kdt.pojavlaunch.Tools
import com.movtery.zalithlauncher.feature.mod.modloader.ForgeDownloadTask
import com.movtery.zalithlauncher.feature.version.Addon
import com.movtery.zalithlauncher.ui.fragment.InstallGameFragment.Companion.BUNDLE_MC_VERSION
import net.kdt.pojavlaunch.modloaders.ForgeUtils
import org.greenrobot.eventbus.EventBus
import java.util.concurrent.Future
import java.util.function.Consumer

class DownloadForgeFragment : ModListFragment() {
    companion object {
        const val TAG: String = "DownloadForgeFragment"
    }

    override fun init() {
        setIcon(ContextCompat.getDrawable(fragmentActivity!!, R.drawable.ic_anvil))
        setTitleText("Forge")
        setLink("https://forums.minecraftforge.net/")
        setMCMod("https://www.mcmod.cn/class/30.html")
        setReleaseCheckBoxGone() //隐藏“仅展示正式版”选择框，在这里没有用处
        super.init()
    }

    override fun initRefresh(): Future<*> {
        return refresh(false)
    }

    override fun refresh(): Future<*> {
        return refresh(true)
    }

    private fun refresh(force: Boolean): Future<*> {
        return TaskExecutors.getDefault().submit {
            runCatching {
                TaskExecutors.runInUIThread {
                    cancelFailedToLoad()
                    componentProcessing(true)
                }
                val forgeVersions = ForgeUtils.downloadForgeVersions(force)
                processModDetails(forgeVersions)
            }.getOrElse { e ->
                TaskExecutors.runInUIThread {
                    componentProcessing(false)
                    setFailedToLoad(e.toString())
                }
                Logging.e("DownloadForge", Tools.printToString(e))
            }
        }
    }

    private fun empty() {
        TaskExecutors.runInUIThread {
            componentProcessing(false)
            setFailedToLoad(getString(R.string.version_install_no_versions))
        }
    }

    private fun processModDetails(forgeVersions: List<String>?) {
        forgeVersions ?: run {
            empty()
            return
        }

        val mcVersion = arguments?.getString(BUNDLE_MC_VERSION) ?: throw IllegalArgumentException("The Minecraft version is not passed")

        val mForgeVersions: MutableMap<String, MutableList<String>> = HashMap()
        forgeVersions.forEach(Consumer { forgeVersion: String ->
            currentTask?.apply { if (isCancelled) return@Consumer }

            //查找并分组Minecraft版本与Forge版本
            val dashIndex = forgeVersion.indexOf("-")
            val gameVersion = forgeVersion.substring(0, dashIndex)
            addIfAbsent(mForgeVersions, gameVersion, forgeVersion)
        })

        currentTask?.apply { if (isCancelled) return }

        val mcForgeVersions = mForgeVersions[mcVersion] ?: run {
            empty()
            return
        }

        val adapter = ModVersionListAdapter(R.drawable.ic_anvil, mcForgeVersions)
        adapter.setOnItemClickListener { version: Any ->
            if (isTaskRunning()) return@setOnItemClickListener false

            val versionString = version.toString()
            EventBus.getDefault().postSticky(
                SelectInstallTaskEvent(
                    Addon.FORGE,
                    versionString,
                    ForgeDownloadTask(versionString)
                )
            )

            ZHTools.onBackPressed(requireActivity())
            true
        }

        currentTask?.apply { if (isCancelled) return }

        TaskExecutors.runInUIThread {
            val recyclerView = recyclerView
            runCatching {
                recyclerView.layoutManager = LinearLayoutManager(fragmentActivity!!)
                recyclerView.adapter = adapter
            }.getOrElse { e ->
                Logging.e("Set Adapter", Tools.printToString(e))
            }

            componentProcessing(false)
            recyclerView.scheduleLayoutAnimation()
        }
    }
}
