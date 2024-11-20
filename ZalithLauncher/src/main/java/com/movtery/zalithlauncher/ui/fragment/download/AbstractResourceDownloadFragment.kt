package com.movtery.zalithlauncher.ui.fragment.download

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.animation.LayoutAnimationController
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import com.movtery.anim.AnimPlayer
import com.movtery.anim.animations.Animations
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.databinding.FragmentDownloadResourceBinding
import com.movtery.zalithlauncher.event.value.DownloadPageSwapEvent
import com.movtery.zalithlauncher.event.value.DownloadRecyclerEnableEvent
import com.movtery.zalithlauncher.feature.download.Filters
import com.movtery.zalithlauncher.feature.download.InfoAdapter
import com.movtery.zalithlauncher.feature.download.SelfReferencingFuture
import com.movtery.zalithlauncher.feature.download.enums.Category
import com.movtery.zalithlauncher.feature.download.enums.Classify
import com.movtery.zalithlauncher.feature.download.enums.ModLoader
import com.movtery.zalithlauncher.feature.download.enums.Platform
import com.movtery.zalithlauncher.feature.download.enums.Sort
import com.movtery.zalithlauncher.feature.download.item.InfoItem
import com.movtery.zalithlauncher.feature.download.item.SearchResult
import com.movtery.zalithlauncher.feature.download.platform.PlatformNotSupportedException
import com.movtery.zalithlauncher.feature.log.Logging
import com.movtery.zalithlauncher.task.TaskExecutors
import com.movtery.zalithlauncher.ui.dialog.SelectVersionDialog
import com.movtery.zalithlauncher.ui.fragment.FragmentWithAnim
import com.movtery.zalithlauncher.ui.subassembly.adapter.ObjectSpinnerAdapter
import com.movtery.zalithlauncher.ui.subassembly.versionlist.VersionSelectedListener
import com.movtery.zalithlauncher.utils.ZHTools
import com.movtery.zalithlauncher.utils.anim.AnimUtils.Companion.setVisibilityAnim
import com.skydoves.powerspinner.PowerSpinnerView
import net.kdt.pojavlaunch.Tools
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import java.util.concurrent.Future

