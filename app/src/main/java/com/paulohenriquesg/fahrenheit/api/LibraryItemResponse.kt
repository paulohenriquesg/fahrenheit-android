package com.paulohenriquesg.fahrenheit.api

import com.google.gson.annotations.SerializedName

data class LibraryItemResponse(
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
    @SerializedName("media") val media: LibraryItemMedia,
    @SerializedName("libraryFiles") val libraryFiles: List<LibraryFile>,
    @SerializedName("size") val size: Long,
    @SerializedName("userMediaProgress") val userMediaProgress: MediaProgressResponse?,
    @SerializedName("rssFeedUrl") val rssFeedUrl: String?
)

data class LibraryItemMedia (
    @SerializedName("id") val id: String,
    @SerializedName("libraryItemId") val libraryItemId: String,
    @SerializedName("metadata") val metadata: LibraryItemMetadata,
    @SerializedName("coverPath") val coverPath: String,
    @SerializedName("tags") val tags: List<String>,
    @SerializedName("audioFiles") val audioFiles: List<AudioFile>,
    @SerializedName("chapters") val chapters: List<Chapter>?,
    @SerializedName("duration") val duration: Double?,
    @SerializedName("size") val size: Long,
    @SerializedName("episodes") val episodes: List<Episode>?,
    @SerializedName("tracks") val tracks: List<Track>,
    @SerializedName("ebookFile") val ebookFile: String?,
    @SerializedName("autoDownloadEpisodes") val autoDownloadEpisodes: Boolean?,
    @SerializedName("autoDownloadSchedule") val autoDownloadSchedule: String?,
    @SerializedName("lastEpisodeCheck") val lastEpisodeCheck: Long?,
    @SerializedName("maxEpisodesToKeep") val maxEpisodesToKeep: Int?,
    @SerializedName("maxNewEpisodesToDownload") val maxNewEpisodesToDownload: Int?
)

data class Episode(
    @SerializedName("libraryItemId") val libraryItemId: String,
    @SerializedName("id") val id: String,
    @SerializedName("index") val index: Int,
    @SerializedName("season") val season: String?,
    @SerializedName("episode") val episode: String?,
    @SerializedName("episodeType") val episodeType: String,
    @SerializedName("title") val title: String,
    @SerializedName("subtitle") val subtitle: String?,
    @SerializedName("description") val description: String,
    @SerializedName("enclosure") val enclosure: Enclosure?,
    @SerializedName("pubDate") val pubDate: String,
    @SerializedName("audioFile") val audioFile: AudioFile?,
    @SerializedName("audioTrack") val audioTrack: AudioTrack?,
    @SerializedName("publishedAt") val publishedAt: Long,
    @SerializedName("addedAt") val addedAt: Long,
    @SerializedName("updatedAt") val updatedAt: Long,
    @SerializedName("duration") val duration: Double? = null,  // From PlayLibraryItemEpisode
    @SerializedName("size") val size: Int? = null  // From PlayLibraryItemEpisode
)

data class Enclosure(
    @SerializedName("url") val url: String,
    @SerializedName("length") val length: Long,
    @SerializedName("type") val type: String
)

data class AudioFile(
    @SerializedName("index") val index: Int,
    @SerializedName("ino") val ino: String,
    @SerializedName("metadata") val metadata: AudioFileMetadata,
    @SerializedName("addedAt") val addedAt: Long,
    @SerializedName("updatedAt") val updatedAt: Long,
    @SerializedName("trackNumFromMeta") val trackNumFromMeta: Int?,
    @SerializedName("discNumFromMeta") val discNumFromMeta: Int?,
    @SerializedName("trackNumFromFilename") val trackNumFromFilename: Int?,
    @SerializedName("discNumFromFilename") val discNumFromFilename: Int?,
    @SerializedName("manuallyVerified") val manuallyVerified: Boolean,
    @SerializedName("exclude") val exclude: Boolean,
    @SerializedName("error") val error: String?,
    @SerializedName("format") val format: String,
    @SerializedName("duration") val duration: Double,
    @SerializedName("bitRate") val bitRate: Int,
    @SerializedName("language") val language: String?,
    @SerializedName("codec") val codec: String,
    @SerializedName("timeBase") val timeBase: String,
    @SerializedName("channels") val channels: Int,
    @SerializedName("channelLayout") val channelLayout: String,
    @SerializedName("chapters") val chapters: List<Chapter>,
    @SerializedName("embeddedCoverArt") val embeddedCoverArt: String?,
    @SerializedName("metaTags") val metaTags: MetaTags,
    @SerializedName("mimeType") val mimeType: String
)

