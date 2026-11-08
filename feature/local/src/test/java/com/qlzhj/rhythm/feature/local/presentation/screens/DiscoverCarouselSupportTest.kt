package com.qlzhj.rhythm.feature.local.presentation.screens

import com.qlzhj.rhythm.core.model.Album
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class DiscoverCarouselSupportTest {

    private fun album(id: String) = Album(id = id, title = "Album $id", artist = "Artist")

    private fun library(size: Int) = (1..size).map { album("a$it") }

    @Test
    fun emptySourceReturnsEmptyList() {
        assertTrue(pickFeaturedAlbums(emptyList(), 5, emptyList(), Random(1)).isEmpty())
    }

    @Test
    fun nonPositiveCountReturnsEmptyList() {
        assertTrue(pickFeaturedAlbums(library(10), 0, emptyList(), Random(1)).isEmpty())
        assertTrue(pickFeaturedAlbums(library(10), -3, emptyList(), Random(1)).isEmpty())
    }

    @Test
    fun firstPickFillsRequestedCountWithoutDuplicates() {
        val source = library(500)
        val picked = pickFeaturedAlbums(source, 8, emptyList(), Random(42))

        assertEquals(8, picked.size)
        assertEquals(8, picked.distinctBy { it.id }.size)
        assertTrue(picked.all { it in source })
    }

    @Test
    fun previouslyPickedIdsSurviveLibraryRefresh() {
        val random = Random(7)
        val source = library(40)
        val first = pickFeaturedAlbums(source, 4, emptyList(), random)
        val refreshed = source.map { it.copy(numberOfSongs = 12) }

        val picked = pickFeaturedAlbums(refreshed, 4, first.map { it.id }, random)

        assertEquals(first.map { it.id }, picked.map { it.id })
        assertTrue(picked.all { it.numberOfSongs == 12 })
    }

    @Test
    fun missingIdsAreFilledFromRemainingAlbums() {
        val source = library(10)
        val picked = pickFeaturedAlbums(source, 5, listOf("a1", "gone"), Random(3))

        assertEquals("a1", picked.first().id)
        assertEquals(5, picked.size)
        assertEquals(5, picked.distinctBy { it.id }.size)
        assertTrue(picked.all { it in source })
    }

    @Test
    fun neverReturnsMoreThanDistinctSourceEntries() {
        val source = library(3) + album("a1")
        val picked = pickFeaturedAlbums(source, 10, emptyList(), Random(9))

        assertEquals(3, picked.size)
        assertEquals(3, picked.distinctBy { it.id }.size)
    }

    @Test
    fun shrinkingLibraryDropsRemovedAlbums() {
        val source = library(6)
        val picked = pickFeaturedAlbums(source, 5, listOf("a1", "a2", "a3", "a4", "a5"), Random(5))

        val smaller = source.take(2)
        val repicked = pickFeaturedAlbums(smaller, 5, picked.map { it.id }, Random(5))

        assertEquals(2, repicked.size)
        assertEquals(listOf("a1", "a2"), repicked.map { it.id })
    }

    @Test
    fun disabledAutoScrollHasNoDelay() {
        assertNull(discoverCarouselAutoScrollDelayMs(false, 5))
        assertNull(discoverCarouselAutoScrollDelayMs(false, 12))
    }

    @Test
    fun autoScrollDelayUsesConfiguredIntervalInSeconds() {
        assertEquals(2_000L, discoverCarouselAutoScrollDelayMs(true, 2))
        assertEquals(5_000L, discoverCarouselAutoScrollDelayMs(true, 5))
        assertEquals(15_000L, discoverCarouselAutoScrollDelayMs(true, 15))
    }

    @Test
    fun autoScrollDelayClampsOutOfRangeIntervals() {
        assertEquals(2_000L, discoverCarouselAutoScrollDelayMs(true, -3))
        assertEquals(2_000L, discoverCarouselAutoScrollDelayMs(true, 0))
        assertEquals(15_000L, discoverCarouselAutoScrollDelayMs(true, 100))
    }
}
