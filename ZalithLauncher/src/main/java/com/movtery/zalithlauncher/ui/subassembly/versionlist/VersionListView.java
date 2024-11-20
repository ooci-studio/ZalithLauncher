package com.movtery.zalithlauncher.ui.subassembly.versionlist;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.movtery.zalithlauncher.R;
import com.movtery.zalithlauncher.event.sticky.MinecraftVersionValueEvent;
import com.movtery.zalithlauncher.task.TaskExecutors;
import com.movtery.zalithlauncher.ui.subassembly.filelist.FileItemBean;
import com.movtery.zalithlauncher.ui.subassembly.filelist.FileRecyclerViewCreator;

import net.kdt.pojavlaunch.JMinecraftVersionList;
import net.kdt.pojavlaunch.utils.FilteredSubList;

import org.greenrobot.eventbus.EventBus;

import java.util.List;

public class VersionListView extends LinearLayout {
    private Context context;
    private List<JMinecraftVersionList.Version> releaseList, snapshotList, betaList, alphaList;
    private FileRecyclerViewCreator fileRecyclerViewCreator;
    private VersionSelectedListener versionSelectedListener;

    public VersionListView(Context context) {
        this(context, null);
    }

    public VersionListView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VersionListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }


    @SuppressLint("UseCompatLoadingForDrawables")
    private void init(Context context) {
        this.context = context;

        LayoutParams layParam = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        setOrientation(VERTICAL);

        RecyclerView mainListView = new RecyclerView(context);

        JMinecraftVersionList.Version[] versionArray;
        MinecraftVersionValueEvent event = EventBus.getDefault().getStickyEvent(MinecraftVersionValueEvent.class);

        if (event != null) {
            JMinecraftVersionList jMinecraftVersionList = event.getList();
            boolean isVersionsNotNull = jMinecraftVersionList != null && jMinecraftVersionList.versions != null;
            versionArray = isVersionsNotNull ? jMinecraftVersionList.versions : new JMinecraftVersionList.Version[0];
        } else {
            versionArray = new JMinecraftVersionList.Version[0];
        }

        releaseList = new FilteredSubList<>(versionArray, item -> item.type.equals("release"));
        snapshotList = new FilteredSubList<>(versionArray, item -> item.type.equals("snapshot"));
        betaList = new FilteredSubList<>(versionArray, item -> item.type.equals("old_beta"));
        alphaList = new FilteredSubList<>(versionArray, item -> item.type.equals("old_alpha"));

        fileRecyclerViewCreator = new FileRecyclerViewCreator(
                context,
                mainListView,
                (position, fileItemBean) -> versionSelectedListener.onVersionSelected(fileItemBean.name),
                null,
                showVersions(VersionType.RELEASE)
        );

        addView(mainListView, layParam);
    }

    private String[] getVersionIds(List<JMinecraftVersionList.Version> versions) {
        String[] strings = new String[versions.size()];
        for (int i = 0; i < versions.size(); i++) {
            strings[i] = versions.get(i).id;
        }
        return strings;
    }

    public void setVersionSelectedListener(VersionSelectedListener versionSelectedListener) {
        this.versionSelectedListener = versionSelectedListener;
    }

    public void setVersionType(VersionType versionType) {
        showVersions(versionType);
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private List<FileItemBean> showVersions(VersionType versionType) {
        switch (versionType) {
            case SNAPSHOT:
                return getVersion(context.getDrawable(R.drawable.ic_command_block), getVersionIds(snapshotList));
            case BETA:
                return getVersion(context.getDrawable(R.drawable.ic_old_cobblestone), getVersionIds(betaList));
            case ALPHA:
                return getVersion(context.getDrawable(R.drawable.ic_old_grass_block), getVersionIds(alphaList));
            case RELEASE:
            default:
                return getVersion(context.getDrawable(R.drawable.ic_minecraft), getVersionIds(releaseList));
        }
    }

    private List<FileItemBean> getVersion(Drawable icon, String[] names) {
        List<FileItemBean> itemBeans = FileRecyclerViewCreator.loadItemBean(icon, names);
        TaskExecutors.runInUIThread(() -> fileRecyclerViewCreator.loadData(itemBeans));
        return itemBeans;
    }
}
