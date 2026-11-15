package com.startune.music.presentation.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.startune.music.data.model.Artist
import com.startune.music.data.network.deezer.DeezerArtistCandidate
import com.startune.music.data.repository.ArtistImageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * The artwork-picker contract the ArtistArtworkSheet drives: Deezer search
 * state plus apply/set/clear for the current artist. ArtistDetailViewModel
 * implements it for its nav-scoped artist; ArtistArtworkViewModel implements
 * it for any artist bound from a list, so the same sheet works from the
 * library list without navigating to the detail screen. Saving goes through
 * ArtistImageRepository, which writes the artist table, so the library's
 * Room paging source refreshes the row on its own.
 */
interface ArtistArtworkController {
    val artworkSearchQuery: StateFlow<String>
    val artworkCandidates: StateFlow<List<DeezerArtistCandidate>>
    val isSearchingArtwork: StateFlow<Boolean>
    fun openArtworkSearch(artistName: String)
    fun setArtworkSearchQuery(query: String)
    fun closeArtworkSearch()
    fun clearCustomImage()
    suspend fun applyDeezerArtwork(pictureUrl: String): Boolean
    fun setCustomImage(sourceUri: Uri)
}

@HiltViewModel
class ArtistArtworkViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val artistImageRepository: ArtistImageRepository,
) : ViewModel(), ArtistArtworkController {

    private var boundArtist: Artist? = null

    /** Point the picker at an artist from a list. Resets the search state. */
    fun bind(artist: Artist) {
        if (boundArtist?.id == artist.id) return
        boundArtist = artist
        closeArtworkSearch()
        _artworkSearchQuery.value = ""
        _artworkCandidates.value = emptyList()
        _isSearchingArtwork.value = false
    }

    private val _artworkSearchQuery = MutableStateFlow("")
    override val artworkSearchQuery: StateFlow<String> = _artworkSearchQuery.asStateFlow()

    private val _artworkCandidates = MutableStateFlow<List<DeezerArtistCandidate>>(emptyList())
    override val artworkCandidates: StateFlow<List<DeezerArtistCandidate>> = _artworkCandidates.asStateFlow()

    private val _isSearchingArtwork = MutableStateFlow(false)
    override val isSearchingArtwork: StateFlow<Boolean> = _isSearchingArtwork.asStateFlow()

    private var artworkSearchJob: Job? = null

    override fun openArtworkSearch(artistName: String) {
        _artworkSearchQuery.value = artistName
        artworkSearchJob?.cancel()
        searchArtworkNow(artistName)
    }

    override fun setArtworkSearchQuery(query: String) {
        _artworkSearchQuery.value = query
        artworkSearchJob?.cancel()
        if (query.isBlank()) {
            _artworkCandidates.value = emptyList()
            _isSearchingArtwork.value = false
            return
        }
        artworkSearchJob = viewModelScope.launch {
            delay(350)
            searchArtworkNow(query)
        }
    }

    override fun closeArtworkSearch() {
        artworkSearchJob?.cancel()
        artworkSearchJob = null
    }

    private fun searchArtworkNow(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            _artworkCandidates.value = emptyList()
            _isSearchingArtwork.value = false
            return
        }
        artworkSearchJob = viewModelScope.launch {
            _isSearchingArtwork.value = true
            try {
                _artworkCandidates.value = artistImageRepository.searchDeezerArtists(trimmed)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.tag("ArtistDebug").w("Deezer artwork search failed: ${e.message}")
                _artworkCandidates.value = emptyList()
            } finally {
                _isSearchingArtwork.value = false
            }
        }
    }

    override suspend fun applyDeezerArtwork(pictureUrl: String): Boolean {
        val artistId = boundArtist?.id ?: return false
        return try {
            val internalPath = artistImageRepository.setCustomArtistImageFromUrl(context, artistId, pictureUrl)
            !internalPath.isNullOrBlank()
        } catch (e: Exception) {
            Timber.tag("ArtistDebug").e("Failed to apply Deezer artwork: ${e.message}")
            false
        }
    }

    override fun setCustomImage(sourceUri: Uri) {
        val artistId = boundArtist?.id ?: return
        viewModelScope.launch {
            try {
                artistImageRepository.setCustomArtistImage(context, artistId, sourceUri)
            } catch (e: Exception) {
                Timber.tag("ArtistDebug").e("Failed to set custom image: ${e.message}")
            }
        }
    }

    override fun clearCustomImage() {
        val artistId = boundArtist?.id ?: return
        viewModelScope.launch {
            try {
                artistImageRepository.clearCustomArtistImage(context, artistId)
            } catch (e: Exception) {
                Timber.tag("ArtistDebug").e("Failed to clear custom image: ${e.message}")
            }
        }
    }
}
