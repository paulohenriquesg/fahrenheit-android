package com.paulohenriquesg.fahrenheit.api

import com.google.gson.annotations.SerializedName

data class PlayLibraryItemResponse(
    @SerializedName("id") val id: String,
    @SerializedName("userId") val userId: String,
    @SerializedName("libraryId") val libraryId: String,
    @SerializedName("libraryItemId") val libraryItemId: String,
    @SerializedName("episodeId") val episodeId: String?,
    @SerializedName("mediaType") val mediaType: String,
    @SerializedName("mediaMetadata") val mediaMetadata: PlayLibraryItemMediaMetadata,
    @SerializedName("chapters") val chapters: List<Any>,
    @SerializedName("displayTitle") val displayTitle: String,
    @SerializedName("displayAuthor") val displayAuthor: String,
    @SerializedName("coverPath") val coverPath: String,
    @SerializedName("duration") val duration: Double,
    @SerializedName("playMethod") val playMethod: Int,
    @SerializedName("mediaPlayer") val mediaPlayer: String,
    @SerializedName("deviceInfo") val deviceInfo: PlayLibraryItemDeviceInfo,
    @SerializedName("date") val date: String,
    @SerializedName("dayOfWeek") val dayOfWeek: String,
    @SerializedName("timeListening") val timeListening: Int,
    @SerializedName("startTime") val startTime: Int,
    @SerializedName("currentTime") val currentTime: Int,
    @SerializedName("startedAt") val startedAt: Long,
    @SerializedName("updatedAt") val updatedAt: Long,
    @SerializedName("audioTracks") val audioTracks: List<PlayLibraryItemAudioTrack>,
    @SerializedName("videoTrack") val videoTrack: Any?,
    @SerializedName("libraryItem") val libraryItem: PlayLibraryItemLibraryItem
)

data class PlayLibraryItemMediaMetadata(
    @SerializedName("title") val title: String,
    @SerializedName("author") val author: String,
    @SerializedName("description") val description: String,
    @SerializedName("releaseDate") val releaseDate: String,
    @SerializedName("genres") val genres: List<String>,
    @SerializedName("feedUrl") val feedUrl: String,
    @SerializedName("imageUrl") val imageUrl: String,
    @SerializedName("itunesPageUrl") val itunesPageUrl: String,
    @SerializedName("itunesId") val itunesId: Int,
    @SerializedName("itunesArtistId") val itunesArtistId: Int,
    @SerializedName("explicit") val explicit: Boolean,
    @SerializedName("language") val language: Any?
)

data class PlayLibraryItemDeviceInfo(
    @SerializedName("deviceId") val deviceId: String?,
    @SerializedName("clientName") val clientName: String?,
    @SerializedName("clientVersion") val clientVersion: String?,
    @SerializedName("manufacturer") val manufacturer: String?,
    @SerializedName("model") val model: String?,
    @SerializedName("sdkVersion") val sdkVersion: Int?
)

data class PlayLibraryItemAudioTrack(
    @SerializedName("index") val index: Int,
    @SerializedName("startOffset") val startOffset: Int,
    @SerializedName("duration") val duration: Double,
    @SerializedName("title") val title: String,
    @SerializedName("contentUrl") val contentUrl: String,
    @SerializedName("mimeType") val mimeType: String,
    @SerializedName("metadata") val metadata: PlayLibraryItemMetadata
)

data class PlayLibraryItemMetadata(
    @SerializedName("filename") val filename: String,
    @SerializedName("ext") val ext: String,
    @SerializedName("path") val path: String,
    @SerializedName("relPath") val relPath: String,
    @SerializedName("size") val size: Int,
    @SerializedName("mtimeMs") val mtimeMs: Long,
    @SerializedName("ctimeMs") val ctimeMs: Long,
    @SerializedName("birthtimeMs") val birthtimeMs: Long
)

data class PlayLibraryItemLibraryItem(
    @SerializedName("id") val id: String,
    @SerializedName("ino") val ino: String,
    @SerializedName("libraryId") val libraryId: String,
    @SerializedName("folderId") val folderId: String,
    @SerializedName("path") val path: String,
    @SerializedName("relPath") val relPath: String,
    @SerializedName("isFile") val isFile: Boolean,
    @SerializedName("mtimeMs") val mtimeMs: Long,
    @SerializedName("ctimeMs") val ctimeMs: Long,
    @SerializedName("birthtimeMs") val birthtimeMs: Long,
    @SerializedName("addedAt") val addedAt: Long,
    @SerializedName("updatedAt") val updatedAt: Long,
    @SerializedName("lastScan") val lastScan: Long,
    @SerializedName("scanVersion") val scanVersion: String,
    @SerializedName("isMissing") val isMissing: Boolean,
    @SerializedName("isInvalid") val isInvalid: Boolean,
    @SerializedName("mediaType") val mediaType: String,
    @SerializedName("media") val media: PlayLibraryItemMedia,
    @SerializedName("libraryFiles") val libraryFiles: List<PlayLibraryItemLibraryFile>,
    @SerializedName("size") val size: Int
)

