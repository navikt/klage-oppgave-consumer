package no.nav.klage.oppgave.clients

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.klage.oppgave.clients.KafkaOppgaveConsumer.Companion.MANGLER_HJEMMEL
import no.nav.klage.oppgave.domain.OppgaveKafkaRecord
import no.nav.klage.oppgave.facades.OppgaveFacade
import no.nav.klage.oppgave.services.HjemmelParsingService
import no.nav.klage.oppgave.services.OppgaveApiService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

internal class KafkaOppgaveConsumerTest {

    @Test
    fun `store found hjemmel when there is no previous hjemmel`() {
        val oppgaveFacade = mockk<OppgaveFacade>(relaxed = true)

        val hjemmelParsingServiceMock = mockk<HjemmelParsingService>()
        every { hjemmelParsingServiceMock.extractHjemmel(any()) } returns listOf("8-14")

        val oppgaveRecordMock = mockk<ConsumerRecord<String, String>>()
        every { oppgaveRecordMock.value() } returns getJsonWithHjemmelInBeskrivelse()

        val kafkaOppgaveConsumer =
            KafkaOppgaveConsumer(mockk(), hjemmelParsingServiceMock, oppgaveFacade)

        kafkaOppgaveConsumer.listen(oppgaveRecordMock)

        verify { oppgaveFacade.updateHjemmel(any(), any()) }
    }

    @Test
    fun `store found hjemmel when there is a previous different hjemmel`() {
        val oppgaveFacade = mockk<OppgaveFacade>(relaxed = true)

        val hjemmelParsingServiceMock = mockk<HjemmelParsingService>()
        every { hjemmelParsingServiceMock.extractHjemmel(any()) } returns listOf("22-19")

        val oppgaveRecordMock = mockk<ConsumerRecord<String, String>>()
        every { oppgaveRecordMock.value() } returns getJsonWithDifferentHjemmelInBeskrivelseAndMetadata(
            hjemmelInMetadata = "8-14"
        )

        val kafkaOppgaveConsumer =
            KafkaOppgaveConsumer(mockk(), hjemmelParsingServiceMock, oppgaveFacade)

        kafkaOppgaveConsumer.listen(oppgaveRecordMock)

        verify { oppgaveFacade.updateHjemmel(any(), any()) }
    }

    @Test
    fun `don't store found hjemmel when previously stored hjemmel is equal`() {
        val oppgaveFacade = mockk<OppgaveFacade>(relaxed = true)

        val hjemmelParsingServiceMock = mockk<HjemmelParsingService>()
        val hjemmel = "8-14"
        every { hjemmelParsingServiceMock.extractHjemmel(any()) } returns listOf(hjemmel)

        val oppgaveRecordMock = mockk<ConsumerRecord<String, String>>()
        every { oppgaveRecordMock.value() } returns getJsonWithDifferentHjemmelInBeskrivelseAndMetadata(
            hjemmelInMetadata = hjemmel
        )

        val kafkaOppgaveConsumer =
            KafkaOppgaveConsumer(mockk(), hjemmelParsingServiceMock, oppgaveFacade)

        kafkaOppgaveConsumer.listen(oppgaveRecordMock)

        verify(exactly = 0) { oppgaveFacade.updateHjemmel(any(), any()) }
    }

    @Test
    fun `don't store MANGLER when previously stored hjemmel has a value, but oppgave has beskrivelse`() {
        val oppgaveFacade = mockk<OppgaveFacade>(relaxed = true)

        val hjemmelParsingServiceMock = mockk<HjemmelParsingService>()
        every { hjemmelParsingServiceMock.extractHjemmel(any()) } returns listOf()

        val oppgaveRecordMock = mockk<ConsumerRecord<String, String>>()
        every { oppgaveRecordMock.value() } returns getJsonWithHjemmelButBeskrivelseWithout()

        val kafkaOppgaveConsumer =
            KafkaOppgaveConsumer(mockk(), hjemmelParsingServiceMock, oppgaveFacade)

        kafkaOppgaveConsumer.listen(oppgaveRecordMock)

        verify(exactly = 0) { oppgaveFacade.updateHjemmel(any(), any()) }
    }

    @Test
    fun `don't store MANGLER when previously stored hjemmel has a value, but oppgave has no beskrivelse`() {
        val oppgaveFacade = mockk<OppgaveFacade>(relaxed = true)

        val hjemmelParsingServiceMock = mockk<HjemmelParsingService>()
        every { hjemmelParsingServiceMock.extractHjemmel(any()) } returns listOf()

        val oppgaveRecordMock = mockk<ConsumerRecord<String, String>>()
        every { oppgaveRecordMock.value() } returns getJsonWithHjemmelButBeskrivelseIsEmpty()

        val kafkaOppgaveConsumer =
            KafkaOppgaveConsumer(mockk(), hjemmelParsingServiceMock, oppgaveFacade)

        kafkaOppgaveConsumer.listen(oppgaveRecordMock)

        verify(exactly = 0) { oppgaveFacade.updateHjemmel(any(), any()) }
    }

    @Test
    fun `store MANGLER if beskrivelse is empty and previous hjemmel is empty`() {
        val oppgaveFacade = mockk<OppgaveFacade>(relaxed = true)

        val hjemmelParsingServiceMock = mockk<HjemmelParsingService>()
        val hjemmel = "8-14"
        every { hjemmelParsingServiceMock.extractHjemmel(any()) } returns listOf(hjemmel)

        val oppgaveRecordMock = mockk<ConsumerRecord<String, String>>()
        every { oppgaveRecordMock.value() } returns getJsonWithoutBeskrivelse()

        val kafkaOppgaveConsumer =
            KafkaOppgaveConsumer(mockk(), hjemmelParsingServiceMock, oppgaveFacade)

        kafkaOppgaveConsumer.listen(oppgaveRecordMock)

        verify { oppgaveFacade.updateHjemmel(any(), MANGLER_HJEMMEL) }
    }

