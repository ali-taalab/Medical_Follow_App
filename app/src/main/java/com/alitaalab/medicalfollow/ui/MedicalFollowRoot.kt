package com.alitaalab.medicalfollow.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Vaccines
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.alitaalab.medicalfollow.R
import com.alitaalab.medicalfollow.ui.screens.HomeScreen
import com.alitaalab.medicalfollow.ui.screens.ProfileScreen
import com.alitaalab.medicalfollow.ui.screens.VaccinesScreen
import com.alitaalab.medicalfollow.ui.screens.VisitDetailScreen
import com.alitaalab.medicalfollow.ui.screens.VisitEditScreen
import com.alitaalab.medicalfollow.ui.screens.VisitsScreen

object Routes {
    const val HOME = "home"
    const val VACCINES = "vaccines"
    const val VISITS = "visits"
    const val PROFILE = "profile"
    const val VISIT_DETAIL = "visit/{id}"
    const val VISIT_EDIT = "visit_edit/{id}"
    fun visitDetail(id: Long) = "visit/$id"
    fun visitEdit(id: Long = 0) = "visit_edit/$id"
}

private data class Tab(val route: String, val label: Int, val icon: ImageVector)

private val tabs = listOf(
    Tab(Routes.HOME, R.string.tab_home, Icons.Default.Home),
    Tab(Routes.VACCINES, R.string.tab_vaccines, Icons.Default.Vaccines),
    Tab(Routes.VISITS, R.string.tab_visits, Icons.Default.MedicalServices),
    Tab(Routes.PROFILE, R.string.tab_profile, Icons.Default.Person),
)

@Composable
fun MedicalFollowRoot() {
    val vm: AppViewModel = viewModel(factory = AppViewModel.Factory)
    val childState by vm.childState.collectAsStateWithLifecycle()

    when {
        !childState.loaded -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        childState.child == null -> ProfileScreen(vm = vm, child = null, isSetup = true)
        else -> MainNavigation(vm)
    }
}

private fun NavHostController.switchTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun MainNavigation(vm: AppViewModel) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBottomBar = tabs.any { it.route == currentRoute }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = { nav.switchTab(tab.route) },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(stringResource(tab.label)) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    vm = vm,
                    onOpenVaccines = { nav.switchTab(Routes.VACCINES) },
                    onOpenVisit = { nav.navigate(Routes.visitDetail(it)) },
                    onAddVisit = { nav.navigate(Routes.visitEdit()) },
                )
            }
            composable(Routes.VACCINES) { VaccinesScreen(vm) }
            composable(Routes.VISITS) {
                VisitsScreen(
                    vm = vm,
                    onOpenVisit = { nav.navigate(Routes.visitDetail(it)) },
                    onAddVisit = { nav.navigate(Routes.visitEdit()) },
                )
            }
            composable(Routes.PROFILE) {
                val childState by vm.childState.collectAsStateWithLifecycle()
                ProfileScreen(vm = vm, child = childState.child, isSetup = false)
            }
            composable(
                Routes.VISIT_DETAIL,
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) { entry ->
                val id = entry.arguments?.getLong("id") ?: 0L
                VisitDetailScreen(
                    vm = vm,
                    visitId = id,
                    onBack = { nav.popBackStack() },
                    onEdit = { nav.navigate(Routes.visitEdit(id)) },
                )
            }
            composable(
                Routes.VISIT_EDIT,
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) {
                VisitEditScreen(
                    onBack = { nav.popBackStack() },
                    onSaved = { savedId, isNew ->
                        nav.popBackStack()
                        if (isNew) nav.navigate(Routes.visitDetail(savedId))
                    },
                )
            }
        }
    }
}
