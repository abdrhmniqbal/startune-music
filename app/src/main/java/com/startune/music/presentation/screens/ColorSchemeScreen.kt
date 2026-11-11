package com.startune.music.presentation.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.startune.music.R
import com.startune.music.data.preferences.AppColorScheme
import com.startune.music.data.preferences.ColorSchemeIds
import com.startune.music.data.preferences.ColorSchemeSort
import com.startune.music.data.preferences.SchemeRoles
import com.startune.music.presentation.components.MiniPlayerHeight
import com.startune.music.presentation.viewmodel.ColorSchemeImportResult
import com.startune.music.presentation.viewmodel.ColorSchemeViewModel
import com.startune.music.ui.theme.BuiltInColorSchemes
import com.startune.music.ui.theme.LocalStartuneDarkTheme
import com.startune.music.ui.theme.hexToColor
import kotlinx.coroutines.launch
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape

private data class SchemeListEntry(
    val id: String,
    val name: String,
    val roles: SchemeRoles?,
    val custom: AppColorScheme?
)

private val colorSchemeRowShape = AbsoluteSmoothCornerShape(
    cornerRadiusBR = 30.dp,
    smoothnessAsPercentTL = 60,
    cornerRadiusTR = 30.dp,
    smoothnessAsPercentTR = 60,
    cornerRadiusBL = 30.dp,
    smoothnessAsPercentBL = 60,
    cornerRadiusTL = 30.dp,
    smoothnessAsPercentBR = 60
)

