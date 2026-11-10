package com.startune.music.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.startune.music.R
import com.startune.music.data.model.MixRule

@Composable
private fun mixRuleTitle(rule: MixRule): String =
    stringResource(
        when (rule) {
            MixRule.TOP_PLAYED -> R.string.mix_rule_top_played_title
            MixRule.RECENTLY_PLAYED -> R.string.mix_rule_recently_played_title
            MixRule.FORGOTTEN_FAVORITES -> R.string.mix_rule_forgotten_favorites_title
            MixRule.NEW_GEMS -> R.string.mix_rule_new_gems_title
        }
    )

@Composable
private fun mixRuleSubtitle(rule: MixRule): String =
    stringResource(
        when (rule) {
            MixRule.TOP_PLAYED -> R.string.mix_rule_top_played_subtitle
            MixRule.RECENTLY_PLAYED -> R.string.mix_rule_recently_played_subtitle
            MixRule.FORGOTTEN_FAVORITES -> R.string.mix_rule_forgotten_favorites_subtitle
            MixRule.NEW_GEMS -> R.string.mix_rule_new_gems_subtitle
        }
    )

/**
 * Create-mix form: a name (required, typed by hand), one of the four presets,
 * and the song count. Same presets and count range as the For You Mix.
 */
@Composable
fun CreateMixDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    onCreate: (String, MixRule, Int) -> Unit
) {
    if (!visible) return
    var mixName by remember { mutableStateOf("") }
    var selectedRule by remember { mutableStateOf(MixRule.TOP_PLAYED) }
    var sliderValue by remember { mutableFloatStateOf(60f) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.mix_create_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = mixName,
                    onValueChange = { mixName = it },
                    label = { Text(stringResource(R.string.mix_name_label)) },
                    placeholder = { Text(stringResource(R.string.mix_name_placeholder)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = stringResource(R.string.mix_preset_label),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    MixRule.entries.forEach { rule ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedRule = rule },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedRule == rule,
                                onClick = { selectedRule = rule }
                            )
                            Column(modifier = Modifier.padding(start = 8.dp)) {
                                Text(
                                    text = mixRuleTitle(rule),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = mixRuleSubtitle(rule),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Text(
                    text = stringResource(R.string.mix_detail_size_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.mix_detail_size_songs, sliderValue.toInt()),
                    style = MaterialTheme.typography.titleMedium
                )
                Slider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    valueRange = 10f..100f,
                    steps = 17
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onCreate(mixName.trim(), selectedRule, sliderValue.toInt()) },
                enabled = mixName.isNotBlank()
            ) {
                Text(stringResource(R.string.presentation_batch_f_create))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
}
