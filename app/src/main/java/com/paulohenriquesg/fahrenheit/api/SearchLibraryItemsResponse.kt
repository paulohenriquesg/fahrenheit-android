package com.paulohenriquesg.fahrenheit.api

import com.google.gson.annotations.SerializedName

data class SearchLibraryItemsResponse(
    @SerializedName("book") val book: List<SearchBookItem>? = null,
    @SerializedName("podcast") val podcast: List<SearchBookItem>? = null,
    @SerializedName("tags") val tags: List<Any>? = null,
    @SerializedName("genres") val genres: List<Any>? = null,
    @SerializedName("authors") val authors: List<SearchAuthor>? = null,
    @SerializedName("narrators") val narrators: List<Any>? = null,
    @SerializedName("series") val series: List<Any>? = null
)

data class SearchBookItem(
    @SerializedName("libraryItem") val libraryItem: LibraryItem? = null,
    @SerializedName("matchKey") val matchKey: String? = null,
    @SerializedName("matchText") val matchText: String? = null
)

data class SearchLibraryItem(
    @SerializedName("id") val id: String? = null,
    @SerializedName("ino") val ino: String? = null,
    @SerializedName("libraryId") val libraryId: String? = null,
    @SerializedName("folderId") val folderId: String? = null,
    @SerializedName("path") val path: String? = null,
    @SerializedName("relPath") val relPath: String? = null,
    @SerializedName("isFile") val isFile: Boolean? = null,
    @SerializedName("mtimeMs") val mtimeMs: Long? = null,
    @SerializedName("ctimeMs") val ctimeMs: Long? = null,
    @SerializedName("birthtimeMs") val birthtimeMs: Long? = null,
    @SerializedName("addedAt") val addedAt: Long? = null,
    @SerializedName("updatedAt") val updatedAt: Long? = null,
    @SerializedName("lastScan") val lastScan: Long? = null,
    @SerializedName("scanVersion") val scanVersion: String? = null,
    @SerializedName("isMissing") val isMissing: Boolean? = null,
    @SerializedName("isInvalid") val isInvalid: Boolean? = null,
    @SerializedName("mediaType") val mediaType: String? = null,
    @SerializedName("media") val media: SearchMedia? = null,
    @SerializedName("libraryFiles") val libraryFiles: List<SearchLibraryFile>? = null,
    @SerializedName("size") val size: Long? = null
)

data class SearchMedia(
    @SerializedName("libraryItemId") val libraryItemId: String? = null,
    @SerializedName("metadata") val metadata: SearchMetadata? = null,
    @SerializedName("coverPath") val coverPath: String? = null,
    @SerializedName("tags") val tags: List<String>? = null,
    @SerializedName("audioFiles") val audioFiles: List<SearchAudioFile>? = null,
    @SerializedName("chapters") val chapters: List<SearchChapter>? = null,
    @SerializedName("duration") val duration: Double? = null,
    @SerializedName("size") val size: Long? = null,
    @SerializedName("tracks") val tracks: List<SearchTrack>? = null,
    @SerializedName("ebookFile") val ebookFile: Any? = null
)

data class SearchMetadata(
    @SerializedName("title") val title: String? = null,
    @SerializedName("titleIgnorePrefix") val titleIgnorePrefix: String? = null,
    @SerializedName("subtitle") val subtitle: String? = null,
    @SerializedName("authors") val authors: List<SearchAuthor>? = null,
    @SerializedName("narrators") val narrators: List<String>? = null,
    @SerializedName("series") val series: List<SearchSeries>? = null,
    @SerializedName("genres") val genres: List<String>? = null,
    @SerializedName("publishedYear") val publishedYear: String? = null,
    @SerializedName("publishedDate") val publishedDate: String? = null,
    @SerializedName("publisher") val publisher: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("isbn") val isbn: String? = null,
    @SerializedName("asin") val asin: String? = null,
    @SerializedName("language") val language: String? = null,
    @SerializedName("explicit") val explicit: Boolean? = null,
    @SerializedName("authorName") val authorName: String? = null,
    @SerializedName("authorNameLF") val authorNameLF: String? = null,
    @SerializedName("narratorName") val narratorName: String? = null,
    @SerializedName("seriesName") val seriesName: String? = null
)

