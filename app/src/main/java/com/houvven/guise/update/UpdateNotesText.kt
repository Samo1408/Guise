package com.houvven.guise.update

import androidx.compose.runtime.Composable
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownTypography

/** Renders the GitHub Release body as Markdown using the app's Material 3 theme. */
@Composable
fun UpdateNotesText(notes: String, modifier: Modifier = Modifier) {
    val typography = MaterialTheme.typography
    Markdown(
        content = notes,
        modifier = modifier,
        typography = markdownTypography(
            h1 = typography.titleLarge,
            h2 = typography.titleLarge,
            h3 = typography.titleMedium,
            h4 = typography.titleMedium,
            h5 = typography.titleSmall,
            h6 = typography.titleSmall,
            text = typography.bodyMedium,
            paragraph = typography.bodyMedium,
            ordered = typography.bodyMedium,
            bullet = typography.bodyMedium,
            list = typography.bodyMedium,
            table = typography.bodyMedium,
            textLink = TextLinkStyles(
                style = typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    textDecoration = TextDecoration.Underline,
                ).toSpanStyle(),
            ),
        ),
    )
}
