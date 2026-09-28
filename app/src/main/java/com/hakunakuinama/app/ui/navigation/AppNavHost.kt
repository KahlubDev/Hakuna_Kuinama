package com.hakunakuinama.app.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.hakunakuinama.app.R
import com.hakunakuinama.app.ui.screen.DashboardScreen
import com.hakunakuinama.app.ui.screen.FavoritesScreen
import com.hakunakuinama.app.ui.screen.MenuBuilderScreen
import com.hakunakuinama.app.ui.screen.RecipeDetailScreen
import com.hakunakuinama.app.ui.viewmodel.DashboardViewModel
import com.hakunakuinama.app.ui.viewmodel.FavoritesEvent
import com.hakunakuinama.app.ui.viewmodel.FavoritesViewModel
import com.hakunakuinama.app.ui.viewmodel.GroceryListEvent
import com.hakunakuinama.app.ui.viewmodel.GroceryListViewModel
import com.hakunakuinama.app.ui.viewmodel.MenuBuilderViewModel
import com.hakunakuinama.app.ui.viewmodel.RecipeDetailEvent
import com.hakunakuinama.app.ui.viewmodel.RecipeDetailUiState
import com.hakunakuinama.app.ui.viewmodel.RecipeDetailViewModel

private val bottomBarRouteStrings: Set<String> =
    bottomNavigationRoutes.mapTo(mutableSetOf()) { it.route }

