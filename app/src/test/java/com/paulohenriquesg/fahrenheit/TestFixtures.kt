package com.paulohenriquesg.fahrenheit

import com.paulohenriquesg.fahrenheit.api.*

object TestFixtures {
    fun createMockServerSettings(maxBackupSize: Double = 1.0) = ServerSettings(
        id = "test-server-id",
        scannerFindCovers = true,
        scannerCoverProvider = "google",
        scannerParseSubtitle = false,
        scannerPreferMatchedMetadata = true,
        scannerDisableWatcher = false,
        storeCoverWithItem = true,
        storeMetadataWithItem = false,
        metadataFileFormat = "json",
        rateLimitLoginRequests = 10,
        rateLimitLoginWindow = 600000L,
        backupSchedule = "0 0 * * *",
        backupsToKeep = 5,
        maxBackupSize = maxBackupSize.toInt(),
        loggerDailyLogsToKeep = 7,
        loggerScannerLogsToKeep = 3,
        homeBookshelfView = 1,
        bookshelfView = 1,
        sortingIgnorePrefix = true,
        sortingPrefixes = listOf("The", "A", "An"),
        chromecastEnabled = false,
        dateFormat = "MM/dd/yyyy",
        language = "en",
        logLevel = 2,
        version = "2.5.0"
    )

    fun createMockUser(username: String = "testuser") = User(
        id = "user-123",
        username = username,
        type = "root",
        token = "mock-token-12345",
        mediaProgress = emptyList(),
        seriesHideFromContinueListening = emptyList(),
        bookmarks = emptyList(),
        isActive = true,
        isLocked = false,
        lastSeen = 1234567890L,
        createdAt = 1234567890L,
        permissions = Permissions(
            download = true,
            update = true,
            delete = true,
            upload = true,
            accessAllLibraries = true,
            accessAllTags = true,
            accessExplicitContent = true
        ),
        librariesAccessible = emptyList(),
        itemTagsAccessible = emptyList()
    )

    fun createMockLoginResponse() = LoginResponse(
        user = createMockUser(),
        userDefaultLibraryId = "library-456",
        serverSettings = createMockServerSettings(),
        source = "test"
    )

    fun createMockLibraryItemMetadata(
        title: String = "Test Book",
        authorName: String = "Test Author"
    ) = LibraryItemMetadata(
        title = title,
        titleIgnorePrefix = title,
        subtitle = null,
        authors = listOf(Author(id = "author-1", name = authorName)),
        narrators = listOf("Test Narrator"),
        series = emptyList(),
        genres = listOf("Fiction"),
        publishedYear = "2024",
        publishedDate = null,
        publisher = "Test Publisher",
        description = "Test description",
        isbn = null,
        asin = "",
        language = "en",
        explicit = false,
        authorName = authorName,
        authorNameLF = "$authorName",
        narratorName = "Test Narrator",
        seriesName = null
    )

    fun createMockAudioTrack(
        index: Int = 0,
        duration: Double = 3600.0,
        contentUrl: String = "/api/items/test-item/file/test-file"
    ) = AudioTrack(
        index = index,
        startOffset = 0.0,
        duration = duration,
        title = "Test Track",
        contentUrl = contentUrl,
        mimeType = "audio/mpeg",
        codec = "mp3",
        metadata = AudioFileMetadata(
            filename = "test.mp3",
            ext = ".mp3",
            path = "/audiobooks/test.mp3",
            relPath = "test.mp3",
            size = 1000000,
            mtimeMs = 1234567890000L,
            ctimeMs = 1234567890000L,
            birthtimeMs = 1234567890000L
        )
    )

