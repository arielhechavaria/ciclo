package com.isora.ciclo.agenda

import com.isora.ciclo.ui.CicloColors
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.isora.ciclo.R
import com.isora.ciclo.patients.PatientId
import com.isora.ciclo.ui.CicloPalette
import java.time.LocalDate
import java.time.temporal.ChronoUnit

private val NAME_WIDTH = 96.dp
private val ROW_HEIGHT = 56.dp
private val DAY_WIDTH = 12.dp
private const val WEEKS_BEFORE = 2L
private const val MIN_WEEKS_AHEAD = 12L
private const val MAX_WEEKS = 78L

/**
 * One row per patient in treatment. Each block is a cycle, each mark a session, and the vertical
 * line is today. The names stay fixed on the left while the weeks scroll sideways.
 */
@Composable
fun TimelineView(snapshot: CareSnapshot, today: LocalDate, onOpenPatient: (PatientId) -> Unit) {
    val rows = snapshot.active(today).sortedBy { it.name.lowercase() }
    if (rows.isEmpty()) {
        Text(stringResource(R.string.agenda_empty))
        return
    }
    val rangeStart = weekStart(today).minusWeeks(WEEKS_BEFORE)
    val naturalEnd = rows.mapNotNull { it.treatment.treatment.endDate }.maxOrNull() ?: today
    val rangeEnd = maxOf(naturalEnd, today.plusWeeks(MIN_WEEKS_AHEAD)).let {
        minOf(it, rangeStart.plusWeeks(MAX_WEEKS))
    }
    val weeks = (ChronoUnit.DAYS.between(rangeStart, rangeEnd) / 7 + 1).toInt()
    val totalWidth = DAY_WIDTH * (weeks * 7)

    val cycleColor = CicloPalette.cycle
    val scroll = rememberScrollState()
    val density = LocalDensity.current
    val dayPx = with(density) { DAY_WIDTH.toPx() }
    val markRadius = with(density) { 5.dp.toPx() }
    val insetPx = with(density) { 10.dp.toPx() }

    Column {
        Text(stringResource(R.string.agenda_scroll_hint), style = MaterialTheme.typography.bodySmall)
        Legend()
        Column(Modifier.verticalScroll(rememberScrollState())) {
            Row {
                Column {
                    Box(Modifier.width(NAME_WIDTH).height(28.dp))
                    rows.forEach { entry ->
                        Box(
                            Modifier.width(NAME_WIDTH).height(ROW_HEIGHT).clickable { onOpenPatient(entry.patient.id) },
                            contentAlignment = Alignment.CenterStart,
                        ) { Text(entry.name, maxLines = 2, style = MaterialTheme.typography.bodyMedium) }
                    }
                }
                Column(Modifier.horizontalScroll(scroll)) {
                    Row(Modifier.width(totalWidth).height(28.dp)) {
                        for (week in 0 until weeks) {
                            val start = rangeStart.plusWeeks(week.toLong())
                            Box(Modifier.width(DAY_WIDTH * 7)) {
                                Text("${start.dayOfMonth}/${start.monthValue}", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                    rows.forEach { entry ->
                        val cycles = cyclesOverlapping(entry, rangeStart, rangeEnd.plusDays(1))
                        Canvas(
                            Modifier.width(totalWidth).height(ROW_HEIGHT).clickable { onOpenPatient(entry.patient.id) },
                        ) {
                            fun x(date: LocalDate) = ChronoUnit.DAYS.between(rangeStart, date) * dayPx
                            for (week in 0 until weeks) {
                                drawLine(
                                    CicloColors.hairlineSoft,
                                    Offset(week * 7 * dayPx, 0f),
                                    Offset(week * 7 * dayPx, size.height),
                                )
                            }
                            cycles.forEach { cycle ->
                                drawRoundRect(
                                    color = cycleColor,
                                    topLeft = Offset(x(cycle.start), insetPx),
                                    size = Size(
                                        (ChronoUnit.DAYS.between(cycle.start, cycle.endExclusive) * dayPx - 2f).coerceAtLeast(2f),
                                        size.height - 2 * insetPx,
                                    ),
                                    cornerRadius = CornerRadius(8f, 8f),
                                )
                                cycle.sessions.forEach { s ->
                                    val color = when {
                                        s.isLast -> CicloPalette.lastSession
                                        s.isPostponed -> CicloPalette.postponed
                                        s.given -> CicloPalette.sessionGiven
                                        else -> CicloPalette.session
                                    }
                                    drawCircle(
                                        color,
                                        if (s.isLast) markRadius * 1.4f else markRadius,
                                        Offset(x(s.date) + dayPx / 2, size.height / 2),
                                    )
                                }
                            }
                            val todayX = x(today) + dayPx / 2
                            drawLine(CicloPalette.today, Offset(todayX, 0f), Offset(todayX, size.height), strokeWidth = 3f)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Legend() {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)) {
        LegendItem(CicloPalette.cycle, R.string.legend_cycle)
        LegendItem(CicloPalette.session, R.string.legend_session)
        LegendItem(CicloPalette.lastSession, R.string.legend_last)
        LegendItem(CicloPalette.postponed, R.string.legend_postponed)
    }
}

@Composable
private fun LegendItem(color: Color, label: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).background(color, CircleShape))
        Text(stringResource(label), Modifier.padding(start = 4.dp), style = MaterialTheme.typography.labelSmall)
    }
}
