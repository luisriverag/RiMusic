package it.fast4x.rimusic.service

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import it.fast4x.rimusic.service.modern.getAvancedInnerTubeStream
import it.fast4x.rimusic.service.modern.getInnerTubeFormatUrl
import it.fast4x.rimusic.service.modern.getInnerTubeStream
import it.fast4x.rimusic.ui.screens.settings.isYouTubeLoggedIn
import it.fast4x.rimusic.ui.screens.settings.isYouTubeLoginEnabled
import it.fast4x.rimusic.useYtLoginOnlyForBrowse
import kotlinx.coroutines.withTimeout
import timber.log.Timber
import androidx.core.net.toUri

//private suspend fun getPipedFormatUrl(
//    videoId: String,
//    audioQualityFormat: AudioQualityFormat
//): Uri {
//    val format = Piped.player( videoId )?.fold(
//        {
//            when (audioQualityFormat) {
//                AudioQualityFormat.Auto -> it?.autoMaxQualityFormat
//                AudioQualityFormat.High -> it?.highestQualityFormat
//                AudioQualityFormat.Medium -> it?.mediumQualityFormat
//                AudioQualityFormat.Low -> it?.lowestQualityFormat
//            }.also {
//                //println("PlayerService MyDownloadHelper DataSpecProcess getPipedFormatUrl before upsert format $it")
//                Database.asyncTransaction {
//                    if ( songExist(videoId) > 0 )
//                        upsert(
//                            Format(
//                                songId = videoId,
//                                itag = it?.itag?.toInt(),
//                                mimeType = it?.mimeType,
//                                contentLength = it?.contentLength?.toLong(),
//                                bitrate = it?.bitrate?.toLong()
//                            )
//                        )
//                }
//                //println("PlayerService MyDownloadHelper DataSpecProcess getPipedFormatUrl after upsert format $it")
//            }
//        },
//        {
//            println("PlayerService MyDownloadHelper DataSpecProcess Error: ${it.stackTraceToString()}")
//            throw it
//        }
//    )
//
//    // Return parsed URL to play song or throw error if none of the responses is valid
//    return Uri.parse( format?.url ) ?: throw NoSuchElementException( "Could not find any playable format from Piped ($videoId)" )
//}

@OptIn(UnstableApi::class)
internal suspend fun PlayerService.dataSpecProcess(
    dataSpec: DataSpec,
    context: Context,
    connectionMetered: Boolean
): DataSpec {
    val songUri = dataSpec.uri.toString()
    val videoId = songUri.substringAfter("watch?v=")
    val chunkLength = 512 * 1024L
    val isCached = try {
        cache.isCached(videoId, dataSpec.position, chunkLength)
    } catch (e: Exception) {
        false
    }
    val isDownloaded = try {
        downloadCache.isCached(videoId, dataSpec.position, if (dataSpec.length >= 0) dataSpec.length else 1)
    } catch (e: Exception) {
        false
    }
    if( dataSpec.isLocal || isCached || isDownloaded ) {
        println("PlayerService DataSpecProcess Playing song ${videoId} from cached or local file")
        return dataSpec.withUri(dataSpec.uri.toString().toUri())
    }

    //try {

    println("PlayerService DataSpecProcess Playing song ${videoId}")
    //if loggedin use advanced player with webPotoken and new newpipe extractor
    val format = if (!useYtLoginOnlyForBrowse() && isYouTubeLoginEnabled() && isYouTubeLoggedIn())
        getAvancedInnerTubeStream(videoId, audioQualityFormat, connectionMetered)
    else getInnerTubeStream(videoId, audioQualityFormat, connectionMetered)

    println("PlayerService DataSpecProcess Playing song ${videoId} from url=${format?.url}")

    if (format?.url == null) throw PlayableFormatNotFoundException()
    return dataSpec.withUri(format.url!!.toUri()).subrange(dataSpec.uriPositionOffset, chunkLength)


//    } catch ( e: LoginRequiredException ) {
//        throw e
////        try {
////            // Switch to Piped
////            val formatUrl = getPipedFormatUrl( videoId, audioQualityFormat )
////
////            println("PlayerService DataSpecProcess Playing song $videoId from url $formatUrl")
////            return dataSpec.withUri( formatUrl )
////
////        } catch ( e: NoSuchElementException ) {
////            throw e
////            // Switch to Invidious
//////            val formatUrl = getInvidiousFormatUrl( videoId, audioQualityFormat )
//////
//////            println("PlayerService DataSpecProcess Playing song $videoId from url $formatUrl")
//////            return dataSpec.withUri( formatUrl )
////        }
//
//    } catch ( e: Exception ) {
//        // Rethrow exception if it's not handled
//        throw e
//    }
}

