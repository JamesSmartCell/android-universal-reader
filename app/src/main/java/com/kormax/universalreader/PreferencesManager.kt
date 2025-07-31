// In a new file, e.g., PreferencesManager.kt
package com.kormax.universalreader

import android.content.Context
import android.content.SharedPreferences

object PreferencesManager {
    private const val PREFS_NAME = "app_prefs"
    private const val KEY_ETHEREUM_ADDRESS = "ethereum_address"

    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveEthereumAddress(context: Context, address: String) {
        getPreferences(context).edit().putString(KEY_ETHEREUM_ADDRESS, address).apply()
    }

    fun getEthereumAddress(context: Context): String {
        return getPreferences(context).getString(KEY_ETHEREUM_ADDRESS, "") ?: ""
    }
}