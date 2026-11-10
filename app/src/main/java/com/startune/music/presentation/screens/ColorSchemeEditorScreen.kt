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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.startune.music.R
import com.startune.music.data.preferences.AppColorScheme
import com.startune.music.data.preferences.SchemeRoles
import com.startune.music.presentation.components.MiniPlayerHeight
import com.startune.music.presentation.viewmodel.ColorSchemeViewModel
import com.startune.music.presentation.viewmodel.EditorInitial
import com.startune.music.ui.theme.hexToColor
import com.startune.music.ui.theme.isValidHex
import com.startune.music.ui.theme.normalizeHex

private enum class ColorRole(val labelRes: Int) {
    PRIMARY(R.string.color_scheme_role_primary),
    SECONDARY(R.string.color_scheme_role_secondary),
    TERTIARY(R.string.color_scheme_role_tertiary),
    BACKGROUND(R.string.color_scheme_role_background),
    SURFACE(R.string.color_scheme_role_surface),
    ERROR(R.string.color_scheme_role_error)
}

private data class PickerTarget(val isDark: Boolean, val role: ColorRole, val hex: String)

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
                    IconButton(onClick = ::save) {
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
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Text(
                    text = stringResource(R.string.color_scheme_light),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
            items(ColorRole.entries.size) { index ->
                val role = ColorRole.entries[index]
                ColorRoleRow(
                    label = stringResource(role.labelRes),
                    hex = light.roleHex(role),
                    onClick = { pickerTarget = PickerTarget(isDark = false, role = role, hex = light.roleHex(role)) }
                )
            }
            item {
                Text(
                    text = stringResource(R.string.color_scheme_dark),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
            items(ColorRole.entries.size) { index ->
                val role = ColorRole.entries[index]
                ColorRoleRow(
                    label = stringResource(role.labelRes),
                    hex = dark.roleHex(role),
                    onClick = { pickerTarget = PickerTarget(isDark = true, role = role, hex = dark.roleHex(role)) }
                )
            }
        }
    }

    pickerTarget?.let { target ->
        ColorPickerDialog(
            title = stringResource(target.role.labelRes),
            currentHex = target.hex,
            onDismiss = { pickerTarget = null },
            onConfirm = { hex ->
                if (target.isDark) {
                    dark = dark.withRole(target.role, hex)
                } else {
                    light = light.withRole(target.role, hex)
                }
                pickerTarget = null
            }
        )
    }
}

@Composable
private fun ColorRoleRow(
    label: String,
    hex: String,
    onClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp).fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
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
private fun ColorPickerDialog(
    title: String,
    currentHex: String,
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
                        isError = hex.isNotBlank() && !valid,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (hex.isNotBlank() && !valid) {
                    Text(
                        text = stringResource(R.string.color_scheme_hex_invalid),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
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
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
}
