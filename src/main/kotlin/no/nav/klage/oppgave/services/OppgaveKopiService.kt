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

/*
    //Oppgaven kan ha gått ping-pong frem og tilbake, så det vi leter etter her er siste gang den ble assignet KA
    private fun findFirstVersionWhereTildeltEnhetIsKA(oppgaveKopiVersjoner: List<OppgaveKopiVersjon>): OppgaveKopiVersjon? =
        oppgaveKopiVersjoner.zipWithNext()
            .firstOrNull {
                it.first.tildeltEnhetsnr.startsWith(KLAGEINSTANS_PREFIX)
                        && !it.second.tildeltEnhetsnr.startsWith(KLAGEINSTANS_PREFIX)
            }
            ?.first
    fun connectOppgaveKopiToKlagebehandling(oppgaveKopierOrdererByVersion: List<OppgaveKopiVersjon>) {
        val lastVersjon = oppgaveKopierOrdererByVersion.first()

        if (lastVersjon.tildeltEnhetsnr.startsWith(KLAGEINSTANS_PREFIX) && (lastVersjon.oppgavetype == "BEH_SAK_MK" || lastVersjon.oppgavetype == "BEH_SAK")) {
            val mottakSomHarPaagaaendeKlagebehandlinger =
                fetchMottakForOppgaveKopi(lastVersjon.id).filter {
                    klagebehandlingRepository.findByMottakId(it.id)?.avsluttet == null
                }
            val klagebehandlingerOgMottak = if (mottakSomHarPaagaaendeKlagebehandlinger.isEmpty()) {
                listOf(createNewMottakAndKlagebehandling(oppgaveKopierOrdererByVersion))
            } else {
                mottakSomHarPaagaaendeKlagebehandlinger.map { updateMottak(it, oppgaveKopierOrdererByVersion) }
            }
            klagebehandlingerOgMottak.map { KlagebehandlingEndretEvent(it.first, emptyList()) }
                .forEach { applicationEventPublisher.publishEvent(it) }
        }
    }

    private fun createNewMottakAndKlagebehandling(oppgaveKopierOrdererByVersion: List<OppgaveKopiVersjon>): Pair<Klagebehandling, Mottak> {
        val lastVersjon = oppgaveKopierOrdererByVersion.first()
        requireNotNull(lastVersjon.ident)
        requireNotNull(lastVersjon.behandlingstype)

        val overfoeringsdata = overfoeringsdataParserService.parseBeskrivelse(lastVersjon.beskrivelse ?: "")

        val createdMottak = mottakRepository.save(
            Mottak(
                tema = mapTema(lastVersjon.tema),
                sakstype = mapSakstype(lastVersjon.behandlingstype),
                referanseId = lastVersjon.saksreferanse,
                foedselsnummer = lastVersjon.ident.folkeregisterident,
                organisasjonsnummer = mapOrganisasjonsnummer(lastVersjon.ident),
                hjemmelListe = mapHjemler(lastVersjon),
                avsenderSaksbehandlerident = findFirstVersionWhereTildeltEnhetIsKA(oppgaveKopierOrdererByVersion)?.endretAv
                    ?: overfoeringsdata?.saksbehandlerWhoMadeTheChange,
                avsenderEnhet = findFirstVersionWhereTildeltEnhetIsKA(oppgaveKopierOrdererByVersion)?.endretAvEnhetsnr
                    ?: overfoeringsdata?.enhetOverfoertFra ?: lastVersjon.opprettetAvEnhetsnr,
                oversendtKaEnhet = findFirstVersionWhereTildeltEnhetIsKA(oppgaveKopierOrdererByVersion)?.tildeltEnhetsnr
                    ?: overfoeringsdata?.enhetOverfoertTil ?: lastVersjon.tildeltEnhetsnr,
                oversendtKaDato = findFirstVersionWhereTildeltEnhetIsKA(oppgaveKopierOrdererByVersion)?.endretTidspunkt?.toLocalDate()
                    ?: overfoeringsdata?.datoForOverfoering ?: lastVersjon.opprettetTidspunkt.toLocalDate(),
                fristFraFoersteinstans = lastVersjon.fristFerdigstillelse,
                beskrivelse = lastVersjon.beskrivelse,
                status = lastVersjon.status.name,
                statusKategori = lastVersjon.statuskategori().name,
                tildeltEnhet = lastVersjon.tildeltEnhetsnr,
                tildeltSaksbehandlerident = lastVersjon.tilordnetRessurs,
                journalpostId = lastVersjon.journalpostId,
                journalpostKilde = lastVersjon.journalpostkilde,
                kilde = Kilde.OPPGAVE,
                oppgavereferanser = mutableListOf(
                    Oppgavereferanse(
                        oppgaveId = lastVersjon.id
                    )
                )
            )
        )

        val createdKlagebehandling = klagebehandlingRepository.save(
            Klagebehandling(
                foedselsnummer = createdMottak.foedselsnummer,
                tema = createdMottak.tema,
                sakstype = createdMottak.sakstype,
                referanseId = createdMottak.referanseId,
                innsendt = null,
                mottattFoersteinstans = null,
                avsenderEnhetFoersteinstans = createdMottak.avsenderEnhet,
                avsenderSaksbehandleridentFoersteinstans = createdMottak.avsenderSaksbehandlerident,
                mottattKlageinstans = createdMottak.oversendtKaDato,
                startet = null,
                avsluttet = null,
                frist = createdMottak.fristFraFoersteinstans,
                tildeltSaksbehandlerident = createdMottak.tildeltSaksbehandlerident,
                tildeltEnhet = createdMottak.tildeltEnhet,
                mottakId = createdMottak.id,
                vedtak = mutableSetOf(),
                kvalitetsvurdering = null,
                hjemler = createdMottak.hjemler().map { hjemmelService.generateHjemmelFromText(it) }.toMutableSet(),
                saksdokumenter = if (createdMottak.journalpostId != null) {
                    mutableSetOf(Saksdokument(journalpostId = createdMottak.journalpostId!!))
                } else {
                    mutableSetOf()
                },
                kilde = Kilde.OPPGAVE
            )
        )
        logger.debug("Created behandling ${createdKlagebehandling.id} with mottak ${createdMottak.id} for oppgave ${lastVersjon.id}")
        return Pair(createdKlagebehandling, createdMottak)
    }


    private fun updateMottak(
        mottak: Mottak,
        oppgaveKopierOrdererByVersion: List<OppgaveKopiVersjon>
    ): Pair<Klagebehandling, Mottak> {
        logger.debug("Updating mottak")

        val lastVersjon = oppgaveKopierOrdererByVersion.first()
        requireNotNull(lastVersjon.ident)
        requireNotNull(lastVersjon.behandlingstype)

        //TODO: Legge til nytt saksdokument hvis journalpostId er oppdatert?
        mottak.apply {
            tema = mapTema(lastVersjon.tema)
            sakstype = mapSakstype(lastVersjon.behandlingstype)
            referanseId = lastVersjon.saksreferanse
            foedselsnummer = lastVersjon.ident.folkeregisterident
            organisasjonsnummer = mapOrganisasjonsnummer(lastVersjon.ident)
            hjemmelListe = mapHjemler(lastVersjon)
            fristFraFoersteinstans = lastVersjon.fristFerdigstillelse
            beskrivelse = lastVersjon.beskrivelse
            status = lastVersjon.status.name
            statusKategori = lastVersjon.statuskategori().name
            journalpostId = lastVersjon.journalpostId
            journalpostKilde = lastVersjon.journalpostkilde
            //TODO: Bør dise oppdateres?
            tildeltEnhet = lastVersjon.tildeltEnhetsnr
            tildeltSaksbehandlerident = lastVersjon.tilordnetRessurs
            //oversendtKaEnhet = mapMottakerEnhet(oppgaveKopierOrdererByVersion)
            //oversendtKaDato = mapOversendtKaDato(oppgaveKopierOrdererByVersion)
            //avsenderSaksbehandlerident = mapAvsenderSaksbehandler(oppgaveKopierOrdererByVersion)
            //avsenderEnhet = mapAvsenderEnhet(oppgaveKopierOrdererByVersion)
        }

        return Pair(klagebehandlingRepository.findByMottakId(mottak.id)!!, mottak)
    }

    private fun mapHjemler(oppgaveKopiVersjon: OppgaveKopiVersjon) =
        hjemmelService.getHjemmelFromOppgaveKopiVersjon(oppgaveKopiVersjon)

    private fun mapOrganisasjonsnummer(ident: VersjonIdent) =
        if (ident.identType == IdentType.ORGNR) {
            ident.verdi
        } else {
            null
        }
*/
