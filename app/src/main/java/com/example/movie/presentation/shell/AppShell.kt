package com.example.movie.presentation.shell

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.movie.di.LocalAppContainer
import com.example.movie.presentation.home.HomeViewModel

@Composable
fun AppShell(
    tabName: String,
    onTabChange: (String) -> Unit,
    onMovieClick: (String) -> Unit,
    handleBack: Boolean = true,
    session: Int = 0,
    alertsRequest: Int = 0,
) {
    val tab = AppTab.valueOf(tabName)
    val container = LocalAppContainer.current
    val homeViewModel: HomeViewModel = viewModel {
        HomeViewModel(
            getHomeShelves = container.getHomeShelves,
            getShelfMovies = container.getShelfMovies,
            getRecommendations = container.getRecommendations,
            searchMovies = container.searchMovies,
            searchHistory = container.searchHistory,
            viewHistory = container.viewHistory,
            resume = container.resume,
            favorites = container.favorites,
            watched = container.watched,
        )
    }
    val homeState by homeViewModel.state.collectAsStateWithLifecycle()
    val shell = rememberShellController(
        tab = tab,
        handleBack = handleBack,
        alertsRequest = alertsRequest,
        onTabChange = onTabChange,
        homeViewModel = homeViewModel,
        homeState = homeState,
    )
    val playResume: (String) -> Unit = { id ->
        val item = container.resume.items.value.firstOrNull { it.id == id }
        if (item != null) shell.resumePlayback = item
    }
    ShellScaffold(
        tab = tab,
        shell = shell,
        homeState = homeState,
        homeViewModel = homeViewModel,
        handleBack = handleBack,
        onMovieClick = onMovieClick,
        onTabChange = onTabChange,
        session = session,
        playResume = playResume,
    )
}
