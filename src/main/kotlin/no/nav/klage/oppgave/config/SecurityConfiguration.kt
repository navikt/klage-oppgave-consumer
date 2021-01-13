package no.nav.klage.oppgave.config

import no.nav.security.token.support.client.spring.oauth2.EnableOAuth2Client
import org.springframework.context.annotation.Configuration

@EnableOAuth2Client(cacheEnabled = true)
@Configuration
internal class SecurityConfiguration