package org.opencds.cqf.dstu3.providers;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.narrative.CustomThymeleafNarrativeGenerator;
import ca.uhn.fhir.narrative.INarrativeGenerator;
import org.hl7.fhir.dstu3.model.DomainResource;
import org.hl7.fhir.dstu3.model.Narrative;

public class NarrativeProvider
{
    private INarrativeGenerator generator;

    public NarrativeProvider()
    {
        this(NarrativeProvider.class.getClassLoader().getResource("narratives/narrative.properties").toString());
    }

    public NarrativeProvider(String pathToPropertiesFile)
    {
        this.generator = new CustomThymeleafNarrativeGenerator("classpath:ca/uhn/fhir/narrative/narratives.properties", pathToPropertiesFile);
    }

    public void generateNarrative(FhirContext context, DomainResource resource)
    {
        // Remove the old generated narrative.
        resource.setText(null);

        this.generator.populateResourceNarrative(context, resource);
    }
}
