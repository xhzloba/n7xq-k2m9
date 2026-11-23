package com.example.movie.presentation.shell

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.movie.R

internal enum class AppTab(@StringRes val label: Int, val color: Color) {
    Home(R.string.nav_home, Color(0xFF3B6EF5)),
    Catalog(R.string.nav_catalog, Color(0xFF14A36A)),
    Favorites(R.string.nav_favorites, Color(0xFFE23B5A)),
    Settings(R.string.nav_settings, Color(0xFFD4920A)),
    ;

    val icon: ImageVector
        get() = when (this) {
            Home -> ShellIcons.home
            Catalog -> ShellIcons.catalog
            Favorites -> ShellIcons.media
            Settings -> ShellIcons.settings
        }
}
