package no.nav.klage.oppgave.services

import no.nav.klage.oppgave.domain.oppgavekopi.Metadata
import no.nav.klage.oppgave.domain.oppgavekopi.OppgaveKopi
import no.nav.klage.oppgave.domain.oppgavekopi.OppgaveKopiVersjon
import no.nav.klage.oppgave.domain.oppgavekopi.OppgaveKopiVersjonId
import no.nav.klage.oppgave.repositories.OppgaveKopiRepository
import no.nav.klage.oppgave.repositories.OppgaveKopiVersjonRepository
import no.nav.klage.oppgave.utils.getLogger
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class OppgaveKopiService(
    private val oppgaveKopiRepository: OppgaveKopiRepository,
    private val oppgaveKopiVersjonRepository: OppgaveKopiVersjonRepository,
    private val applicationEventPublisher: ApplicationEventPublisher
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    /**
     * This method needs to be idempotent
     */
    fun saveOppgaveKopi(oppgaveKopi: OppgaveKopi) {
        logger.debug("Received oppgavekopi with id ${oppgaveKopi.id} and versjon ${oppgaveKopi.versjon} for storing")
        if (oppgaveKopiRepository.existsById(oppgaveKopi.id)) {
            val existingOppgaveKopi = oppgaveKopiRepository.getOne(oppgaveKopi.id)
            if (existingOppgaveKopi.versjon < oppgaveKopi.versjon) {

                mergeMetadata(oppgaveKopi.metadata, existingOppgaveKopi.metadata)

                oppgaveKopiRepository.save(oppgaveKopi)
            } else {
                logger.debug("Oppgavekopi with id ${existingOppgaveKopi.id} and versjon ${existingOppgaveKopi.versjon} stored before, won't overwrite")
            }
        } else {
            oppgaveKopiRepository.save(oppgaveKopi)
        }

        //check if version is already stored
        if (oppgaveKopiVersjonRepository.existsById(OppgaveKopiVersjonId(oppgaveKopi.id, oppgaveKopi.versjon))) {
            logger.debug("Oppgavekopiversjon with id ${oppgaveKopi.id} and versjon ${oppgaveKopi.versjon} stored before, won't overwrite")
        } else {
            oppgaveKopiVersjonRepository.saveAndFlush(oppgaveKopi.toVersjon())
        }

        val alleVersjoner = oppgaveKopiVersjonRepository.findByIdOrderByVersjonDesc(oppgaveKopi.id)
//        applicationEventPublisher.publishEvent(OppgaveMottattEvent(alleVersjoner))
    }

    private fun mergeMetadata(metadataNew: Set<Metadata>, metadataOld: Set<Metadata>) {
        metadataNew.forEach { newMetadata ->
            val oldRow = metadataOld.find { oldMetadata -> oldMetadata.noekkel == newMetadata.noekkel }
            if (oldRow != null) {
                newMetadata.id = oldRow.id
            }
        }
    }

    fun getOppgaveKopi(oppgaveKopiId: Long): OppgaveKopi {
        return oppgaveKopiRepository.getOne(oppgaveKopiId)
    }

    fun getOppgaveKopiVersjon(oppgaveKopiId: Long, versjon: Int): OppgaveKopiVersjon {
        return oppgaveKopiVersjonRepository.getOne(OppgaveKopiVersjonId(oppgaveKopiId, versjon))
    }

    fun getOppgaveKopiSisteVersjon(oppgaveKopiId: Long): OppgaveKopiVersjon {
        return oppgaveKopiVersjonRepository.findFirstByIdOrderByVersjonDesc(oppgaveKopiId)
    }
}
