package com.houvven.guise.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.houvven.guise.ui.routing.NavigationRoute
import com.houvven.guise.ui.theme.GuiseTheme
import com.houvven.guise.update.AppUpdateHost


class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GuiseTheme {
                Scaffold(
                    contentWindowInsets = WindowInsets(0),
                    snackbarHost = {
                        SnackbarHost(
                            hostState = GlobalSnackbarHost.state,
                            modifier = Modifier.padding(bottom = 80.dp),
                        ) { data ->
                            Surface(
                                modifier = Modifier.widthIn(max = 560.dp),
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.92f),
                                contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                                shadowElevation = 8.dp,
                            ) {
                                androidx.compose.material3.Text(
                                    text = data.visuals.message,
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    }
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.padding(
                            top = it.calculateTopPadding(), bottom = it.calculateBottomPadding()
                        )
                    ) {
                        Box {
                            NavigationRoute()
                            AppUpdateHost()
                        }
                    }
                }
            }
        }
    }
}
