package com.paulohenriquesg.fahrenheit.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File

object InstallationHelper {
    private const val TAG = "InstallationHelper"

    /**
     * Install APK using Android's Package Installer
     * @param context Application context
     * @param apkPath Absolute path to APK file
     * @return Result indicating success or failure with error message
     */
    fun installApk(context: Context, apkPath: String): Result<Unit> {
        try {
            val apkFile = File(apkPath)

            // Verify APK file exists
            if (!apkFile.exists()) {
                val error = "APK file does not exist: $apkPath"
                Log.e(TAG, error)
                return Result.failure(Exception(error))
            }

            // Check if app can install packages (Android 8.0+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!canInstallPackages(context)) {
                    val error = "App does not have permission to install packages"
                    Log.e(TAG, error)
                    return Result.failure(Exception(error))
                }
            }

            // Generate FileProvider URI
            val apkUri = getApkUri(context, apkFile)
            Log.d(TAG, "Generated APK URI: $apkUri")

            // Create installation intent
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            // Launch Package Installer
            context.startActivity(intent)
            Log.d(TAG, "Package Installer launched successfully")

            return Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Installation error: ${e.message}", e)
            return Result.failure(e)
        }
    }

    /**
     * Generate FileProvider URI for APK file
     * @param context Application context
     * @param apkFile APK file
     * @return Content URI for the APK file
     */
    private fun getApkUri(context: Context, apkFile: File): Uri {
        val authority = "${context.packageName}.update_provider"
        return FileProvider.getUriForFile(context, authority, apkFile)
    }

    /**
     * Check if the app can install packages
     * @param context Application context
     * @return true if permission granted, false otherwise
     */
    fun canInstallPackages(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            // Permission not required for Android 7.1 and below
            true
        }
    }

    /**
     * Open system settings to grant install permission
     * @param context Application context
     */
    fun openInstallPermissionSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                Log.d(TAG, "Opened install permission settings")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to open install permission settings: ${e.message}", e)
            }
        }
    }
}
