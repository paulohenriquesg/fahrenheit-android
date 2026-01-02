package com.paulohenriquesg.fahrenheit.api

import com.google.gson.annotations.SerializedName

data class LoginResponse(
    @SerializedName("user") val user: User,
    @SerializedName("userDefaultLibraryId") val userDefaultLibraryId: String,
    @SerializedName("serverSettings") val serverSettings: ServerSettings,
    @SerializedName("Source") val source: String
)

data class User(
    @SerializedName("id") val id: String,
    @SerializedName("username") val username: String,
    @SerializedName("type") val type: String,
    @SerializedName("token") val token: String,
    @SerializedName("mediaProgress") val mediaProgress: List<MediaProgressResponse>,
    @SerializedName("seriesHideFromContinueListening") val seriesHideFromContinueListening: List<Any>,
    @SerializedName("bookmarks") val bookmarks: List<Any>,
    @SerializedName("isActive") val isActive: Boolean,
    @SerializedName("isLocked") val isLocked: Boolean,
    @SerializedName("lastSeen") val lastSeen: Long,
    @SerializedName("createdAt") val createdAt: Long,
    @SerializedName("permissions") val permissions: Permissions,
    @SerializedName("librariesAccessible") val librariesAccessible: List<Any>,
    @SerializedName("itemTagsAccessible") val itemTagsAccessible: List<Any>
)

data class Permissions(
    @SerializedName("download") val download: Boolean,
    @SerializedName("update") val update: Boolean,
    @SerializedName("delete") val delete: Boolean,
    @SerializedName("upload") val upload: Boolean,
    @SerializedName("accessAllLibraries") val accessAllLibraries: Boolean,
    @SerializedName("accessAllTags") val accessAllTags: Boolean,
    @SerializedName("accessExplicitContent") val accessExplicitContent: Boolean
)

data class ServerSettings(
    @SerializedName("id") val id: String,
    @SerializedName("scannerFindCovers") val scannerFindCovers: Boolean,
    @SerializedName("scannerCoverProvider") val scannerCoverProvider: String,
    @SerializedName("scannerParseSubtitle") val scannerParseSubtitle: Boolean,
    @SerializedName("scannerPreferMatchedMetadata") val scannerPreferMatchedMetadata: Boolean,
    @SerializedName("scannerDisableWatcher") val scannerDisableWatcher: Boolean,
    @SerializedName("storeCoverWithItem") val storeCoverWithItem: Boolean,
    @SerializedName("storeMetadataWithItem") val storeMetadataWithItem: Boolean,
    @SerializedName("metadataFileFormat") val metadataFileFormat: String,
    @SerializedName("rateLimitLoginRequests") val rateLimitLoginRequests: Int,
    @SerializedName("rateLimitLoginWindow") val rateLimitLoginWindow: Long,
    @SerializedName("backupSchedule") val backupSchedule: String,
    @SerializedName("backupsToKeep") val backupsToKeep: Int,
    @SerializedName("maxBackupSize") val maxBackupSize: Int,
    @SerializedName("loggerDailyLogsToKeep") val loggerDailyLogsToKeep: Int,
    @SerializedName("loggerScannerLogsToKeep") val loggerScannerLogsToKeep: Int,
    @SerializedName("homeBookshelfView") val homeBookshelfView: Int,
    @SerializedName("bookshelfView") val bookshelfView: Int,
    @SerializedName("sortingIgnorePrefix") val sortingIgnorePrefix: Boolean,
    @SerializedName("sortingPrefixes") val sortingPrefixes: List<String>,
    @SerializedName("chromecastEnabled") val chromecastEnabled: Boolean,
    @SerializedName("dateFormat") val dateFormat: String,
    @SerializedName("language") val language: String,
    @SerializedName("logLevel") val logLevel: Int,
    @SerializedName("version") val version: String
)