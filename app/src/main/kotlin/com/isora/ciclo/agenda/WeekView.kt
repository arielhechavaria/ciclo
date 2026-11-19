package com.isora.ciclo.agenda

import com.isora.ciclo.ui.CicloSpacing
import com.isora.ciclo.ui.CicloCard
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.isora.ciclo.R
import com.isora.ciclo.patients.PatientId
import com.isora.ciclo.ui.CicloPalette
import com.isora.ciclo.ui.formatShort
import java.time.LocalDate

/** Design A: the sessions of the selected week grouped by day, flagging patients finishing and postponed sessions. */
@Composable
fun WeekView(snapshot: CareSnapshot, today: LocalDate, onOpenPatient: (PatientId) -> Unit) {
    var start by remember { mutableStateOf(weekStart(today)) }
    val end = start.plusDays(7)
    val sessions = snapshot.sessionsBetween(start, end).groupBy { it.session.date }
    val finishing = snapshot.finishingBetween(start, end)

    Column(Modifier.verticalScroll(rememberScrollState())) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = { start = start.minusWeeks(1) }) { Text(stringResource(R.string.week_previous)) }
            TextButton(onClick = { start = weekStart(today) }) {
                Text(stringResource(R.string.week_of, start.formatShort()))
            }
            TextButton(onClick = { start = start.plusWeeks(1) }) { Text(stringResource(R.string.week_next)) }
        }
        for (offset in 0L until 7L) {
            val day = start.plusDays(offset)
            val daySessions = sessions[day].orEmpty()
            val dayFinishing = finishing.filter { it.treatment.treatment.endDate == day }
            Text(
                day.formatShort(),
                style = MaterialTheme.typography.titleMedium,
                color = if (day == today) CicloPalette.today else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = CicloSpacing.sm, bottom = CicloSpacing.xxs),
            )
            if (daySessions.isEmpty() && dayFinishing.isEmpty()) {
                Text(stringResource(R.string.week_no_sessions), style = MaterialTheme.typography.bodySmall)
            }
            daySessions.forEach { item ->
                CicloCard(Modifier.fillMaxWidth().padding(vertical = CicloSpacing.xxs).clickable { onOpenPatient(item.entry.patient.id) }) {
                    Column(Modifier.padding(CicloSpacing.lg)) {
                        Text(item.entry.name, style = MaterialTheme.typography.titleSmall)
                        Text(
                            stringResource(R.string.week_session_line, item.session.ref.cycle, item.session.ref.session),
                            style = MaterialTheme.typography.bodySmall,
                        )
                        if (item.session.isPostponed) {
                            Text(stringResource(R.string.flag_postponed), color = CicloPalette.postponed, style = MaterialTheme.typography.labelMedium)
                        }
                        if (item.session.isLast) {
                            Text(stringResource(R.string.last_session), color = CicloPalette.lastSession, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
            dayFinishing.forEach { entry ->
                CicloCard(Modifier.fillMaxWidth().padding(vertical = CicloSpacing.xxs).clickable { onOpenPatient(entry.patient.id) }) {
                    Column(Modifier.padding(CicloSpacing.lg)) {
                        Text(entry.name, style = MaterialTheme.typography.titleSmall)
                        Text(stringResource(R.string.flag_finishing), color = CicloPalette.lastSession, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}