@Composable
fun ColorSchemeScreen(
    onBack: () -> Unit,
    onEditTheme: (String) -> Unit,
    viewModel: ColorSchemeViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val selectedId by viewModel.selectedSchemeId.collectAsStateWithLifecycle()
    val customs by viewModel.customSchemes.collectAsStateWithLifecycle()
    val sort by viewModel.colorSchemeSort.collectAsStateWithLifecycle()
    val isDark = LocalStartuneDarkTheme.current

    var exportScheme by remember { mutableStateOf<AppColorScheme?>(null) }
    var deleteScheme by remember { mutableStateOf<AppColorScheme?>(null) }
    var showSortSheet by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val scheme = exportScheme
        exportScheme = null
        if (uri == null || scheme == null) return@rememberLauncherForActivityResult
        try {
            context.contentResolver.openOutputStream(uri)?.use {
                it.write(viewModel.exportJson(scheme).toByteArray())
            }
        } catch (e: Exception) {
            Toast.makeText(
                context,
                context.getString(R.string.color_scheme_export_failed),
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val text = try {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText()
            } catch (e: Exception) {
                null
            }
            when (val result = text?.let { viewModel.importFromJson(it) }) {
                is ColorSchemeImportResult.Success -> Toast.makeText(
                    context,
                    context.getString(R.string.color_scheme_imported, result.name),
                    Toast.LENGTH_SHORT
                ).show()
                else -> Toast.makeText(
                    context,
                    context.getString(R.string.color_scheme_invalid_file),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    val dynamicName = stringResource(R.string.color_scheme_dynamic_name)
    val displaySchemes = remember(customs, isDark, sort, dynamicName) {
        val base = buildList {
            add(
                SchemeListEntry(
                    id = ColorSchemeIds.DYNAMIC,
                    name = dynamicName,
                    roles = null,
                    custom = null
                )
            )
            BuiltInColorSchemes.all.forEach { scheme ->
                add(
                    SchemeListEntry(
                        id = scheme.id,
                        name = scheme.name,
                        roles = if (isDark) scheme.dark else scheme.light,
                        custom = null
                    )
                )
            }
            customs.forEach { scheme ->
                add(
                    SchemeListEntry(
                        id = scheme.id,
                        name = scheme.name,
                        roles = if (isDark) scheme.dark else scheme.light,
                        custom = scheme
                    )
                )
            }
        }
        when (sort) {
            ColorSchemeSort.DEFAULT -> base
            ColorSchemeSort.NAME_AZ -> base.sortedBy { it.name.lowercase() }
            ColorSchemeSort.NAME_ZA -> base.sortedByDescending { it.name.lowercase() }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.setcat_color_scheme_title),
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
                        onClick = { importLauncher.launch(arrayOf("application/json")) },
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                    ) {
                        Icon(
                            Icons.Rounded.FileOpen,
                            contentDescription = stringResource(R.string.color_scheme_import)
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
                bottom = scaffoldPadding.calculateBottomPadding() + MiniPlayerHeight + 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(
                        onClick = { onEditTheme("new") },
                        shape = RoundedCornerShape(26.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 4.dp,
                            pressedElevation = 6.dp
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Rounded.PlaylistAdd,
                                contentDescription = stringResource(R.string.cd_create_new_playlist),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = stringResource(R.string.library_action_new),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    FilledTonalIconButton(
                        onClick = { showSortSheet = true },
                        shape = RoundedCornerShape(26.dp),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.Sort,
                            contentDescription = stringResource(R.string.cd_sort_options)
                        )
                    }
                }
            }
            items(displaySchemes, key = { it.id }) { entry ->
                ColorSchemeRow(
                    name = entry.name,
                    roles = entry.roles,
                    selected = selectedId == entry.id,
                    onClick = { viewModel.selectScheme(entry.id) },
                    onEdit = entry.custom?.let { { onEditTheme(it.id) } },
                    onExport = entry.custom?.let { scheme ->
                        {
                            exportScheme = scheme
                            exportLauncher.launch("${scheme.name}.json")
                        }
                    },
                    onDelete = entry.custom?.let { scheme -> { deleteScheme = scheme } },
                    onCopy = if (entry.id == ColorSchemeIds.DYNAMIC) null
                    else ({ onEditTheme("copy_${entry.id}") })
                )
            }
        }
    }

    if (showSortSheet) {
        ColorSchemeSortSheet(
            selected = sort,
            onOptionSelected = {
                viewModel.setColorSchemeSort(it)
                showSortSheet = false
            },
            onDismissRequest = { showSortSheet = false }
        )
    }

    deleteScheme?.let { scheme ->
        AlertDialog(
            onDismissRequest = { deleteScheme = null },
            title = { Text(stringResource(R.string.color_scheme_delete_title)) },
            text = { Text(stringResource(R.string.color_scheme_delete_message, scheme.name)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteCustomScheme(scheme.id)
                    deleteScheme = null
                    Toast.makeText(
                        context,
                        context.getString(R.string.color_scheme_deleted),
                        Toast.LENGTH_SHORT
                    ).show()
                }) {
                    Text(stringResource(R.string.color_scheme_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteScheme = null }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun ColorSchemeRow(
    name: String,
    roles: SchemeRoles?,
    selected: Boolean,
    onClick: () -> Unit,
    onEdit: (() -> Unit)? = null,
    onExport: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onCopy: (() -> Unit)? = null
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.surfaceContainer
    val contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
    else MaterialTheme.colorScheme.onSurface
    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = colorSchemeRowShape,
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 72.dp)
            .clip(colorSchemeRowShape)
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Box(
                modifier = Modifier.width(68.dp),
                contentAlignment = Alignment.Center
            ) {
                if (selected) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.Check,
                            contentDescription = stringResource(R.string.color_scheme_selected),
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else if (roles != null) {
                    SchemeStack(roles = roles, borderColor = containerColor)
                } else {
                    Icon(
                        Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp, end = 8.dp)
            )
            if (onEdit != null || onCopy != null || onExport != null || onDelete != null) {
                Box {
                    FilledIconButton(
                        onClick = { menuExpanded = true },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = if (selected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = null)
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        if (onEdit != null) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.color_scheme_edit)) },
                                leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    onEdit()
                                }
                            )
                        }
                        if (onCopy != null) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.color_scheme_copy)) },
                                leadingIcon = { Icon(Icons.Rounded.ContentCopy, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    onCopy()
                                }
                            )
                        }
                        if (onExport != null) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.color_scheme_export)) },
                                leadingIcon = { Icon(Icons.Rounded.Upload, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    onExport()
                                }
                            )
                        }
                        if (onDelete != null) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.color_scheme_delete)) },
                                leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    onDelete()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SchemeStack(roles: SchemeRoles, borderColor: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(-8.dp)) {
        listOf(roles.primary, roles.secondary, roles.background).forEach { hex ->
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(hexToColor(hex))
                    .border(
                        width = 2.dp,
                        color = borderColor,
                        shape = CircleShape
                    )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColorSchemeSortSheet(
    selected: ColorSchemeSort,
    onOptionSelected: (ColorSchemeSort) -> Unit,
    onDismissRequest: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismissRequest) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = stringResource(R.string.color_scheme_sort_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 8.dp)
            )
            ColorSchemeSort.entries.forEach { option ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOptionSelected(option) }
                        .padding(horizontal = 24.dp, vertical = 14.dp)
                ) {
                    Text(
                        text = stringResource(option.labelRes),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    if (option == selected) {
                        Icon(
                            Icons.Rounded.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Spacer(modifier = Modifier.size(24.dp))
                    }
                }
            }
        }
    }
}
