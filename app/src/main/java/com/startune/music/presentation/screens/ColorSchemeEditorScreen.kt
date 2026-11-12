package com.startune.music.presentation.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.startune.music.R
import com.startune.music.data.preferences.AppColorScheme
import com.startune.music.data.preferences.ColorRoleKeys
import com.startune.music.data.preferences.SchemeRoles
import com.startune.music.presentation.components.HsvColorPicker
import com.startune.music.presentation.components.MiniPlayerHeight
import com.startune.music.presentation.utils.getSegmentedItemShape
import com.startune.music.presentation.viewmodel.ColorSchemeViewModel
import com.startune.music.presentation.viewmodel.EditorInitial
import com.startune.music.ui.theme.LocalStartuneDarkTheme
import com.startune.music.ui.theme.colorToHex
import com.startune.music.ui.theme.hexToColor
import com.startune.music.ui.theme.isValidHex
import com.startune.music.ui.theme.normalizeHex
import com.startune.music.ui.theme.roleColor
import com.startune.music.ui.theme.toColorScheme

private enum class ColorRole(val labelRes: Int) {
    PRIMARY(R.string.color_scheme_role_primary),
    SECONDARY(R.string.color_scheme_role_secondary),
    TERTIARY(R.string.color_scheme_role_tertiary),
    BACKGROUND(R.string.color_scheme_role_background),
    SURFACE(R.string.color_scheme_role_surface),
    ERROR(R.string.color_scheme_role_error)
}

/**
 * An overridable (advanced) role: a Material3 ColorScheme property that the
 * six base roles only guess at. Setting one stores an explicit value in
 * SchemeRoles.overrides; clearing it returns the role to its auto-derived
 * value.
 */
private data class AdvancedRole(val key: String, val labelRes: Int)

private data class AdvancedGroup(val headerRes: Int, val roles: List<AdvancedRole>)