@OptIn(UnstableApi::class)
internal suspend fun MyDownloadHelper.dataSpecProcess(
    dataSpec: DataSpec,
    context: Context,
    connectionMetered: Boolean = false
): DataSpec {
    val songUri = dataSpec.uri.toString()
    val videoId = songUri.substringAfter("watch?v=")
    val chunkLength = 512 * 1024L
    val length = if (dataSpec.length >= 0) dataSpec.length else 1

    val isDownloaded = try {
        downloadCache.isCached(videoId, dataSpec.position, length)
    } catch (e: Exception) {
        false
    }

    Timber.d("MyDownloadHelper DataSpecProcess Playing song ${videoId} dataSpec position ${dataSpec.position} length ${dataSpec.length}")
    println("MyDownloadHelper DataSpecProcess Playing song ${videoId} dataSpec position ${dataSpec.position} length ${dataSpec.length}")
    if( dataSpec.isLocal || isDownloaded ) {
        Timber.d("MyDownloadHelper DataSpecProcess download song ${videoId} from cached or local file")
        println("MyDownloadHelper DataSpecProcess download song ${videoId} from cached or local file")
        return dataSpec.withUri(dataSpec.uri.toString().toUri())
    }

    Timber.d("MyDownloadHelper DataSpecProcess Playing song ${videoId}")
    val format = getInnerTubeStream(videoId, audioQualityFormat, connectionMetered)

    println("MyDownloadHelper DataSpecProcess Playing song ${videoId} from url=${format?.url}")

    if (format?.url == null) throw PlayableFormatNotFoundException()

    return dataSpec.withUri(format.url!!.toUri())

//    try {
//        Timber.d("MyDownloadHelper DataSpecProcess Playing song start timeout ${videoId}")
//
//        //if loggedin use advanced player with webPotoken and new newpipe extractor
////        val format = if (!useYtLoginOnlyForBrowse() && isYouTubeLoginEnabled() && isYouTubeLoggedIn())
////            getAvancedInnerTubeStream(videoId, audioQualityFormat, connectionMetered)
////        else getInnerTubeStream(videoId, audioQualityFormat, connectionMetered)
//
//        val format = getInnerTubeStream(videoId, audioQualityFormat, connectionMetered)
//
//        println("MyDownloadHelper DataSpecProcess Playing song ${videoId} from url=${format?.url}")
//
//        if (format?.url == null) throw PlayableFormatNotFoundException()
//
//        return dataSpec.withUri(Uri.parse(format?.url))
//
//    } catch ( e: Exception ) {
//        Timber.e("MyDownloadHelper DataSpecProcess Error: ${e.stackTraceToString()}")
//        println("MyDownloadHelper DataSpecProcess Error: ${e.stackTraceToString()}")
//        val format = getInnerTubeStream(videoId, audioQualityFormat, connectionMetered)
//        return dataSpec.withUri(Uri.parse(format?.url))
////        println("MyDownloadHelper DataSpecProcess Playing song $videoId from ALTERNATIVE url")
////        val alternativeUrl = "https://jossred.josprox.com/yt/stream/$videoId"
////        return dataSpec.withUri(alternativeUrl.toUri())
//
//    } catch ( e: Exception ) {
//        // Rethrow exception if it's not handled
//        Timber.e("MyDownloadHelper DataSpecProcess Error: ${e.stackTraceToString()}")
//        println("MyDownloadHelper DataSpecProcess Error: ${e.stackTraceToString()}")
//        throw e
//    }
}