data class SearchAuthor(
    @SerializedName("id") val id: String? = null,
    @SerializedName("asin") val asin: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("imagePath") val imagePath: String? = null,
    @SerializedName("addedAt") val addedAt: Long? = null,
    @SerializedName("updatedAt") val updatedAt: Long? = null,
    @SerializedName("numBooks") val numBooks: Int? = null
)

data class SearchSeries(
    @SerializedName("id") val id: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("sequence") val sequence: String? = null
)

data class SearchAudioFile(
    @SerializedName("index") val index: Int? = null,
    @SerializedName("ino") val ino: String? = null,
    @SerializedName("metadata") val metadata: SearchFileMetadata? = null,
    @SerializedName("addedAt") val addedAt: Long? = null,
    @SerializedName("updatedAt") val updatedAt: Long? = null,
    @SerializedName("trackNumFromMeta") val trackNumFromMeta: Int? = null,
    @SerializedName("discNumFromMeta") val discNumFromMeta: Int? = null,
    @SerializedName("trackNumFromFilename") val trackNumFromFilename: Int? = null,
    @SerializedName("discNumFromFilename") val discNumFromFilename: Int? = null,
    @SerializedName("manuallyVerified") val manuallyVerified: Boolean? = null,
    @SerializedName("exclude") val exclude: Boolean? = null,
    @SerializedName("error") val error: String? = null,
    @SerializedName("format") val format: String? = null,
    @SerializedName("duration") val duration: Double? = null,
    @SerializedName("bitRate") val bitRate: Int? = null,
    @SerializedName("language") val language: String? = null,
    @SerializedName("codec") val codec: String? = null,
    @SerializedName("timeBase") val timeBase: String? = null,
    @SerializedName("channels") val channels: Int? = null,
    @SerializedName("channelLayout") val channelLayout: String? = null,
    @SerializedName("chapters") val chapters: List<Any>? = null,
    @SerializedName("embeddedCoverArt") val embeddedCoverArt: Any? = null,
    @SerializedName("metaTags") val metaTags: SearchMetaTags? = null,
    @SerializedName("mimeType") val mimeType: String? = null
)

data class SearchFileMetadata(
    @SerializedName("filename") val filename: String? = null,
    @SerializedName("ext") val ext: String? = null,
    @SerializedName("path") val path: String? = null,
    @SerializedName("relPath") val relPath: String? = null,
    @SerializedName("size") val size: Long? = null,
    @SerializedName("mtimeMs") val mtimeMs: Long? = null,
    @SerializedName("ctimeMs") val ctimeMs: Long? = null,
    @SerializedName("birthtimeMs") val birthtimeMs: Long? = null
)

data class SearchMetaTags(
    @SerializedName("tagAlbum") val tagAlbum: String? = null,
    @SerializedName("tagArtist") val tagArtist: String? = null,
    @SerializedName("tagGenre") val tagGenre: String? = null,
    @SerializedName("tagTitle") val tagTitle: String? = null,
    @SerializedName("tagTrack") val tagTrack: String? = null,
    @SerializedName("tagAlbumArtist") val tagAlbumArtist: String? = null,
    @SerializedName("tagComposer") val tagComposer: String? = null
)

data class SearchChapter(
    @SerializedName("id") val id: Int? = null,
    @SerializedName("start") val start: Double? = null,
    @SerializedName("end") val end: Double? = null,
    @SerializedName("title") val title: String? = null
)

data class SearchTrack(
    @SerializedName("index") val index: Int? = null,
    @SerializedName("startOffset") val startOffset: Double? = null,
    @SerializedName("duration") val duration: Double? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("contentUrl") val contentUrl: String? = null,
    @SerializedName("mimeType") val mimeType: String? = null,
    @SerializedName("metadata") val metadata: SearchFileMetadata? = null
)

data class SearchLibraryFile(
    @SerializedName("ino") val ino: String? = null,
    @SerializedName("metadata") val metadata: SearchFileMetadata? = null,
    @SerializedName("addedAt") val addedAt: Long? = null,
    @SerializedName("updatedAt") val updatedAt: Long? = null,
    @SerializedName("fileType") val fileType: String? = null
)