private val ADVANCED_GROUPS = listOf(
    AdvancedGroup(
        R.string.color_scheme_group_containers,
        listOf(
            AdvancedRole(ColorRoleKeys.PRIMARY_CONTAINER, R.string.color_scheme_role_primary_container),
            AdvancedRole(ColorRoleKeys.ON_PRIMARY_CONTAINER, R.string.color_scheme_role_on_primary_container),
            AdvancedRole(ColorRoleKeys.SECONDARY_CONTAINER, R.string.color_scheme_role_secondary_container),
            AdvancedRole(ColorRoleKeys.ON_SECONDARY_CONTAINER, R.string.color_scheme_role_on_secondary_container),
            AdvancedRole(ColorRoleKeys.TERTIARY_CONTAINER, R.string.color_scheme_role_tertiary_container),
            AdvancedRole(ColorRoleKeys.ON_TERTIARY_CONTAINER, R.string.color_scheme_role_on_tertiary_container),
            AdvancedRole(ColorRoleKeys.ERROR_CONTAINER, R.string.color_scheme_role_error_container),
            AdvancedRole(ColorRoleKeys.ON_ERROR_CONTAINER, R.string.color_scheme_role_on_error_container)
        )
    ),
    AdvancedGroup(
        R.string.color_scheme_group_surface,
        listOf(
            AdvancedRole(ColorRoleKeys.SURFACE_VARIANT, R.string.color_scheme_role_surface_variant),
            AdvancedRole(ColorRoleKeys.SURFACE_DIM, R.string.color_scheme_role_surface_dim),
            AdvancedRole(ColorRoleKeys.SURFACE_BRIGHT, R.string.color_scheme_role_surface_bright),
            AdvancedRole(ColorRoleKeys.SURFACE_CONTAINER_LOWEST, R.string.color_scheme_role_surface_container_lowest),
            AdvancedRole(ColorRoleKeys.SURFACE_CONTAINER_LOW, R.string.color_scheme_role_surface_container_low),
            AdvancedRole(ColorRoleKeys.SURFACE_CONTAINER, R.string.color_scheme_role_surface_container),
            AdvancedRole(ColorRoleKeys.SURFACE_CONTAINER_HIGH, R.string.color_scheme_role_surface_container_high),
            AdvancedRole(ColorRoleKeys.SURFACE_CONTAINER_HIGHEST, R.string.color_scheme_role_surface_container_highest)
        )
    ),
    AdvancedGroup(
        R.string.color_scheme_group_text,
        listOf(
            AdvancedRole(ColorRoleKeys.ON_PRIMARY, R.string.color_scheme_role_on_primary),
            AdvancedRole(ColorRoleKeys.ON_SECONDARY, R.string.color_scheme_role_on_secondary),
            AdvancedRole(ColorRoleKeys.ON_TERTIARY, R.string.color_scheme_role_on_tertiary),
            AdvancedRole(ColorRoleKeys.ON_BACKGROUND, R.string.color_scheme_role_on_background),
            AdvancedRole(ColorRoleKeys.ON_SURFACE, R.string.color_scheme_role_on_surface),
            AdvancedRole(ColorRoleKeys.ON_ERROR, R.string.color_scheme_role_on_error),
            AdvancedRole(ColorRoleKeys.ON_SURFACE_VARIANT, R.string.color_scheme_role_on_surface_variant)
        )
    ),
    AdvancedGroup(
        R.string.color_scheme_group_outline,
        listOf(
            AdvancedRole(ColorRoleKeys.OUTLINE, R.string.color_scheme_role_outline),
            AdvancedRole(ColorRoleKeys.OUTLINE_VARIANT, R.string.color_scheme_role_outline_variant)
        )
    ),
    AdvancedGroup(
        R.string.color_scheme_group_fixed,
        listOf(
            AdvancedRole(ColorRoleKeys.PRIMARY_FIXED, R.string.color_scheme_role_primary_fixed),
            AdvancedRole(ColorRoleKeys.PRIMARY_FIXED_DIM, R.string.color_scheme_role_primary_fixed_dim),
            AdvancedRole(ColorRoleKeys.ON_PRIMARY_FIXED, R.string.color_scheme_role_on_primary_fixed),
            AdvancedRole(ColorRoleKeys.ON_PRIMARY_FIXED_VARIANT, R.string.color_scheme_role_on_primary_fixed_variant),
            AdvancedRole(ColorRoleKeys.SECONDARY_FIXED, R.string.color_scheme_role_secondary_fixed),
            AdvancedRole(ColorRoleKeys.SECONDARY_FIXED_DIM, R.string.color_scheme_role_secondary_fixed_dim),
            AdvancedRole(ColorRoleKeys.ON_SECONDARY_FIXED, R.string.color_scheme_role_on_secondary_fixed),
            AdvancedRole(ColorRoleKeys.ON_SECONDARY_FIXED_VARIANT, R.string.color_scheme_role_on_secondary_fixed_variant),
            AdvancedRole(ColorRoleKeys.TERTIARY_FIXED, R.string.color_scheme_role_tertiary_fixed),
            AdvancedRole(ColorRoleKeys.TERTIARY_FIXED_DIM, R.string.color_scheme_role_tertiary_fixed_dim),
            AdvancedRole(ColorRoleKeys.ON_TERTIARY_FIXED, R.string.color_scheme_role_on_tertiary_fixed),
            AdvancedRole(ColorRoleKeys.ON_TERTIARY_FIXED_VARIANT, R.string.color_scheme_role_on_tertiary_fixed_variant)
        )
    ),
    AdvancedGroup(
        R.string.color_scheme_group_inverse,
        listOf(
            AdvancedRole(ColorRoleKeys.INVERSE_SURFACE, R.string.color_scheme_role_inverse_surface),
            AdvancedRole(ColorRoleKeys.INVERSE_ON_SURFACE, R.string.color_scheme_role_inverse_on_surface),
            AdvancedRole(ColorRoleKeys.INVERSE_PRIMARY, R.string.color_scheme_role_inverse_primary)
        )
    ),
    AdvancedGroup(
        R.string.color_scheme_group_other,
        listOf(
            AdvancedRole(ColorRoleKeys.SCRIM, R.string.color_scheme_role_scrim),
            AdvancedRole(ColorRoleKeys.SURFACE_TINT, R.string.color_scheme_role_surface_tint)
        )
    )
)

private sealed interface PickerTarget {
    val isDark: Boolean
    val labelRes: Int
    val hex: String

    /** A base role (one of the six); no reset, it always has a value. */
    data class Base(
        override val isDark: Boolean,
        val role: ColorRole,
        override val hex: String
    ) : PickerTarget {
        override val labelRes: Int get() = role.labelRes
    }

    /** An advanced role; clearing it returns the role to auto-derived. */
    data class Override(
        override val isDark: Boolean,
        val key: String,
        override val labelRes: Int,
        override val hex: String
    ) : PickerTarget
}

private val PRESET_COLORS = listOf(
    "#F44336", "#E91E63", "#9C27B0", "#673AB7",
    "#3F51B5", "#2196F3", "#03A9F4", "#00BCD4",
    "#009688", "#4CAF50", "#8BC34A", "#CDDC39",
    "#FFEB3B", "#FFC107", "#FF9800", "#FF5722",
    "#795548", "#9E9E9E", "#607D8B", "#000000",
    "#FFFFFF", "#E6B450", "#88C0D0", "#CBA6F7"
)

