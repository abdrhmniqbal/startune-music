package com.startune.music.presentation.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChangelogMarkdownTest {

    @Test
    fun `beta section parses into paragraph, quote, and bullets`() {
        val body = """
            First beta of the Kotlin rewrite: Startune Music rebuilt as a native Kotlin app, analytics-free.

            > If you installed one of the earlier test APKs, uninstall it first. This beta is signed with the release keystore, so it cannot update over the throwaway-signed test builds.

            - Removed the "How backup works" banner from Data & Storage.
            - Removed everything F-Droid related.
        """.trimIndent()

        val blocks = parseChangelogMarkdown(body)
        assertEquals(4, blocks.size)
        assertTrue(blocks[0] is ChangelogBlock.Paragraph)
        assertEquals(
            "First beta of the Kotlin rewrite: Startune Music rebuilt as a native Kotlin app, analytics-free.",
            (blocks[0] as ChangelogBlock.Paragraph).text.text
        )
        assertTrue(blocks[1] is ChangelogBlock.Quote)
        assertEquals(
            "If you installed one of the earlier test APKs, uninstall it first. This beta is signed with the release keystore, so it cannot update over the throwaway-signed test builds.",
            (blocks[1] as ChangelogBlock.Quote).text.text
        )
        assertTrue(blocks[2] is ChangelogBlock.Bullet)
        assertEquals(
            "Removed the \"How backup works\" banner from Data & Storage.",
            (blocks[2] as ChangelogBlock.Bullet).text.text
        )
        assertTrue(blocks[3] is ChangelogBlock.Bullet)
    }

    @Test
    fun `bullet-only body parses into bullets`() {
        val body = "- First change.\n- Second change.\n- Third change."
        val blocks = parseChangelogMarkdown(body)
        assertEquals(3, blocks.size)
        assertTrue(blocks.all { it is ChangelogBlock.Bullet })
        assertEquals("Second change.", (blocks[1] as ChangelogBlock.Bullet).text.text)
    }

    @Test
    fun `multi-line blockquote joins into one quote`() {
        val body = "> First line of the warning.\n> Second line of the warning.\n\n- A bullet."
        val blocks = parseChangelogMarkdown(body)
        assertEquals(2, blocks.size)
        assertTrue(blocks[0] is ChangelogBlock.Quote)
        assertEquals(
            "First line of the warning. Second line of the warning.",
            (blocks[0] as ChangelogBlock.Quote).text.text
        )
        assertTrue(blocks[1] is ChangelogBlock.Bullet)
    }

    @Test
    fun `inline bold and code are parsed`() {
        val annotated = parseInlineMarkdown("Use **bold** and `code` here.")
        assertEquals("Use bold and code here.", annotated.text)
        // "bold" is at 4..8, "code" is at 13..17 in the plain text.
        val bold = annotated.spanStyles.filter { it.start == 4 && it.end == 8 }
        assertTrue(bold.isNotEmpty())
        val code = annotated.spanStyles.filter { it.start == 13 && it.end == 17 }
        assertTrue(code.isNotEmpty())
    }

    @Test
    fun `inline link keeps its label and URL annotation`() {
        val annotated = parseInlineMarkdown("Check out the [past release notes](https://example.com/releases) now.")
        assertEquals("Check out the past release notes now.", annotated.text)
        val annotations = annotated.getStringAnnotations("URL", 14, 32)
        assertEquals(1, annotations.size)
        assertEquals("https://example.com/releases", annotations[0].item)
    }

    @Test
    fun `empty body yields no blocks`() {
        assertTrue(parseChangelogMarkdown("").isEmpty())
        assertTrue(parseChangelogMarkdown("   \n\n  ").isEmpty())
    }
}
