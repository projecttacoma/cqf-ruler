package org.opencds.cqf.dstu3.providers;

import ca.uhn.fhir.jpa.dao.IFhirSystemDao;
import ca.uhn.fhir.jpa.rp.dstu3.MeasureResourceProvider;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.RestOperationTypeEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Measure;
import org.opencds.cqf.cql.runtime.Interval;
import org.opencds.cqf.dstu3.JpaDataProvider;
import org.opencds.cqf.dstu3.utils.Dstu3LibraryLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;

public class MeasureProvider extends MeasureResourceProvider
{
    private JpaDataProvider provider;
    private IFhirSystemDao systemDao;
    private Dstu3LibraryLoader libraryLoader;

    private NarrativeProvider narrativeProvider;

    private Interval measurementPeriod;

    private static final Logger logger = LoggerFactory.getLogger(MeasureProvider.class);

    public MeasureProvider(
            JpaDataProvider dataProvider,
            IFhirSystemDao systemDao,
            Dstu3LibraryLoader libraryLoader,
            NarrativeProvider narrativeProvider)
    {
        this.provider = dataProvider;
        this.systemDao = systemDao;
        this.libraryLoader = libraryLoader;
        this.narrativeProvider = narrativeProvider;
    }

    @Operation(name="refresh-generated-content")
    public MethodOutcome refreshGeneratedContent(
            HttpServletRequest theRequest,
            RequestDetails theRequestDetails,
            @IdParam IdType theId)
    {
        Measure theResource = this.getDao().read(theId);
        this.generateNarrative(theResource);
        return super.update(theRequest, theResource, theId, theRequestDetails.getConditionalUrl(RestOperationTypeEnum.UPDATE), theRequestDetails);
    }

    private void generateNarrative(Measure measure)
    {
        this.narrativeProvider.generateNarrative(this.getContext(), measure);
    }


}
