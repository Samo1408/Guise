package com.houvven.guise.ui.routing

import android.annotation.SuppressLint
import android.net.Uri
import androidx.activity.BackEventCompat
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.houvven.guise.db.Template
import com.houvven.guise.ui.routing.editor.AddTemplateScreen
import com.houvven.guise.ui.routing.editor.EditTemplateScreen
import com.houvven.guise.ui.routing.launcher.LauncherRoute
import com.houvven.guise.ui.routing.template.EnableTemplateScreen
import com.houvven.guise.ui.theme.predictiveBack

@SuppressLint("StaticFieldLeak")
object LocalNavController {
    lateinit var current: NavHostController
}

@Composable
fun NavigationRoute() {
    val navController = rememberNavController()
    val predictiveBackEnabled by predictiveBack
    val activity = LocalActivity.current
    LocalNavController.current = navController
    BackHandler(
        enabled = !predictiveBackEnabled,
    ) {
        if (!navController.popBackStack()) activity?.finish()
    }

    NavHost(
        navController = navController,
        startDestination = NavRoutingTypes.LAUNCHER.name,
        enterTransition = {
            if (predictiveBackEnabled) {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(durationMillis = 220),
                )
            } else {
                EnterTransition.None
            }
        },
        exitTransition = { ExitTransition.None },
        popEnterTransition = {
            EnterTransition.None
        },
        popExitTransition = {
            if (predictiveBackEnabled) {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(durationMillis = 220),
                )
            } else {
                ExitTransition.None
            }
        },
        predictivePopEnterTransition = { EnterTransition.None },
        predictivePopExitTransition = { swipeEdge ->
            if (predictiveBackEnabled) {
                slideOutOfContainer(
                    towards = if (swipeEdge == BackEventCompat.EDGE_RIGHT) {
                        AnimatedContentTransitionScope.SlideDirection.Right
                    } else {
                        AnimatedContentTransitionScope.SlideDirection.Left
                    },
                    animationSpec = tween(
                        durationMillis = 220,
                        // Keep page displacement proportional to predictive-back progress.
                        easing = LinearEasing,
                    ),
                )
            } else {
                ExitTransition.None
            }
        },
    ) {
        composable(NavRoutingTypes.LAUNCHER.name) { LauncherRoute() }

        composable(NavRoutingTypes.ADD_TEMPLATE.name) { AddTemplateScreen() }

        composable(
            route = "${NavRoutingTypes.EDIT_TEMPLATE.name}/{template}",
            arguments = listOf(navArgument("template") { type = NavType.StringType })
        ) {
            val template = Template.deserialization(it.arguments!!.getString("template")!!)
            EditTemplateScreen(template)
        }

        composable(
            route = "${NavRoutingTypes.ENABLE_TEMPLATE.name}/{template}",
            arguments = listOf(navArgument("template") { type = NavType.StringType })
        ) {
            val template = Template.deserialization(it.arguments!!.getString("template")!!)
            EnableTemplateScreen(template)
        }
    }
}


fun NavHostController.navigateWithTemplate(route: String, template: Template) {
    navigate("$route/${Uri.encode(template.serialization())}")
}
