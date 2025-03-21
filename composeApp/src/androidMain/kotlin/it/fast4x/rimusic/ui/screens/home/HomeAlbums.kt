package it.fast4x.rimusic.ui.screens.home

import android.annotation.SuppressLint
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import it.fast4x.compose.persist.persistList
import it.fast4x.rimusic.Database
import it.fast4x.rimusic.LocalPlayerServiceBinder
import it.fast4x.rimusic.MODIFIED_PREFIX
import it.fast4x.rimusic.R
import it.fast4x.rimusic.enums.AlbumSortBy
import it.fast4x.rimusic.enums.AlbumsType
import it.fast4x.rimusic.enums.ArtistsType
import it.fast4x.rimusic.enums.NavigationBarPosition
import it.fast4x.rimusic.enums.UiType
import it.fast4x.rimusic.models.Album
import it.fast4x.rimusic.models.Song
import it.fast4x.rimusic.models.SongPlaylistMap
import it.fast4x.rimusic.ui.components.ButtonsRow
import it.fast4x.rimusic.ui.components.LocalMenuState
import it.fast4x.rimusic.ui.components.themed.AlbumsItemMenu
import it.fast4x.rimusic.ui.components.themed.FloatingActionsContainerWithScrollToTop
import it.fast4x.rimusic.ui.components.themed.HeaderInfo
import it.fast4x.rimusic.ui.components.themed.InputTextDialog
import it.fast4x.rimusic.ui.components.themed.MultiFloatingActionsContainer
import it.fast4x.rimusic.ui.items.AlbumItem
import it.fast4x.rimusic.ui.styling.Dimensions
import it.fast4x.rimusic.utils.addNext
import it.fast4x.rimusic.utils.albumSortByKey
import it.fast4x.rimusic.utils.albumSortOrderKey
import it.fast4x.rimusic.utils.albumTypeKey
import it.fast4x.rimusic.utils.artistTypeKey
import it.fast4x.rimusic.utils.asMediaItem
import it.fast4x.rimusic.utils.disableScrollingTextKey
import it.fast4x.rimusic.utils.enqueue
import it.fast4x.rimusic.utils.rememberPreference
import it.fast4x.rimusic.utils.showFloatingIconKey
import kotlinx.coroutines.flow.map
import me.knighthat.colorPalette
import me.knighthat.component.Search
import me.knighthat.component.header.TabToolBar
import me.knighthat.component.tab.ItemSize
import me.knighthat.component.tab.Sort
import me.knighthat.component.tab.TabHeader
import me.knighthat.component.tab.toolbar.Randomizer
import me.knighthat.component.tab.toolbar.SongsShuffle
import me.knighthat.preference.Preference.HOME_ALBUM_ITEM_SIZE
import me.knighthat.thumbnailShape

