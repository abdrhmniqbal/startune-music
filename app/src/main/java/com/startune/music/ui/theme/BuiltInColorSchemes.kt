package com.startune.music.ui.theme

import com.startune.music.data.preferences.AppColorScheme
import com.startune.music.data.preferences.ColorSchemeIds
import com.startune.music.data.preferences.SchemeRoles

/**
 * Built-in color schemes. Colors are taken from the old Startune static themes
 * (src/core/theme/static-themes.ts), mapped onto the six editable roles.
 * Dynamic Color is not a scheme here: selecting it means "no pair", so
 * StartuneTheme falls back to wallpaper dynamic color (or the Startune pair
 * below Android 12), exactly like the app behaved before schemes existed.
 */
object BuiltInColorSchemes {
    val startune = AppColorScheme(
        id = ColorSchemeIds.STARTUNE,
        name = "Startune",
        light = SchemeRoles(
            primary = "#6C4FF5",
            secondary = "#F06292",
            tertiary = "#FF8A65",
            background = "#F7F2FF",
            surface = "#FBF8FF",
            error = "#D32F2F"
        ),
        dark = SchemeRoles(
            primary = "#AB47BC",
            secondary = "#F06292",
            tertiary = "#FF8A65",
            background = "#1E1234",
            surface = "#2A1F40",
            error = "#FF5252"
        )
    )

    val nord = AppColorScheme(
        id = "nord",
        name = "Nord",
        light = SchemeRoles(
            primary = "#4C709C",
            secondary = "#81A1C1",
            tertiary = "#B48EAD",
            background = "#ECEFF4",
            surface = "#E5E9F0",
            error = "#BF616A"
        ),
        dark = SchemeRoles(
            primary = "#88C0D0",
            secondary = "#5E81AC",
            tertiary = "#B48EAD",
            background = "#2E3440",
            surface = "#3B4252",
            error = "#BF616A"
        )
    )

    val catppuccin = AppColorScheme(
        id = "catppuccin",
        name = "Catppuccin",
        light = SchemeRoles(
            primary = "#8839EF",
            secondary = "#1E66F5",
            tertiary = "#D351B0",
            background = "#EFF1F5",
            surface = "#E6E9EF",
            error = "#D20F39"
        ),
        dark = SchemeRoles(
            primary = "#CBA6F7",
            secondary = "#89B4FA",
            tertiary = "#F5C2E7",
            background = "#1E1E2E",
            surface = "#181825",
            error = "#F38BA8"
        )
    )

    val dracula = AppColorScheme(
        id = "dracula",
        name = "Dracula",
        light = SchemeRoles(
            primary = "#644AC9",
            secondary = "#036A96",
            tertiary = "#4C6F10",
            background = "#FFFBEB",
            surface = "#DEDCCF",
            error = "#CB3A2A"
        ),
        dark = SchemeRoles(
            primary = "#BD93F9",
            secondary = "#FF79C6",
            tertiary = "#8AE234",
            background = "#282A36",
            surface = "#343746",
            error = "#FF5555"
        )
    )

    val gruvbox = AppColorScheme(
        id = "gruvbox",
        name = "Gruvbox",
        light = SchemeRoles(
            primary = "#D79921",
            secondary = "#458588",
            tertiary = "#B16286",
            background = "#FBF1C7",
            surface = "#F9F5D7",
            error = "#CC241D"
        ),
        dark = SchemeRoles(
            primary = "#FABD2F",
            secondary = "#83A598",
            tertiary = "#D3869B",
            background = "#282828",
            surface = "#32302F",
            error = "#FB4934"
        )
    )

    val tokyo = AppColorScheme(
        id = "tokyo",
        name = "Tokyo Night",
        light = SchemeRoles(
            primary = "#1054B1",
            secondary = "#2E7DE9",
            tertiary = "#9854F1",
            background = "#D5D6DB",
            surface = "#FFFFFF",
            error = "#F52A65"
        ),
        dark = SchemeRoles(
            primary = "#7AA2F7",
            secondary = "#2AC3DE",
            tertiary = "#BB9AF7",
            background = "#1A1B26",
            surface = "#24283B",
            error = "#F7768E"
        )
    )

    val solarized = AppColorScheme(
        id = "solarized",
        name = "Solarized",
        light = SchemeRoles(
            primary = "#1B74B3",
            secondary = "#2AA198",
            tertiary = "#D33682",
            background = "#FDF6E3",
            surface = "#FEFCF3",
            error = "#DC322F"
        ),
        dark = SchemeRoles(
            primary = "#268BD2",
            secondary = "#2AA198",
            tertiary = "#D33682",
            background = "#002B36",
            surface = "#073642",
            error = "#DC322F"
        )
    )

    val everforest = AppColorScheme(
        id = "everforest",
        name = "Everforest",
        light = SchemeRoles(
            primary = "#8DA101",
            secondary = "#35A77C",
            tertiary = "#8B7EC8",
            background = "#FDF6E3",
            surface = "#FFF9E8",
            error = "#E66868"
        ),
        dark = SchemeRoles(
            primary = "#A7C080",
            secondary = "#7FBBB3",
            tertiary = "#D699B6",
            background = "#2D353B",
            surface = "#343F44",
            error = "#E67E80"
        )
    )

    val ayu = AppColorScheme(
        id = "ayu",
        name = "Ayu",
        light = SchemeRoles(
            primary = "#E6B450",
            secondary = "#399EE6",
            tertiary = "#F07178",
            background = "#FAF7F0",
            surface = "#FFFFFF",
            error = "#F07178"
        ),
        dark = SchemeRoles(
            primary = "#E6B450",
            secondary = "#59C2FF",
            tertiary = "#F07178",
            background = "#1F2430",
            surface = "#242936",
            error = "#F28779"
        )
    )

    val all: List<AppColorScheme> = listOf(
        startune, nord, catppuccin, dracula, gruvbox, tokyo, solarized, everforest, ayu
    )

    fun find(id: String): AppColorScheme? = all.firstOrNull { it.id == id }
}
