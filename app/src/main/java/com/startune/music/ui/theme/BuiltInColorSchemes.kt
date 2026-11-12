package com.startune.music.ui.theme

import com.startune.music.data.preferences.AppColorScheme
import com.startune.music.data.preferences.ColorRoleKeys
import com.startune.music.data.preferences.SchemeRoles

/**
 * Built-in color schemes, redefined 2026-11-10 from each theme's own official
 * source, and recreated 2026-11-12 with explicit values for the roles the
 * mapper used to guess, so each theme matches its official palette instead
 * of only approximating it through six colors. Nothing here comes from the
 * old Startune static themes.
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
 * How the official palette maps to Material3 roles:
 * onSurface/onBackground is the theme's foreground (not pure black/white,
 * which the mapper used to guess); the surface container ramp is the
 * theme's own raised levels in order (bg1/bg2/bg3, Surface0/1/2, current
 * line, base02...); primaryContainer is the theme's selection color with
 * the accent as its on-color; onSurfaceVariant is the theme's muted
 * foreground (comment/subtext); outline is the theme's border level.
 * Roles with no override keep the mapper's derivation.
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
            error = "#BF616A",
            overrides = mapOf(
                ColorRoleKeys.ON_SURFACE to "#2E3440",
                ColorRoleKeys.ON_BACKGROUND to "#2E3440",
                ColorRoleKeys.SURFACE_CONTAINER_LOWEST to "#ECEFF4",
                ColorRoleKeys.SURFACE_CONTAINER_LOW to "#E5E9F0",
                ColorRoleKeys.SURFACE_CONTAINER to "#D8DEE9",
                ColorRoleKeys.SURFACE_CONTAINER_HIGH to "#D8DEE9",
                ColorRoleKeys.SURFACE_CONTAINER_HIGHEST to "#D8DEE9",
                ColorRoleKeys.PRIMARY_CONTAINER to "#D8DEE9",
                ColorRoleKeys.ON_PRIMARY_CONTAINER to "#4C709C",
                ColorRoleKeys.ON_SURFACE_VARIANT to "#4C566A",
                ColorRoleKeys.OUTLINE to "#D8DEE9"
            )
        ),
        dark = SchemeRoles(
            primary = "#88C0D0",
            secondary = "#81A1C1",
            tertiary = "#5E81AC",
            background = "#2E3440",
            surface = "#434C5E",
            error = "#BF616A",
            overrides = mapOf(
                ColorRoleKeys.ON_SURFACE to "#D8DEE9",
                ColorRoleKeys.ON_BACKGROUND to "#D8DEE9",
                ColorRoleKeys.SURFACE_CONTAINER_LOWEST to "#2E3440",
                ColorRoleKeys.SURFACE_CONTAINER_LOW to "#3B4252",
                ColorRoleKeys.SURFACE_CONTAINER to "#434C5E",
                ColorRoleKeys.SURFACE_CONTAINER_HIGH to "#4C566A",
                ColorRoleKeys.SURFACE_CONTAINER_HIGHEST to "#4C566A",
                ColorRoleKeys.PRIMARY_CONTAINER to "#4C566A",
                ColorRoleKeys.ON_PRIMARY_CONTAINER to "#88C0D0",
                ColorRoleKeys.ON_SURFACE_VARIANT to "#7B8694",
                ColorRoleKeys.OUTLINE to "#4C566A"
            )
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
            error = "#D20F39",
            overrides = mapOf(
                ColorRoleKeys.ON_SURFACE to "#4C4F69",
                ColorRoleKeys.ON_BACKGROUND to "#4C4F69",
                ColorRoleKeys.SURFACE_CONTAINER_LOWEST to "#E6E9EF",
                ColorRoleKeys.SURFACE_CONTAINER_LOW to "#EFF1F5",
                ColorRoleKeys.SURFACE_CONTAINER to "#CCD0DA",
                ColorRoleKeys.SURFACE_CONTAINER_HIGH to "#BCC0CC",
                ColorRoleKeys.SURFACE_CONTAINER_HIGHEST to "#ACB0BE",
                ColorRoleKeys.PRIMARY_CONTAINER to "#BCC0CC",
                ColorRoleKeys.ON_PRIMARY_CONTAINER to "#8839EF",
                ColorRoleKeys.ON_SURFACE_VARIANT to "#6C6F85",
                ColorRoleKeys.OUTLINE to "#9CA0B0"
            )
        ),
        dark = SchemeRoles(
            primary = "#CBA6F7",
            secondary = "#89B4FA",
            tertiary = "#F5C2E7",
            background = "#1E1E2E",
            surface = "#313244",
            error = "#F38BA8",
            overrides = mapOf(
                ColorRoleKeys.ON_SURFACE to "#CDD6F4",
                ColorRoleKeys.ON_BACKGROUND to "#CDD6F4",
                ColorRoleKeys.SURFACE_CONTAINER_LOWEST to "#181825",
                ColorRoleKeys.SURFACE_CONTAINER_LOW to "#1E1E2E",
                ColorRoleKeys.SURFACE_CONTAINER to "#313244",
                ColorRoleKeys.SURFACE_CONTAINER_HIGH to "#45475A",
                ColorRoleKeys.SURFACE_CONTAINER_HIGHEST to "#585B70",
                ColorRoleKeys.PRIMARY_CONTAINER to "#45475A",
                ColorRoleKeys.ON_PRIMARY_CONTAINER to "#CBA6F7",
                ColorRoleKeys.ON_SURFACE_VARIANT to "#A6ADC8",
                ColorRoleKeys.OUTLINE to "#6C7086"
            )
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
            error = "#CB3A2A",
            overrides = mapOf(
                ColorRoleKeys.ON_SURFACE to "#282A36",
                ColorRoleKeys.ON_BACKGROUND to "#282A36",
                ColorRoleKeys.SURFACE_CONTAINER_LOWEST to "#FFFBEB",
                ColorRoleKeys.SURFACE_CONTAINER_LOW to "#F5F0DC",
                ColorRoleKeys.SURFACE_CONTAINER to "#DEDCCF",
                ColorRoleKeys.SURFACE_CONTAINER_HIGH to "#D4D0C0",
                ColorRoleKeys.SURFACE_CONTAINER_HIGHEST to "#C9C4B0",
                ColorRoleKeys.PRIMARY_CONTAINER to "#DEDCCF",
                ColorRoleKeys.ON_PRIMARY_CONTAINER to "#644AC9",
                ColorRoleKeys.ON_SURFACE_VARIANT to "#6272A4",
                ColorRoleKeys.OUTLINE to "#DEDCCF"
            )
        ),
        dark = SchemeRoles(
            primary = "#BD93F9",
            secondary = "#FF79C6",
            tertiary = "#8BE9FD",
            background = "#282A36",
            surface = "#44475A",
            error = "#FF5555",
            overrides = mapOf(
                ColorRoleKeys.ON_SURFACE to "#F8F8F2",
                ColorRoleKeys.ON_BACKGROUND to "#F8F8F2",
                ColorRoleKeys.SURFACE_CONTAINER_LOWEST to "#282A36",
                ColorRoleKeys.SURFACE_CONTAINER_LOW to "#343746",
                ColorRoleKeys.SURFACE_CONTAINER to "#44475A",
                ColorRoleKeys.SURFACE_CONTAINER_HIGH to "#44475A",
                ColorRoleKeys.SURFACE_CONTAINER_HIGHEST to "#4D4F68",
                ColorRoleKeys.PRIMARY_CONTAINER to "#44475A",
                ColorRoleKeys.ON_PRIMARY_CONTAINER to "#BD93F9",
                ColorRoleKeys.ON_SURFACE_VARIANT to "#6272A4",
                ColorRoleKeys.OUTLINE to "#44475A"
            )
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
            error = "#9D0006",
            overrides = mapOf(
                ColorRoleKeys.ON_SURFACE to "#3C3836",
                ColorRoleKeys.ON_BACKGROUND to "#3C3836",
                ColorRoleKeys.SURFACE_CONTAINER_LOWEST to "#FBF1C7",
                ColorRoleKeys.SURFACE_CONTAINER_LOW to "#EBDBB2",
                ColorRoleKeys.SURFACE_CONTAINER to "#D5C4A1",
                ColorRoleKeys.SURFACE_CONTAINER_HIGH to "#BDAE93",
                ColorRoleKeys.SURFACE_CONTAINER_HIGHEST to "#A89984",
                ColorRoleKeys.PRIMARY_CONTAINER to "#D5C4A1",
                ColorRoleKeys.ON_PRIMARY_CONTAINER to "#B57614",
                ColorRoleKeys.ON_SURFACE_VARIANT to "#7C6F64",
                ColorRoleKeys.OUTLINE to "#D5C4A1"
            )
        ),
        dark = SchemeRoles(
            primary = "#FABD2F",
            secondary = "#83A598",
            tertiary = "#D3869B",
            background = "#282828",
            surface = "#3C3836",
            error = "#FB4934",
            overrides = mapOf(
                ColorRoleKeys.ON_SURFACE to "#EBDBB2",
                ColorRoleKeys.ON_BACKGROUND to "#EBDBB2",
                ColorRoleKeys.SURFACE_CONTAINER_LOWEST to "#282828",
                ColorRoleKeys.SURFACE_CONTAINER_LOW to "#3C3836",
                ColorRoleKeys.SURFACE_CONTAINER to "#504945",
                ColorRoleKeys.SURFACE_CONTAINER_HIGH to "#665C54",
                ColorRoleKeys.SURFACE_CONTAINER_HIGHEST to "#7C6F64",
                ColorRoleKeys.PRIMARY_CONTAINER to "#504945",
                ColorRoleKeys.ON_PRIMARY_CONTAINER to "#FABD2F",
                ColorRoleKeys.ON_SURFACE_VARIANT to "#A89984",
                ColorRoleKeys.OUTLINE to "#665C54"
            )
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
            error = "#8C4351",
            overrides = mapOf(
                ColorRoleKeys.ON_SURFACE to "#3760BF",
                ColorRoleKeys.ON_BACKGROUND to "#3760BF",
                ColorRoleKeys.SURFACE_CONTAINER_LOWEST to "#E1E2E7",
                ColorRoleKeys.SURFACE_CONTAINER_LOW to "#DBDCE2",
                ColorRoleKeys.SURFACE_CONTAINER to "#C4C8DA",
                ColorRoleKeys.SURFACE_CONTAINER_HIGH to "#B4B8D0",
                ColorRoleKeys.SURFACE_CONTAINER_HIGHEST to "#A8AECB",
                ColorRoleKeys.PRIMARY_CONTAINER to "#C4C8DA",
                ColorRoleKeys.ON_PRIMARY_CONTAINER to "#34548A",
                ColorRoleKeys.ON_SURFACE_VARIANT to "#848CB5",
                ColorRoleKeys.OUTLINE to "#B4B8D0"
            )
        ),
        dark = SchemeRoles(
            primary = "#7AA2F7",
            secondary = "#BB9AF7",
            tertiary = "#7DCFFF",
            background = "#1A1B26",
            surface = "#292E42",
            error = "#F7768E",
            overrides = mapOf(
                ColorRoleKeys.ON_SURFACE to "#C0CAF5",
                ColorRoleKeys.ON_BACKGROUND to "#C0CAF5",
                ColorRoleKeys.SURFACE_CONTAINER_LOWEST to "#16161E",
                ColorRoleKeys.SURFACE_CONTAINER_LOW to "#1A1B26",
                ColorRoleKeys.SURFACE_CONTAINER to "#292E42",
                ColorRoleKeys.SURFACE_CONTAINER_HIGH to "#292E42",
                ColorRoleKeys.SURFACE_CONTAINER_HIGHEST to "#3B4261",
                ColorRoleKeys.PRIMARY_CONTAINER to "#292E42",
                ColorRoleKeys.ON_PRIMARY_CONTAINER to "#7AA2F7",
                ColorRoleKeys.ON_SURFACE_VARIANT to "#565F89",
                ColorRoleKeys.OUTLINE to "#3B4261"
            )
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
            error = "#DC322F",
            overrides = mapOf(
                ColorRoleKeys.ON_SURFACE to "#657B83",
                ColorRoleKeys.ON_BACKGROUND to "#657B83",
                ColorRoleKeys.PRIMARY_CONTAINER to "#EEE8D5",
                ColorRoleKeys.ON_PRIMARY_CONTAINER to "#268BD2",
                ColorRoleKeys.ON_SURFACE_VARIANT to "#93A1A1",
                ColorRoleKeys.OUTLINE to "#EEE8D5"
            )
        ),
        dark = SchemeRoles(
            primary = "#268BD2",
            secondary = "#6C71C4",
            tertiary = "#2AA198",
            background = "#002B36",
            surface = "#073642",
            error = "#DC322F",
            overrides = mapOf(
                ColorRoleKeys.ON_SURFACE to "#839496",
                ColorRoleKeys.ON_BACKGROUND to "#839496",
                ColorRoleKeys.PRIMARY_CONTAINER to "#073642",
                ColorRoleKeys.ON_PRIMARY_CONTAINER to "#268BD2",
                ColorRoleKeys.ON_SURFACE_VARIANT to "#586E75",
                ColorRoleKeys.OUTLINE to "#073642"
            )
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
            error = "#F85552",
            overrides = mapOf(
                ColorRoleKeys.ON_SURFACE to "#5C6A72",
                ColorRoleKeys.ON_BACKGROUND to "#5C6A72",
                ColorRoleKeys.PRIMARY_CONTAINER to "#F0EAD4",
                ColorRoleKeys.ON_PRIMARY_CONTAINER to "#8DA101",
                ColorRoleKeys.ON_SURFACE_VARIANT to "#829181",
                ColorRoleKeys.OUTLINE to "#E8DFC8"
            )
        ),
        dark = SchemeRoles(
            primary = "#A7C080",
            secondary = "#83C092",
            tertiary = "#7FBBB3",
            background = "#2D353B",
            surface = "#3D484D",
            error = "#E67E80",
            overrides = mapOf(
                ColorRoleKeys.ON_SURFACE to "#D3C6AA",
                ColorRoleKeys.ON_BACKGROUND to "#D3C6AA",
                ColorRoleKeys.PRIMARY_CONTAINER to "#323C41",
                ColorRoleKeys.ON_PRIMARY_CONTAINER to "#A7C080",
                ColorRoleKeys.ON_SURFACE_VARIANT to "#859289",
                ColorRoleKeys.OUTLINE to "#445055"
            )
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
            error = "#F07171",
            overrides = mapOf(
                ColorRoleKeys.ON_SURFACE to "#5C6773",
                ColorRoleKeys.ON_BACKGROUND to "#5C6773",
                ColorRoleKeys.PRIMARY_CONTAINER to "#F0F1F2",
                ColorRoleKeys.ON_PRIMARY_CONTAINER to "#F29718",
                ColorRoleKeys.ON_SURFACE_VARIANT to "#828C99",
                ColorRoleKeys.OUTLINE to "#E8E9EB"
            )
        ),
        dark = SchemeRoles(
            primary = "#E6B450",
            secondary = "#59C2FF",
            tertiary = "#AAD94C",
            background = "#10141C",
            surface = "#161A24",
            error = "#F07178",
            overrides = mapOf(
                ColorRoleKeys.ON_SURFACE to "#B3B1AD",
                ColorRoleKeys.ON_BACKGROUND to "#B3B1AD",
                ColorRoleKeys.PRIMARY_CONTAINER to "#161A24",
                ColorRoleKeys.ON_PRIMARY_CONTAINER to "#E6B450",
                ColorRoleKeys.ON_SURFACE_VARIANT to "#626A73",
                ColorRoleKeys.OUTLINE to "#1C212E"
            )
        )
    )

    val all: List<AppColorScheme> = listOf(
        nord, catppuccin, dracula, gruvbox, tokyo, solarized, everforest, ayu
    )

    fun find(id: String): AppColorScheme? = all.firstOrNull { it.id == id }
}
