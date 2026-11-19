package com.isora.ciclo.treatments

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.isora.ciclo.protocols.CatalogProtocol
import com.isora.ciclo.protocols.CycleCount
import com.isora.ciclo.protocols.Protocol
import com.isora.ciclo.protocols.ProtocolId
import com.isora.ciclo.protocols.Regimen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AssignTreatmentDialogTest {
    @get:Rule
    val rule = createComposeRule()

    private val protocols = listOf(
        CatalogProtocol(ProtocolId(1), Protocol("AC", Regimen(21, listOf(1)), CycleCount.Fixed(4)), archived = false),
        CatalogProtocol(ProtocolId(2), Protocol("FOLFOX", Regimen(14, listOf(1)), CycleCount.Fixed(12)), archived = false),
    )

    private fun show(preselected: String?) = rule.setContent {
        MaterialTheme { AssignTreatmentDialog(protocols, onAssign = { _, _ -> }, onCancel = {}, preselected = preselected) }
    }

    @Test
    fun startsOnThePreselectedProtocol() {
        show("FOLFOX")
        rule.onNodeWithText("FOLFOX").assertIsSelected()
        rule.onNodeWithText("AC").assertIsNotSelected()
    }

    @Test
    fun withoutAPreselectionStartsOnTheFirstProtocol() {
        show(null)
        rule.onNodeWithText("AC").assertIsSelected()
    }
}