@OptIn(UnstableApi::class)
internal suspend fun MyPreCacheHelper.dataSpecProcess(
    dataSpec: DataSpec,
    context: Context,
    connectionMetered: Boolean = false
): DataSpec {
    val songUri = dataSpec.uri.toString()
    val videoId = songUri.substringAfter("watch?v=")
    val chunkLength = 512 * 1024L
    val length = if (dataSpec.length >= 0) dataSpec.length else 1
    val isCached = try {
        cache.isCached(videoId, dataSpec.position, length)
    } catch (e: Exception) {
        false
    }
    Timber.d("MyPreCacheHelper DataSpecProcess Playing song ${videoId} dataSpec position ${dataSpec.position} length ${dataSpec.length}")
    println("MyPreCacheHelper DataSpecProcess Playing song ${videoId} dataSpec position ${dataSpec.position} length ${dataSpec.length}")
    if( dataSpec.isLocal || isCached) {
        Timber.d("MyPreCacheHelper DataSpecProcess download song ${videoId} from cached or local file")
        println("MyPreCacheHelper DataSpecProcess download song ${videoId} from cached or local file")
        return dataSpec.withUri(dataSpec.uri.toString().toUri())
    }

    Timber.d("MyPreCacheHelper DataSpecProcess Playing song ${videoId}")
    val format = getInnerTubeStream(videoId, audioQualityFormat, connectionMetered)
    println("MyPreCacheHelper DataSpecProcess Playing song ${videoId} from url=${format?.url}")
    if (format?.url == null) throw PlayableFormatNotFoundException()

    return dataSpec.withUri(format.url!!.toUri())

//    try {
//        //runBlocking(Dispatchers.IO) {
//        Timber.d("MyPreCacheHelper DataSpecProcess Playing song ${videoId}")
//
//            //if loggedin use advanced player with webPotoken and new newpipe extractor
////        val format = if (!useYtLoginOnlyForBrowse() && isYouTubeLoginEnabled() && isYouTubeLoggedIn())
////            getAvancedInnerTubeStream(videoId, audioQualityFormat, connectionMetered)
////        else getInnerTubeStream(videoId, audioQualityFormat, connectionMetered)
//
//        val format = getInnerTubeStream(videoId, audioQualityFormat, connectionMetered)
//
//        println("MyPreCacheHelper DataSpecProcess Playing song ${videoId} from url=${format?.url}")
//
//        if (format?.url == null) throw PlayableFormatNotFoundException()
//
//        return dataSpec.withUri(Uri.parse(format?.url))
//
//    } catch ( e: Exception ) {
//        Timber.e("MyPreCacheHelper DataSpecProcess Error: ${e.stackTraceToString()}")
//        println("MyPreCacheHelper DataSpecProcess Error: ${e.stackTraceToString()}")
//        val format = getInnerTubeStream(videoId, audioQualityFormat, connectionMetered)
//        return dataSpec.withUri(Uri.parse(format?.url))
////        println("MyPreCacheHelper DataSpecProcess Playing song $videoId from ALTERNATIVE url")
////        val alternativeUrl = "https://jossred.josprox.com/yt/stream/$videoId"
////        return dataSpec.withUri(alternativeUrl.toUri())
//
//    } catch ( e: Exception ) {
//        // Rethrow exception if it's not handled
//        Timber.e("MyPreCacheHelper DataSpecProcess Error: ${e.stackTraceToString()}")
//        println("MyPreCacheHelper DataSpecProcess Error: ${e.stackTraceToString()}")
//        throw e
//    }
}


