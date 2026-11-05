package com.controlevendas.app.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeletorMesesBar(
    meses: List<String>,
    mesSelecionado: String?,
    onSelectMes: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val chipColors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = androidx.compose.ui.graphics.Color.White
        )
        meses.forEach { mes ->
            FilterChip(
                selected = mesSelecionado == mes,
                onClick = { onSelectMes(mes) },
                label = { Text(mes) },
                colors = chipColors
            )
        }
        FilterChip(
            selected = mesSelecionado == null || mesSelecionado == "Todos",
            onClick = { onSelectMes("Todos") },
            label = { Text("📅 Todos os Meses") },
            colors = chipColors
        )
    }
}
