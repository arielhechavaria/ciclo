package com.isora.ciclo.agenda

import com.isora.ciclo.ui.CicloColors
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.foundation.shape.CircleShape
import com.isora.ciclo.ui.CicloSpacing
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.isora.ciclo.R
import com.isora.ciclo.patients.PatientId
import java.time.LocalDate

private enum class AgendaMode(val label: Int) {
    WEEK(R.string.agenda_week),
    MONTH(R.string.agenda_month),
    LINE(R.string.agenda_line),
}

/** The Agenda tab: "Semana · Mes · Línea", with the timeline as the default. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgendaScreen(snapshot: CareSnapshot, today: LocalDate, onOpenPatient: (PatientId) -> Unit) {
    var mode by remember { mutableStateOf(AgendaMode.LINE) }
    Column(Modifier.fillMaxSize().padding(horizontal = CicloSpacing.md)) {
        Text(
            stringResource(R.string.agenda_title),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(vertical = CicloSpacing.md),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(CicloSpacing.xs), modifier = Modifier.padding(bottom = CicloSpacing.sm)) {
            AgendaMode.values().forEach { option ->
                FilterChip(
                    selected = mode == option,
                    onClick = { mode = option },
                    label = { Text(stringResource(option.label)) },
                    shape = CircleShape,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CicloColors.primary,
                        selectedLabelColor = CicloColors.onPrimary,
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = mode == option,
                        borderColor = CicloColors.hairline,
                        selectedBorderColor = CicloColors.primary,
                    ),
                )
            }
        }
        when (mode) {
            AgendaMode.LINE -> TimelineView(snapshot, today, onOpenPatient)
            AgendaMode.WEEK -> WeekView(snapshot, today, onOpenPatient)
            AgendaMode.MONTH -> MonthView(snapshot, today, onOpenPatient)
        }
    }
}