data class AudioTrack(
    @SerializedName("index") val index: Int,
    @SerializedName("startOffset") val startOffset: Double,
    @SerializedName("duration") val duration: Double,
    @SerializedName("title") val title: String,
    @SerializedName("contentUrl") val contentUrl: String,
    @SerializedName("mimeType") val mimeType: String,
    @SerializedName("codec") val codec: String,
    @SerializedName("metadata") val metadata: AudioFileMetadata
)

data class AudioFileMetadata(
    @SerializedName("filename") val filename: String,
    @SerializedName("ext") val ext: String,
    @SerializedName("path") val path: String,
    @SerializedName("relPath") val relPath: String,
    @SerializedName("size") val size: Long,
    @SerializedName("mtimeMs") val mtimeMs: Long,
    @SerializedName("ctimeMs") val ctimeMs: Long,
    @SerializedName("birthtimeMs") val birthtimeMs: Long
)

data class MetaTags(
    @SerializedName("tagAlbum") val tagAlbum: String? = null,
    @SerializedName("tagArtist") val tagArtist: String? = null,
    @SerializedName("tagGenre") val tagGenre: String? = null,
    @SerializedName("tagTitle") val tagTitle: String? = null,
    @SerializedName("tagGrouping") val tagGrouping: String? = null,  // Not in Search/Play variants
    @SerializedName("tagTrack") val tagTrack: String? = null,
    @SerializedName("tagAlbumArtist") val tagAlbumArtist: String? = null,
    @SerializedName("tagDate") val tagDate: String? = null,
    @SerializedName("tagComposer") val tagComposer: String? = null,
    @SerializedName("tagComment") val tagComment: String? = null,  // Not in Search/Play variants
    @SerializedName("tagDescription") val tagDescription: String? = null,  // Not in Search/Play variants
    @SerializedName("tagEncoder") val tagEncoder: String? = null
)

data class Chapter(
    @SerializedName("id") val id: Int? = null,
    @SerializedName("start") val start: Double? = null,
    @SerializedName("end") val end: Double? = null,
    @SerializedName("title") val title: String? = null
)

data class Track(
    @SerializedName("index") val index: Int,
    @SerializedName("startOffset") val startOffset: Double,
    @SerializedName("duration") val duration: Double,
    @SerializedName("title") val title: String,
    @SerializedName("contentUrl") val contentUrl: String,
    @SerializedName("mimeType") val mimeType: String,
    @SerializedName("codec") val codec: String? = null,  // From AudioTrack - not always present
    @SerializedName("metadata") val metadata: AudioFileMetadata
)

data class LibraryFile(
    @SerializedName("ino") val ino: String,
    @SerializedName("metadata") val metadata: AudioFileMetadata,
    @SerializedName("addedAt") val addedAt: Long,
    @SerializedName("updatedAt") val updatedAt: Long,
    @SerializedName("fileType") val fileType: String
)

data class LibraryItemMetadata(
    @SerializedName("title") val title: String,
    @SerializedName("titleIgnorePrefix") val titleIgnorePrefix: String,
    @SerializedName("subtitle") val subtitle: String?,
    // Detailed API responses (array format)
    @SerializedName("authors") val authors: List<Author>? = null,
    @SerializedName("narrators") val narrators: List<String>? = null,
    @SerializedName("series") val series: List<Series>? = null,
    @SerializedName("genres") val genres: List<String>,
    @SerializedName("publishedYear") val publishedYear: String,
    @SerializedName("publishedDate") val publishedDate: String?,
    @SerializedName("publisher") val publisher: String,
    @SerializedName("description") val description: String,
    @SerializedName("isbn") val isbn: String?,
    @SerializedName("asin") val asin: String,
    @SerializedName("language") val language: String?,
    @SerializedName("explicit") val explicit: Boolean,
    // Minified API responses (string format)
    @SerializedName("authorName") val authorName: String? = null,
    @SerializedName("authorNameLF") val authorNameLF: String? = null,
    @SerializedName("narratorName") val narratorName: String? = null,
    @SerializedName("seriesName") val seriesName: String? = null
)

data class Author(
    @SerializedName("id") val id: String,
    @SerializedName("asin") val asin: String?,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String?,
    @SerializedName("imagePath") val imagePath: String?,
    @SerializedName("addedAt") val addedAt: Long,
    @SerializedName("updatedAt") val updatedAt: Long,
    @SerializedName("numBooks") val numBooks: Int? = null  // From SearchAuthor - only in search responses
)

data class Series(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("sequence") val sequence: String
)