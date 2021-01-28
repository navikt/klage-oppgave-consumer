package no.nav.klage.oppgave.api

import no.nav.klage.oppgave.domain.*
import no.nav.klage.oppgave.domain.ResponseStatus
import no.nav.klage.oppgave.exceptions.OppgaveClientException
import no.nav.klage.oppgave.service.OppgaveService
import no.nav.klage.oppgave.utils.getLogger
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.web.bind.annotation.*
import org.springframework.web.context.request.WebRequest
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@RestController
@Unprotected
class BatchUpdateController(
    private val oppgaveService: OppgaveService
) {
    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    @PostMapping("/batchupdate")
    fun triggerBatchUpdate(@RequestBody request: BatchUpdateRequest): BatchUpdateResponse {
        logger.info("Triggered batchUpdate with dryRun = {}", request.dryRun)
        return oppgaveService.bulkUpdateHjemmel(request)
    }

    @PostMapping("/batchstore")
    fun triggerBatchStore(@RequestBody request: BatchStoreRequest): BatchStoreResponse {
        logger.info("Triggered batchstore with dryRun = {}", request.dryRun)
        return oppgaveService.batchStore(request)
    }

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
