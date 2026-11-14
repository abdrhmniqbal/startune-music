package com.startune.music.presentation.components

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class LyricCleanerTest {
    @Test
    fun `line timestamps are stripped`() {
        assertThat(
            LyricCleaner.clean("[00:12.34]Burn it up\n[01:23]Let it go")
        ).containsExactly("Burn it up", "Let it go").inOrder()
    }

    @Test
    fun `word timestamps are stripped and the line stays whole`() {
        assertThat(
            LyricCleaner.clean(
                "[00:12.34]<00:12.34>Burn <00:13.00>it <00:13.50>up\n" +
                    "[00:15.00]<00:15.00>Let <00:15.60>it <00:16.00>go"
            )
        ).containsExactly("Burn it up", "Let it go").inOrder()
    }

    @Test
    fun `kugou line and word timestamps are stripped`() {
        assertThat(
            LyricCleaner.clean(
                "[1234,5678]Burn it up\n[6000,2000]<6000,500,300>Let <6500,500,300>it go"
            )
        ).containsExactly("Burn it up", "Let it go").inOrder()
    }

    @Test
    fun `metadata background and translation lines are dropped`() {
        assertThat(
            LyricCleaner.clean(
                "[ti:Burn it]\n[ar:BIBI]\n[00:12.34]Burn it up\n[bg:(oh)]\n[tr: Bakar saja]"
            )
        ).containsExactly("Burn it up")
    }

    @Test
    fun `the same line sung at several timestamps is one entry`() {
        assertThat(
            LyricCleaner.clean("[00:30.00]Burn it up\n[00:45.00]Burn it up\n[01:00.00]Let it go")
        ).containsExactly("Burn it up", "Let it go").inOrder()
    }

    @Test
    fun `duet voice prefix is stripped`() {
        assertThat(LyricCleaner.clean("[00:12.34]v1: Hello\n[00:15.00]v2: World"))
            .containsExactly("Hello", "World").inOrder()
    }

    @Test
    fun `plain text is unchanged and blank input is empty`() {
        assertThat(LyricCleaner.clean("Burn it up\nLet it go"))
            .containsExactly("Burn it up", "Let it go").inOrder()
        assertThat(LyricCleaner.clean(null)).isEmpty()
        assertThat(LyricCleaner.clean("   ")).isEmpty()
    }
}