@OptIn(ExperimentalMaterial3Api::class)
@ExperimentalTextApi
@UnstableApi
@SuppressLint("SuspiciousIndentation")
@ExperimentalFoundationApi
@ExperimentalAnimationApi
@Composable
fun HomeAlbums(
    onAlbumClick: (Album) -> Unit,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    // Essentials
    val menuState = LocalMenuState.current
    //val context = LocalContext.current
    val binder = LocalPlayerServiceBinder.current
    val lazyGridState = rememberLazyGridState()

    val disableScrollingText by rememberPreference(disableScrollingTextKey, false)

    var items by persistList<Album>( "home/albums" )

    var itemsOnDisplay by persistList<Album>( "home/albums/on_display" )

    val search = Search.init()

    val sort = Sort.init(
        albumSortOrderKey,
        AlbumSortBy.entries,
        rememberPreference(albumSortByKey, AlbumSortBy.DateAdded)
    )

    val itemSize = ItemSize.init( HOME_ALBUM_ITEM_SIZE )

    val randomizer = object: Randomizer<Album> {
        override fun getItems(): List<Album> = itemsOnDisplay
        override fun onClick(index: Int) = onAlbumClick( itemsOnDisplay[index] )
    }
    val shuffle = SongsShuffle.init {
        Database.songsInAllBookmarkedAlbums().map { it.map( Song::asMediaItem ) }
    }

    var albumType by rememberPreference(albumTypeKey, AlbumsType.Favorites )
    val buttonsList = AlbumsType.entries.map { it to it.textName }

    LaunchedEffect( sort.sortBy, sort.sortOrder, albumType ) {
        when ( albumType ) {
            AlbumsType.Favorites -> Database.albums( sort.sortBy, sort.sortOrder ).collect { items = it }
            AlbumsType.Library -> Database.albumsInLibrary( sort.sortBy, sort.sortOrder ).collect { items = it }
            //AlbumsType.All -> Database.albumsWithSongsSaved( sort.sortBy, sort.sortOrder ).collect { items = it }

        }
    }
    LaunchedEffect( items, search.input ) {
        val scrollIndex = lazyGridState.firstVisibleItemIndex
        val scrollOffset = lazyGridState.firstVisibleItemScrollOffset

        itemsOnDisplay = items.filter {
            it.title?.contains( search.input, true) ?: false
                    || it.year?.contains( search.input, true) ?: false
                    || it.authorsText?.contains( search.input, true) ?: false
        }

        lazyGridState.scrollToItem( scrollIndex, scrollOffset )
    }

    Box(
        modifier = Modifier
            .background(colorPalette().background0)
            .fillMaxHeight()
            .fillMaxWidth(
                if( NavigationBarPosition.Right.isCurrent() )
                    Dimensions.contentWidthRightBar
                else
                    1f
            )
    ) {
        Column( Modifier.fillMaxSize() ) {
            // Sticky tab's title
            TabHeader(R.string.albums) {
                HeaderInfo(items.size.toString(), R.drawable.album)
            }

            // Sticky tab's tool bar
            TabToolBar.Buttons( sort, search, randomizer, shuffle, itemSize )

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    //.padding(vertical = 4.dp)
                    .padding(bottom = 8.dp)
                    .fillMaxWidth()
            ) {
                ButtonsRow(
                    chips = buttonsList,
                    currentValue = albumType,
                    onValueUpdate = { albumType = it } ,
                    modifier = Modifier.padding(end = 12.dp)
                )
            }

            // Sticky search bar
            search.SearchBar( this )

            LazyVerticalGrid(
                state = lazyGridState,
                columns = GridCells.Adaptive( itemSize.size.dp ),
                //contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
                modifier = Modifier.background( colorPalette().background0 )
                                   .fillMaxSize(),
                contentPadding = PaddingValues( bottom = Dimensions.bottomSpacer )
            ) {
                items(
                    items = itemsOnDisplay,
                    key = Album::id
                ) { album ->
                    var songs = remember { listOf<Song>() }
                    Database.asyncQuery {
                        songs = albumSongsList(album.id)
                    }

                    var showDialogChangeAlbumTitle by remember {
                        mutableStateOf(false)
                    }
                    var showDialogChangeAlbumAuthors by remember {
                        mutableStateOf(false)
                    }
                    var showDialogChangeAlbumCover by remember {
                        mutableStateOf(false)
                    }

                    var onDismiss: () -> Unit = {}
                    var titleId = 0
                    var defValue = ""
                    var placeholderTextId: Int = 0
                    var queryBlock: (Database, String, String) -> Int = { _, _, _ -> 0}

                    if( showDialogChangeAlbumCover ) {
                        onDismiss = { showDialogChangeAlbumCover = false }
                        titleId = R.string.update_cover
                        defValue = album.thumbnailUrl.toString()
                        placeholderTextId = R.string.cover
                        queryBlock = Database::updateAlbumCover
                    } else if( showDialogChangeAlbumTitle ) {
                        onDismiss = { showDialogChangeAlbumTitle = false }
                        titleId = R.string.update_title
                        defValue = album.title.toString()
                        placeholderTextId = R.string.title
                        queryBlock = Database::updateAlbumTitle
                    } else if( showDialogChangeAlbumAuthors ) {
                        onDismiss = { showDialogChangeAlbumAuthors = false }
                        titleId = R.string.update_authors
                        defValue = album.authorsText.toString()
                        placeholderTextId = R.string.authors
                        queryBlock = Database::updateAlbumAuthors
                    }

                    if( showDialogChangeAlbumTitle || showDialogChangeAlbumAuthors || showDialogChangeAlbumCover )
                        InputTextDialog(
                            onDismiss = onDismiss,
                            title = stringResource( titleId ),
                            value = defValue,
                            placeholder = stringResource( placeholderTextId ),
                            setValue = {
                                if (it.isNotEmpty())
                                    Database.asyncTransaction { queryBlock( this, album.id, it ) }
                            },
                            prefix = MODIFIED_PREFIX
                        )

                    var position by remember {
                        mutableIntStateOf(0)
                    }
                    val context = LocalContext.current

                    AlbumItem(
                        alternative = true,
                        showAuthors = true,
                        album = album,
                        thumbnailSizeDp = itemSize.size.dp,
                        thumbnailSizePx = itemSize.size.px,
                        modifier = Modifier
                            .combinedClickable(

                                onLongClick = {
                                    menuState.display {
                                        AlbumsItemMenu(
                                            onDismiss = menuState::hide,
                                            album = album,
                                            onChangeAlbumTitle = {
                                                showDialogChangeAlbumTitle = true
                                            },
                                            onChangeAlbumAuthors = {
                                                showDialogChangeAlbumAuthors = true
                                            },
                                            onChangeAlbumCover = {
                                                showDialogChangeAlbumCover = true
                                            },
                                            onPlayNext = {
                                                println("mediaItem ${songs}")
                                                binder?.player?.addNext(
                                                    songs.map(Song::asMediaItem), context
                                                )

                                            },
                                            onEnqueue = {
                                                println("mediaItem ${songs}")
                                                binder?.player?.enqueue(
                                                    songs.map(Song::asMediaItem), context
                                                )

                                            },
                                            onAddToPlaylist = { playlistPreview ->
                                                position =
                                                    playlistPreview.songCount.minus(1) ?: 0
                                                //Log.d("mediaItem", " maxPos in Playlist $it ${position}")
                                                if (position > 0) position++ else position =
                                                    0
                                                //Log.d("mediaItem", "next initial pos ${position}")
                                                //if (listMediaItems.isEmpty()) {
                                                songs.forEachIndexed { index, song ->
                                                    Database.asyncTransaction {
                                                        insert(song.asMediaItem)
                                                        insert(
                                                            SongPlaylistMap(
                                                                songId = song.asMediaItem.mediaId,
                                                                playlistId = playlistPreview.playlist.id,
                                                                position = position + index
                                                            )
                                                        )
                                                    }
                                                    //Log.d("mediaItemPos", "added position ${position + index}")
                                                }
                                                //}
                                            },
                                            disableScrollingText = disableScrollingText
                                        )
                                    }
                                },
                                onClick = {
                                    search.onItemSelected()
                                    onAlbumClick( album )
                                }
                            )
                            .clip(thumbnailShape()),
                        disableScrollingText = disableScrollingText
                    )
                }
            }
        }

        FloatingActionsContainerWithScrollToTop( lazyGridState )

        val showFloatingIcon by rememberPreference(showFloatingIconKey, false)
        if ( UiType.ViMusic.isCurrent() && showFloatingIcon )
            MultiFloatingActionsContainer(
                iconId = R.drawable.search,
                onClick = onSearchClick,
                onClickSettings = onSettingsClick,
                onClickSearch = onSearchClick
            )
    }
}
