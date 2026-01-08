package com.paulohenriquesg.fahrenheit.update

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

object UpdateService {
    private const val TAG = "UpdateService"
    private const val APK_FILE_NAME = "fahrenheit_update.apk"
    private const val MIN_APK_SIZE = 1_000_000L // 1 MB minimum
    private const val DOWNLOAD_TIMEOUT_SECONDS = 60L
    private const val BUFFER_SIZE = 8192

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    private var currentDownloadJob: (() -> Unit)? = null

    /**
     * Download APK from the given URL to cache directory
     * @param context Application context
     * @param downloadUrl Direct URL to APK file
     * @return Result containing APK file path on success, or error message on failure
     */
    suspend fun downloadApk(context: Context, downloadUrl: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Starting APK download from: $downloadUrl")

                // Clean up old APK if exists
                cleanupOldApk(context)

                // Set downloading state
                _downloadState.value = DownloadState.Downloading(0)

                // Create OkHttp client with timeout
                val client = OkHttpClient.Builder()
                    .connectTimeout(DOWNLOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .readTimeout(DOWNLOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .writeTimeout(DOWNLOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .build()

                // Create request
                val request = Request.Builder()
                    .url(downloadUrl)
                    .build()

                // Execute download
                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    val error = "Download failed with HTTP ${response.code}"
                    Log.e(TAG, error)
                    _downloadState.value = DownloadState.Error(error)
                    return@withContext Result.failure(Exception(error))
                }

                val body = response.body ?: run {
                    val error = "Response body is null"
                    Log.e(TAG, error)
                    _downloadState.value = DownloadState.Error(error)
                    return@withContext Result.failure(Exception(error))
                }

                val contentLength = body.contentLength()
                Log.d(TAG, "Download size: ${contentLength / 1024 / 1024} MB")

                // Prepare output file
                val apkFile = File(context.cacheDir, APK_FILE_NAME)
                val outputStream = FileOutputStream(apkFile)

                // Stream download with progress tracking
                val inputStream = body.byteStream()
                val buffer = ByteArray(BUFFER_SIZE)
                var bytesRead: Long = 0
                var lastProgress = 0

                try {
                    var read: Int
                    while (inputStream.read(buffer).also { read = it } != -1) {
                        outputStream.write(buffer, 0, read)
                        bytesRead += read

                        // Calculate and emit progress
                        if (contentLength > 0) {
                            val progress = ((bytesRead * 100) / contentLength).toInt()
                            if (progress != lastProgress) {
                                _downloadState.value = DownloadState.Downloading(progress)
                                lastProgress = progress
                                Log.d(TAG, "Download progress: $progress%")
                            }
                        }
                    }
                } finally {
                    outputStream.close()
                    inputStream.close()
                }

                Log.d(TAG, "Download complete. File size: ${apkFile.length() / 1024 / 1024} MB")

                // Validate downloaded APK
                val validationResult = validateApk(context, apkFile.absolutePath)
                if (validationResult.isFailure) {
                    val error = validationResult.exceptionOrNull()?.message
                        ?: "APK validation failed"
                    Log.e(TAG, error)
                    _downloadState.value = DownloadState.Error(error)
                    apkFile.delete()
                    return@withContext Result.failure(Exception(error))
                }

                // Success!
                _downloadState.value = DownloadState.Complete(apkFile.absolutePath)
                Log.d(TAG, "APK download and validation successful")
                Result.success(apkFile.absolutePath)

            } catch (e: Exception) {
                Log.e(TAG, "Download error: ${e.message}", e)
                val error = "Download failed: ${e.message}"
                _downloadState.value = DownloadState.Error(error)
                Result.failure(e)
            }
        }

    /**
     * Validate downloaded APK file
     * @param context Application context
     * @param apkPath Path to APK file
     * @return Result indicating success or failure with error message
     */
    private fun validateApk(context: Context, apkPath: String): Result<Unit> {
        try {
            val apkFile = File(apkPath)

            // Check 1: File exists
            if (!apkFile.exists()) {
                return Result.failure(Exception("APK file does not exist"))
            }

            // Check 2: File size is reasonable (> 1 MB)
            if (apkFile.length() < MIN_APK_SIZE) {
                return Result.failure(
                    Exception("APK file too small (${apkFile.length()} bytes), possibly corrupted")
                )
            }

            // Check 3: PackageManager can parse the APK
            val packageManager = context.packageManager
            val packageInfo = packageManager.getPackageArchiveInfo(apkPath, 0)
                ?: return Result.failure(Exception("Invalid APK file"))

            // Check 4: Package name matches current app
            if (packageInfo.packageName != context.packageName) {
                return Result.failure(
                    Exception(
                        "Package name mismatch: " +
                                "expected ${context.packageName}, " +
                                "got ${packageInfo.packageName}"
                    )
                )
            }

            Log.d(TAG, "APK validation successful: ${packageInfo.packageName}")
            return Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "APK validation error: ${e.message}", e)
            return Result.failure(e)
        }
    }

    /**
     * Cancel ongoing download
     * @param context Application context
     */
    fun cancelDownload(context: Context) {
        Log.d(TAG, "Cancelling download")
        currentDownloadJob?.invoke()
        _downloadState.value = DownloadState.Cancelled
        cleanupOldApk(context)
    }

    /**
     * Reset download state to Idle
     */
    fun resetState() {
        _downloadState.value = DownloadState.Idle
    }

    /**
     * Clean up old APK file from cache
     * @param context Application context
     */
    private fun cleanupOldApk(context: Context) {
        val apkFile = File(context.cacheDir, APK_FILE_NAME)
        if (apkFile.exists()) {
            val deleted = apkFile.delete()
            Log.d(TAG, "Old APK cleanup: ${if (deleted) "success" else "failed"}")
        }
    }
}

/**
 * Sealed class representing download states
 */
sealed class DownloadState {
    object Idle : DownloadState()
    data class Downloading(val progress: Int) : DownloadState()
    data class Complete(val apkPath: String) : DownloadState()
    data class Error(val message: String) : DownloadState()
    object Cancelled : DownloadState()
}
