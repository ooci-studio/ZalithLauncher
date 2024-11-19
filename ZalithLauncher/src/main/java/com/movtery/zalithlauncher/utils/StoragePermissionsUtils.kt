package com.movtery.zalithlauncher.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.ui.dialog.TipDialog

class StoragePermissionsUtils {
    companion object {
        private const val REQUEST_CODE_PERMISSIONS: Int = 0

        /**
         * 检查存储权限，返回是否拥有存储权限
         */
        fun checkPermissions(activity: Activity): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                checkPermissionsForAndroid11AndAbove()
            } else {
                checkPermissions(activity)
            }
        }

        /**
         * 检查存储权限，如果没有存储权限，则弹出弹窗向用户申请
         */
        fun checkPermissions(activity: Activity, title: Int = R.string.generic_warning, permissionGranted: PermissionGranted?) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                handlePermissionsForAndroid11AndAbove(activity, title, permissionGranted)
            } else {
                handlePermissionsForAndroid10AndBelow(activity, title, permissionGranted)
            }
        }

        @RequiresApi(api = Build.VERSION_CODES.R)
        private fun checkPermissionsForAndroid11AndAbove() = Environment.isExternalStorageManager()

        @RequiresApi(api = Build.VERSION_CODES.R)
        private fun handlePermissionsForAndroid11AndAbove(activity: Activity, title: Int, permissionGranted: PermissionGranted?) {
            if (!checkPermissionsForAndroid11AndAbove()) {
                showPermissionRequestDialog(activity, title) {
                    val intent =
                        Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.setData(Uri.parse("package:" + activity.packageName))
                    activity.startActivityForResult(intent, REQUEST_CODE_PERMISSIONS)
                }
            } else {
                permissionGranted?.granted()
            }
        }

        private fun handlePermissionsForAndroid10AndBelow(activity: Activity, title: Int, permissionGranted: PermissionGranted?) {
            if (!hasStoragePermissions(activity)) {
                showPermissionRequestDialog(activity, title) {
                    ActivityCompat.requestPermissions(
                        activity, arrayOf(
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                        ), REQUEST_CODE_PERMISSIONS
                    )
                }
            } else {
                permissionGranted?.granted()
            }
        }

        private fun hasStoragePermissions(context: Context): Boolean {
            return ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }

        private fun showPermissionRequestDialog(context: Context, title: Int, requestPermissions: RequestPermissions) {
            TipDialog.Builder(context)
                .setTitle(title)
                .setMessage(context.getString(R.string.permissions_manage_external_storage))
                .setConfirmClickListener { requestPermissions.onRequest() }
                .setCancelable(false)
                .buildDialog()
        }
    }

    fun interface RequestPermissions {
        fun onRequest()
    }

    fun interface PermissionGranted {
        fun granted()
    }
}