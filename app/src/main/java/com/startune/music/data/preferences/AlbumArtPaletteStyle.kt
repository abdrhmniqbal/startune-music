package com.startune.music.data.preferences
import androidx.annotation.StringRes
import com.startune.music.R

enum class AlbumArtPaletteStyle(
    val storageKey: String,
    @StringRes val labelRes: Int
) {
    TONAL_SPOT("tonal_spot", R.string.presentation_batch_f_palette_label_tonal_spot),
    VIBRANT("vibrant", R.string.presentation_batch_f_palette_label_vibrant),
    EXPRESSIVE("expressive", R.string.presentation_batch_f_palette_label_expressive),
    FRUIT_SALAD("fruit_salad", R.string.presentation_batch_f_palette_label_fruit_salad);

    companion object {
        val default: AlbumArtPaletteStyle = TONAL_SPOT

        fun fromStorageKey(value: String?): AlbumArtPaletteStyle {
            return entries.firstOrNull { it.storageKey == value } ?: default
        }
    }
}
