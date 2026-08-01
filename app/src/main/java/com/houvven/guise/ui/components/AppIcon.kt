package com.houvven.guise.ui.components

import android.graphics.Bitmap
import android.util.LruCache
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.houvven.guise.ui.components.simplify.SimplifyImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private object AppIconCache {
    private val cache = object : LruCache<String, Bitmap>(8 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int =
            (value.allocationByteCount / 1024).coerceAtLeast(1)
    }

    fun get(key: String): Bitmap? = cache.get(key)

    fun put(key: String, bitmap: Bitmap) {
        cache.put(key, bitmap)
    }
}

@Composable
fun AppIcon(
    packageName: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val iconSizePx = with(LocalDensity.current) { 48.dp.roundToPx() }
    val cacheKey = "$packageName@$iconSizePx"
    val bitmap by produceState(
        initialValue = AppIconCache.get(cacheKey),
        cacheKey,
    ) {
        if (value == null) {
            value = withContext(Dispatchers.IO) {
                runCatching {
                    context.packageManager.getApplicationIcon(packageName)
                        .toBitmap(width = iconSizePx, height = iconSizePx)
                }.getOrNull()?.also { AppIconCache.put(cacheKey, it) }
            }
        }
    }

    val loadedBitmap = bitmap
    if (loadedBitmap != null) {
        SimplifyImage(
            bitmap = loadedBitmap.asImageBitmap(),
            modifier = modifier,
            contentScale = ContentScale.Fit,
        )
    } else {
        Icon(
            imageVector = Icons.Default.Android,
            contentDescription = null,
            modifier = modifier,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
