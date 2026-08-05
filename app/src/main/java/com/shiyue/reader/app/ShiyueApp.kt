package com.shiyue.reader.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.shiyue.reader.app.navigation.ShiyueDestination
import com.shiyue.reader.app.navigation.ShiyueRoutes
import com.shiyue.reader.feature.bookedit.AddBookRoute
import com.shiyue.reader.feature.bookshelf.BookshelfRoute
import com.shiyue.reader.feature.note.NoteScreen
import com.shiyue.reader.feature.reading.ReadingScreen
import com.shiyue.reader.feature.review.ReviewScreen

@Composable
fun ShiyueApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = ShiyueDestination.entries.any { it.route == currentRoute }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    ShiyueDestination.entries.forEach { destination ->
                        val selected = currentRoute == destination.route
                        val label = stringResource(destination.labelRes)
                        val isReadingEntry = destination == ShiyueDestination.Reading

                        NavigationBarItem(
                            modifier = Modifier.semantics {
                                contentDescription = label
                            },
                            selected = selected,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(ShiyueDestination.Bookshelf.route) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Box(
                                    modifier = Modifier
                                        .size(if (isReadingEntry) 40.dp else 32.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                selected -> MaterialTheme.colorScheme.secondaryContainer
                                                isReadingEntry -> MaterialTheme.colorScheme.tertiaryContainer
                                                else -> MaterialTheme.colorScheme.surfaceContainer
                                            },
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        painter = painterResource(destination.iconRes),
                                        contentDescription = null,
                                        modifier = Modifier.size(if (isReadingEntry) 25.dp else 22.dp),
                                    )
                                }
                            },
                            label = { Text(label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.surfaceContainer,
                                unselectedIconColor = if (isReadingEntry) {
                                    MaterialTheme.colorScheme.onTertiaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                unselectedTextColor = if (isReadingEntry) {
                                    MaterialTheme.colorScheme.tertiary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            ),
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = ShiyueDestination.Bookshelf.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            composable(ShiyueDestination.Bookshelf.route) {
                BookshelfRoute(
                    onAddBook = { navController.navigate(ShiyueRoutes.AddBook) },
                )
            }
            composable(ShiyueDestination.Reading.route) { ReadingScreen() }
            composable(ShiyueDestination.Note.route) { NoteScreen() }
            composable(ShiyueDestination.Review.route) { ReviewScreen() }
            composable(ShiyueRoutes.AddBook) {
                AddBookRoute(onBack = { navController.popBackStack() })
            }
        }
    }
}
