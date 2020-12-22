package no.nav.klage.oppgave.clients

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.klage.oppgave.clients.KafkaOppgaveConsumer.Companion.MANGLER_HJEMMEL
import no.nav.klage.oppgave.service.HjemmelParsingService
import no.nav.klage.oppgave.service.OppgaveService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

internal class KafkaOppgaveConsumerTest {

    @Test
    fun `store found hjemmel when there is no previous hjemmel`() {
        val oppgaveServiceMock = mockk<OppgaveService>(relaxed = true)

        val hjemmelParsingServiceMock = mockk<HjemmelParsingService>()
        every { hjemmelParsingServiceMock.extractHjemmel(any()) } returns listOf("8-14")

        val oppgaveRecordMock = mockk<ConsumerRecord<String, String>>()
        every { oppgaveRecordMock.value() } returns getJsonWithHjemmelInBeskrivelse()

        val kafkaOppgaveConsumer = KafkaOppgaveConsumer(mockk(), hjemmelParsingServiceMock, oppgaveServiceMock)

        kafkaOppgaveConsumer.listen(oppgaveRecordMock)

        verify { oppgaveServiceMock.updateHjemmel(any(), any()) }
    }

    @Test
    fun `store found hjemmel when there is a previous different hjemmel`() {
        val oppgaveServiceMock = mockk<OppgaveService>(relaxed = true)

        val hjemmelParsingServiceMock = mockk<HjemmelParsingService>()
        every { hjemmelParsingServiceMock.extractHjemmel(any()) } returns listOf("22-19")

        val oppgaveRecordMock = mockk<ConsumerRecord<String, String>>()
        every { oppgaveRecordMock.value() } returns getJsonWithDifferentHjemmelInBeskrivelseAndMetadata(
            hjemmelInMetadata = "8-14"
        )

        val kafkaOppgaveConsumer = KafkaOppgaveConsumer(mockk(), hjemmelParsingServiceMock, oppgaveServiceMock)

        kafkaOppgaveConsumer.listen(oppgaveRecordMock)

        verify { oppgaveServiceMock.updateHjemmel(any(), any()) }
    }

    @Test
    fun `don't store found hjemmel when previously stored hjemmel is equal`() {
        val oppgaveServiceMock = mockk<OppgaveService>(relaxed = true)

        val hjemmelParsingServiceMock = mockk<HjemmelParsingService>()
        val hjemmel = "8-14"
        every { hjemmelParsingServiceMock.extractHjemmel(any()) } returns listOf(hjemmel)

        val oppgaveRecordMock = mockk<ConsumerRecord<String, String>>()
        every { oppgaveRecordMock.value() } returns getJsonWithDifferentHjemmelInBeskrivelseAndMetadata(
            hjemmelInMetadata = hjemmel
        )

        val kafkaOppgaveConsumer = KafkaOppgaveConsumer(mockk(), hjemmelParsingServiceMock, oppgaveServiceMock)

        kafkaOppgaveConsumer.listen(oppgaveRecordMock)

        verify(exactly = 0) { oppgaveServiceMock.updateHjemmel(any(), any()) }
    }

    @Test
    fun `don't store MANGLER when previously stored hjemmel has a value, but oppgave has beskrivelse`() {
        val oppgaveServiceMock = mockk<OppgaveService>(relaxed = true)

        val hjemmelParsingServiceMock = mockk<HjemmelParsingService>()
        every { hjemmelParsingServiceMock.extractHjemmel(any()) } returns listOf()

        val oppgaveRecordMock = mockk<ConsumerRecord<String, String>>()
        every { oppgaveRecordMock.value() } returns getJsonWithHjemmelButBeskrivelseWithout()

        val kafkaOppgaveConsumer = KafkaOppgaveConsumer(mockk(), hjemmelParsingServiceMock, oppgaveServiceMock)

        kafkaOppgaveConsumer.listen(oppgaveRecordMock)

        verify(exactly = 0) { oppgaveServiceMock.updateHjemmel(any(), any()) }
    }

    @Test
    fun `don't store MANGLER when previously stored hjemmel has a value, but oppgave has no beskrivelse`() {
        val oppgaveServiceMock = mockk<OppgaveService>(relaxed = true)

        val hjemmelParsingServiceMock = mockk<HjemmelParsingService>()
        every { hjemmelParsingServiceMock.extractHjemmel(any()) } returns listOf()

        val oppgaveRecordMock = mockk<ConsumerRecord<String, String>>()
        every { oppgaveRecordMock.value() } returns getJsonWithHjemmelButBeskrivelseIsEmpty()

        val kafkaOppgaveConsumer = KafkaOppgaveConsumer(mockk(), hjemmelParsingServiceMock, oppgaveServiceMock)

        kafkaOppgaveConsumer.listen(oppgaveRecordMock)

        verify(exactly = 0) { oppgaveServiceMock.updateHjemmel(any(), any()) }
    }

