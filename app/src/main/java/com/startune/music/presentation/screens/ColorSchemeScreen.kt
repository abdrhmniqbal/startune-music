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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.startune.music.R
import com.startune.music.data.preferences.AppColorScheme
import com.startune.music.data.preferences.SchemeRoles
import com.startune.music.presentation.components.MiniPlayerHeight
import com.startune.music.presentation.viewmodel.ColorSchemeImportResult
import com.startune.music.presentation.viewmodel.ColorSchemeViewModel
import com.startune.music.ui.theme.BuiltInColorSchemes
import com.startune.music.data.preferences.ColorSchemeIds
import com.startune.music.ui.theme.LocalStartuneDarkTheme
import com.startune.music.ui.theme.hexToColor
import kotlinx.coroutines.launch

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
    val isDark = LocalStartuneDarkTheme.current

    var exportScheme by remember { mutableStateOf<AppColorScheme?>(null) }
    var deleteScheme by remember { mutableStateOf<AppColorScheme?>(null) }

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
                    IconButton(onClick = { importLauncher.launch(arrayOf("application/json")) }) {
                        Icon(
                            Icons.Rounded.FileDownload,
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
                FilledTonalButton(
                    onClick = { onEditTheme("new") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Text(text = stringResource(R.string.color_scheme_create))
                }
            }
            item {
                Text(
                    text = stringResource(R.string.color_scheme_builtin),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item {
                ColorSchemeRow(
                    name = stringResource(R.string.color_scheme_dynamic_name),
                    subtitle = stringResource(R.string.color_scheme_dynamic_sub),
                    roles = null,
                    selected = selectedId == ColorSchemeIds.DYNAMIC,
                    onClick = { viewModel.selectScheme(ColorSchemeIds.DYNAMIC) }
                )
            }
            items(BuiltInColorSchemes.all, key = { it.id }) { scheme ->
                val roles = if (isDark) scheme.dark else scheme.light
                ColorSchemeRow(
                    name = scheme.name,
                    subtitle = null,
                    roles = roles,
                    selected = selectedId == scheme.id,
                    onClick = { viewModel.selectScheme(scheme.id) }
                )
            }

            if (customs.isNotEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.color_scheme_custom),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }
            items(customs, key = { it.id }) { scheme ->
                val roles = if (isDark) scheme.dark else scheme.light
                ColorSchemeRow(
                    name = scheme.name,
                    subtitle = null,
                    roles = roles,
                    selected = selectedId == scheme.id,
                    onClick = { viewModel.selectScheme(scheme.id) },
                    onEdit = { onEditTheme(scheme.id) },
                    onExport = {
                        exportScheme = scheme
                        exportLauncher.launch("${scheme.name}.json")
                    },
                    onDelete = { deleteScheme = scheme }
                )
            }
        }
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
    subtitle: String?,
    roles: SchemeRoles?,
    selected: Boolean,
    onClick: () -> Unit,
    onEdit: (() -> Unit)? = null,
    onExport: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    var menuExpanded by remember { mutableStateOf(false) }
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
            if (roles != null) {
                SchemeDots(roles = roles)
            } else {
                Icon(
                    Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp, end = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(text = name, style = MaterialTheme.typography.titleMedium)
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (onEdit != null || onExport != null || onDelete != null) {
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
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
            RadioButton(selected = selected, onClick = null)
        }
    }
}

@Composable
private fun SchemeDots(roles: SchemeRoles) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(roles.primary, roles.secondary, roles.background).forEach { hex ->
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(hexToColor(hex))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = CircleShape
                    )
            )
        }
    }
}
