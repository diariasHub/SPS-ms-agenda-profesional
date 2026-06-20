package com.ms_agenda_profesional.dto;

public record AppointmentDTO(
    String id,
    String patientId,
    String patientName,
    String patientRut,
    String start,
    String status,
    String patientAge
) {}