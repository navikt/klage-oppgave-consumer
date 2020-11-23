package no.nav.klage.oppgave.api

import no.nav.klage.oppgave.domain.BatchUpdateResponse
import no.nav.klage.oppgave.domain.ResponseStatus
import no.nav.klage.oppgave.exceptions.OppgaveClientException
import no.nav.klage.oppgave.service.OppgaveService
import no.nav.klage.oppgave.utils.getLogger
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.context.request.WebRequest
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@RestController
class BatchUpdateController(
        private val oppgaveService: OppgaveService
) {

    @PostMapping("/batchupdate")
    fun triggerBatchUpdate(): BatchUpdateResponse = oppgaveService.bulkUpdateHjemmel()

}

@ControllerAdvice
class BatchUpdateControllerAdvice {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    @ExceptionHandler(OppgaveClientException::class)
    fun handleException(ex: OppgaveClientException, request: WebRequest): BatchUpdateResponse {
        logger.error("Fetch oppgave failed.", ex)
        return BatchUpdateResponse(
                finished = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME),
                status = ResponseStatus.ERROR,
                message = "Error from batchupdate ${ex.message}"
        )
    }
}
