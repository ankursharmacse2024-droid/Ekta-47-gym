package com.example.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

enum class CloudSyncState(val label: String) {
    SYNCED("Cloud Synced"),
    SYNCING("Synchronizing…"),
    OFFLINE("Offline Mode • Local Cache Active"),
    FAILED("Sync Error")
}

class NetworkMonitor(context: Context) {
    private val connectivityManager =
        context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    private val _isOnline = MutableStateFlow(checkInitialConnectivity())
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _syncState = MutableStateFlow(
        if (_isOnline.value) CloudSyncState.SYNCED else CloudSyncState.OFFLINE
    )
    val syncState: StateFlow<CloudSyncState> = _syncState.asStateFlow()

    private val _lastSyncTimeMillis = MutableStateFlow(System.currentTimeMillis())
    val lastSyncTimeMillis: StateFlow<Long> = _lastSyncTimeMillis.asStateFlow()

    private val istDateFormat = SimpleDateFormat("hh:mm a", Locale.ENGLISH).apply {
        timeZone = TimeZone.getTimeZone("Asia/Kolkata")
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            _isOnline.value = true
            _syncState.value = CloudSyncState.SYNCING
            // Once connected, record sync
            _lastSyncTimeMillis.value = System.currentTimeMillis()
            _syncState.value = CloudSyncState.SYNCED
        }

        override fun onLost(network: Network) {
            _isOnline.value = false
            _syncState.value = CloudSyncState.OFFLINE
        }

        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            _isOnline.value = hasInternet
            if (hasInternet && _syncState.value == CloudSyncState.OFFLINE) {
                _syncState.value = CloudSyncState.SYNCED
                _lastSyncTimeMillis.value = System.currentTimeMillis()
            } else if (!hasInternet) {
                _syncState.value = CloudSyncState.OFFLINE
            }
        }
    }

    init {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager?.registerNetworkCallback(request, networkCallback)
    }

    private fun checkInitialConnectivity(): Boolean {
        val cm = connectivityManager ?: return false
        val activeNetwork = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun markSyncing() {
        if (_isOnline.value) {
            _syncState.value = CloudSyncState.SYNCING
        }
    }

    fun markSyncSuccess() {
        _lastSyncTimeMillis.value = System.currentTimeMillis()
        _syncState.value = if (_isOnline.value) CloudSyncState.SYNCED else CloudSyncState.OFFLINE
    }

    fun markSyncFailed() {
        _syncState.value = if (_isOnline.value) CloudSyncState.FAILED else CloudSyncState.OFFLINE
    }

    fun getFormattedLastSyncTime(): String {
        return "Last synced: " + istDateFormat.format(Date(_lastSyncTimeMillis.value)) + " IST"
    }
}
