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
 * line, base02...); onSurfaceVariant is the theme's muted foreground
 * (comment/subtext); outline is the theme's border level.
 * primaryContainer is deliberately NOT overridden: the themes' selection
 * colors sit only ~2-8 lightness points from surfaceContainerLow, so a
 * primaryContainer highlight (current song row, tonal buttons) blended
 * into the surface. The mapper derives it instead (7 points beyond
 * surfaceContainer, primary hue kept, on-color by WCAG), which keeps the
 * highlight visible in every theme. Roles with no override keep the
 * mapper's derivation.
 *
 * Rainbow: each variant stores its official accent palette as
 * SchemeRoles.rainbow (genre cards, Settings icons). Nord Aurora is the
 * reference: nord11-15 only, five colors, no Frost blue (Frost nord7-10
 * is the primary-accent palette, not the rainbow). Aurora has only five
 * colors, so the other three slots are new colors with the same feel
 * (Aurora's average lightness/saturation): lime #BDCA7D, violet #967DCA
 * and pink #CA7DA3, so all eight slots are distinct. Aurora is the same
 * in both ambiances, so both variants store the same eight colors in hue
 * order. The stored palette is what the UI shows; the derived
 * hue-rotation palette is only the fallback for custom themes without a
 * stored rainbow. The rainbow is editable in the theme editor and travels
 * in the exported JSON.
 *
 * Surface rule: surface is the theme's official elevated level (panel,
 * modal, button: nord1, bg1, surface0), NOT the selection/current-line
 * level (nord2 is selection, not a surface). It must sit at least ~5
 * lightness points from the background; when the elevated level is closer,
 * the next official level is used. Two themes have no official level that
 * far: Solarized dark
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
            primary = "#88C0D0",
            secondary = "#81A1C1",
            tertiary = "#B48EAD",
            background = "#ECEFF4",
            surface = "#D8DEE9",
            error = "#BF616A",
            overrides = mapOf(
                ColorRoleKeys.ON_SURFACE to "#2E3440",
                ColorRoleKeys.ON_BACKGROUND to "#2E3440",
                ColorRoleKeys.ON_SURFACE_VARIANT to "#4C566A",
                ColorRoleKeys.OUTLINE to "#D8DEE9"
            ),
            rainbow = listOf("#BF616A", "#D08770", "#EBCB8B", "#BDCA7D", "#A3BE8C", "#967DCA", "#B48EAD", "#CA7DA3")
        ),
        dark = SchemeRoles(
            primary = "#88C0D0",
            secondary = "#81A1C1",
            tertiary = "#5E81AC",
            background = "#2E3440",
            surface = "#3B4252",
            error = "#BF616A",
            overrides = mapOf(
                ColorRoleKeys.ON_SURFACE to "#D8DEE9",
                ColorRoleKeys.ON_BACKGROUND to "#D8DEE9",
                ColorRoleKeys.ON_SURFACE_VARIANT to "#E5E9F0",
                ColorRoleKeys.OUTLINE to "#4C566A"
            ),
            rainbow = listOf("#BF616A", "#D08770", "#EBCB8B", "#BDCA7D", "#A3BE8C", "#967DCA", "#B48EAD", "#CA7DA3")
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
            surface = "#E6E9EF",
            error = "#D20F39",
            overrides = mapOf(
                ColorRoleKeys.ON_SURFACE to "#4C4F69",
                ColorRoleKeys.ON_BACKGROUND to "#4C4F69",
                ColorRoleKeys.ON_SURFACE_VARIANT to "#6C6F85",
                ColorRoleKeys.OUTLINE to "#9CA0B0"
            ),
            rainbow = listOf("#D20F39", "#FE640B", "#DF8E1D", "#40A02B", "#179299", "#1E66F5", "#8839EF", "#EA76CB")
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
                ColorRoleKeys.ON_SURFACE_VARIANT to "#A6ADC8",
                ColorRoleKeys.OUTLINE to "#6C7086"
            ),
            rainbow = listOf("#F38BA8", "#FAB387", "#F9E2AF", "#A6E3A1", "#94E2D5", "#89B4FA", "#CBA6F7", "#F5C2E7")
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
                ColorRoleKeys.ON_SURFACE to "#1F1F1F",
                ColorRoleKeys.ON_BACKGROUND to "#1F1F1F",
                ColorRoleKeys.ON_SURFACE_VARIANT to "#6C664B",
                ColorRoleKeys.OUTLINE to "#DEDCCF"
            ),
            rainbow = listOf("#CB3A2A", "#A34D14", "#846E15", "#14710A", "#036A96", "#644AC9", "#A3144D", "#6C664B")
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
                ColorRoleKeys.ON_SURFACE_VARIANT to "#F8F8F2",
                ColorRoleKeys.OUTLINE to "#44475A"
            ),
            rainbow = listOf("#8BE9FD", "#50FA7B", "#FFB86C", "#FF79C6", "#BD93F9", "#FF5555", "#F1FA8C", "#6272A4")
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
                ColorRoleKeys.ON_SURFACE_VARIANT to "#7C6F64",
                ColorRoleKeys.OUTLINE to "#D5C4A1"
            ),
            rainbow = listOf("#CC241D", "#98971A", "#D79921", "#458588", "#B16286", "#689D6A", "#D65D0E", "#7C6F64")
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
                ColorRoleKeys.ON_SURFACE_VARIANT to "#A89984",
                ColorRoleKeys.OUTLINE to "#665C54"
            ),
            rainbow = listOf("#FB4934", "#B8BB26", "#FABD2F", "#83A598", "#D3869B", "#8EC07C", "#FE8019", "#928374")
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
                ColorRoleKeys.ON_SURFACE to "#343B58",
                ColorRoleKeys.ON_BACKGROUND to "#343B58",
                ColorRoleKeys.ON_SURFACE_VARIANT to "#343B58",
                ColorRoleKeys.OUTLINE to "#B4B5B9"
            ),
            rainbow = listOf("#2E7DE9", "#007197", "#587539", "#9854F1", "#FF4774", "#F52A65", "#8C6C3E", "#A463FF")
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
                ColorRoleKeys.ON_SURFACE_VARIANT to "#A9B1D6",
                ColorRoleKeys.OUTLINE to "#3B4261"
            ),
            rainbow = listOf("#7AA2F7", "#7DCFFF", "#9ECE6A", "#BB9AF7", "#FF9E64", "#F7768E", "#E0AF68", "#9D7CD8")
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
                ColorRoleKeys.ON_SURFACE_VARIANT to "#586E75",
                ColorRoleKeys.OUTLINE to "#EEE8D5"
            ),
            rainbow = listOf("#B58900", "#CB4B16", "#DC322F", "#D33682", "#6C71C4", "#268BD2", "#2AA198", "#859900")
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
                ColorRoleKeys.ON_SURFACE_VARIANT to "#839496",
                ColorRoleKeys.OUTLINE to "#073642"
            ),
            rainbow = listOf("#B58900", "#CB4B16", "#DC322F", "#D33682", "#6C71C4", "#268BD2", "#2AA198", "#859900")
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
                ColorRoleKeys.ON_SURFACE_VARIANT to "#5C6A72",
                ColorRoleKeys.OUTLINE to "#E6E2CC"
            ),
            rainbow = listOf("#F85552", "#F57D26", "#DFA000", "#8DA101", "#35A77C", "#3A94C5", "#DF69BA", "#5C6A72")
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
                ColorRoleKeys.ON_SURFACE_VARIANT to "#D3C6AA",
                ColorRoleKeys.OUTLINE to "#475258"
            ),
            rainbow = listOf("#E67E80", "#E69875", "#DBBC7F", "#A7C080", "#83C092", "#7FBBB3", "#D699B6", "#7A8478")
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
            surface = "#FFFFFF",
            error = "#F07171",
            overrides = mapOf(
                ColorRoleKeys.ON_SURFACE to "#5C6166",
                ColorRoleKeys.ON_BACKGROUND to "#5C6166",
                ColorRoleKeys.ON_SURFACE_VARIANT to "#828E9F",
                ColorRoleKeys.OUTLINE to "#ADAEB1"
            ),
            rainbow = listOf("#F07171", "#F29718", "#EBA400", "#86B300", "#4CBF99", "#22A4E6", "#A37ACC", "#F2A191")
        ),
        dark = SchemeRoles(
            primary = "#E6B450",
            secondary = "#59C2FF",
            tertiary = "#AAD94C",
            background = "#10141C",
            surface = "#161A24",
            error = "#F07178",
            overrides = mapOf(
                ColorRoleKeys.ON_SURFACE to "#BFBDB6",
                ColorRoleKeys.ON_BACKGROUND to "#BFBDB6",
                ColorRoleKeys.ON_SURFACE_VARIANT to "#5A6378",
                ColorRoleKeys.OUTLINE to "#5A6673"
            ),
            rainbow = listOf("#F07178", "#FF8F40", "#E6B450", "#AAD94C", "#95E6CB", "#39BAE6", "#D2A6FF", "#F26D78")
        )
    )

    val all: List<AppColorScheme> = listOf(
        nord, catppuccin, dracula, gruvbox, tokyo, solarized, everforest, ayu
    )

    fun find(id: String): AppColorScheme? = all.firstOrNull { it.id == id }
}
