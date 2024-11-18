package net.kdt.pojavlaunch.lifecycle;

import static net.kdt.pojavlaunch.MainActivity.INTENT_MINECRAFT_VERSION;
import static net.kdt.pojavlaunch.MainActivity.INTENT_VERSION;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import com.movtery.zalithlauncher.R;
import com.movtery.zalithlauncher.context.ContextExecutor;
import com.movtery.zalithlauncher.feature.version.Version;
import com.movtery.zalithlauncher.setting.AllSettings;

import net.kdt.pojavlaunch.MainActivity;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.progresskeeper.ProgressKeeper;
import net.kdt.pojavlaunch.tasks.AsyncMinecraftDownloader;
import net.kdt.pojavlaunch.utils.NotificationUtils;

public class ContextAwareDoneListener implements AsyncMinecraftDownloader.DoneListener, ContextExecutorTask {
    private final String mErrorString;
    private final String mVersionName;
    private final Version mVersion;

    public ContextAwareDoneListener(Context baseContext, String versionName, Version version) {
        this.mErrorString = baseContext.getString(R.string.mc_download_failed);
        this.mVersionName = versionName;
        this.mVersion = version;
    }

    private Intent createGameStartIntent(Context context) {
        Intent mainIntent = new Intent(context, MainActivity.class);
        mainIntent.putExtra(INTENT_MINECRAFT_VERSION, mVersionName);
        mainIntent.putExtra(INTENT_VERSION, mVersion);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return mainIntent;
    }

    @Override
    public void onDownloadDone() {
        ProgressKeeper.waitUntilDone(() -> ContextExecutor.executeTask(this));
    }

    @Override
    public void onDownloadFailed(Throwable throwable) {
        Tools.showErrorRemote(mErrorString, throwable);
    }

    @Override
    public void executeWithActivity(Activity activity) {
        try {
            Intent gameStartIntent = createGameStartIntent(activity);
            activity.startActivity(gameStartIntent);
            if (AllSettings.getQuitLauncher()) {
                activity.finish();
                android.os.Process.killProcess(android.os.Process.myPid()); //You should kill yourself, NOW!
            }
        } catch (Throwable e) {
            Tools.showError(activity.getBaseContext(), e);
        }
    }

    @Override
    public void executeWithApplication(Context context) {
        Intent gameStartIntent = createGameStartIntent(context);
        // Since the game is a separate process anyway, it does not matter if it gets invoked
        // from somewhere other than the launcher activity.
        // The only problem may arise if the launcher starts doing something when the user starts the notification.
        // So, the notification is automatically removed once there are tasks ongoing in the ProgressKeeper
        NotificationUtils.sendBasicNotification(context,
                R.string.notif_download_finished,
                R.string.notif_download_finished_desc,
                gameStartIntent,
                NotificationUtils.PENDINGINTENT_CODE_GAME_START,
                NotificationUtils.NOTIFICATION_ID_GAME_START
        );
        // You should keep yourself safe, NOW!
        // otherwise android does weird things...
    }
}
