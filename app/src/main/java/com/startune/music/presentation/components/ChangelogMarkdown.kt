package com.startune.music.presentation.components

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri

/**
 * Lightweight markdown rendering for the changelog, no library needed.
 * The release notes we show come from CHANGELOG.md sections, and their
 * actual content is only three block types: a plain paragraph, a bullet
 * list (`- ...`), and a blockquote (`> ...`, the beta install warning).
 * Inline bold, inline code, and inline links are parsed too, so a future
 * entry using them still renders right.
 *
 * Anything unrecognized falls back to a plain paragraph, so a new markdown
 * shape never breaks the sheet, it just shows as text.
 */
sealed interface ChangelogBlock {
    data class Paragraph(val text: AnnotatedString) : ChangelogBlock
    data class Bullet(val text: AnnotatedString) : ChangelogBlock
    data class Quote(val text: AnnotatedString) : ChangelogBlock
}

/**
 * Splits a changelog body into blocks. Consecutive bullet lines become
 * separate [ChangelogBlock.Bullet] items; a `> ` line becomes a
 * [ChangelogBlock.Quote]; everything else is a [ChangelogBlock.Paragraph].
 * Blank lines separate paragraphs. A blockquote may span several `> ` lines,
 * which are joined into one quote.
 */
fun parseChangelogMarkdown(markdown: String, linkColor: Color = Color.Blue): List<ChangelogBlock> {
    val blocks = mutableListOf<ChangelogBlock>()
    val paragraphLines = mutableListOf<String>()
    val quoteLines = mutableListOf<String>()

    fun flushParagraph() {
        if (paragraphLines.isEmpty()) return
        val text = paragraphLines.joinToString(" ").trim()
        if (text.isNotEmpty()) blocks.add(ChangelogBlock.Paragraph(parseInlineMarkdown(text, linkColor)))
        paragraphLines.clear()
    }

    fun flushQuote() {
        if (quoteLines.isEmpty()) return
        val text = quoteLines.joinToString(" ").trim()
        if (text.isNotEmpty()) blocks.add(ChangelogBlock.Quote(parseInlineMarkdown(text, linkColor)))
        quoteLines.clear()
    }

    for (rawLine in markdown.lineSequence()) {
        val line = rawLine.trim()
        when {
            line.isEmpty() -> {
                flushParagraph()
                flushQuote()
            }
            line.startsWith("> ") || line == ">" -> {
                flushParagraph()
                quoteLines.add(line.removePrefix(">").trim())
            }
            line.startsWith("- ") -> {
                flushParagraph()
                flushQuote()
                val text = line.removePrefix("- ").trim()
                if (text.isNotEmpty()) blocks.add(ChangelogBlock.Bullet(parseInlineMarkdown(text, linkColor)))
            }
            else -> {
                flushQuote()
                paragraphLines.add(line)
            }
        }
    }
    flushParagraph()
    flushQuote()
    return blocks
}

/**
 * Parses inline markdown into an [AnnotatedString]: `**bold**`, `` `code` ``,
 * and `[label](url)`. A link span carries a "URL" string annotation, so the
 * renderer can open it. Unmatched markers are left as literal text.
 */
fun parseInlineMarkdown(text: String, linkColor: Color = Color.Blue): AnnotatedString {
    val linkRegex = Regex("""\[([^\]]+)\]\(([^)\s]+)\)""")
    val boldRegex = Regex("""\*\*([^*]+)\*\*""")
    val codeRegex = Regex("""`([^`]+)`""")

    return buildAnnotatedString {
        var index = 0
        val matches = mutableListOf<InlineMatch>()
        linkRegex.findAll(text).forEach { matches.add(InlineMatch(it.range, InlineKind.Link, it.groupValues[1], it.groupValues[2])) }
        boldRegex.findAll(text).forEach { matches.add(InlineMatch(it.range, InlineKind.Bold, it.groupValues[1], null)) }
        codeRegex.findAll(text).forEach { matches.add(InlineMatch(it.range, InlineKind.Code, it.groupValues[1], null)) }
        // Earliest match wins; overlapping matches are skipped.
        matches.sortBy { it.range.first }
        var lastEnd = 0
        for (match in matches) {
            if (match.range.first < lastEnd) continue
            if (match.range.first > index) append(text.substring(index, match.range.first))
            when (match.kind) {
                InlineKind.Link -> {
                    pushStringAnnotation(tag = "URL", annotation = match.url ?: "")
                    withStyle(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)) {
                        append(match.label)
                    }
                    pop()
                }
                InlineKind.Bold -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(match.label) }
                }
                InlineKind.Code -> {
                    withStyle(SpanStyle(fontFamily = FontFamily.Monospace)) { append(match.label) }
                }
            }
            lastEnd = match.range.last + 1
            index = lastEnd
        }
        if (index < text.length) append(text.substring(index))
    }
}

private enum class InlineKind { Link, Bold, Code }

private data class InlineMatch(
    val range: IntRange,
    val kind: InlineKind,
    val label: String,
    val url: String?
)

/**
 * Renders a parsed changelog body: paragraphs as normal text, bullets as
 * rows with a dot, and blockquotes as a toned block with a primary accent
 * bar on the left. Links open in the browser.
 */
@Composable
fun ChangelogMarkdownBody(
    body: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val linkColor = MaterialTheme.colorScheme.primary
    val blocks = parseChangelogMarkdown(body, linkColor)
    val onLinkClick: (String) -> Unit = { url -> openChangelogUrl(context, url) }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        blocks.forEach { block ->
            when (block) {
                is ChangelogBlock.Paragraph -> {
                    ChangelogClickableText(text = block.text, onLinkClick = onLinkClick)
                }
                is ChangelogBlock.Bullet -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 9.dp)
                                .size(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                        ChangelogClickableText(
                            text = block.text,
                            onLinkClick = onLinkClick,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                is ChangelogBlock.Quote -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .heightIn(min = 48.dp)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        ChangelogClickableText(
                            text = block.text,
                            onLinkClick = onLinkClick,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            fontStyle = FontStyle.Italic
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChangelogClickableText(
    text: AnnotatedString,
    onLinkClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    fontStyle: FontStyle? = null
) {
    ClickableText(
        text = text,
        modifier = modifier.fillMaxWidth(),
        style = MaterialTheme.typography.bodyLarge.copy(
            color = MaterialTheme.colorScheme.onSurface,
            fontStyle = fontStyle ?: FontStyle.Normal
        ),
        onClick = { offset ->
            text.getStringAnnotations(tag = "URL", start = offset, end = offset)
                .firstOrNull()?.let { onLinkClick(it.item) }
        }
    )
}

private fun openChangelogUrl(context: Context, url: String) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    }
}
