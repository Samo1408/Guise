package com.houvven.guise.update

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink

/** Renders Markdown-style links such as `[project page](https://example.com)`. */
@Composable
fun UpdateNotesText(notes: String, modifier: Modifier = Modifier) {
    val linkColor = MaterialTheme.colorScheme.primary
    val annotatedNotes = remember(notes, linkColor) {
        updateNotesAnnotatedString(notes, linkColor)
    }
    Text(annotatedNotes, modifier)
}

internal fun updateNotesAnnotatedString(notes: String, linkColor: Color) =
    buildAnnotatedString {
        var cursor = 0
        UPDATE_NOTE_LINK.findAll(notes).forEach { match ->
            append(notes, cursor, match.range.first)
            withLink(
                LinkAnnotation.Url(
                    url = match.groupValues[2],
                    styles = TextLinkStyles(
                        style = SpanStyle(
                            color = linkColor,
                            textDecoration = TextDecoration.Underline,
                        ),
                    ),
                ),
            ) {
                append(match.groupValues[1])
            }
            cursor = match.range.last + 1
        }
        append(notes, cursor, notes.length)
    }

private val UPDATE_NOTE_LINK = Regex(
    pattern = """\[([^\]\r\n]+)]\((https?://[^\s)]+)\)""",
    option = RegexOption.IGNORE_CASE,
)
