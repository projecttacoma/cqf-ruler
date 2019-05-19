package org.opencds.cqf.dstu3;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.provider.dstu3.JpaResourceProviderDstu3;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.*;
import ca.uhn.fhir.rest.server.IResourceProvider;
import org.hl7.fhir.instance.model.api.IAnyResource;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.cql.data.fhir.FhirDataProviderStu3;
import org.opencds.cqf.cql.runtime.Code;
import org.opencds.cqf.cql.runtime.DateTime;
import org.opencds.cqf.cql.runtime.Interval;
import org.opencds.cqf.cql.terminology.ValueSetInfo;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class JpaDataProvider extends FhirDataProviderStu3
{
    private List<IResourceProvider> resourceProviders;

    public JpaDataProvider(List<IResourceProvider> resourceProviders, FhirContext fhirContext)
    {
        this.resourceProviders = resourceProviders;
        // Assuming STU3 TODO: generify
        setPackageName("org.hl7.fhir.dstu3.model");
        setFhirContext(fhirContext);
    }

    public List<IResourceProvider> getResourceProviders() {
        return resourceProviders;
    }

    public void setResourceProviders(List<IResourceProvider> resourceProviders)
    {
        this.resourceProviders = resourceProviders;
    }

    public Iterable<Object> retrieve(String context, Object contextValue, String dataType, String templateId,
                                     String codePath, Iterable<Code> codes, String valueSet, String datePath,
                                     String dateLowPath, String dateHighPath, Interval dateRange)
    {
        SearchParameterMap map = new SearchParameterMap();
        map.setLastUpdated(new DateRangeParam());

        // if (templateId != null && !templateId.equals("")) { }

        if (valueSet != null && valueSet.startsWith("urn:oid:"))
        {
            valueSet = valueSet.replace("urn:oid:", "");
        }

        if (codePath == null && (codes != null || valueSet != null))
        {
            throw new IllegalArgumentException("A code path must be provided when filtering on codes or a valueset.");
        }

        if (dataType == null)
        {
            throw new IllegalArgumentException("A data type (i.e. Procedure, Valueset, etc...) must be specified for clinical data retrieval");
        }

        if (context != null && context.equals("Patient") && contextValue != null)
        {
            ReferenceParam patientParam = new ReferenceParam(contextValue.toString());
            map.add(getPatientSearchParam(dataType), patientParam);
        }

        if (codePath != null && !codePath.equals(""))
        {
            if (valueSet != null && terminologyProvider != null && expandValueSets)
            {
                ValueSetInfo valueSetInfo = new ValueSetInfo().withId(valueSet);
                codes = terminologyProvider.expand(valueSetInfo);
            }

            else if (valueSet != null)
            {
                map.add(
                        convertPathToSearchParam(dataType, codePath),
                        new TokenParam(null, valueSet).setModifier(TokenParamModifier.IN)
                );
            }

            if (codes != null)
            {
                TokenOrListParam codeParams = new TokenOrListParam();

                for (Code code : codes)
                {
                    codeParams.addOr(new TokenParam(code.getSystem(), code.getCode()));
                }

                map.add(convertPathToSearchParam(dataType, codePath), codeParams);
            }
        }

        if (dateRange != null)
        {
            DateParam low = null;
            DateParam high = null;
            if (dateRange.getLow() != null)
            {
                low = new DateParam(ParamPrefixEnum.GREATERTHAN_OR_EQUALS, Date.from(((DateTime) dateRange.getLow()).getDateTime().toInstant()));
            }

            if (dateRange.getHigh() != null)
            {
                high = new DateParam(ParamPrefixEnum.LESSTHAN_OR_EQUALS, Date.from(((DateTime) dateRange.getHigh()).getDateTime().toInstant()));
            }

            DateRangeParam rangeParam;
            if (low == null && high != null)
            {
                rangeParam = new DateRangeParam(high);
            }

            else if (high == null && low != null)
            {
                rangeParam = new DateRangeParam(low);
            }

            else
            {
                rangeParam = new DateRangeParam(low, high);
            }

            map.add(convertPathToSearchParam(dataType, datePath), rangeParam);
        }

        JpaResourceProviderDstu3<? extends IAnyResource> jpaResProvider = resolveResourceProvider(dataType);
        IBundleProvider bundleProvider = jpaResProvider.getDao().search(map);

        if (bundleProvider.size() == 0)
        {
            return new ArrayList<>();
        }

        List<IBaseResource> resourceList = bundleProvider.getResources(0, bundleProvider.size());
        return resolveResourceList(resourceList);
    }

    private Iterable<Object> resolveResourceList(List<IBaseResource> resourceList)
    {
        List<Object> ret = new ArrayList<>();
        for (IBaseResource res : resourceList)
        {
            Class clazz = res.getClass();
            ret.add(clazz.cast(res));
        }

        return ret;
    }

    public JpaResourceProviderDstu3<? extends IAnyResource> resolveResourceProvider(String datatype)
    {
        for (IResourceProvider resource : resourceProviders)
        {
            if (resource.getResourceType().getSimpleName().toLowerCase().equals(datatype.toLowerCase()))
            {
                return (JpaResourceProviderDstu3<? extends IAnyResource>) resource;
            }
        }

        throw new RuntimeException("Could not find resource provider for type: " + datatype);
    }
}
