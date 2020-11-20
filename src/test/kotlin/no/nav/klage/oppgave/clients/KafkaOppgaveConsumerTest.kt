package no.nav.klage.oppgave.clients

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.klage.oppgave.domain.OppgaveKafkaRecord
import no.nav.klage.oppgave.repositories.OppgaveRepository
import no.nav.klage.oppgave.service.HjemmelParsingService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.Test

internal class KafkaOppgaveConsumerTest {

    @Test
    fun `store found hjemmel when there is no previous hjemmel`() {
        val oppgaveRepositoryMock = mockk<OppgaveRepository>(relaxed = true)

        val hjemmelParsingServiceMock = mockk<HjemmelParsingService>()
        every { hjemmelParsingServiceMock.extractHjemmel(any()) } returns listOf("8-14")

        val oppgaveRecordMock = mockk<ConsumerRecord<String, OppgaveKafkaRecord>>()
        every { oppgaveRecordMock.value() } returns OppgaveKafkaRecord(
            id = 1,
            metadata = emptyMap(),
            beskrivelse = "notEmptyString"
        )

        val kafkaOppgaveConsumer = KafkaOppgaveConsumer(mockk(), hjemmelParsingServiceMock, oppgaveRepositoryMock)

        kafkaOppgaveConsumer.listen(oppgaveRecordMock)

        verify { oppgaveRepositoryMock.storeHjemmelInMetadata(any(), any()) }
    }

    @Test
    fun `store found hjemmel when there is a previous different hjemmel`() {
        val oppgaveRepositoryMock = mockk<OppgaveRepository>(relaxed = true)

        val hjemmelParsingServiceMock = mockk<HjemmelParsingService>()
        every { hjemmelParsingServiceMock.extractHjemmel(any()) } returns listOf("22-19")

        val oppgaveRecordMock = mockk<ConsumerRecord<String, OppgaveKafkaRecord>>()
        every { oppgaveRecordMock.value() } returns OppgaveKafkaRecord(
            id = 1,
            metadata = mapOf(OppgaveKafkaRecord.MetadataKey.HJEMMEL to "8-14"),
            beskrivelse = "notEmptyString"
        )

        val kafkaOppgaveConsumer = KafkaOppgaveConsumer(mockk(), hjemmelParsingServiceMock, oppgaveRepositoryMock)

        kafkaOppgaveConsumer.listen(oppgaveRecordMock)

        verify { oppgaveRepositoryMock.storeHjemmelInMetadata(any(), any()) }
    }

    @Test
    fun `don't store found hjemmel when previously stored hjemmel is equal`() {
        val oppgaveRepositoryMock = mockk<OppgaveRepository>(relaxed = true)

        val hjemmelParsingServiceMock = mockk<HjemmelParsingService>()
        val hjemmel = "8-14"
        every { hjemmelParsingServiceMock.extractHjemmel(any()) } returns listOf(hjemmel)

        val oppgaveRecordMock = mockk<ConsumerRecord<String, OppgaveKafkaRecord>>()
        every { oppgaveRecordMock.value() } returns OppgaveKafkaRecord(
            id = 1,
            metadata = mapOf(OppgaveKafkaRecord.MetadataKey.HJEMMEL to hjemmel),
            beskrivelse = "notEmptyString"
        )

        val kafkaOppgaveConsumer = KafkaOppgaveConsumer(mockk(), hjemmelParsingServiceMock, oppgaveRepositoryMock)

        kafkaOppgaveConsumer.listen(oppgaveRecordMock)

        verify(exactly = 0) { oppgaveRepositoryMock.storeHjemmelInMetadata(any(), any()) }
    }

    @Test
    fun `store MANGLER if beskrivelse is empty`() {
        val oppgaveRepositoryMock = mockk<OppgaveRepository>(relaxed = true)

        val hjemmelParsingServiceMock = mockk<HjemmelParsingService>()
        val hjemmel = "8-14"
        every { hjemmelParsingServiceMock.extractHjemmel(any()) } returns listOf(hjemmel)

        val oppgaveRecordMock = mockk<ConsumerRecord<String, OppgaveKafkaRecord>>()
        every { oppgaveRecordMock.value() } returns OppgaveKafkaRecord(id = 1, metadata = emptyMap())

        val kafkaOppgaveConsumer = KafkaOppgaveConsumer(mockk(), hjemmelParsingServiceMock, oppgaveRepositoryMock)

        kafkaOppgaveConsumer.listen(oppgaveRecordMock)

        verify { oppgaveRepositoryMock.storeHjemmelInMetadata(any(), "MANGLER") }
    }

    @Test
    fun `store MANGLER if beskrivelse does not contain hjemmel`() {
        val oppgaveRepositoryMock = mockk<OppgaveRepository>(relaxed = true)

        val hjemmelParsingServiceMock = mockk<HjemmelParsingService>()
        every { hjemmelParsingServiceMock.extractHjemmel(any()) } returns emptyList()

        val oppgaveRecordMock = mockk<ConsumerRecord<String, OppgaveKafkaRecord>>()
        every { oppgaveRecordMock.value() } returns OppgaveKafkaRecord(
            id = 1,
            metadata = emptyMap(),
            beskrivelse = "noe her som ikke er hjemmel"
        )

        val kafkaOppgaveConsumer = KafkaOppgaveConsumer(mockk(), hjemmelParsingServiceMock, oppgaveRepositoryMock)

        kafkaOppgaveConsumer.listen(oppgaveRecordMock)

        verify { oppgaveRepositoryMock.storeHjemmelInMetadata(any(), "MANGLER") }
    }
}