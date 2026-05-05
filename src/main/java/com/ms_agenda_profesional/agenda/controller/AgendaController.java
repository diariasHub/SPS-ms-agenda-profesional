package com.ms_agenda_profesional.agenda.controller;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Schedule;
import org.hl7.fhir.r4.model.Slot;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;

@RestController
@RequestMapping("/agendas")
public class AgendaController {

    private final IGenericClient fhirClient;

    public AgendaController(IGenericClient fhirClient) {
        this.fhirClient = fhirClient;
    }

    // 1. Crear una Agenda (Schedule) para un Médico
    @PostMapping
    public ResponseEntity<String> crearAgenda(@RequestParam String idMedico, 
                                              @RequestParam String nombreMedico) {
        
        Schedule agenda = new Schedule();
        agenda.setActive(true);
        
        // FHIR usa "Referencias" para relacionar recursos. 
        // Aquí decimos que esta agenda pertenece a un "Practitioner" (Médico)
        Reference actorRef = new Reference("Practitioner/" + idMedico);
        actorRef.setDisplay("Dr. " + nombreMedico);
        agenda.addActor(actorRef);

        // Guardamos en el servidor FHIR
        MethodOutcome resultado = fhirClient.create()
                .resource(agenda)
                .execute();

        // Obtenemos el ID que el servidor FHIR le asignó a esta nueva agenda
        String idAgendaCreada = resultado.getId().getIdPart();

        return ResponseEntity.ok("Agenda creada con ID: " + idAgendaCreada);
    }

    // 2. Agregar un bloque de tiempo (Slot) a una Agenda específica
    @PostMapping("/{idAgenda}/bloques")
    public ResponseEntity<String> crearBloque(@PathVariable String idAgenda) {
        
        Slot bloque = new Slot();
        
        // Relacionamos este bloque con la agenda usando su ID
        bloque.setSchedule(new Reference("Schedule/" + idAgenda));
        
        // Estado del bloque: FREE (Libre para ser agendado)
        bloque.setStatus(Slot.SlotStatus.FREE);

        // Configuramos inicio y fin (Ejemplo estático, en la vida real lo recibes por parámetro)
        // Ejemplo: Bloque ahora mismo, que dura 30 minutos
        Date fechaInicio = new Date(); 
        Date fechaFin = new Date(fechaInicio.getTime() + (30 * 60 * 1000)); 
        
        bloque.setStart(fechaInicio);
        bloque.setEnd(fechaFin);

        // Guardamos el bloque en el servidor FHIR
        MethodOutcome resultado = fhirClient.create()
                .resource(bloque)
                .execute();

        return ResponseEntity.ok("Bloque de tiempo creado con ID: " + resultado.getId().getIdPart());
    }

    // 3. Consultar los bloques LIBRES de una agenda
    @GetMapping("/{idAgenda}/bloques-libres")
    public ResponseEntity<List<String>> listarBloquesLibres(@PathVariable String idAgenda) {
        
        // Hacemos una búsqueda filtrando por ID de la agenda y estado "free"
        Bundle response = fhirClient.search()
                .forResource(Slot.class)
                .where(Slot.SCHEDULE.hasId(idAgenda))
                .and(Slot.STATUS.exactly().code(Slot.SlotStatus.FREE.toCode()))
                .returnBundle(Bundle.class)
                .execute();

        List<String> bloques = new ArrayList<>();

        response.getEntry().forEach(entry -> {
            Slot slot = (Slot) entry.getResource();
            String infoBloque = "ID Slot: " + slot.getIdElement().getIdPart() + 
                                " | Inicio: " + slot.getStart() + 
                                " | Fin: " + slot.getEnd();
            bloques.add(infoBloque);
        });

        return ResponseEntity.ok(bloques);
    }
}