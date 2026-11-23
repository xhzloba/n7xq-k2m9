package com.example.movie.presentation.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import com.example.movie.presentation.common.appBackground
import com.example.movie.presentation.home.HomeUiState
import com.example.movie.presentation.home.HomeViewModel

@Composable
internal fun ShellScaffold(
    tab: AppTab,
    shell: ShellController,
    homeState: HomeUiState,
    homeViewModel: HomeViewModel,
    handleBack: Boolean,
    onMovieClick: (String) -> Unit,
    onTabChange: (String) -> Unit,
    session: Int,
    playResume: (String) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
            .appBackground(),
    ) {
        ShellPager(
            tab = tab,
            shell = shell,
            handleBack = handleBack,
            onMovieClick = onMovieClick,
            onTabChange = onTabChange,
            playResume = playResume,
            session = session,
            homeViewModel = homeViewModel,
        )
        ShellRoutes(
            shell = shell,
            onMovieClick = onMovieClick,
            playResume = playResume,
        )
        ShellChrome(
            tab = tab,
            shell = shell,
            homeState = homeState,
            homeViewModel = homeViewModel,
            onMovieClick = onMovieClick,
            onTabChange = onTabChange,
            playResume = playResume,
        )
    }
}
