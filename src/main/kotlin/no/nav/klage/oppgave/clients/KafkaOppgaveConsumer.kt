package no.nav.klage.oppgave.clients

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.klage.oppgave.domain.OppgaveKafkaRecord
import no.nav.klage.oppgave.domain.OppgaveKafkaRecord.MetadataKey.HJEMMEL
import no.nav.klage.oppgave.service.HjemmelParsingService
import no.nav.klage.oppgave.service.OppgaveService
import no.nav.klage.oppgave.utils.getLogger
import no.nav.klage.oppgave.utils.getSecureLogger
import no.nav.slackposter.SlackClient
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class KafkaOppgaveConsumer(
    private val slackClient: SlackClient,
    private val hjemmelParsingService: HjemmelParsingService,
    private val oppgaveService: OppgaveService
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
        private val secureLogger = getSecureLogger()

        const val MANGLER_HJEMMEL = "MANGLER"

        private val mapper = jacksonObjectMapper()
    }

    @KafkaListener(topics = ["\${KAFKA_TOPIC}"])
    fun listen(oppgaveRecord: ConsumerRecord<String, String>) {
        runCatching {
            val oppgave = oppgaveRecord.value().toOppgave()

            if (oppgave.isKlage() && oppgave.isTildeltKlageenhet()) {
                logger.debug("Changed klage-oppgave in enhet ${oppgave.tildeltEnhetsnr} received from Kafka topic")

                oppgave.logIt()

                logger.debug("Attempting to extract hjemler from beskrivelse")

                if (oppgave.beskrivelse.isNullOrBlank()) {
                    logger.debug("Beskrivelse was empty or null")
                    if (!oppgave.metadata?.get(HJEMMEL).isNullOrBlank()) {
                        logger.debug("HJEMMEL is already set to {}", oppgave.metadata?.get(HJEMMEL))
                    } else {
                        logger.debug("Setting HJEMMEL to {}", MANGLER_HJEMMEL)
                        oppgaveService.updateHjemmel(oppgave.id, MANGLER_HJEMMEL)
                    }
                } else {
                    val foundHjemler = hjemmelParsingService.extractHjemmel(oppgave.beskrivelse)

                    if (foundHjemler.isNotEmpty()) {
                        logger.debug("Found hjemler: {}. Picking first if many.", foundHjemler)

                        if (shouldStoreHjemmelInOppgave(oppgave.metadata?.get(HJEMMEL), foundHjemler.first())) {
                            logger.debug("Storing new hjemmel in oppgave")
                            oppgaveService.updateHjemmel(oppgave.id, foundHjemler.first())
                        } else {
                            logger.debug("No need to store hjemmel")
                        }
                    } else {
                        logger.debug("No hjemler found in beskrivelse. See more in secure log.")
                        secureLogger.debug("No hjemler found in beskrivelse. Beskrivelse: {}", oppgave.beskrivelse)
                        if (!oppgave.metadata?.get(HJEMMEL).isNullOrBlank()) {
                            logger.debug("HJEMMEL is already set to {}", oppgave.metadata?.get(HJEMMEL))
                        } else {
                            logger.debug("Setting HJEMMEL to {}", MANGLER_HJEMMEL)
                            oppgaveService.updateHjemmel(oppgave.id, MANGLER_HJEMMEL)
                        }
                    }
                }
            }
        }.onFailure {
//            slackClient.postMessage("Nylig mottatt oppgave feilet! (${causeClass(rootCause(it))})", Severity.ERROR)
            secureLogger.error("Failed to process oppgave", it)
            throw RuntimeException("Could not process oppgave. See more details in secure log.")
        }
    }

    private fun String.toOppgave() = mapper.readValue(this, OppgaveKafkaRecord::class.java)

    private fun shouldStoreHjemmelInOppgave(hjemmelInOppgave: String?, parsedHjemmel: String): Boolean {
        return when {
            hjemmelInOppgave.isNullOrBlank() -> {
                logger.debug("No previous hjemmel was stored in oppgave")
                true
            }
            hjemmelInOppgave == parsedHjemmel -> {
                logger.debug("Same hjemmel was already found in oppgave")
                false
            }
            else -> {
                logger.debug(
                    "Parsed hjemmel {} was different from previously stored hjemmel {}",
                    parsedHjemmel,
                    hjemmelInOppgave
                )
                true
            }
        }
    }

    private fun OppgaveKafkaRecord.logIt() {
//        slackClient.postMessage(String.format("Oppgave med id <%s|%s> mottatt.", Kibana.createUrl(id), id))
        logger.debug("Received oppgave has id: {}", this.id)
    }

    private fun rootCause(t: Throwable): Throwable = t.cause?.run { rootCause(this) } ?: t

    private fun causeClass(t: Throwable) = t.stackTrace?.firstOrNull()?.className ?: ""
}
