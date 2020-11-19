package no.nav.klage.oppgave.clients

import brave.Tracer
import no.nav.klage.oppgave.domain.Oppgave
import no.nav.klage.oppgave.domain.OppgaveResponse
import no.nav.klage.oppgave.utils.getLogger
import no.nav.klage.oppgave.utils.getSecureLogger
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.bodyToMono

const val FETCH_LIMIT = 10

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

        const val HJEMMEL = "HJEMMEL"
    }

    @Retryable
    fun fetchHjemmel(offset: Int, limit: Int = FETCH_LIMIT) =
            logTimingAndWebClientResponseException("getOppgaver ($offset)") {
            oppgaveWebClient.get()
                    .uri { uriBuilder ->
                        uriBuilder.queryParam("limit", limit)
                        uriBuilder.queryParam("offset", offset)
                        uriBuilder.build()
                    }
                    .header("X-Correlation-ID", tracer.currentSpan().context().traceIdString())
                    .header("Nav-Consumer-Id", applicationName)
                    .retrieve()
                    .bodyToMono<OppgaveResponse>()
                    .block() ?: throw RuntimeException("Oppgaver could not be fetched")
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
                    .block() ?: throw java.lang.RuntimeException("Oppgave could not be put")
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
            throw ex
        } catch (rtex: RuntimeException) {
            logger.warn("Caught RuntimeException", rtex)
            throw rtex
        } finally {
            val end: Long = System.currentTimeMillis()
            logger.info("Method {} took {} millis", methodName, (end - start))
        }
    }

}