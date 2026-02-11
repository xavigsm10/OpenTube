package com.opentube.ui.navigation

/**
 * Navigation routes for the app
 */
sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Home : Screen("home")
    object Shorts : Screen("shorts")
    object Search : Screen("search")
    object VideoPlayer : Screen("video/{videoId}") {
        fun createRoute(videoId: String) = "video/$videoId"
    }
    object Channel : Screen("channel/{channelId}") {
        fun createRoute(channelId: String) = "channel/$channelId"
    }
    object Library : Screen("library")
    object Subscriptions : Screen("subscriptions")
    object History : Screen("history")
    object Favorites : Screen("favorites")
    object Playlists : Screen("playlists")
    object PlaylistDetail : Screen("playlist/{playlistId}") {
        fun createRoute(playlistId: String) = "playlist/$playlistId"
    }
    object AlbumDetail : Screen("album/{albumId}") {
        fun createRoute(albumId: String) = "album/$albumId"
    }
    object Settings : Screen("settings")
}
