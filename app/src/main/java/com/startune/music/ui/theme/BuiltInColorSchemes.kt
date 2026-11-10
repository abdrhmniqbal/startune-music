package com.startune.music.ui.theme

import com.startune.music.data.preferences.AppColorScheme
import com.startune.music.data.preferences.SchemeRoles

/**
 * Built-in color schemes, redefined 2026-11-10 from each theme's own official
 * source. Nothing here comes from the old Startune static themes.
 *
 * Sources: Nord nordtheme.com/docs/colors-and-palettes; Catppuccin
 * github.com/catppuccin/palette (palette.json v1.8.0); Dracula
 * draculatheme.com/spec and github.com/dracula/dracula-theme (Alucard is the
 * official light variant); Gruvbox github.com/morhetz/gruvbox
 * (colors/gruvbox.vim); Tokyo Night folke/tokyonight.nvim (Storm/Night) and
 * the original enkia theme (Day/Light); Solarized
 * github.com/altercation/solarized; Everforest
 * github.com/sainnhe/everforest (palette.md, medium contrast); Ayu
 * github.com/dempfi/ayu (ayu-dark / ayu-light).
 *
 * Surface rule: surface is the theme's first official raised level (panel,
 * current line, bg1, surface0) that sits at least ~5 lightness points from
 * the background; when the first level is closer, the next official level
 * is used. Two themes have no official level that far: Solarized dark
 * (base02 is 3.7 points above base03) and Ayu dark (line_highlight is 2.7
 * points above background); their surfaces stay the official values and
 * the separation there comes from the derived container ramp instead.
 *
 * Nord has no official light theme: the light variant reuses the Snow Storm
 * colours as a bright ambiance (nord6 background). Tokyo Night Day has no
 * literal hex values in the folke repo (it inverts Night at runtime), so the
 * light surface is Night's bg_highlight with its lightness inverted, and the
 * other light values come from the original enkia Light theme.
 *
 * Dynamic Color is not a scheme here: selecting it means "no pair", so
 * StartuneTheme falls back to wallpaper dynamic color (or the hardcoded
 * fallback pair below Android 12), exactly like the app behaved before
 * schemes existed.
 */
object BuiltInColorSchemes {
    val nord = AppColorScheme(
        id = "nord",
        name = "Nord",
        light = SchemeRoles(
            primary = "#4C709C",
            secondary = "#81A1C1",
            tertiary = "#B48EAD",
            background = "#ECEFF4",
            surface = "#D8DEE9",
            error = "#BF616A"
        ),
        dark = SchemeRoles(
            primary = "#88C0D0",
            secondary = "#81A1C1",
            tertiary = "#5E81AC",
            background = "#2E3440",
            surface = "#434C5E",
            error = "#BF616A"
        )
    )

    val catppuccin = AppColorScheme(
        id = "catppuccin",
        name = "Catppuccin",
        light = SchemeRoles(
            primary = "#8839EF",
            secondary = "#1E66F5",
            tertiary = "#EA76CB",
            background = "#EFF1F5",
            surface = "#CCD0DA",
            error = "#D20F39"
        ),
        dark = SchemeRoles(
            primary = "#CBA6F7",
            secondary = "#89B4FA",
            tertiary = "#F5C2E7",
            background = "#1E1E2E",
            surface = "#313244",
            error = "#F38BA8"
        )
    )

    val dracula = AppColorScheme(
        id = "dracula",
        name = "Dracula",
        light = SchemeRoles(
            primary = "#644AC9",
            secondary = "#A3144D",
            tertiary = "#036A96",
            background = "#FFFBEB",
            surface = "#DEDCCF",
            error = "#CB3A2A"
        ),
        dark = SchemeRoles(
            primary = "#BD93F9",
            secondary = "#FF79C6",
            tertiary = "#8BE9FD",
            background = "#282A36",
            surface = "#44475A",
            error = "#FF5555"
        )
    )

    val gruvbox = AppColorScheme(
        id = "gruvbox",
        name = "Gruvbox",
        light = SchemeRoles(
            primary = "#B57614",
            secondary = "#076678",
            tertiary = "#8F3F71",
            background = "#FBF1C7",
            surface = "#EBDBB2",
            error = "#9D0006"
        ),
        dark = SchemeRoles(
            primary = "#FABD2F",
            secondary = "#83A598",
            tertiary = "#D3869B",
            background = "#282828",
            surface = "#3C3836",
            error = "#FB4934"
        )
    )

    val tokyo = AppColorScheme(
        id = "tokyo",
        name = "Tokyo Night",
        light = SchemeRoles(
            primary = "#34548A",
            secondary = "#5A4A78",
            tertiary = "#0F4B6E",
            background = "#D5D6DB",
            surface = "#BDC2D6",
            error = "#8C4351"
        ),
        dark = SchemeRoles(
            primary = "#7AA2F7",
            secondary = "#BB9AF7",
            tertiary = "#7DCFFF",
            background = "#1A1B26",
            surface = "#292E42",
            error = "#F7768E"
        )
    )

    val solarized = AppColorScheme(
        id = "solarized",
        name = "Solarized",
        light = SchemeRoles(
            primary = "#268BD2",
            secondary = "#6C71C4",
            tertiary = "#2AA198",
            background = "#FDF6E3",
            surface = "#EEE8D5",
            error = "#DC322F"
        ),
        dark = SchemeRoles(
            primary = "#268BD2",
            secondary = "#6C71C4",
            tertiary = "#2AA198",
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
            tertiary = "#3A94C5",
            background = "#FDF6E3",
            surface = "#EFEBD4",
            error = "#F85552"
        ),
        dark = SchemeRoles(
            primary = "#A7C080",
            secondary = "#83C092",
            tertiary = "#7FBBB3",
            background = "#2D353B",
            surface = "#3D484D",
            error = "#E67E80"
        )
    )

    val ayu = AppColorScheme(
        id = "ayu",
        name = "Ayu",
        light = SchemeRoles(
            primary = "#F29718",
            secondary = "#22A4E6",
            tertiary = "#86B300",
            background = "#FCFCFC",
            surface = "#F8F9FA",
            error = "#F07171"
        ),
        dark = SchemeRoles(
            primary = "#E6B450",
            secondary = "#59C2FF",
            tertiary = "#AAD94C",
            background = "#10141C",
            surface = "#161A24",
            error = "#F07178"
        )
    )

    val all: List<AppColorScheme> = listOf(
        nord, catppuccin, dracula, gruvbox, tokyo, solarized, everforest, ayu
    )

    fun find(id: String): AppColorScheme? = all.firstOrNull { it.id == id }
}
