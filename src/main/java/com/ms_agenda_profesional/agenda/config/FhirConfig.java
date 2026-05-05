package com.ms_agenda_profesional.agenda.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;


@Configuration
public class FhirConfig {
    
    @Value("${fhir.server.url}")
    private String ServerUrl;


    @Bean
    public IGenericClient fhirClient() {
        FhirContext ctx = FhirContext.forR4();
        return ctx.newRestfulGenericClient(ServerUrl);
    }
}