    @Test
    fun `store MANGLER if beskrivelse or metadata does not contain hjemmel and previous hjemmel is empty`() {
        val oppgaveFacade = mockk<OppgaveFacade>(relaxed = true)

        val hjemmelParsingServiceMock = mockk<HjemmelParsingService>()
        every { hjemmelParsingServiceMock.extractHjemmel(any()) } returns emptyList()

        val oppgaveRecordMock = mockk<ConsumerRecord<String, String>>()
        every { oppgaveRecordMock.value() } returns getJsonWithoutHjemmel()

        val kafkaOppgaveConsumer =
            KafkaOppgaveConsumer(mockk(), hjemmelParsingServiceMock, oppgaveFacade)

        kafkaOppgaveConsumer.listen(oppgaveRecordMock)

        verify { oppgaveFacade.updateHjemmel(any(), MANGLER_HJEMMEL) }
    }

    @Test
    fun `parsing all fields does not throw exception`() {
        val mapper = jacksonObjectMapper().registerModule(JavaTimeModule())
        mapper.readValue(getKafkaInput(), OppgaveKafkaRecord::class.java)
    }

    @Language("json")
    fun getKafkaInput() = """
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
          "beskrivelse": "--- 25.11.2020 13:09 F_Z994488 E_Z994488 (Z994488, 4291) ---\nTest 7\n\n--- 25.11.2020 12:59 F_Z994488 E_Z994488 (Z994488, 4291) ---\nTest 6\n\n--- 25.11.2020 12:46 F_Z994488 E_Z994488 (Z994488, 4291) ---\nTest 5\n\n--- 25.11.2020 12:36 F_Z994488 E_Z994488 (Z994488, 4291) ---\nTest 4\n\n--- 24.11.2020 22:15 F_Z994488 E_Z994488 (Z994488, 4291) ---\nTest 3\n\n--- 24.11.2020 21:51 F_Z994488 E_Z994488 (Z994488, 4291) ---\nTest 2\n\n--- 24.11.2020 08:54 F_Z994488 E_Z994488 (Z994488, 4291) ---\nTest\n\nMASKERT",
          "fristFerdigstillelse": "2019-05-01",
          "opprettetTidspunkt": "2019-02-12T10:47:14.846+01:00",
          "aktivDato": "2019-02-12",
          "opprettetAv": "L105731",
          "endretAv": "Z994488",
          "endretTidspunkt": "2020-12-20T12:00:00.000+01:00",
          "prioritet": "NORM",
          "status": "AAPNET",
          "statuskategori": "AAPEN",
          "ident": {
            "identType": "AKTOERID",
            "verdi": "1000098656903",
            "folkeregisterident": "12098227111",
            "registrert_dato": {
              "year": 2020,
              "month": "JULY",
              "monthValue": 7,
              "dayOfMonth": 14,
              "chronology": {
                "calendarType": "iso8601",
                "id": "ISO"
              },
              "era": "CE",
              "dayOfWeek": "TUESDAY",
              "leapYear": true,
              "dayOfYear": 196
            }
          },
          "mappeId": 100024220,
          "metadata": {
            "HJEMMEL": "8-22"
          }
        }
    """.trimIndent()

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
          "status": "AAPNET",
          "statuskategori": "AAPEN",
          "prioritet": "NORM",
          "aktivDato": "2019-02-12",
          "opprettetAv": "L105731",
          "opprettetTidspunkt": "2019-02-12T10:47:14.846+01:00",
          "versjon": 25,
          "beskrivelse": "--- 8-14"
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
          "status": "AAPNET",
          "statuskategori": "AAPEN",
          "prioritet": "NORM",
          "aktivDato": "2019-02-12",
          "opprettetAv": "L105731",     
          "opprettetTidspunkt": "2019-02-12T10:47:14.846+01:00",              
          "versjon": 25
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
          "status": "AAPNET",
          "statuskategori": "AAPEN",
          "prioritet": "NORM",
          "aktivDato": "2019-02-12",
          "opprettetAv": "L105731",     
          "opprettetTidspunkt": "2019-02-12T10:47:14.846+01:00",              
          "versjon": 25,
          "beskrivelse": "--- 8-14"
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
          "status": "AAPNET",
          "statuskategori": "AAPEN",
          "prioritet": "NORM",
          "aktivDato": "2019-02-12",
          "opprettetAv": "L105731",    
          "opprettetTidspunkt": "2019-02-12T10:47:14.846+01:00",
          "versjon": 25,
          "beskrivelse": "--- 6-66",
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
          "status": "AAPNET",
          "statuskategori": "AAPEN",
          "prioritet": "NORM",
          "aktivDato": "2019-02-12",
          "opprettetAv": "L105731",       
          "opprettetTidspunkt": "2019-02-12T10:47:14.846+01:00",           
          "versjon": 25,
          "beskrivelse": "---",
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
          "status": "AAPNET",
          "statuskategori": "AAPEN",
          "prioritet": "NORM",
          "aktivDato": "2019-02-12",
          "opprettetAv": "L105731",
          "opprettetTidspunkt": "2019-02-12T10:47:14.846+01:00",          
          "versjon": 25,
          "beskrivelse": "",
          "metadata": {
            "HJEMMEL": "MANGLER"
          }
        }
    """.trimIndent()
}