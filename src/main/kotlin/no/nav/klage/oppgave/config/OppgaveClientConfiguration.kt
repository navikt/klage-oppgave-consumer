package no.nav.klage.oppgave.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient.newConnection
import java.util.*

@Configuration
class OppgaveClientConfiguration(private val webClientBuilder: WebClient.Builder) {
    @Value("\${OPPGAVE_URL}")
    private lateinit var oppgaveServiceURL: String

    @Value("\${OPPGAVE_APIKEY_Q1}")
    private lateinit var apiKey: String

    @Value("\${SERVICE_USER_USERNAME}")
    private lateinit var username: String

    @Value("\${SERVICE_USER_PASSWORD}")
    private lateinit var password: String

    @Bean("oppgaveWebClient")
    fun oppgaveWebClient(): WebClient {
        return webClientBuilder
            .defaultHeader("x-nav-apiKey", apiKey)
            .defaultHeader("Authorization", "Basic ${base64EncodedAuth()}")
            .baseUrl(oppgaveServiceURL)
            .clientConnector(ReactorClientHttpConnector(newConnection()))
            .build()
    }

    private fun base64EncodedAuth() = Base64.getEncoder().encodeToString("$username:$password".encodeToByteArray())
}
