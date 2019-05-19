package org.opencds.cqf.dstu3.providers;

import java.io.IOException;

import javax.xml.bind.JAXBException;

import org.hl7.fhir.dstu3.model.CarePlan;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.exceptions.FHIRException;

import ca.uhn.fhir.jpa.rp.dstu3.PlanDefinitionResourceProvider;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.RequiredParam;
import org.opencds.cqf.dstu3.JpaDataProvider;

public class PlanDefinitionProvider extends PlanDefinitionResourceProvider
{
    private JpaDataProvider dataProvider;

    public PlanDefinitionProvider(JpaDataProvider dataProvider)
    {
        this.dataProvider = dataProvider;
    }

    @Operation(name = "$apply", idempotent = true)
    public CarePlan applyPlanDefinition(
            @IdParam IdType theId,
            @RequiredParam(name="patient") String patientId,
            @OptionalParam(name="encounter") String encounterId,
            @OptionalParam(name="practitioner") String practitionerId,
            @OptionalParam(name="organization") String organizationId,
            @OptionalParam(name="userType") String userType,
            @OptionalParam(name="userLanguage") String userLanguage,
            @OptionalParam(name="userTaskContext") String userTaskContext,
            @OptionalParam(name="setting") String setting,
            @OptionalParam(name="settingContext") String settingContext)
            throws IOException, JAXBException, FHIRException
    {
        String id = theId.getId();
        return null;
    }
}
