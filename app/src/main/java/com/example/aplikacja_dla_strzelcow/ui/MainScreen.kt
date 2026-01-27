package com.example.aplikacja_dla_strzelcow.ui


import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.aplikacja_dla_strzelcow.ui.navigation.AppScreen
import kotlinx.coroutines.launch
import com.example.aplikacja_dla_strzelcow.data.FirestoreRepository


@Composable
fun MainScreen(
    onLogout: () -> Unit,
    onNewTraining: () -> Unit,
    homeContent: @Composable (FirestoreRepository) -> Unit
) {
    val pagerState = rememberPagerState { AppScreen.values().size }
    val scope = rememberCoroutineScope()
    val repository = remember { FirestoreRepository() } // Tworzymy instancję repozytorium

    Scaffold(
        bottomBar = {
            NavigationBar {
                AppScreen.values().forEachIndexed { index, screen ->
                    NavigationBarItem(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) }
                    )
                }
            }
        }
    ) { padding ->

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) { page ->

            when (AppScreen.values()[page]) {
                AppScreen.HOME -> homeContent(repository)

                AppScreen.COMMUNITY -> CommunityScreen()

                AppScreen.PROFILE -> ProfileScreen(onLogout)
            }
        }
    }
}