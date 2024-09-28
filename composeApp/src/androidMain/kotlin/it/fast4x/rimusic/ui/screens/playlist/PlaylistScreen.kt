package it.fast4x.rimusic.ui.screens.playlist

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import it.fast4x.compose.persist.PersistMapCleanup
import it.fast4x.compose.routing.RouteHandler
import it.fast4x.rimusic.R
import it.fast4x.rimusic.enums.UiType
import it.fast4x.rimusic.ui.components.Scaffold
import it.fast4x.rimusic.ui.screens.globalRoutes
import it.fast4x.rimusic.utils.UiTypeKey
import it.fast4x.rimusic.utils.rememberPreference

@ExperimentalMaterialApi
@ExperimentalTextApi
@ExperimentalFoundationApi
@ExperimentalAnimationApi
@ExperimentalComposeUiApi
@UnstableApi
@Composable
fun PlaylistScreen(
    navController: NavController,
    browseId: String,
    params: String?,
    maxDepth: Int? = null,
    playerEssential: @Composable () -> Unit = {},
) {
    val saveableStateHolder = rememberSaveableStateHolder()
    PersistMapCleanup(tagPrefix = "playlist/$browseId")


    RouteHandler(listenToGlobalEmitter = true) {
        globalRoutes()
        val uiType  by rememberPreference(UiTypeKey, UiType.RiMusic)
        host {
            Scaffold(
                navController = navController,
                playerEssential = playerEssential,
                onTopIconButtonClick = pop,
                showButton1 = uiType != UiType.RiMusic,
                onTopIconButton2Click = pop,
                tabColumnContent = { Item ->
                    Item(0, stringResource(R.string.songs), R.drawable.musical_notes)
                },
                onTabChanged = {},
                onHomeClick = {},
                onBottomIconButtonClick = {},
                showBottomButton = false,
                tabIndex = 0,
                topIconButtonId = R.drawable.ui,
                topIconButton2Id = R.drawable.ui,
                showButton2 = false
            ) { currentTabIndex ->
                saveableStateHolder.SaveableStateProvider(key = currentTabIndex) {
                    when (currentTabIndex) {
                        0 -> PlaylistSongListModern(
                            navController = navController,
                            browseId = browseId,
                            params = params,
                            maxDepth = maxDepth
                        )
                    }
                }
            }
        }
    }
}
