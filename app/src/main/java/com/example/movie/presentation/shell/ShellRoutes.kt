package com.example.movie.presentation.shell

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import com.example.movie.presentation.common.appBackground
import com.example.movie.presentation.home.CatalogGenresPage
import com.example.movie.presentation.home.CatalogListRoute
import com.example.movie.presentation.home.CatalogSort
import com.example.movie.presentation.home.GenreAll
import com.example.movie.presentation.home.HomeSection
import com.example.movie.presentation.home.SectionRoute
import com.example.movie.presentation.tv.remoteEntry

@Composable
internal fun ShellRoutes(
    shell: ShellController,
    onMovieClick: (String) -> Unit,
    playResume: (String) -> Unit,
) {
    val shown = shell.sectionShown
    if (shown != null) {
        SectionRoute(
            section = shown,
            onBack = { shell.sectionName = null },
            onMovieClick = if (shown == HomeSection.Continue) playResume else onMovieClick,
            onMoviePeek = { shell.peek = it },
            onHideBar = { shell.barHidden = it },
            modifier = Modifier
                .remoteEntry()
                .slideBy({ shell.sectionShift.value }, shell.stageWidth)
                .fillMaxSize()
                .appBackground(),
        )
    }
    if (shell.genresShown) {
        CatalogGenresPage(
            scrollTop = shell.catalogTop,
            type = shell.genresHold.orEmpty(),
            onBack = { shell.genresType = null },
            onGenre = { name ->
                val type = shell.genresHold ?: return@CatalogGenresPage
                shell.catalogListKey = if (name == GenreAll) {
                    "$type:popular"
                } else {
                    "$type:g:$name"
                }
            },
            modifier = Modifier
                .remoteEntry()
                .fillMaxSize()
                .graphicsLayer { alpha = shell.genresVeil.value },
        )
    }
    val opened = shell.listShown
    if (opened != null) {
        val type = opened.substringBefore(':')
        val rest = opened.substringAfter(':')
        val genreName = rest.removePrefix("g:").takeIf { rest.startsWith("g:") }
        if (genreName != null) {
            CatalogListRoute(
                scrollTop = shell.catalogTop,
                type = type,
                sort = "",
                genre = genreName,
                title = genreName,
                onBack = { shell.catalogListKey = null },
                onMovieClick = onMovieClick,
                onMoviePeek = { shell.peek = it },
                onHideBar = { shell.barHidden = it },
                modifier = Modifier
                    .remoteEntry()
                    .slideBy({ shell.listShift.value }, shell.stageWidth)
                    .fillMaxSize()
                    .appBackground(),
            )
        } else {
            val titleRes = CatalogSort.entries.firstOrNull { it.sort == rest }?.title
            if (titleRes != null) {
                CatalogListRoute(
                    scrollTop = shell.catalogTop,
                    type = type,
                    sort = rest,
                    title = stringResource(titleRes),
                    onBack = { shell.catalogListKey = null },
                    onMovieClick = onMovieClick,
                    onMoviePeek = { shell.peek = it },
                    onHideBar = { shell.barHidden = it },
                    modifier = Modifier
                        .remoteEntry()
                        .slideBy({ shell.listShift.value }, shell.stageWidth)
                        .fillMaxSize()
                        .appBackground(),
                )
            }
        }
    }
}
