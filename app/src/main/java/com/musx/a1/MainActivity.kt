package com.musx.a1

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.musx.a1.data.AppDatabase
import com.musx.a1.repository.AppRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import android.content.Intent
import com.musx.a1.ui.screens.bookmarks.BookmarksScreen
import com.musx.a1.ui.screens.bookmarks.BookmarksViewModel
import com.musx.a1.ui.screens.library.LibraryScreen
import com.musx.a1.ui.screens.library.LibraryViewModel
import com.musx.a1.ui.screens.onboarding.WelcomeScreen
import com.musx.a1.ui.screens.player.PlayerScreen
import com.musx.a1.ui.screens.player.PlayerViewModel
import com.musx.a1.ui.screens.settings.SettingsScreen
import com.musx.a1.ui.screens.settings.SettingsViewModel
import com.musx.a1.ui.theme.MusxA1Theme

class MainActivity : ComponentActivity() {
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MusxA1Theme {
                val permissionsToRequest = mutableListOf<String>()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissionsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES)
                    permissionsToRequest.add(Manifest.permission.READ_MEDIA_AUDIO)
                    permissionsToRequest.add(Manifest.permission.READ_MEDIA_VIDEO)
                    permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
                }

                val launcher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { _ -> }

                LaunchedEffect(Unit) {
                    launcher.launch(permissionsToRequest.toTypedArray())
                }

                val navController = rememberNavController()
                val db = AppDatabase.getDatabase(this)
                val repository = remember { AppRepository(db.bookDao(), db.folderDao()) }

                Scaffold(
                    bottomBar = {
                        val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
                        if (currentRoute != "welcome") {
                            BottomNavigationBar(navController)
                        }
                    }
                ) { innerPadding ->
                    AppNavigation(
                        navController = navController,
                        repository = repository,
                        modifier = Modifier.padding(innerPadding),
                        db = db
                    )
                }
            }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    NavigationBar {
        val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
        NavigationBarItem(
            selected = currentRoute == "library",
            onClick = { navController.navigate("library") },
            icon = { Icon(Icons.Default.Home, contentDescription = "Library") },
            label = { Text("Library") }
        )
        NavigationBarItem(
            selected = currentRoute == "player",
            onClick = { navController.navigate("player") },
            icon = { Icon(Icons.Default.PlayArrow, contentDescription = "Player") },
            label = { Text("Player") }
        )
        NavigationBarItem(
            selected = currentRoute == "bookmarks",
            onClick = { navController.navigate("bookmarks") },
            icon = { Icon(Icons.Default.List, contentDescription = "Bookmarks") },
            label = { Text("Bookmarks") }
        )
        NavigationBarItem(
            selected = currentRoute == "settings",
            onClick = { navController.navigate("settings") },
            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
            label = { Text("Settings") }
        )
    }
}

@Composable
fun AppNavigation(
    navController: NavHostController,
    repository: AppRepository,
    db: AppDatabase,
    modifier: Modifier = Modifier
) {
    NavHost(navController = navController, startDestination = "welcome", modifier = modifier) {
        composable("welcome") {
            WelcomeScreen { navController.navigate("onboarding_folder") }
        }
        composable("onboarding_folder") {
            val context = androidx.compose.ui.platform.LocalContext.current
            val scope = androidx.compose.runtime.rememberCoroutineScope()
            com.musx.a1.ui.screens.onboarding.FolderAccessScreen { uri ->
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                scope.launch {
                    repository.insertFolder(com.musx.a1.data.entity.Folder(uri = uri.toString()))
                    val scanner = com.musx.a1.engine.scanner.FolderScanner(
                        context,
                        repository,
                        com.musx.a1.engine.PdfParser(context)
                    )
                    scanner.scanFolder(uri.toString())
                }
                navController.navigate("library")
            }
        }
        composable("library") {
            val viewModel: LibraryViewModel = viewModel(factory = ViewModelFactory(repository, db))
            LibraryScreen(viewModel) { book ->
                navController.navigate("player/${book.id}")
            }
        }
        composable("player") {
            val viewModel: PlayerViewModel = viewModel(factory = ViewModelFactory(repository, db))
            val context = androidx.compose.ui.platform.LocalContext.current
            LaunchedEffect(Unit) { viewModel.initializeController(context) }
            PlayerScreen(viewModel)
        }
        composable("player/{bookId}") { backStackEntry ->
            val bookId = backStackEntry.arguments?.getString("bookId")?.toLong() ?: 0L
            val viewModel: PlayerViewModel = viewModel(factory = ViewModelFactory(repository, db))
            val context = androidx.compose.ui.platform.LocalContext.current
            LaunchedEffect(Unit) { viewModel.initializeController(context) }
            LaunchedEffect(bookId) { viewModel.loadBook(bookId) }
            PlayerScreen(viewModel)
        }
        composable("bookmarks") {
            val viewModel: BookmarksViewModel = viewModel(factory = ViewModelFactory(repository, db))
            BookmarksScreen(viewModel)
        }
        composable("settings") {
            val viewModel: SettingsViewModel = viewModel(factory = ViewModelFactory(repository, db))
            SettingsScreen(viewModel)
        }
    }
}

class ViewModelFactory(
    private val repository: AppRepository,
    private val db: AppDatabase
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(LibraryViewModel::class.java) -> LibraryViewModel(repository) as T
            modelClass.isAssignableFrom(PlayerViewModel::class.java) -> PlayerViewModel(repository) as T
            modelClass.isAssignableFrom(BookmarksViewModel::class.java) -> BookmarksViewModel(db.bookmarkDao()) as T
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> SettingsViewModel(repository) as T
            else -> throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
