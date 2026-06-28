package com.musx.a1

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.musx.a1.data.AppDatabase
import com.musx.a1.repository.AppRepository
import com.musx.a1.ui.screens.bookmarks.BookmarksScreen
import com.musx.a1.ui.screens.bookmarks.BookmarksViewModel
import com.musx.a1.ui.screens.playlists.PlaylistDetailScreen
import com.musx.a1.ui.screens.playlists.PlaylistsScreen
import com.musx.a1.ui.screens.playlists.PlaylistsViewModel
import com.musx.a1.ui.screens.dashboard.DashboardScreen
import com.musx.a1.ui.screens.library.LibraryScreen
import com.musx.a1.ui.screens.library.LibraryViewModel
import com.musx.a1.ui.screens.onboarding.WelcomeScreen
import com.musx.a1.ui.screens.player.PlayerScreen
import com.musx.a1.ui.screens.player.PlayerViewModel
import com.musx.a1.ui.screens.settings.SettingsScreen
import com.musx.a1.ui.screens.settings.SettingsViewModel
import com.musx.a1.ui.theme.MusxA1Theme
import com.musx.a1.ui.state.AppState
import com.musx.a1.ui.components.FullScreenLoading
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private val _appState = MutableStateFlow<AppState>(AppState.Idle)
    val appState: StateFlow<AppState> = _appState
    private var externalPdfUri by mutableStateOf<android.net.Uri?>(null)

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        handleIntent(intent)

        setContent {
            MusxA1Theme {
                val scope = rememberCoroutineScope()
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
                val repository = remember { AppRepository(db.bookDao(), db.folderDao(), db.playlistDao(), db.progressDao(), db.chapterDao(), db.bookmarkDao()) }

                val sheetState = rememberModalBottomSheetState()
                var showImportSheet by remember { mutableStateOf(false) }

                LaunchedEffect(externalPdfUri) {
                    if (externalPdfUri != null) {
                        showImportSheet = true
                    }
                }

                if (showImportSheet && externalPdfUri != null) {
                    ModalBottomSheet(
                        onDismissRequest = {
                            showImportSheet = false
                            externalPdfUri = null
                        },
                        sheetState = sheetState
                    ) {
                        com.musx.a1.ui.screens.importer.ImportBottomSheet(
                            fileName = "External PDF",
                            onListenNow = {
                                val uriString = externalPdfUri.toString()
                                showImportSheet = false
                                externalPdfUri = null
                                navController.navigate("player?uri=${android.net.Uri.encode(uriString)}")
                            },
                            onAddToLibrary = {
                                val uri = externalPdfUri
                                showImportSheet = false
                                externalPdfUri = null
                                if (uri != null) {
                                    scope.launch {
                                        _appState.value = AppState.ParsingPdf
                                        repository.insertBook(com.musx.a1.data.entity.Book(
                                            title = "Imported PDF",
                                            filePath = uri.toString(),
                                            totalPages = withContext(Dispatchers.IO) {
                                                com.musx.a1.engine.PdfParser(this@MainActivity).getPageCount(uri.toString())
                                            },
                                            coverImage = null
                                        ))
                                        _appState.value = AppState.Ready
                                    }
                                }
                            },
                            onCancel = {
                                showImportSheet = false
                                externalPdfUri = null
                            }
                        )
                    }
                }

                val currentAppState by appState.collectAsState()

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
                        val showBottomBar = when {
                            currentRoute == "welcome" -> false
                            currentRoute == "onboarding_folder" -> false
                            currentRoute?.startsWith("player") == true -> false
                            currentRoute == "settings" -> false
                            else -> true
                        }
                        if (showBottomBar) {
                            BottomNavigationBar(navController)
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        AppNavigation(
                            navController = navController,
                            repository = repository,
                            db = db,
                            onUpdateAppState = { _appState.value = it }
                        )
                        FullScreenLoading(state = currentAppState)
                    }
                }
            }
        }
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        when (intent.action) {
            Intent.ACTION_VIEW -> {
                externalPdfUri = intent.data
            }
            Intent.ACTION_SEND -> {
                if ("application/pdf" == intent.type) {
                    externalPdfUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(Intent.EXTRA_STREAM, android.net.Uri::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(Intent.EXTRA_STREAM)
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    NavigationBar {
        val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
        NavigationBarItem(
            selected = currentRoute == "dashboard" || currentRoute == "playlists",
            onClick = { navController.navigate("dashboard") },
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            label = { Text("Home") }
        )
        NavigationBarItem(
            selected = currentRoute == "library",
            onClick = { navController.navigate("library") },
            icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Library") },
            label = { Text("Library") }
        )
        NavigationBarItem(
            selected = currentRoute == "bookmarks",
            onClick = { navController.navigate("bookmarks") },
            icon = { Icon(Icons.Default.FavoriteBorder, contentDescription = "Favorites") },
            label = { Text("Favorites") }
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
    onUpdateAppState: (AppState) -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(navController = navController, startDestination = "welcome", modifier = modifier) {
        composable("dashboard") {
            val libraryViewModel: LibraryViewModel = viewModel(factory = ViewModelFactory(repository, db))
            val playerViewModel: PlayerViewModel = viewModel(factory = ViewModelFactory(repository, db))
            val playlistsViewModel: PlaylistsViewModel = viewModel(factory = ViewModelFactory(repository, db))
            val context = androidx.compose.ui.platform.LocalContext.current
            LaunchedEffect(Unit) { playerViewModel.initializeController(context) }
            DashboardScreen(
                libraryViewModel = libraryViewModel,
                playerViewModel = playerViewModel,
                playlistsViewModel = playlistsViewModel,
                onSettingsClick = { navController.navigate("settings") },
                onLibraryClick = { navController.navigate("library") },
                onPlayerClick = { navController.navigate("player") },
                onPlaylistsClick = { navController.navigate("playlists") },
                onFavoritesClick = { navController.navigate("library/1") },
                onRecentsClick = { navController.navigate("library/2") }
            )
        }
        composable("welcome") {
            WelcomeScreen { navController.navigate("onboarding_folder") }
        }
        composable("onboarding_folder") {
            val context = androidx.compose.ui.platform.LocalContext.current
            val scope = rememberCoroutineScope()
            com.musx.a1.ui.screens.onboarding.FolderAccessScreen { uri ->
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                scope.launch {
                    onUpdateAppState(AppState.ScanningFolders)
                    repository.insertFolder(com.musx.a1.data.entity.Folder(uri = uri.toString()))
                    val scanner = com.musx.a1.engine.scanner.FolderScanner(
                        context,
                        repository,
                        com.musx.a1.engine.PdfParser(context)
                    )
                    scanner.scanFolder(uri.toString())
                    onUpdateAppState(AppState.Ready)
                    withContext(Dispatchers.Main) {
                        navController.navigate("dashboard")
                    }
                }
            }
        }
        composable("library") {
            val viewModel: LibraryViewModel = viewModel(factory = ViewModelFactory(repository, db))
            val state by viewModel.appState.collectAsState()
            Box {
                LibraryScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onSettingsClick = { navController.navigate("settings") },
                    onBookClick = { book ->
                        navController.navigate("player/${book.id}")
                    }
                )
                FullScreenLoading(state = state)
            }
        }
        composable("library/{tabIndex}") { backStackEntry ->
            val tabIndex = backStackEntry.arguments?.getString("tabIndex")?.toInt() ?: 0
            val viewModel: LibraryViewModel = viewModel(factory = ViewModelFactory(repository, db))
            val state by viewModel.appState.collectAsState()
            Box {
                LibraryScreen(
                    viewModel = viewModel,
                    initialTab = tabIndex,
                    onBack = { navController.popBackStack() },
                    onSettingsClick = { navController.navigate("settings") },
                    onBookClick = { book ->
                        navController.navigate("player/${book.id}")
                    }
                )
                FullScreenLoading(state = state)
            }
        }
        composable("player?uri={uri}") { backStackEntry ->
            val uri = backStackEntry.arguments?.getString("uri")
            val viewModel: PlayerViewModel = viewModel(factory = ViewModelFactory(repository, db))
            val state by viewModel.appState.collectAsState()
            val context = androidx.compose.ui.platform.LocalContext.current
            LaunchedEffect(Unit) { viewModel.initializeController(context) }
            LaunchedEffect(uri) {
                if (uri != null) {
                    viewModel.loadExternalUri(android.net.Uri.decode(uri))
                }
            }
            Box {
                PlayerScreen(viewModel, onBack = { navController.popBackStack() })
                FullScreenLoading(state = state)
            }
        }
        composable("player") {
            val viewModel: PlayerViewModel = viewModel(factory = ViewModelFactory(repository, db))
            val state by viewModel.appState.collectAsState()
            val context = androidx.compose.ui.platform.LocalContext.current
            LaunchedEffect(Unit) { viewModel.initializeController(context) }
            Box {
                PlayerScreen(viewModel, onBack = { navController.popBackStack() })
                FullScreenLoading(state = state)
            }
        }
        composable("player/{bookId}") { backStackEntry ->
            val bookId = backStackEntry.arguments?.getString("bookId")?.toLong() ?: 0L
            val viewModel: PlayerViewModel = viewModel(factory = ViewModelFactory(repository, db))
            val state by viewModel.appState.collectAsState()
            val context = androidx.compose.ui.platform.LocalContext.current
            LaunchedEffect(Unit) { viewModel.initializeController(context) }
            LaunchedEffect(bookId) { viewModel.loadBook(bookId) }
            Box {
                PlayerScreen(viewModel, onBack = { navController.popBackStack() })
                FullScreenLoading(state = state)
            }
        }
        composable("bookmarks") {
            val viewModel: BookmarksViewModel = viewModel(factory = ViewModelFactory(repository, db))
            BookmarksScreen(viewModel)
        }
        composable("playlists") {
            val viewModel: PlaylistsViewModel = viewModel(factory = ViewModelFactory(repository, db))
            PlaylistsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onPlaylistClick = { playlist ->
                    navController.navigate("playlist_detail/${playlist.id}/${playlist.name}")
                }
            )
        }
        composable("playlist_detail/{playlistId}/{playlistName}") { backStackEntry ->
            val playlistId = backStackEntry.arguments?.getString("playlistId")?.toLong() ?: 0L
            val playlistName = backStackEntry.arguments?.getString("playlistName") ?: "Playlist"
            val books by repository.getBooksInPlaylist(playlistId).collectAsState(initial = emptyList())

            PlaylistDetailScreen(
                playlistName = playlistName,
                books = books,
                onBack = { navController.popBackStack() },
                onBookClick = { book -> navController.navigate("player/${book.id}") },
                onPlayAll = { if (books.isNotEmpty()) navController.navigate("player/${books[0].id}") }
            )
        }
        composable("settings") {
            val viewModel: SettingsViewModel = viewModel(factory = ViewModelFactory(repository, db))
            SettingsScreen(viewModel, onBack = { navController.popBackStack() })
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
            modelClass.isAssignableFrom(BookmarksViewModel::class.java) -> BookmarksViewModel(repository) as T
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> SettingsViewModel(repository) as T
            modelClass.isAssignableFrom(PlaylistsViewModel::class.java) -> PlaylistsViewModel(repository) as T
            else -> throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
