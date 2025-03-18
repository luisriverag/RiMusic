package it.fast4x.rimusic.service

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR
import io.ktor.client.plugins.ClientRequestException
import it.fast4x.rimusic.Database
import it.fast4x.rimusic.utils.asSong
import it.fast4x.rimusic.utils.isConnectionMetered
import it.fast4x.rimusic.utils.okHttpDataSourceFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import it.fast4x.rimusic.appContext
import it.fast4x.rimusic.utils.InvalidHttpCodeException
import it.fast4x.rimusic.utils.findCause
import it.fast4x.rimusic.utils.handleRangeErrors
import it.fast4x.rimusic.utils.principalCache
import it.fast4x.rimusic.utils.retryIf
import timber.log.Timber
import java.io.IOException
import java.lang.InterruptedException

@OptIn(UnstableApi::class)
internal fun PlayerService.createDataSourceFactory(): DataSource.Factory {
    return ResolvingDataSource.Factory(
        CacheDataSource.Factory()
            .setCache(downloadCache)
            .setUpstreamDataSourceFactory(
                CacheDataSource.Factory()
                    .setCache(cache)
                    .setUpstreamDataSourceFactory(
                        appContext().okHttpDataSourceFactory
                    )
            )
            .setCacheWriteDataSinkFactory(null)
            .setFlags(FLAG_IGNORE_CACHE_ON_ERROR)
    ) { dataSpec: DataSpec ->
        //try {

            // Get song from player
             val mediaItem = runBlocking {
                 withContext(Dispatchers.Main) {
                     player.currentMediaItem
                 }
            }
            // Ensure that the song is in database
            Database.asyncTransaction {
                if (mediaItem != null) {
                    insert(mediaItem.asSong)
                }
            }


            //println("PlayerService DataSourcefactory currentMediaItem: ${mediaItem?.mediaId}")
            //dataSpec.key?.let { player.findNextMediaItemById(it)?.mediaMetadata }

        return@Factory runBlocking {
            try {
                dataSpecProcess(dataSpec, appContext(), appContext().isConnectionMetered())
            } catch (e: Exception) {
                Timber.e("PlayerService DataSourcefactory return@Factory Error: ${e.stackTraceToString()}")
                println("PlayerService DataSourcefactory return@Factory Error: ${e.stackTraceToString()}")
                dataSpec
            }
        }
//        }
//        catch (e: Throwable) {
//            println("PlayerService DataSourcefactory Error: ${e.message}")
//            throw IOException(e)
//        }
    }
}


@OptIn(UnstableApi::class)
internal fun MyDownloadHelper.createDataSourceFactory(): DataSource.Factory {
    return ResolvingDataSource.Factory(
        CacheDataSource.Factory()
            .setCache(getDownloadCache(appContext())).apply {
                setUpstreamDataSourceFactory(
                    appContext().okHttpDataSourceFactory
                )
                setCacheWriteDataSinkFactory(null)
            }
    ) { dataSpec: DataSpec ->
        //try {

            return@Factory runBlocking {
                try {
                    dataSpecProcess(dataSpec, appContext(), appContext().isConnectionMetered())
                } catch (e: Exception) {
                    Timber.e("MyDownloadHelper DataSourcefactory return@Factory Error: ${e.stackTraceToString()}")
                    println("MyDownloadHelper DataSourcefactory return@Factory Error: ${e.stackTraceToString()}")
                    dataSpec
                }
            }
//        } catch (e: Throwable) {
//            Timber.e("MyDownloadHelper DataSourcefactory Error: ${e.stackTraceToString()}")
//            println("MyDownloadHelper DataSourcefactory Error: ${e.stackTraceToString()}")
//            dataSpec
//        }
    }.retryIf<UnplayableException>(
        maxRetries = 3,
        printStackTrace = true
    )
        .retryIf(
            maxRetries = 1,
            printStackTrace = true
        ) { ex ->
            ex.findCause<InvalidResponseCodeException>()?.responseCode == 403 ||
                    ex.findCause<ClientRequestException>()?.response?.status?.value == 403 ||
                    ex.findCause<InvalidHttpCodeException>() != null
        }.handleRangeErrors()
}

@OptIn(UnstableApi::class)
internal fun MyPreCacheHelper.createDataSourceFactory(): DataSource.Factory {
    return ResolvingDataSource.Factory(
        CacheDataSource.Factory()
            .setCache(principalCache.getInstance(appContext())).apply {
                setUpstreamDataSourceFactory(
                    appContext().okHttpDataSourceFactory
                )
                setCacheWriteDataSinkFactory(null)
            }
    ) { dataSpec: DataSpec ->
        //try {

            return@Factory runBlocking {
                try {
                    dataSpecProcess(dataSpec, appContext(), appContext().isConnectionMetered())
                } catch (e: Exception) {
                    Timber.e("MyPreCacheHelper DataSourcefactory return@Factory Error: ${e.stackTraceToString()}")
                    println("MyPreCacheHelper DataSourcefactory return@Factory Error: ${e.stackTraceToString()}")
                    dataSpec
                }
            }
//        }
//        catch (e: Throwable) {
//            Timber.e("MyPreCacheHelper DataSourcefactory Error: ${e.stackTraceToString()}")
//            println("MyPreCacheHelper DataSourcefactory Error: ${e.stackTraceToString()}")
//            dataSpec
//        }
    }.retryIf<UnplayableException>(
        maxRetries = 3,
        printStackTrace = true
    )
        .retryIf(
            maxRetries = 1,
            printStackTrace = true
        ) { ex ->
            ex.findCause<InvalidResponseCodeException>()?.responseCode == 403 ||
                    ex.findCause<ClientRequestException>()?.response?.status?.value == 403 ||
                    ex.findCause<InvalidHttpCodeException>() != null
        }.handleRangeErrors()
}