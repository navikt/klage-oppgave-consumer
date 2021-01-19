package no.nav.klage.oppgave.clients

import brave.Tracer
import no.nav.klage.oppgave.domain.Oppgave
import no.nav.klage.oppgave.domain.OppgaveResponse
import no.nav.klage.oppgave.domain.Statuskategori
import no.nav.klage.oppgave.exceptions.OppgaveClientException
import no.nav.klage.oppgave.utils.getLogger
import no.nav.klage.oppgave.utils.getSecureLogger
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.bodyToMono
import java.time.LocalDate
import java.time.format.DateTimeFormatter

const val FETCH_LIMIT = 100
const val HJEMMEL = "HJEMMEL"

@Component
class OppgaveClient(
    private val oppgaveWebClient: WebClient,
    private val tracer: Tracer,
    @Value("\${spring.application.name}") private val applicationName: String
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
        private val securelogger = getSecureLogger()

        const val BEHANDLINGSTYPE_KLAGE = "ae0058"
        const val BEHANDLINGSTYPE_ANKE = "ae0046"
    }

    @Retryable
    fun fetchOppgaver(includeFrom: LocalDate?, offset: Int, limit: Int = FETCH_LIMIT) =
        logTimingAndWebClientResponseException("getOppgaver ($offset)") {
            oppgaveWebClient.get()
                .uri { uriBuilder ->
                    uriBuilder.queryParam("statuskategori", Statuskategori.AAPEN)
                    uriBuilder.queryParam("limit", limit)
                    uriBuilder.queryParam("offset", offset)
                    uriBuilder.queryParam("behandlingstype", BEHANDLINGSTYPE_KLAGE)
                    uriBuilder.queryParam("tema", "SYK")
                    includeFrom?.let {
                        uriBuilder.queryParam("fristFom", DateTimeFormatter.ISO_LOCAL_DATE.format(it))
                    }
                    uriBuilder.build()
                }
                .header("X-Correlation-ID", tracer.currentSpan().context().traceIdString())
                .header("Nav-Consumer-Id", applicationName)
                .retrieve()
                .bodyToMono<OppgaveResponse>()
                .block() ?: throw OppgaveClientException("Oppgaver could not be fetched")
        }

    @Retryable
    fun putOppgave(oppgave: Oppgave) =
        logTimingAndWebClientResponseException("putOppgave") {
            oppgaveWebClient.put()
                .uri { uriBuilder ->
                    uriBuilder.pathSegment("{id}").build(oppgave.id)
                }
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Correlation-ID", tracer.currentSpan().context().traceIdString())
                .header("Nav-Consumer-Id", applicationName)
                .bodyValue(oppgave)
                .retrieve()
                .bodyToMono<Oppgave>()
                .block() ?: throw OppgaveClientException("Oppgave could not be put")
        }

    @Retryable
    fun getOppgave(oppgaveId: Long) =
        logTimingAndWebClientResponseException("getOppgave") {
            oppgaveWebClient.get()
                .uri { uriBuilder ->
                    uriBuilder.pathSegment("{id}").build(oppgaveId)
                }
                .header("X-Correlation-ID", tracer.currentSpan().context().traceIdString())
                .header("Nav-Consumer-Id", applicationName)
                .retrieve()
                .bodyToMono<Oppgave>()
                .block() ?: throw OppgaveClientException("Oppgave could not be fetched")
        }

    private fun <T> logTimingAndWebClientResponseException(methodName: String, function: () -> T): T {
        val start: Long = System.currentTimeMillis()
        try {
            return function.invoke()
        } catch (ex: WebClientResponseException) {
            logger.warn("Caught WebClientResponseException, see securelogs for details")
            securelogger.error(
                "Got a {} error calling Oppgave {} {} with message {}",
                ex.statusCode,
                ex.request?.method ?: "-",
                ex.request?.uri ?: "-",
                ex.responseBodyAsString
            )
            throw OppgaveClientException("Caught WebClientResponseException", ex)
        } catch (rtex: RuntimeException) {
            logger.warn("Caught RuntimeException", rtex)
            throw OppgaveClientException("Caught runtimeexception", rtex)
        } finally {
            val end: Long = System.currentTimeMillis()
            logger.info("Method {} took {} millis", methodName, (end - start))
        }
    }

}
