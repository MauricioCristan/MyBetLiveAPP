package com.mybetlive.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/** A single scrollable row; additional markets never wrap or require a dropdown. */
@Composable internal fun ScrollableChoices(selected: String, options: List<Pair<String, String>>,
    onSelected: (String) -> Unit, enabled: Boolean = true, tagPrefix: String) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).testTag("$tagPrefix-row"),
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (id, label) ->
            FilterChip(selected = selected == id, onClick = { onSelected(id) }, enabled = enabled,
                label = { LocalizedText(label, maxLines = 1) },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Green.copy(alpha = .13f),
                    selectedLabelColor = Green, labelColor = Muted),
                modifier = Modifier.testTag("$tagPrefix-$id"))
        }
    }
}