private fun SchemeRoles.withRole(role: ColorRole, hex: String): SchemeRoles = when (role) {
    ColorRole.PRIMARY -> copy(primary = hex)
    ColorRole.SECONDARY -> copy(secondary = hex)
    ColorRole.TERTIARY -> copy(tertiary = hex)
    ColorRole.BACKGROUND -> copy(background = hex)
    ColorRole.SURFACE -> copy(surface = hex)
    ColorRole.ERROR -> copy(error = hex)
}

private fun SchemeRoles.roleHex(role: ColorRole): String = when (role) {
    ColorRole.PRIMARY -> primary
    ColorRole.SECONDARY -> secondary
    ColorRole.TERTIARY -> tertiary
    ColorRole.BACKGROUND -> background
    ColorRole.SURFACE -> surface
    ColorRole.ERROR -> error
}

private fun SchemeRoles.withOverride(key: String, hex: String): SchemeRoles =
    copy(overrides = overrides + (key to hex))

private fun SchemeRoles.withoutOverride(key: String): SchemeRoles =
    copy(overrides = overrides - key)

@Composable
fun ColorSchemeEditorScreen(
    themeId: String,
    onBack: () -> Unit,
    viewModel: ColorSchemeViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var initial by remember { mutableStateOf<EditorInitial?>(null) }
    var notFound by remember { mutableStateOf(false) }

    LaunchedEffect(themeId) {
        initial = viewModel.loadEditorInitial(themeId)
        if (initial == null) notFound = true
    }
    LaunchedEffect(notFound) {
        if (notFound) onBack()
    }

    val loaded = initial
    if (loaded == null) {
        Box(modifier = Modifier.fillMaxSize())
        return
    }

    var name by remember(loaded) { mutableStateOf(loaded.name) }
    var light by remember(loaded) { mutableStateOf(loaded.light) }
    var dark by remember(loaded) { mutableStateOf(loaded.dark) }
    var pickerTarget by remember { mutableStateOf<PickerTarget?>(null) }

    fun save() {
        if (name.isBlank()) {
            Toast.makeText(
                context,
                context.getString(R.string.color_scheme_name_required),
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        viewModel.saveCustomScheme(
            AppColorScheme(
                id = loaded.id ?: viewModel.newSchemeId(),
                name = name.trim(),
                light = light,
                dark = dark
            )
        )
        onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(
                            if (loaded.id == null) R.string.color_scheme_new
                            else R.string.color_scheme_edit_title
                        ),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    FilledTonalIconButton(
                        onClick = onBack,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.auth_cd_back)
                        )
                    }
                },
                actions = {
                    FilledTonalIconButton(
                        onClick = ::save,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                    ) {
                        Icon(
                            Icons.Rounded.Check,
                            contentDescription = stringResource(R.string.color_scheme_save)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            )
        }
    ) { scaffoldPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = scaffoldPadding.calculateTopPadding() + 12.dp,
                end = 16.dp,
                bottom = MiniPlayerHeight + 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.color_scheme_name_label)) },
                    placeholder = { Text(stringResource(R.string.color_scheme_name_placeholder)) },
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    )
                )
            }
            item {
                Text(
                    text = stringResource(R.string.color_scheme_preview),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
            item {
                val isDark = LocalStartuneDarkTheme.current
                val draftRoles = if (isDark) dark else light
                MaterialTheme(colorScheme = draftRoles.toColorScheme(dark = isDark)) {
                    Surface(
                        shape = RoundedCornerShape(28.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(16.dp).fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Rounded.PlayArrow,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 16.dp)
                            ) {
                                Text(
                                    text = name.ifBlank { stringResource(R.string.color_scheme_name_placeholder) },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stringResource(
                                        if (isDark) R.string.color_scheme_dark
                                        else R.string.color_scheme_light
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
            item {
                Text(
                    text = stringResource(R.string.color_scheme_light),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    ColorRole.entries.forEachIndexed { index, role ->
                        ColorRoleRow(
                            label = stringResource(role.labelRes),
                            hex = light.roleHex(role),
                            shape = getSegmentedItemShape(index, ColorRole.entries.size),
                            onClick = { pickerTarget = PickerTarget.Base(isDark = false, role = role, hex = light.roleHex(role)) }
                        )
                    }
                }
            }
            item {
                Text(
                    text = stringResource(R.string.color_scheme_dark),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    ColorRole.entries.forEachIndexed { index, role ->
                        ColorRoleRow(
                            label = stringResource(role.labelRes),
                            hex = dark.roleHex(role),
                            shape = getSegmentedItemShape(index, ColorRole.entries.size),
                            onClick = { pickerTarget = PickerTarget.Base(isDark = true, role = role, hex = dark.roleHex(role)) }
                        )
                    }
                }
            }
            item {
                Text(
                    text = stringResource(R.string.color_scheme_advanced),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
            item {
                Text(
                    text = stringResource(R.string.color_scheme_light),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item {
                AdvancedRolesSection(
                    roles = light,
                    isDark = false,
                    onPick = { key, labelRes, hex ->
                        pickerTarget = PickerTarget.Override(isDark = false, key = key, labelRes = labelRes, hex = hex)
                    }
                )
            }
            item {
                Text(
                    text = stringResource(R.string.color_scheme_dark),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item {
                AdvancedRolesSection(
                    roles = dark,
                    isDark = true,
                    onPick = { key, labelRes, hex ->
                        pickerTarget = PickerTarget.Override(isDark = true, key = key, labelRes = labelRes, hex = hex)
                    }
                )
            }
        }
    }

    pickerTarget?.let { target ->
        ColorPickerDialog(
            title = stringResource(target.labelRes),
            currentHex = target.hex,
            showResetToAuto = target is PickerTarget.Override,
            onResetToAuto = {
                val override = target as PickerTarget.Override
                if (override.isDark) {
                    dark = dark.withoutOverride(override.key)
                } else {
                    light = light.withoutOverride(override.key)
                }
                pickerTarget = null
            },
            onDismiss = { pickerTarget = null },
            onConfirm = { hex ->
                when (target) {
                    is PickerTarget.Base -> {
                        if (target.isDark) {
                            dark = dark.withRole(target.role, hex)
                        } else {
                            light = light.withRole(target.role, hex)
                        }
                    }
                    is PickerTarget.Override -> {
                        if (target.isDark) {
                            dark = dark.withOverride(target.key, hex)
                        } else {
                            light = light.withOverride(target.key, hex)
                        }
                    }
                }
                pickerTarget = null
            }
        )
    }
}

@Composable
private fun AdvancedRolesSection(
    roles: SchemeRoles,
    isDark: Boolean,
    onPick: (key: String, labelRes: Int, hex: String) -> Unit
) {
    val derived = remember(roles, isDark) { roles.toColorScheme(dark = isDark) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ADVANCED_GROUPS.forEach { group ->
            Text(
                text = stringResource(group.headerRes),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                group.roles.forEachIndexed { index, role ->
                    val isAuto = !roles.overrides.containsKey(role.key)
                    val effectiveHex = colorToHex(derived.roleColor(role.key))
                    AdvancedRoleRow(
                        label = stringResource(role.labelRes),
                        hex = effectiveHex,
                        isAuto = isAuto,
                        shape = getSegmentedItemShape(index, group.roles.size),
                        onClick = { onPick(role.key, role.labelRes, effectiveHex) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ColorRoleRow(
    label: String,
    hex: String,
    shape: Shape,
    onClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = shape,
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 72.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp).fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(hexToColor(hex))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = CircleShape
                    )
            )
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp)
            )
            Text(
                text = hex,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AdvancedRoleRow(
    label: String,
    hex: String,
    isAuto: Boolean,
    shape: Shape,
    onClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = shape,
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 72.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp).fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(hexToColor(hex))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = CircleShape
                    )
            )
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp)
            )
            if (isAuto) {
                Text(
                    text = stringResource(R.string.color_scheme_auto),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
            Text(
                text = hex,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ColorPickerDialog(
    title: String,
    currentHex: String,
    showResetToAuto: Boolean,
    onResetToAuto: () -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var hex by remember { mutableStateOf(currentHex) }
    val normalized = normalizeHex(hex)
    val valid = isValidHex(normalized)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(if (valid) hexToColor(normalized) else MaterialTheme.colorScheme.surfaceVariant)
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant,
                                shape = CircleShape
                            )
                    )
                    OutlinedTextField(
                        value = hex,
                        onValueChange = { hex = it },
                        label = { Text(stringResource(R.string.color_scheme_hex_label)) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        isError = hex.isNotBlank() && !valid,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        )
                    )
                }
                if (hex.isNotBlank() && !valid) {
                    Text(
                        text = stringResource(R.string.color_scheme_hex_invalid),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                HsvColorPicker(
                    color = if (valid) hexToColor(normalized) else MaterialTheme.colorScheme.primary,
                    onColorChange = { picked -> hex = colorToHex(picked) },
                    modifier = Modifier.fillMaxWidth()
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PRESET_COLORS.chunked(8).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { preset ->
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(hexToColor(preset))
                                        .border(
                                            width = 1.dp,
                                            color = MaterialTheme.colorScheme.outlineVariant,
                                            shape = CircleShape
                                        )
                                        .clickable { hex = preset }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = { onConfirm(normalized) }
            ) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            Row {
                if (showResetToAuto) {
                    TextButton(onClick = onResetToAuto) {
                        Text(stringResource(R.string.color_scheme_reset_auto))
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        }
    )
}
