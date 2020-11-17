package no.nav.klage.oppgave.clients

import no.nav.klage.oppgave.domain.OppgaveKafkaRecord
import no.nav.klage.oppgave.utils.getLogger
import no.nav.klage.oppgave.utils.getSecureLogger
import no.nav.slackposter.SlackClient
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class KafkaOppgaveConsumer(private val slackClient: SlackClient) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
        private val secureLogger = getSecureLogger()
    }

    @KafkaListener(topics = ["\${KAFKA_TOPIC}"])
    fun listen(oppgaveRecord: ConsumerRecord<String, OppgaveKafkaRecord>) {
        logger.debug("Changed oppgave received from Kafka topic")
        secureLogger.debug("Changed oppgave received from Kafka topic: {}", oppgaveRecord.value())

        runCatching {
            oppgaveRecord.value().logIt()

            //TODO: parse and store hjemmel
        }.onFailure {
//            slackClient.postMessage("Nylig mottatt oppgave feilet! (${causeClass(rootCause(it))})", Severity.ERROR)
            secureLogger.error("Failed to process oppgave", it)
            throw RuntimeException("Could not process oppgave. See more details in secure log.")
        }
    }

    private fun OppgaveKafkaRecord.logIt() {
//        slackClient.postMessage(String.format("Oppgave med id <%s|%s> mottatt.", Kibana.createUrl(id), id))
        logger.debug("Received oppgave has id: {}", this.id)
    }

    private fun rootCause(t: Throwable): Throwable = t.cause?.run { rootCause(this) } ?: t

    private fun causeClass(t: Throwable) = t.stackTrace?.firstOrNull()?.className ?: ""
}
