package com.isora.ciclo.protocols

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.isora.ciclo.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

/** What the open form is doing: adding a protocol, or editing the one with [editing]. */
private data class FormState(val editing: CatalogProtocol?, val form: ProtocolForm)

/**
 * Catalog management: list, add, edit and archive protocols. Catalog calls are blocking, so they run
 * on [io]; tests pass an empty context to stay on the test thread.
 */
@Composable
fun ProtocolCatalogScreen(catalog: ProtocolCatalog, io: CoroutineContext = Dispatchers.IO) {
    val scope = rememberCoroutineScope()
    var showArchived by remember { mutableStateOf(false) }
    var items by remember { mutableStateOf(emptyList<CatalogProtocol>()) }
    var formState by remember { mutableStateOf<FormState?>(null) }
    var errors by remember { mutableStateOf(emptySet<ProtocolFormError>()) }
    var duplicateName by remember { mutableStateOf(false) }

    suspend fun reload() {
        items = withContext(io) { catalog.all(includeArchived = showArchived) }
    }

    fun change(action: () -> Unit) {
        scope.launch {
            withContext(io) { action() }
            reload()
        }
    }

    LaunchedEffect(showArchived) { reload() }

    fun closeForm() {
        formState = null
        errors = emptySet()
        duplicateName = false
    }

    fun save(state: FormState) {
        when (val result = state.form.validate()) {
            is ProtocolFormResult.Invalid -> errors = result.errors
            is ProtocolFormResult.Valid -> scope.launch {
                val saved = withContext(io) {
                    try {
                        val editing = state.editing
                        if (editing == null) catalog.add(result.protocol) else catalog.update(editing.id, result.protocol)
                        true
                    } catch (e: DuplicateProtocolNameException) {
                        false
                    }
                }
                if (saved) {
                    closeForm()
                    reload()
                } else {
                    errors = emptySet()
                    duplicateName = true
                }
            }
        }
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { formState = FormState(null, ProtocolForm()) }) {
                Text(stringResource(R.string.protocols_add))
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding).padding(horizontal = 16.dp)) {
            Text(
                stringResource(R.string.protocols_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(vertical = 16.dp),
            )
            LabeledSwitch(
                label = stringResource(R.string.protocols_show_archived),
                checked = showArchived,
                onChange = { showArchived = it },
            )
            if (items.isEmpty()) Text(stringResource(R.string.protocols_empty))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(items, key = { it.id.value }) { entry ->
                    ProtocolRow(
                        entry = entry,
                        onEdit = { formState = FormState(entry, ProtocolForm.from(entry.protocol)) },
                        onToggleArchived = {
                            change { if (entry.archived) catalog.unarchive(entry.id) else catalog.archive(entry.id) }
                        },
                    )
                }
            }
        }
    }

    formState?.let { state ->
        ProtocolFormDialog(
            state = state,
            errors = errors,
            duplicateName = duplicateName,
            onChange = { formState = state.copy(form = it) },
            onSave = { save(state) },
            onCancel = ::closeForm,
        )
    }
}

@Composable
private fun ProtocolRow(entry: CatalogProtocol, onEdit: () -> Unit, onToggleArchived: () -> Unit) {
    Column(Modifier.fillMaxWidth().testTag("protocol-${entry.protocol.name}")) {
        Text(entry.protocol.name, style = MaterialTheme.typography.titleMedium)
        Text(protocolSummary(entry.protocol), style = MaterialTheme.typography.bodyMedium)
        Row {
            if (entry.archived) Text(stringResource(R.string.protocol_archived_badge), Modifier.padding(end = 8.dp))
            TextButton(onClick = onEdit) { Text(stringResource(R.string.protocol_edit)) }
            TextButton(onClick = onToggleArchived) {
                Text(stringResource(if (entry.archived) R.string.protocol_restore else R.string.protocol_archive))
            }
        }
    }
}

@Composable
private fun protocolSummary(protocol: Protocol): String {
    val days = protocol.regimen.sessionDays.joinToString(", ") { "D$it" }
    val cycles = when (val count = protocol.defaultCycles) {
        is CycleCount.Fixed -> pluralStringResource(R.plurals.protocol_cycles, count.cycles, count.cycles)
        is CycleCount.UntilProgression ->
            stringResource(R.string.protocol_until_progression, count.reevaluateEveryCycles)
    }
    return stringResource(R.string.protocol_summary, protocol.regimen.cycleLengthDays, days, cycles)
}

@Composable
private fun ProtocolFormDialog(
    state: FormState,
    errors: Set<ProtocolFormError>,
    duplicateName: Boolean,
    onChange: (ProtocolForm) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    val form = state.form
    val focus = LocalFocusManager.current

    @Composable
    fun field(label: Int, value: String, error: Int?, onValue: (String) -> Unit) = OutlinedTextField(
        value = value,
        onValueChange = onValue,
        label = { Text(stringResource(label)) },
        isError = error != null,
        supportingText = error?.let { { Text(stringResource(it)) } },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )

    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Text(
                stringResource(
                    if (state.editing == null) R.string.protocol_form_new_title else R.string.protocol_form_edit_title,
                ),
            )
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                field(
                    R.string.protocol_form_name,
                    form.name,
                    when {
                        ProtocolFormError.NAME_REQUIRED in errors -> R.string.protocol_error_name
                        duplicateName -> R.string.protocol_error_duplicate
                        else -> null
                    },
                ) { onChange(form.copy(name = it)) }
                field(
                    R.string.protocol_form_cycle_length,
                    form.cycleLength,
                    R.string.protocol_error_cycle_length.takeIf { ProtocolFormError.CYCLE_LENGTH_INVALID in errors },
                ) { onChange(form.copy(cycleLength = it)) }
                field(
                    R.string.protocol_form_session_days,
                    form.sessionDays,
                    R.string.protocol_error_session_days.takeIf { ProtocolFormError.SESSION_DAYS_INVALID in errors },
                ) { onChange(form.copy(sessionDays = it)) }
                LabeledSwitch(
                    label = stringResource(R.string.protocol_form_until_progression),
                    checked = form.untilProgression,
                    onChange = { onChange(form.copy(untilProgression = it)) },
                )
                field(
                    if (form.untilProgression) R.string.protocol_form_reevaluate else R.string.protocol_form_cycles,
                    form.cycles,
                    R.string.protocol_error_cycles.takeIf { ProtocolFormError.CYCLES_INVALID in errors },
                ) { onChange(form.copy(cycles = it)) }
            }
        },
        confirmButton = { TextButton(onClick = { focus.clearFocus(); onSave() }) { Text(stringResource(R.string.protocol_form_save)) } },
        dismissButton = { TextButton(onClick = onCancel) { Text(stringResource(R.string.protocol_form_cancel)) } },
    )
}

/** A switch whose label is part of the touch target, so tapping the text toggles it. */
@Composable
private fun LabeledSwitch(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.toggleable(value = checked, role = Role.Switch, onValueChange = onChange).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Switch(checked = checked, onCheckedChange = null)
        Text(label, Modifier.padding(start = 8.dp))
    }
}
