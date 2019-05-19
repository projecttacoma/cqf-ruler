package org.opencds.cqf.dstu3.daos;

import ca.uhn.fhir.jpa.dao.DaoMethodOutcome;
import ca.uhn.fhir.jpa.dao.IFhirResourceDaoValueSet;
import ca.uhn.fhir.jpa.dao.dstu3.FhirResourceDaoDstu3;
import ca.uhn.fhir.jpa.dao.dstu3.FhirResourceDaoValueSetDstu3;
import ca.uhn.fhir.jpa.rp.dstu3.CodeSystemResourceProvider;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.UriParam;
import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ValueSetDao extends FhirResourceDaoValueSetDstu3
{
    private CodeSystemResourceProvider codeSystemResourceProvider;
    private Map<String, CodeSystem> codeSystems = new HashMap<>();

    public ValueSetDao() { }

    public ValueSetDao(CodeSystemResourceProvider codeSystemResourceProvider)
    {
        this.codeSystemResourceProvider = codeSystemResourceProvider;
    }

    public void setCodeSystemResourceProvider(CodeSystemResourceProvider codeSystemResourceProvider)
    {
        this.codeSystemResourceProvider = codeSystemResourceProvider;
    }

//    @Override
//    public DaoMethodOutcome create(final ValueSet theResource)
//    {
//        try
//        {
//            return create(theResource, null, true, new Date(), null);
//        }
//        finally
//        {
//            populateCodeSystem(theResource);
//        }
//    }
//
//    @Override
//    public DaoMethodOutcome create(final ValueSet theResource, RequestDetails theRequestDetails)
//    {
//        try
//        {
//            return create(theResource, null, true, new Date(), theRequestDetails);
//        }
//        finally
//        {
//            populateCodeSystem(theResource);
//        }
//    }
//
//    @Override
//    public DaoMethodOutcome create(final ValueSet theResource, String theIfNoneExist)
//    {
//        try
//        {
//            return create(theResource, theIfNoneExist, null);
//        }
//        finally
//        {
//            populateCodeSystem(theResource);
//        }
//    }

    @Override
    public DaoMethodOutcome create(final ValueSet theResource, String theIfNoneExist, RequestDetails theRequestDetails)
    {
        try
        {
            return create(theResource, theIfNoneExist, true, new Date(), theRequestDetails);
        }
        finally
        {
            populateCodeSystem(theResource);
        }
    }

//    @Override
//    public DaoMethodOutcome update(ValueSet theResource)
//    {
//        try
//        {
//            return update(theResource, null, null);
//        }
//        finally
//        {
//            populateCodeSystem(theResource);
//        }
//    }
//
//    @Override
//    public DaoMethodOutcome update(ValueSet theResource, RequestDetails theRequestDetails)
//    {
//        try
//        {
//            return update(theResource, null, theRequestDetails);
//        }
//        finally
//        {
//            populateCodeSystem(theResource);
//        }
//    }
//
//    @Override
//    public DaoMethodOutcome update(ValueSet theResource, String theMatchUrl)
//    {
//        try
//        {
//            return update(theResource, theMatchUrl, null);
//        }
//        finally
//        {
//            populateCodeSystem(theResource);
//        }
//    }
//
//    @Override
//    public DaoMethodOutcome update(ValueSet theResource, String theMatchUrl, RequestDetails theRequestDetails)
//    {
//        try
//        {
//            return update(theResource, theMatchUrl, true, theRequestDetails);
//        }
//        finally
//        {
//            populateCodeSystem(theResource);
//        }
//    }

    @Override
    public DaoMethodOutcome update(ValueSet theResource, String theMatchUrl, boolean thePerformIndexing, RequestDetails theRequestDetails)
    {
        try
        {
            return update(theResource, theMatchUrl, thePerformIndexing, false, theRequestDetails);
        }
        finally
        {
            populateCodeSystem(theResource);
        }
    }

    private void populateCodeSystem(ValueSet valueSet)
    {
        if (valueSet.hasCompose() && valueSet.getCompose().hasInclude())
        {
            for (ValueSet.ConceptSetComponent include : valueSet.getCompose().getInclude())
            {
                CodeSystem codeSystem = null;
                boolean updateCodeSystem = false;

                if (include.hasSystem())
                {
                    // fetch the CodeSystem associated with the system url
                    IBundleProvider bundleProvider = codeSystemResourceProvider
                            .getDao()
                            .search(
                                    new SearchParameterMap()
                                            .add("url", new UriParam(include.getSystem()))
                            );

                    List<IBaseResource> resources = bundleProvider.getResources(0, 1);

                    if (!resources.isEmpty())
                    {
                        codeSystem = (CodeSystem) resources.get(0);
                    }

                    // if the CodeSystem doesn't exist, create it
                    else
                    {
                        String id = "CodeSystem-" + Integer.toString(include.getSystem().hashCode());
                        if (codeSystems.containsKey(id))
                        {
                            codeSystem = codeSystems.get(id);
                        }
                        else
                        {
                            codeSystem = new CodeSystem()
                                    .setUrl(include.getSystem())
                                    .setStatus(Enumerations.PublicationStatus.ACTIVE);
                            if (!include.getSystem().equals("http://snomed.info/sct"))
                            {
                                codeSystem.setContent(CodeSystem.CodeSystemContentMode.EXAMPLE);
                            }
                            codeSystem.setId(id);
                            codeSystems.put(id, codeSystem);
                        }
                    }

                    // Go through the codes in the ValueSet and, if the codes are not included in the CodeSystem, add them
                    for (ValueSet.ConceptReferenceComponent concept : include.getConcept())
                    {
                        if (concept.hasCode())
                        {
                            boolean isCodeInSystem = false;
                            for (CodeSystem.ConceptDefinitionComponent codeConcept : codeSystem.getConcept())
                            {
                                if (codeConcept.hasCode() && codeConcept.getCode().equals(concept.getCode()))
                                {
                                    isCodeInSystem = true;
                                    break;
                                }
                            }
                            if (!isCodeInSystem)
                            {
                                codeSystem.addConcept(
                                        new CodeSystem.ConceptDefinitionComponent()
                                                .setCode(concept.getCode())
                                                .setDisplay(concept.getDisplay())
                                );
                                updateCodeSystem = true;
                            }
                        }
                    }
                }

                // update the CodeSystem if missing codes were found
                if (codeSystem != null && updateCodeSystem)
                {
                    codeSystemResourceProvider.getDao().update(codeSystem);
                }
            }
        }
    }
}