abstract class AbstractResourceDownloadFragment(
    parentFragment: Fragment?,
    private val classify: Classify,
    private val categoryList: List<Category>,
    private val showModloader: Boolean,
    private val recommendedPlatform: Platform = Platform.CURSEFORGE
) : FragmentWithAnim(R.layout.fragment_download_resource) {
    private lateinit var binding: FragmentDownloadResourceBinding

    private lateinit var mPlatformAdapter: ObjectSpinnerAdapter<Platform>
    private lateinit var mSortAdapter: ObjectSpinnerAdapter<Sort>
    private lateinit var mCategoryAdapter: ObjectSpinnerAdapter<Category>
    private lateinit var mModLoaderAdapter: ObjectSpinnerAdapter<ModLoader>
    private var mCurrentPlatform: Platform = Platform.CURSEFORGE
    private val mFilters: Filters = Filters()

    private val mInfoAdapter = InfoAdapter(parentFragment,
        object : InfoAdapter.CallSearchListener {
            override fun isLastPage() = mLastPage

            override fun loadMoreResult() {
                mTaskInProgress?.let { return }
                mTaskInProgress = SelfReferencingFuture(SearchApiTask(mCurrentResult))
                    .startOnExecutor(TaskExecutors.getDefault())
            }
        })

    private var mTaskInProgress: Future<*>? = null
    private var mCurrentResult: SearchResult? = null
    protected var mLastPage = false

    abstract fun initInstallButton(installButton: Button)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDownloadResourceBinding.inflate(layoutInflater)

        mPlatformAdapter = ObjectSpinnerAdapter(binding.platformSpinner) { platform -> platform.pName }
        mSortAdapter = ObjectSpinnerAdapter(binding.sortSpinner) { sort -> getString(sort.resNameID) }
        mCategoryAdapter = ObjectSpinnerAdapter(binding.categorySpinner) { category -> getString(category.resNameID) }
        mModLoaderAdapter = ObjectSpinnerAdapter(binding.modloaderSpinner) { modloader ->
            if (modloader == ModLoader.ALL) getString(R.string.generic_all)
            else modloader.loaderName
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.apply {
            recyclerView.apply {
                layoutManager = LinearLayoutManager(requireContext())
                layoutAnimation = LayoutAnimationController(
                    AnimationUtils.loadAnimation(requireContext(), R.anim.fade_downwards)
                )
                addOnScrollListener(object : OnScrollListener() {
                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                        val lm = layoutManager as LinearLayoutManager
                        val lastPosition = lm.findLastVisibleItemPosition()
                        setVisibilityAnim(backToTop, lastPosition >= 12)
                    }
                })
                adapter = mInfoAdapter
            }

            backToTop.setOnClickListener { recyclerView.smoothScrollToPosition(0) }

            searchView.setOnClickListener { search() }
            nameEdit.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                }

                override fun afterTextChanged(s: Editable?) {
                    mFilters.name = s?.toString() ?: ""
                }
            })
            nameEdit.setOnEditorActionListener { _, _, _ ->
                search()
                nameEdit.clearFocus()
                false
            }

            // 打开版本选择弹窗
            mcVersionButton.setOnClickListener {
                val selectVersionDialog = SelectVersionDialog(requireContext())
                selectVersionDialog.setOnVersionSelectedListener(object : VersionSelectedListener() {
                    override fun onVersionSelected(version: String?) {
                        selectedMcVersionView.text = version
                        mFilters.mcVersion = version
                        selectVersionDialog.dismiss()
                    }
                })
                selectVersionDialog.show()
            }
        }

        // 初始化 Spinner
        mPlatformAdapter.setItems(Platform.entries)
        mSortAdapter.setItems(Sort.entries)
        mCategoryAdapter.setItems(categoryList)
        mModLoaderAdapter.setItems(ModLoader.entries)

        binding.apply {
            initInstallButton(binding.installButton)

            platformSpinner.setSpinnerAdapter(mPlatformAdapter)
            setSpinnerListener<Platform>(platformSpinner) {
                if (mCurrentPlatform == it) return@setSpinnerListener
                mCurrentPlatform = it
                search()
            }
            platformSpinner.selectItemByIndex(recommendedPlatform.ordinal)

            sortSpinner.setSpinnerAdapter(mSortAdapter)
            setSpinnerListener<Sort>(sortSpinner) { mFilters.sort = it }
            sortSpinner.selectItemByIndex(0)

            categorySpinner.setSpinnerAdapter(mCategoryAdapter)
            setSpinnerListener<Category>(binding.categorySpinner) { mFilters.category = it }
            categorySpinner.selectItemByIndex(0)

            modloaderSpinner.setSpinnerAdapter(mModLoaderAdapter)
            setSpinnerListener<ModLoader>(modloaderSpinner) {
                mFilters.modloader = it.takeIf { loader -> loader != ModLoader.ALL }
            }
            modloaderSpinner.selectItemByIndex(0)

            reset.setOnClickListener {
                nameEdit.setText("")
                platformSpinner.selectItemByIndex(0)
                sortSpinner.selectItemByIndex(0)
                categorySpinner.selectItemByIndex(0)
                modloaderSpinner.selectItemByIndex(0)
                binding.selectedMcVersionView.text = null
                mFilters.mcVersion = null
            }

            returnButton.setOnClickListener { ZHTools.onBackPressed(requireActivity()) }
        }

        showModLoader()
        checkSearch()
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }

    override fun onPause() {
        closeSpinner()
        super.onPause()
    }

    private fun onSearchFinished() {
        binding.apply {
            setStatusText(false)
            setLoadingLayout(false)
            setRecyclerView(true)
        }
    }

    private fun onSearchError(error: Int) {
        binding.apply {
            statusText.text = when (error) {
                ERROR_INTERNAL -> getString(R.string.download_search_failed)
                ERROR_PLATFORM_NOT_SUPPORTED -> getString(R.string.download_search_platform_not_supported)
                else -> getString(R.string.download_search_no_result)
            }
        }
        setLoadingLayout(false)
        setRecyclerView(false)
        setStatusText(true)
    }

    override fun slideIn(animPlayer: AnimPlayer) {
        binding.apply {
            animPlayer.apply(AnimPlayer.Entry(operateLayout, Animations.BounceInLeft))
                .apply(AnimPlayer.Entry(downloadLayout, Animations.BounceInDown))
        }
    }

    override fun slideOut(animPlayer: AnimPlayer) {
        binding.apply {
            animPlayer.apply(AnimPlayer.Entry(operateLayout, Animations.FadeOutRight))
                .apply(AnimPlayer.Entry(downloadLayout, Animations.FadeOutUp))
        }
    }

    private fun setStatusText(shouldShow: Boolean) {
        setVisibilityAnim(binding.statusText, shouldShow)
    }

    private fun setLoadingLayout(shouldShow: Boolean) {
        setVisibilityAnim(binding.loadingLayout, shouldShow)
    }

    private fun setRecyclerView(shouldShow: Boolean) {
        binding.apply {
            recyclerView.visibility = if (shouldShow) View.VISIBLE else View.GONE
            if (shouldShow) recyclerView.scheduleLayoutAnimation()
        }
    }

    private fun <E> setSpinnerListener(spinnerView: PowerSpinnerView, func: (E) -> Unit) {
        spinnerView.setOnSpinnerItemSelectedListener<E> { _, _, _, newItem -> func(newItem) }
    }

    private fun closeSpinner() {
        binding.platformSpinner.dismiss()
        binding.sortSpinner.dismiss()
        binding.categorySpinner.dismiss()
        binding.modloaderSpinner.dismiss()
    }

    private fun showModLoader() {
        binding.apply {
            modloaderLayout.visibility = if (showModloader) View.VISIBLE else View.GONE
            if (showModloader) {
                modloaderSpinner.setSpinnerAdapter(mModLoaderAdapter)
                modloaderSpinner.selectItemByIndex(0)
            } else {
                mFilters.modloader = null
            }
        }
    }

    /**
     * 清除上一次的搜索状态，然后执行搜索
     */
    private fun search() {
        setStatusText(false)
        setRecyclerView(false)
        setLoadingLayout(true)
        binding.recyclerView.scrollToPosition(0)

        if (mTaskInProgress != null) {
            mTaskInProgress!!.cancel(true)
            mTaskInProgress = null
        }
        this.mLastPage = false
        mTaskInProgress = SelfReferencingFuture(SearchApiTask(null))
            .startOnExecutor(TaskExecutors.getDefault())
    }

    /**
     * 检查当前适配器内的item数量是否为0，如果是，那么执行搜索
     */
    private fun checkSearch() {
        if (mInfoAdapter.itemCount == 0) search()
    }

    @Subscribe
    fun event(event: DownloadRecyclerEnableEvent) {
        binding.recyclerView.isEnabled = event.enable
        closeSpinner()
    }

    @Subscribe
    fun event(event: DownloadPageSwapEvent) {
        if (event.index == classify.type) slideIn()
    }

    private inner class SearchApiTask(
        private val mPreviousResult: SearchResult?
    ) :
        SelfReferencingFuture.FutureInterface {

        override fun run(myFuture: Future<*>) {
            runCatching {
                val result: SearchResult? = mCurrentPlatform.helper.search(classify, mFilters, mPreviousResult ?: SearchResult())

                TaskExecutors.runInUIThread {
                    if (myFuture.isCancelled) return@runInUIThread
                    mTaskInProgress = null

                    when {
                        result == null -> {
                            onSearchError(ERROR_INTERNAL)
                        }
                        result.isLastPage -> {
                            if (result.infoItems.isEmpty()) {
                                onSearchError(ERROR_NO_RESULTS)
                            } else {
                                mLastPage = true
                                mInfoAdapter.setItems(result.infoItems)
                                onSearchFinished()
                                return@runInUIThread
                            }
                        }
                        else -> {
                            onSearchFinished()
                        }
                    }

                    if (result == null) {
                        mInfoAdapter.setItems(MOD_ITEMS_EMPTY)
                        return@runInUIThread
                    } else {
                        mInfoAdapter.setItems(result.infoItems)
                        mCurrentResult = result
                    }
                }
            }.getOrElse { e ->
                TaskExecutors.runInUIThread {
                    mInfoAdapter.setItems(MOD_ITEMS_EMPTY)
                    Logging.e("SearchTask", Tools.printToString(e))
                    if (e is PlatformNotSupportedException) {
                        onSearchError(ERROR_PLATFORM_NOT_SUPPORTED)
                    } else {
                        onSearchError(ERROR_NO_RESULTS)
                    }
                }
            }
        }
    }

    companion object {
        private val MOD_ITEMS_EMPTY: MutableList<InfoItem> = ArrayList()

        const val ERROR_INTERNAL: Int = 0
        const val ERROR_NO_RESULTS: Int = 1
        const val ERROR_PLATFORM_NOT_SUPPORTED: Int = 2
    }
}