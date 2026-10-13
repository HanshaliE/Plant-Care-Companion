package com.example.plantcarecompanion.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

fun interface ConnectivityChecker {
    fun isOnline(): Boolean
}

class AndroidConnectivityChecker(context: Context) : ConnectivityChecker {

    private val appContext = context.applicationContext

    override fun isOnline(): Boolean {
        val cm = appContext.getSystemService(ConnectivityManager::class.java) ?: return true
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
