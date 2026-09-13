/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package tech.tekkiech.mussiech.extensions

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import tech.tekkiech.mussiech.constants.InnerTubeCookieKey
import tech.tekkiech.mussiech.constants.YtmSyncKey
import moe.rukamori.archivetune.innertube.utils.hasYouTubeLoginCookie
import tech.tekkiech.mussiech.utils.dataStore
import tech.tekkiech.mussiech.utils.get

fun Context.isSyncEnabled(): Boolean = dataStore.get(YtmSyncKey, true) && isUserLoggedIn()

fun Context.isUserLoggedIn(): Boolean {
    val cookie = dataStore[InnerTubeCookieKey] ?: ""
    return hasYouTubeLoginCookie(cookie) && isInternetConnected()
}

fun Context.isInternetConnected(): Boolean {
    val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
    return networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ?: false
}
