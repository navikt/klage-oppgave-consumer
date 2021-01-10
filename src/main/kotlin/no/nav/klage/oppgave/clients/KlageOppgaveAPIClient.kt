package no.nav.klage.oppgave.clients

import no.nav.klage.oppgave.domain.OppgaveKafkaRecord
import no.nav.klage.oppgave.utils.getLogger
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService
import no.nav.security.token.support.client.spring.ClientConfigurationProperties
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

@Component
class KlageDittnavAPIClient(
    private val klageOppgaveAPIWebClient: WebClient,
    private val clientConfigurationProperties: ClientConfigurationProperties,
    private val oAuth2AccessTokenService: OAuth2AccessTokenService
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    fun storeOppgave(oppgave: OppgaveKafkaRecord) {
        logger.debug("Storing oppgave")
        klageOppgaveAPIWebClient.post()
            .uri("/oppgaver")
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${getAccessTokenWithKlageOppgaveAPIScope()}")
            .bodyValue(oppgave)
            .retrieve()
            .toBodilessEntity()
            .block() ?: throw RuntimeException("Unable to store oppgave in klage-oppgave-api.")
    }

    fun getAccessTokenWithKlageOppgaveAPIScope(): String {
        val clientProperties = clientConfigurationProperties.registration["klage-oppgave-api"]
        val response = oAuth2AccessTokenService.getAccessToken(clientProperties)
        return response.accessToken
    }
}