package com.startune.music.presentation.recap

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class RecapShareTest {
    @Test
    fun `story canvas is exactly 9 by 16 for a wide capture`() {
        val (width, height) = storyCanvasSize(1000, 1500)
        assertThat(width * 16).isEqualTo(height * 9)
        assertThat(width).isAtLeast(1000)
        assertThat(height).isAtLeast(1500)
    }

    @Test
    fun `story canvas is exactly 9 by 16 for a tall capture`() {
        val (width, height) = storyCanvasSize(600, 1400)
        assertThat(width * 16).isEqualTo(height * 9)
        assertThat(width).isAtLeast(600)
        assertThat(height).isAtLeast(1400)
    }

    @Test
    fun `story canvas keeps a 9 by 16 capture unchanged`() {
        assertThat(storyCanvasSize(1080, 1920)).isEqualTo(1080 to 1920)
    }

    @Test
    fun `story canvas keeps a square capture at its own width`() {
        val (width, height) = storyCanvasSize(1000, 1000)
        assertThat(width).isEqualTo(1008)
        assertThat(height).isEqualTo(1792)
        assertThat(width * 16).isEqualTo(height * 9)
    }
}
