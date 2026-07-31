package com.houvven.guise.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import com.houvven.guise.util.android.IntentUtils

@Composable
fun Hyperlink(
    modifier: Modifier = Modifier,
    label: String? = null,
    url: String,
    color: Color = LocalContentColor.current,
    style: TextStyle = TextStyle.Default,
    softWrap: Boolean = true,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {},
) {
    val text = buildAnnotatedString {
        append(label ?: url)
        addStyle(SpanStyle(color = color), 0, length)
    }
    Text(
        text = text,
        modifier = modifier.clickable { IntentUtils.openBrowser(url) },
        style = style,
        softWrap = softWrap,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
    )
}
