package com.isora.ciclo.protocols

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.printToString
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.coroutines.EmptyCoroutineContext

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ProtocolCatalogScreenTest {
    @get:Rule
    val rule = createComposeRule()

    private val catalog = InMemoryProtocolCatalog()

    private fun show() = rule.setContent {
        MaterialTheme { ProtocolCatalogScreen(catalog, EmptyCoroutineContext) }
    }

    private fun scrollTo(name: String) {
        rule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("protocol-$name"))
    }

    private fun inRow(name: String, text: String) =
        hasText(text) and hasAnyAncestor(hasTestTag("protocol-$name"))

    @Test
    fun listsTheActiveProtocolsWithTheirStructure() {
        show()
        rule.onNode(inRow("AC", "Ciclo de 21 días · D1 · 4 ciclos")).assertIsDisplayed()
        scrollTo("Gemcitabina + cisplatino")
        rule.onNode(inRow("Gemcitabina + cisplatino", "Ciclo de 21 días · D1, D8 · 4 ciclos")).assertIsDisplayed()
    }

    @Test
    fun archivingMovesAProtocolOutOfTheListAndItCanBeRestored() {
        show()
        rule.onNode(inRow("AC", "Archivar")).performClick()
        rule.onNode(hasTestTag("protocol-AC")).assertDoesNotExist()

        rule.onNodeWithText("Mostrar archivados").performClick()
        println("TREE-AFTER-TOGGLE: " + rule.onRoot().printToString(60) + " ALL=" + catalog.all(true).map { it.protocol.name to it.archived })
        rule.onNode(hasTestTag("protocol-AC")).assertExists()
        rule.onNodeWithText("Archivado").assertExists()
        rule.onNode(inRow("AC", "Restaurar")).performClick()
        rule.onNodeWithText("Mostrar archivados").performClick()
        rule.onNode(hasTestTag("protocol-AC")).assertIsDisplayed()
        assertEquals(8, catalog.all().size)
    }

    @Test
    fun addsAProtocolFromTheForm() {
        show()
        rule.onNodeWithText("Añadir protocolo").performClick()
        rule.onNodeWithText("Nombre").performTextInput("FLOT")
        rule.onNodeWithText("Duración del ciclo (días)").performTextInput("14")
        rule.onNodeWithText("Días de sesión (ej. 1, 8)").performTextInput("1")
        rule.onNodeWithText("Número de ciclos").performTextInput("4")
        rule.onNodeWithText("Guardar").performClick()

        scrollTo("FLOT")
        rule.onNode(inRow("FLOT", "Ciclo de 14 días · D1 · 4 ciclos")).assertIsDisplayed()
        assertEquals(9, catalog.all().size)
    }

    @Test
    fun untilProgressionProtocolReadsTheNumberAsReevaluationInterval() {
        show()
        rule.onNodeWithText("Añadir protocolo").performClick()
        rule.onNodeWithText("Nombre").performTextInput("Trastuzumab")
        rule.onNodeWithText("Duración del ciclo (días)").performTextInput("21")
        rule.onNodeWithText("Días de sesión (ej. 1, 8)").performTextInput("1")
        rule.onNodeWithText("Hasta progresión").performClick()
        rule.onNodeWithText("Reevaluar cada (ciclos)").performTextInput("4")
        rule.onNodeWithText("Guardar").performClick()

        scrollTo("Trastuzumab")
        rule.onNode(
            inRow("Trastuzumab", "Ciclo de 21 días · D1 · hasta progresión, reevaluar cada 4 ciclos"),
        ).assertIsDisplayed()
    }

    @Test
    fun invalidFormShowsTheErrorsAndSavesNothing() {
        show()
        rule.onNodeWithText("Añadir protocolo").performClick()
        rule.onNodeWithText("Guardar").performClick()

        rule.onNodeWithText("Escribe un nombre").assertExists()
        rule.onNodeWithText("Número de días inválido").assertExists()
        rule.onNodeWithText("Días de sesión inválidos").assertExists()
        rule.onNodeWithText("Número de ciclos inválido").assertExists()
        assertEquals(8, catalog.all().size)
    }

    @Test
    fun duplicateNameShowsAnErrorAndKeepsTheForm() {
        show()
        rule.onNodeWithText("Añadir protocolo").performClick()
        rule.onNodeWithText("Nombre").performTextInput("AC")
        rule.onNodeWithText("Duración del ciclo (días)").performTextInput("21")
        rule.onNodeWithText("Días de sesión (ej. 1, 8)").performTextInput("1")
        rule.onNodeWithText("Número de ciclos").performTextInput("4")
        rule.onNodeWithText("Guardar").performClick()

        rule.onNodeWithText("Ya existe un protocolo con ese nombre").assertExists()
        assertEquals(8, catalog.all().size)
    }

    @Test
    fun editsAnExistingProtocol() {
        show()
        rule.onNode(inRow("AC", "Editar")).performClick()
        rule.onNodeWithText("Número de ciclos").performTextClearance()
        rule.onNodeWithText("Número de ciclos").performTextInput("6")
        rule.onNodeWithText("Guardar").performClick()

        rule.onNode(inRow("AC", "Ciclo de 21 días · D1 · 6 ciclos")).assertIsDisplayed()
        assertEquals(CycleCount.Fixed(6), catalog.all().first { it.protocol.name == "AC" }.protocol.defaultCycles)
    }

    @Test
    fun cancelLeavesTheCatalogUntouched() {
        show()
        rule.onNodeWithText("Añadir protocolo").performClick()
        rule.onNodeWithText("Cancelar").performClick()
        rule.onNodeWithText("Guardar").assertDoesNotExist()
        assertEquals(8, catalog.all().size)
    }
}
