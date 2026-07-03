package com.ella.music.ui.artist

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import com.ella.music.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun rememberArtistCoverUri(
    artistName: String,
    folderLocation: String,
    mainViewModel: MainViewModel
): Uri? {
    val state by produceState<Uri?>(
        initialValue = null,
        artistName,
        folderLocation
    ) {
        value = if (artistName.isBlank() || folderLocation.isBlank()) {
            null
        } else {
            withContext(Dispatchers.IO) {
                mainViewModel.getArtistCoverUri(artistName, folderLocation)
            }
        }
    }
    return state
}