/**
 * The whole navigation graph: four destinations, one Scaffold, three bottom-bar tabs.
 *
 * Every destination is a private `…Route` composable that owns its ViewModel and whatever
 * local state it needs, then hands a plain state object plus callbacks to a stateless
 * screen. That split is what makes the screens previewable and screenshot-testable
 * without Hilt, a database, or a clock.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HakunaApp(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            // The recipe screen is pushed *over* the tabs, so the bar is hidden there.
            // Showing it would let a user tap "Favorites" and lose their place in the
            // recipe they were reading.
            if (currentRoute in bottomBarRouteStrings) {
                // Flat, no tonal elevation: the design draws the bar in the page's own
                // surface tone with a hairline, and M3's default container tint fights
                // that. The indicator pill is suppressed for the same reason — the
                // teal icon and label carry the selection on their own.
                Column {
                    // The rule the design draws along the top of the bar. It is a Column
                    // and not a Surface border because Material's NavigationBar clips its
                    // own content, and a border drawn on the bar itself is half a pixel off
                    // the screen edge.
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    NavigationBar(
                        // `surfaceContainer`, not a hardcoded near-white. The mockups draw
                        // the bar a hair off the cards' white, and `surfaceContainer` is
                        // the role for exactly that: a surface above `surface` with no
                        // shadow. Naming a light hex here is what pinned the bar to light
                        // mode, because a Color has no dark-mode counterpart to swap to.
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        contentColor = MaterialTheme.colorScheme.primary,
                        tonalElevation = 0.dp,
                    ) {
                        bottomNavigationRoutes.forEach { route ->
                            val selected = currentRoute == route.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navController.navigateToTab(route) },
                                icon = {
                                    Icon(
                                        // Outlined, not filled: the design's icons are
                                        // hairline outlines, and a filled heart next to two
                                        // outlined glyphs reads as a different tab set.
                                        imageVector = route.icon(),
                                        contentDescription = null,
                                        tint = if (selected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(22.dp),
                                    )
                                },
                                label = {
                                    Text(
                                        text = stringResource(route.labelRes()),
                                        // labelMedium, not the M3 default: the bar's own
                                        // 12sp "Menu builder" has to fit under its icon
                                        // without wrapping to two lines on a 360dp screen.
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (selected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    // The indicator pill is suppressed: the mockup draws no
                                    // pill, and the teal icon and label carry the selection.
                                    indicatorColor = Color.Transparent,
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                ),
                            )
                        }
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Route.Home.route,
            modifier = Modifier.padding(padding),
        ) {
            composable(route = Route.Home.route) {
                DashboardRoute(
                    onOpenRecipe = { id -> navController.navigate(Route.RecipeDetail.build(id)) },
                    // A tab, not a push: the FAB and the bar's own item must land on the
                    // same back-stack entry, or BACK has to be pressed twice to leave a
                    // screen the user entered once.
                    onOpenBuilder = { navController.navigateToTab(Route.MenuBuilder) },
                )
            }

            composable(route = Route.MenuBuilder.route) {
                MenuBuilderRoute(
                    onOpenRecipe = { id -> navController.navigate(Route.RecipeDetail.build(id)) },
                )
            }

            composable(route = Route.Favorites.route) {
                FavoritesRoute(
                    onOpenRecipe = { id -> navController.navigate(Route.RecipeDetail.build(id)) },
                    snackbarHostState = snackbarHostState,
                )
            }

            composable(
                route = Route.RecipeDetail.route,
                arguments = listOf(
                    // The key comes from the shared constant, and LongType matters: the
                    // ViewModel reads the argument as a Long, so an IntType route would
                    // throw on retrieval instead of merely rendering nothing.
                    navArgument(ARG_RECIPE_ID) { type = NavType.LongType },
                ),
            ) {
                RecipeDetailRoute(
                    onBack = { navController.popBackStack() },
                    snackbarHostState = snackbarHostState,
                )
            }
        }
    }
}

// ------------------------------------------------------------------ tab chrome

@androidx.annotation.StringRes
private fun Route.labelRes(): Int = when (this) {
    Route.Home -> R.string.nav_home
    Route.MenuBuilder -> R.string.nav_builder
    Route.Favorites -> R.string.nav_favorites
    Route.RecipeDetail -> R.string.nav_home // never shown: not a tab
}

private fun Route.icon(): ImageVector = when (this) {
    Route.Home -> Icons.Outlined.Home
    // A 2x2 grid, which is what the mockup draws for the builder — a fork-and-knife says
    // "recipes", and the builder is not a recipe list, it is a pantry.
    Route.MenuBuilder -> Icons.Outlined.GridView
    Route.Favorites -> Icons.Outlined.Favorite
    Route.RecipeDetail -> Icons.Outlined.Home
}

// ------------------------------------------------------------------ destinations

@Composable
private fun DashboardRoute(
    onOpenRecipe: (Long) -> Unit,
    onOpenBuilder: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // No snackbar host and no event collection: the redesign gives the dashboard no way to
    // write anything. The hearts that used to live on the hero card and the saved-recipes
    // row are gone from this screen, so the only favourite control is on the recipe and on
    // the Favorites tab — and a favourite failure there is already reported by whichever
    // route owns the control the user actually pressed.
    DashboardScreen(
        uiState = uiState,
        onOpenRecipe = onOpenRecipe,
        onOpenBuilder = onOpenBuilder,
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
private fun MenuBuilderRoute(
    onOpenRecipe: (Long) -> Unit,
    viewModel: MenuBuilderViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // The ingredient selection is the ViewModel's business. Only the filter text is
    // view state, so rememberSaveable is exactly enough for it.
    var filterText by rememberSaveable { mutableStateOf("") }

    MenuBuilderScreen(
        uiState = uiState,
        filterText = filterText,
        onFilterTextChange = { filterText = it },
        onToggleIngredient = viewModel::onIngredientToggled,
        onClearSelection = viewModel::onSelectionCleared,
        onOpenRecipe = onOpenRecipe,
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
private fun FavoritesRoute(
    onOpenRecipe: (Long) -> Unit,
    snackbarHostState: SnackbarHostState,
    viewModel: FavoritesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // Captured at composition: a coroutine body is not a composable scope, so it must
    // not call stringResource() itself. The meal name is only known at event time,
    // hence Context.getString with a format argument.
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is FavoritesEvent.Removed ->
                    snackbarHostState.showSnackbar(
                        context.getString(R.string.favorites_removed, event.mealName),
                    )

                FavoritesEvent.RemoveFailed ->
                    snackbarHostState.showSnackbar(
                        context.getString(R.string.favorites_remove_failed),
                    )
            }
        }
    }

    FavoritesScreen(
        uiState = uiState,
        onRemove = viewModel::onRemove,
        onOpenRecipe = onOpenRecipe,
        modifier = Modifier.fillMaxSize(),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecipeDetailRoute(
    onBack: () -> Unit,
    snackbarHostState: SnackbarHostState,
    viewModel: RecipeDetailViewModel = hiltViewModel(),
    groceryViewModel: GroceryListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val groceryState by groceryViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // The ingredient checklist is deliberately *not* saved across configuration changes:
    // rememberSaveable would try to put a Set<String> into a Bundle, which fails at save
    // time. It is a within-session convenience, so plain remember is the honest choice.
    var checkedNames by remember { mutableStateOf(emptySet<String>()) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is RecipeDetailEvent.FavoriteChanged -> snackbarHostState.showSnackbar(
                    context.getString(
                        if (event.isFavourite) {
                            R.string.action_favorite_add
                        } else {
                            R.string.action_favorite_remove
                        },
                    ),
                )

                RecipeDetailEvent.FavoriteFailed -> snackbarHostState.showSnackbar(
                    context.getString(R.string.favorites_remove_failed),
                )
            }
        }
    }

    LaunchedEffect(groceryViewModel) {
        groceryViewModel.events.collect { event ->
            when (event) {
                is GroceryListEvent.ListGenerated -> snackbarHostState.showSnackbar(
                    context.resources.getQuantityString(
                        R.plurals.grocery_added,
                        event.lineCount,
                        event.lineCount,
                    ),
                )

                GroceryListEvent.GenerateFailed -> snackbarHostState.showSnackbar(
                    context.getString(R.string.grocery_failed),
                )
            }
        }
    }

    val meal = (uiState as? RecipeDetailUiState.Ready)?.meal

    // No topBar: the design shows only a floating back arrow over the artwork, which
    // RecipeDetailScreen owns. The Scaffold stays because it hosts the snackbar, and
    // because the system back gesture is the one affordance that works in every state.
    Scaffold { padding ->
        RecipeDetailScreen(
            uiState = uiState,
            groceryState = groceryState,
            checkedIngredientNames = checkedNames,
            onIngredientChecked = { name, checked ->
                checkedNames = if (checked) checkedNames + name else checkedNames - name
            },
            onToggleFavorite = viewModel::onFavoriteClicked,
            onGenerateShoppingList = {
                // No-op until the recipe has loaded: there is no id to generate from, and
                // silently doing nothing is better than generating a list for meal 0.
                meal?.let { groceryViewModel.onGenerateForRecipe(it.id) }
            },
            onGroceryItemChecked = groceryViewModel::onItemChecked,
            onSheetRequested = groceryViewModel::onSheetRequested,
            onSheetDismissed = groceryViewModel::onSheetDismissed,
            onBack = onBack,
            modifier = Modifier.fillMaxSize(),
            contentPadding = padding,
        )
    }
}
