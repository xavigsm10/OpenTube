package com.opentube.util

import android.content.Context
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

object LocaleManager {

    fun setLocale(languageCode: String) {
        val appLocale: LocaleListCompat = if (languageCode == "system") {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(languageCode)
        }
        AppCompatDelegate.setApplicationLocales(appLocale)
    }

    fun getCurrentLanguage(): String {
        val current = AppCompatDelegate.getApplicationLocales().get(0)
        return current?.language ?: "system"
    }
    
    fun getAvailableLanguages(): Map<String, String> {
        return mapOf(
            "system" to "System Default",
            "en" to "English",
            "es" to "Español",
            "fr" to "Français", // Example expansion
            "ru" to "Русский"   // Example expansion
        )
    }
}
