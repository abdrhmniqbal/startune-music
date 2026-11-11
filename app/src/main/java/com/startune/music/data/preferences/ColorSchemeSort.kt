package com.startune.music.data.preferences

import androidx.annotation.StringRes
import com.startune.music.R

/** Sort order for the Color Scheme settings list. Persisted in ThemePreferencesRepository. */
enum class ColorSchemeSort(val storageKey: String, @StringRes val labelRes: Int) {
    DEFAULT("default", R.string.sort_method_default_order),
    NAME_AZ("name_az", R.string.color_scheme_sort_name_az),
    NAME_ZA("name_za", R.string.color_scheme_sort_name_za);

    companion object {
        fun fromStorageKey(value: String?): ColorSchemeSort =
            entries.firstOrNull { it.storageKey == value } ?: DEFAULT
    }
}