    fun createMockLibraryItem(
        id: String = "item-123",
        title: String = "Test Book",
        duration: Double = 3600.0
    ) = LibraryItemResponse(
        id = id,
        ino = "ino-123",
        libraryId = "library-456",
        folderId = "folder-789",
        path = "/audiobooks/testbook",
        relPath = "testbook",
        isFile = false,
        mtimeMs = 1234567890000L,
        ctimeMs = 1234567890000L,
        birthtimeMs = 1234567890000L,
        addedAt = 1234567890000L,
        updatedAt = 1234567890000L,
        lastScan = 1234567890000L,
        scanVersion = "2.5.0",
        isMissing = false,
        isInvalid = false,
        mediaType = "book",
        media = LibraryItemMedia(
            id = "media-123",
            libraryItemId = id,
            metadata = createMockLibraryItemMetadata(title),
            coverPath = "/covers/test.jpg",
            tags = emptyList(),
            audioFiles = listOf(AudioFile(
                index = 0,
                ino = "file-ino",
                metadata = AudioFileMetadata(
                    filename = "test.mp3",
                    ext = ".mp3",
                    path = "/audiobooks/test.mp3",
                    relPath = "test.mp3",
                    size = 1000000,
                    mtimeMs = 1234567890000L,
                    ctimeMs = 1234567890000L,
                    birthtimeMs = 1234567890000L
                ),
                addedAt = 1234567890000L,
                updatedAt = 1234567890000L,
                trackNumFromMeta = null,
                discNumFromMeta = null,
                trackNumFromFilename = null,
                discNumFromFilename = null,
                manuallyVerified = false,
                exclude = false,
                error = null,
                format = "MP3",
                duration = duration,
                bitRate = 128000,
                language = null,
                codec = "mp3",
                timeBase = "1/14112000",
                channels = 2,
                channelLayout = "stereo",
                chapters = emptyList(),
                embeddedCoverArt = null,
                metaTags = MetaTags(
                    tagAlbum = null,
                    tagArtist = null,
                    tagGenre = null,
                    tagTitle = null,
                    tagGrouping = null,
                    tagTrack = null,
                    tagAlbumArtist = null,
                    tagDate = null,
                    tagComposer = null,
                    tagComment = null,
                    tagDescription = null,
                    tagEncoder = null
                ),
                mimeType = "audio/mpeg"
            )),
            chapters = emptyList(),
            duration = duration,
            size = 1000000,
            episodes = null,
            tracks = listOf(Track(
                index = 0,
                startOffset = 0.0,
                duration = duration,
                title = "Test Track",
                contentUrl = "/api/items/test-item/file/test-file",
                mimeType = "audio/mpeg",
                codec = "mp3",
                metadata = AudioFileMetadata(
                    filename = "test.mp3",
                    ext = ".mp3",
                    path = "/audiobooks/test.mp3",
                    relPath = "test.mp3",
                    size = 1000000,
                    mtimeMs = 1234567890000L,
                    ctimeMs = 1234567890000L,
                    birthtimeMs = 1234567890000L
                )
            )),
            ebookFile = null,
            autoDownloadEpisodes = null,
            autoDownloadSchedule = null,
            lastEpisodeCheck = null,
            maxEpisodesToKeep = null,
            maxNewEpisodesToDownload = null
        ),
        libraryFiles = emptyList(),
        size = 1000000,
        userMediaProgress = null,
        rssFeedUrl = null
    )

    fun createMockMediaProgress(
        id: String = "item-123",
        currentTime: Double = 500.0,
        duration: Double = 3600.0
    ) = MediaProgressResponse(
        id = "progress-123",
        libraryItemId = id,
        episodeId = null,
        duration = duration,
        progress = currentTime / duration,
        currentTime = currentTime,
        isFinished = false,
        hideFromContinueListening = false,
        lastUpdate = 1234567890000L,
        startedAt = 1234567890000L,
        finishedAt = null
    )

    fun createMockShelf(
        id: String = "shelf-123",
        label: String = "Continue Listening",
        type: String = "book"
    ) = Shelf(
        id = id,
        label = label,
        labelStringKey = "LabelContinueListening",
        type = type,
        bookEntities = listOf(
            LibraryItem(
                id = "item-123",
                ino = "ino-123",
                libraryId = "library-456",
                folderId = "folder-789",
                path = "/audiobooks/testbook",
                relPath = "testbook",
                isFile = false,
                mtimeMs = 1234567890000L,
                ctimeMs = 1234567890000L,
                birthtimeMs = 1234567890000L,
                addedAt = 1234567890000L,
                updatedAt = 1234567890000L,
                isMissing = false,
                isInvalid = false,
                mediaType = "book",
                media = Media(
                    metadata = createMockLibraryItemMetadata(),
                    coverPath = "/covers/test.jpg",
                    tags = emptyList(),
                    numTracks = 1,
                    numAudioFiles = 1,
                    numChapters = 0,
                    duration = 3600.0,
                    size = 1000000,
                    ebookFileFormat = null
                ),
                numFiles = 1,
                size = 1000000,
                collapsedSeries = null,
                numEpisodesIncomplete = null
            )
        ),
        total = 1
    )
}