    @Test
    fun `store MANGLER if beskrivelse is empty and previous hjemmel is empty`() {
        val oppgaveServiceMock = mockk<OppgaveService>(relaxed = true)

        val hjemmelParsingServiceMock = mockk<HjemmelParsingService>()
        val hjemmel = "8-14"
        every { hjemmelParsingServiceMock.extractHjemmel(any()) } returns listOf(hjemmel)

        val oppgaveRecordMock = mockk<ConsumerRecord<String, String>>()
        every { oppgaveRecordMock.value() } returns getJsonWithoutBeskrivelse()

        val kafkaOppgaveConsumer = KafkaOppgaveConsumer(mockk(), hjemmelParsingServiceMock, oppgaveServiceMock)

        kafkaOppgaveConsumer.listen(oppgaveRecordMock)

        verify { oppgaveServiceMock.updateHjemmel(any(), MANGLER_HJEMMEL) }
    }

    @Test
    fun `store MANGLER if beskrivelse or metadata does not contain hjemmel and previous hjemmel is empty`() {
        val oppgaveServiceMock = mockk<OppgaveService>(relaxed = true)

        val hjemmelParsingServiceMock = mockk<HjemmelParsingService>()
        every { hjemmelParsingServiceMock.extractHjemmel(any()) } returns emptyList()

        val oppgaveRecordMock = mockk<ConsumerRecord<String, String>>()
        every { oppgaveRecordMock.value() } returns getJsonWithoutHjemmel()

        val kafkaOppgaveConsumer = KafkaOppgaveConsumer(mockk(), hjemmelParsingServiceMock, oppgaveServiceMock)

        kafkaOppgaveConsumer.listen(oppgaveRecordMock)

        verify { oppgaveServiceMock.updateHjemmel(any(), MANGLER_HJEMMEL) }
    }

    @Language("JSON")
    fun getJsonWithHjemmelInBeskrivelse() = """
        {
          "id": 301848147,
          "tildeltEnhetsnr": "4291",
          "endretAvEnhetsnr": "4291",
          "opprettetAvEnhetsnr": "4418",
          "tilordnetRessurs": "Z994488",
          "tema": "SYK",
          "oppgavetype": "BEH_SAK_MK",
          "behandlingstype": "ae0058",
          "versjon": 25,
          "beskrivelse": "--- 8-14",
          "someOtherField": "random"
        }
    """.trimIndent()

    @Language("JSON")
    fun getJsonWithoutBeskrivelse() = """
        {
          "id": 301848147,
          "tildeltEnhetsnr": "4291",
          "endretAvEnhetsnr": "4291",
          "opprettetAvEnhetsnr": "4418",
          "tilordnetRessurs": "Z994488",
          "tema": "SYK",
          "oppgavetype": "BEH_SAK_MK",
          "behandlingstype": "ae0058",
          "versjon": 25,
          "someOtherField": "random"
        }
    """.trimIndent()

    @Language("JSON")
    fun getJsonWithoutHjemmel() = """
        {
          "id": 301848147,
          "tildeltEnhetsnr": "4291",
          "endretAvEnhetsnr": "4291",
          "opprettetAvEnhetsnr": "4418",
          "tilordnetRessurs": "Z994488",
          "tema": "SYK",
          "oppgavetype": "BEH_SAK_MK",
          "behandlingstype": "ae0058",
          "versjon": 25,
          "beskrivelse": "--- 8-14",
          "someOtherField": "random"
        }
    """.trimIndent()

    @Language("JSON")
    fun getJsonWithDifferentHjemmelInBeskrivelseAndMetadata(hjemmelInMetadata: String) = """
        {
          "id": 301848147,
          "tildeltEnhetsnr": "4291",
          "endretAvEnhetsnr": "4291",
          "opprettetAvEnhetsnr": "4418",
          "journalpostId": "444997220",
          "tilordnetRessurs": "Z994488",
          "tema": "SYK",
          "oppgavetype": "BEH_SAK_MK",
          "behandlingstype": "ae0058",
          "versjon": 25,
          "beskrivelse": "--- 6-66",
          "someOtherField": "random",
          "metadata": {
            "HJEMMEL": "$hjemmelInMetadata"
          }
        }
    """.trimIndent()

    @Language("JSON")
    fun getJsonWithHjemmelButBeskrivelseWithout() = """
        {
          "id": 301848147,
          "tildeltEnhetsnr": "4291",
          "endretAvEnhetsnr": "4291",
          "opprettetAvEnhetsnr": "4418",
          "journalpostId": "444997220",
          "tilordnetRessurs": "Z994488",
          "tema": "SYK",
          "oppgavetype": "BEH_SAK_MK",
          "behandlingstype": "ae0058",
          "versjon": 25,
          "beskrivelse": "---",
          "someOtherField": "random",
          "metadata": {
            "HJEMMEL": "MANGLER"
          }
        }
    """.trimIndent()

    @Language("JSON")
    fun getJsonWithHjemmelButBeskrivelseIsEmpty() = """
        {
          "id": 301848147,
          "tildeltEnhetsnr": "4291",
          "endretAvEnhetsnr": "4291",
          "opprettetAvEnhetsnr": "4418",
          "journalpostId": "444997220",
          "tilordnetRessurs": "Z994488",
          "tema": "SYK",
          "oppgavetype": "BEH_SAK_MK",
          "behandlingstype": "ae0058",
          "versjon": 25,
          "beskrivelse": "",
          "someOtherField": "random",
          "metadata": {
            "HJEMMEL": "MANGLER"
          }
        }
    """.trimIndent()
}