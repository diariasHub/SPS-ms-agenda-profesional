package com.ms_agenda_profesional.agenda.controller;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.hl7.fhir.r4.model.Appointment;
import org.hl7.fhir.r4.model.Appointment.AppointmentStatus;
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

import com.ms_agenda_profesional.dto.ConfirmacionReservaResponse;

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
        
        Reference actorRef = new Reference("Practitioner/" + idMedico);
        actorRef.setDisplay("Dr. " + nombreMedico);
        agenda.addActor(actorRef);

        MethodOutcome resultado = fhirClient.create()
                .resource(agenda)
                .execute();

        String idAgendaCreada = resultado.getId().getIdPart();

        return ResponseEntity.ok("Agenda creada con ID: " + idAgendaCreada);
    }

    // 2. Agregar un bloque de tiempo (Slot) a una Agenda específica
    @PostMapping("/{idAgenda}/bloques")
    public ResponseEntity<String> crearBloque(@PathVariable String idAgenda) {
        
        Slot bloque = new Slot();
        bloque.setSchedule(new Reference("Schedule/" + idAgenda));
        bloque.setStatus(Slot.SlotStatus.FREE);

        Date fechaInicio = new Date(); 
        Date fechaFin = new Date(fechaInicio.getTime() + (30 * 60 * 1000)); 
        
        bloque.setStart(fechaInicio);
        bloque.setEnd(fechaFin);

        MethodOutcome resultado = fhirClient.create()
                .resource(bloque)
                .execute();

        return ResponseEntity.ok("Bloque de tiempo creado con ID: " + resultado.getId().getIdPart());
    }

    // 3. Consultar los bloques LIBRES de una agenda
    @GetMapping("/{idAgenda}/bloques-libres")
    public ResponseEntity<List<String>> listarBloquesLibres(@PathVariable String idAgenda) {
        
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

    // 4. Reservar un turno (Crear Appointment y actualizar el Slot a BUSY)
    // 2. CAMBIAMOS EL TIPO DE RETORNO A ResponseEntity<Object>
    @PostMapping("/bloques/{idSlot}/reservar")
    public ResponseEntity<Object> reservarTurno(@PathVariable String idSlot,
                                                @RequestParam String idPaciente,
                                                @RequestParam String nombrePaciente,
                                                @RequestParam String idMedico) {
        
        Slot slot = fhirClient.read()
                .resource(Slot.class)
                .withId(idSlot)
                .execute();

        if (slot.getStatus() != Slot.SlotStatus.FREE) {
            return ResponseEntity.badRequest().body("El bloque de tiempo ya no está disponible.");
        }

        Appointment cita = new Appointment();
        cita.setStatus(AppointmentStatus.BOOKED);
        cita.setStart(slot.getStart());
        cita.setEnd(slot.getEnd());
        
        cita.addSlot(new Reference("Slot/" + idSlot));

        Appointment.AppointmentParticipantComponent participantePaciente = new Appointment.AppointmentParticipantComponent();
        participantePaciente.setActor(new Reference("Patient/" + idPaciente).setDisplay(nombrePaciente));
        participantePaciente.setStatus(Appointment.ParticipationStatus.ACCEPTED);
        cita.addParticipant(participantePaciente);

        Appointment.AppointmentParticipantComponent participanteMedico = new Appointment.AppointmentParticipantComponent();
        participanteMedico.setActor(new Reference("Practitioner/" + idMedico));
        participanteMedico.setStatus(Appointment.ParticipationStatus.ACCEPTED);
        cita.addParticipant(participanteMedico);

        MethodOutcome resultadoCita = fhirClient.create()
                .resource(cita)
                .execute();

        String idCitaCreada = resultadoCita.getId().getIdPart();

        slot.setStatus(Slot.SlotStatus.BUSY);
        try {
            fhirClient.update()
                    .resource(slot)
                    .withAdditionalHeader("If-Match", slot.getIdElement().getVersionIdPart())
                    .execute();
        } catch (ca.uhn.fhir.rest.server.exceptions.PreconditionFailedException e) {
            return ResponseEntity.status(409).body("Error: El bloque de tiempo fue reservado por otro usuario de manera simultánea.");
        }

        // 3. ELIMINAMOS EL fhirClient.update() DUPLICADO Y DEVOLVEMOS EL DTO
        ConfirmacionReservaResponse respuesta = new ConfirmacionReservaResponse(
            "Reserva confirmada con éxito.",
            idCitaCreada,
            idSlot,
            "BOOKED"
        );

        return ResponseEntity.ok(respuesta);
    }
}