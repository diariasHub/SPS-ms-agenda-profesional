package com.ms_agenda_profesional.dto;


public record ConfirmacionReservaResponse(
    String mensaje,
    String idCita,
    String idSlot,
    String estadoCita
) {}