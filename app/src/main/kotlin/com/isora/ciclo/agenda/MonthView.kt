package com.isora.ciclo.agenda

import com.isora.ciclo.ui.CicloColors
import com.isora.ciclo.ui.CicloSpacing
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.isora.ciclo.R
import com.isora.ciclo.patients.PatientId
import com.isora.ciclo.ui.CicloPalette
import com.isora.ciclo.ui.formatShort
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val monthTitle = DateTimeFormatter.ofPattern("MMMM yyyy", Locale("es"))
private val weekdayLetters = listOf("L", "M", "X", "J", "V", "S", "D")

/** Design D: a month grid with a badge counting each day's sessions; tapping a day lists its patients. */
@Composable
fun MonthView(snapshot: CareSnapshot, today: LocalDate, onOpenPatient: (PatientId) -> Unit) {
    var month by remember { mutableStateOf(YearMonth.from(today)) }
    var selected by remember { mutableStateOf<LocalDate?>(today) }

    val gridStart = weekStart(month.atDay(1))
    val gridEnd = weekStart(month.atEndOfMonth()).plusDays(7)
    val byDay = snapshot.sessionsBetween(gridStart, gridEnd).groupBy { it.session.date }

    Column(Modifier.verticalScroll(rememberScrollState())) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { month = month.minusMonths(1) }) { Text(stringResource(R.string.month_previous)) }
            Text(
                monthTitle.format(month).replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.titleMedium,
            )
            TextButton(onClick = { month = month.plusMonths(1) }) { Text(stringResource(R.string.month_next)) }
        }
        Row(Modifier.fillMaxWidth()) {
            weekdayLetters.forEach {
                Text(it, Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
        var weekFirst = gridStart
        while (weekFirst < gridEnd) {
            Row(Modifier.fillMaxWidth()) {
                for (offset in 0L until 7L) {
                    val day = weekFirst.plusDays(offset)
                    DayCell(
                        day = day,
                        inMonth = YearMonth.from(day) == month,
                        isToday = day == today,
                        isSelected = day == selected,
                        count = byDay[day]?.size ?: 0,
                        modifier = Modifier.weight(1f),
                        onClick = { selected = day },
                    )
                }
            }
            weekFirst = weekFirst.plusDays(7)
        }
        selected?.let { day ->
            val list = byDay[day].orEmpty().ifEmpty { snapshot.sessionsOn(day) }
            Text(
                day.formatShort(),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = CicloSpacing.md, bottom = CicloSpacing.xxs),
            )
            if (list.isEmpty()) Text(stringResource(R.string.week_no_sessions), style = MaterialTheme.typography.bodySmall)
            list.forEach { item ->
                Column(Modifier.fillMaxWidth().clickable { onOpenPatient(item.entry.patient.id) }.padding(vertical = CicloSpacing.xs)) {
                    Text(item.entry.name, style = MaterialTheme.typography.titleSmall)
                    Text(
                        stringResource(R.string.week_session_line, item.session.ref.cycle, item.session.ref.session),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    day: LocalDate,
    inMonth: Boolean,
    isToday: Boolean,
    isSelected: Boolean,
    count: Int,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val outline = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isToday -> CicloPalette.today
        else -> Color.Transparent
    }
    Box(
        modifier.aspectRatio(1f).padding(CicloSpacing.xxs).border(1.dp, outline, CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                day.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = if (inMonth) MaterialTheme.colorScheme.onSurface else CicloColors.muted,
            )
            if (count > 0) {
                Box(Modifier.background(CicloPalette.session, CircleShape).padding(horizontal = CicloSpacing.xs)) {
                    Text(count.toString(), color = CicloColors.onPrimary, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
