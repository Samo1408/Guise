package com.houvven.guise.ui.routing

import android.annotation.SuppressLint
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.houvven.guise.db.Template
import com.houvven.guise.ui.routing.editor.AddTemplateScreen
import com.houvven.guise.ui.routing.editor.DeployConfigEditScreen
import com.houvven.guise.ui.routing.editor.EditTemplateScreen
import com.houvven.guise.ui.routing.launcher.LauncherRoute
import com.houvven.guise.ui.routing.template.EnableTemplateScreen

@SuppressLint("StaticFieldLeak")
object LocalNavController {
    lateinit var current: NavHostController
}

@Composable
fun NavigationRoute() {
    val navController = rememberNavController()
    LocalNavController.current = navController

    NavHost(navController, NavRoutingTypes.LAUNCHER.name) {
        composable(NavRoutingTypes.LAUNCHER.name) { LauncherRoute() }

        composable(
            route = "${NavRoutingTypes.DEPLOY_CONFIG_EDITOR.name}/{name}/{packageName}",
            arguments = listOf(
                navArgument("name") { type = NavType.StringType },
                navArgument("packageName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val name = backStackEntry.arguments!!.getString("name")!!
            val packageName = backStackEntry.arguments!!.getString("packageName")!!
            DeployConfigEditScreen(name, packageName)
        }

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