data class PlayLibraryItemMedia(
    @SerializedName("libraryItemId") val libraryItemId: String,
    @SerializedName("metadata") val metadata: PlayLibraryItemMediaMetadata,
    @SerializedName("coverPath") val coverPath: String,
    @SerializedName("tags") val tags: List<Any>,
    @SerializedName("episodes") val episodes: List<PlayLibraryItemEpisode>,
    @SerializedName("autoDownloadEpisodes") val autoDownloadEpisodes: Boolean,
    @SerializedName("autoDownloadSchedule") val autoDownloadSchedule: String,
    @SerializedName("lastEpisodeCheck") val lastEpisodeCheck: Long,
    @SerializedName("maxEpisodesToKeep") val maxEpisodesToKeep: Int,
    @SerializedName("maxNewEpisodesToDownload") val maxNewEpisodesToDownload: Int,
    @SerializedName("size") val size: Int
)

data class PlayLibraryItemEpisode(
    @SerializedName("libraryItemId") val libraryItemId: String,
    @SerializedName("id") val id: String,
    @SerializedName("index") val index: Int,
    @SerializedName("season") val season: String,
    @SerializedName("episode") val episode: String,
    @SerializedName("episodeType") val episodeType: String,
    @SerializedName("title") val title: String,
    @SerializedName("subtitle") val subtitle: String,
    @SerializedName("description") val description: String,
    @SerializedName("enclosure") val enclosure: PlayLibraryItemEnclosure,
    @SerializedName("pubDate") val pubDate: String,
    @SerializedName("audioFile") val audioFile: PlayLibraryItemAudioFile,
    @SerializedName("audioTrack") val audioTrack: PlayLibraryItemAudioTrack,
    @SerializedName("publishedAt") val publishedAt: Long,
    @SerializedName("addedAt") val addedAt: Long,
    @SerializedName("updatedAt") val updatedAt: Long,
    @SerializedName("duration") val duration: Double,
    @SerializedName("size") val size: Int
)

data class PlayLibraryItemEnclosure(
    @SerializedName("url") val url: String,
    @SerializedName("type") val type: String,
    @SerializedName("length") val length: String
)

data class PlayLibraryItemAudioFile(
    @SerializedName("index") val index: Int,
    @SerializedName("ino") val ino: String,
    @SerializedName("metadata") val metadata: PlayLibraryItemMetadata,
    @SerializedName("addedAt") val addedAt: Long,
    @SerializedName("updatedAt") val updatedAt: Long,
    @SerializedName("trackNumFromMeta") val trackNumFromMeta: Any?,
    @SerializedName("discNumFromMeta") val discNumFromMeta: Any?,
    @SerializedName("trackNumFromFilename") val trackNumFromFilename: Any?,
    @SerializedName("discNumFromFilename") val discNumFromFilename: Any?,
    @SerializedName("manuallyVerified") val manuallyVerified: Boolean,
    @SerializedName("exclude") val exclude: Boolean,
    @SerializedName("error") val error: Any?,
    @SerializedName("format") val format: String,
    @SerializedName("duration") val duration: Double,
    @SerializedName("bitRate") val bitRate: Int,
    @SerializedName("language") val language: Any?,
    @SerializedName("codec") val codec: String,
    @SerializedName("timeBase") val timeBase: String,
    @SerializedName("channels") val channels: Int,
    @SerializedName("channelLayout") val channelLayout: String,
    @SerializedName("chapters") val chapters: List<Any>,
    @SerializedName("embeddedCoverArt") val embeddedCoverArt: String,
    @SerializedName("metaTags") val metaTags: PlayLibraryItemMetaTags,
    @SerializedName("mimeType") val mimeType: String
)

data class PlayLibraryItemMetaTags(
    @SerializedName("tagAlbum") val tagAlbum: String,
    @SerializedName("tagArtist") val tagArtist: String,
    @SerializedName("tagGenre") val tagGenre: String,
    @SerializedName("tagTitle") val tagTitle: String,
    @SerializedName("tagDate") val tagDate: String,
    @SerializedName("tagEncoder") val tagEncoder: String
)

data class PlayLibraryItemLibraryFile(
    @SerializedName("ino") val ino: String? = null,
    @SerializedName("metadata") val metadata: SearchFileMetadata? = null,
    @SerializedName("addedAt") val addedAt: Long? = null,
    @SerializedName("updatedAt") val updatedAt: Long? = null,
    @SerializedName("fileType") val fileType: String? = null
)