package org.opencds.cqf.r4.providers;

import ca.uhn.fhir.jpa.rp.r4.BundleResourceProvider;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.ModelManager;
import org.cqframework.cql.elm.execution.Library;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.exceptions.FHIRException;
import org.opencds.cqf.cql.execution.Context;
import org.opencds.cqf.cql.runtime.DateTime;
import org.opencds.cqf.r4.helpers.LibraryHelper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class FHIRBundleResourceProvider extends BundleResourceProvider {

    private JpaDataProvider provider;
    public FHIRBundleResourceProvider(JpaDataProvider provider) {
        this.provider = provider;
    }

    @Operation(name = "$apply-cql")
    public Bundle apply(@IdParam IdType id) throws FHIRException {
        Bundle bundle = this.getDao().read(id);
        if (bundle == null) {
            throw new IllegalArgumentException("Could not find Bundle/" + id.getIdPart());
        }
        return applyCql(bundle);
    }

    @Operation(name = "$apply-cql")
    public Bundle apply(@OperationParam(name = "resourceBundle", min = 1, max = 1, type = Bundle.class) Bundle bundle)
            throws FHIRException
    {
        return applyCql(bundle);
    }

    public Bundle applyCql(Bundle bundle) throws FHIRException {
        for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
            if (entry.hasResource()) {
                applyCqlToResource(entry.getResource());
            }
        }

        return bundle;
    }

    public Resource applyCqlToResource(Resource resource) throws FHIRException {
        Library library;
        Context context;
        for (Property child : resource.children()) {
            for (Base base : child.getValues()) {
                if (base != null) {
                    List<String> extension = getExtension(base);
                    if (!extension.isEmpty()) {
                        String cql = String.format("using FHIR version '4.0.0' define x: %s", extension.get(1));
                        library = LibraryHelper.translateLibrary(cql, new LibraryManager(new ModelManager()), new ModelManager());
                        context = new Context(library);
                        context.registerDataProvider("http://hl7.org/fhir", provider);
                        Object result = context.resolveExpressionRef("x").getExpression().evaluate(context);
                        if (extension.get(0).equals("extension")) {
                            resource.setProperty(child.getName(), resolveType(result, base.fhirType()));
                        }
                        else {
                            String type = base.getChildByName(extension.get(0)).getTypeCode();
                            base.setProperty(extension.get(0), resolveType(result, type));
                        }
                    }
                }
            }
        }
        return resource;
    }

    private List<String> getExtension(Base base) {
        List<String> retVal = new ArrayList<>();
        for (Property child : base.children()) {
            for (Base childBase : child.getValues()) {
                if (childBase != null) {
                    if (((Element) childBase).hasExtension()) {
                        for (Extension extension : ((Element) childBase).getExtension()) {
                            if (extension.getUrl().equals("http://hl7.org/fhir/StructureDefinition/cqif-cqlExpression")) {
                                retVal.add(child.getName());
                                retVal.add(extension.getValue().primitiveValue());
                            }
                        }
                    }
                    else if (childBase instanceof Extension) {
                        retVal.add(child.getName());
                        retVal.add(((Extension) childBase).getValue().primitiveValue());
                    }
                }
            }
        }
        return retVal;
    }

    private Base resolveType(Object source, String type) {
        if (source instanceof Integer) {
            return new IntegerType((Integer) source);
        }
        else if (source instanceof BigDecimal) {
            return new DecimalType((BigDecimal) source);
        }
        else if (source instanceof Boolean) {
            return new BooleanType().setValue((Boolean) source);
        }
        else if (source instanceof String) {
            return new StringType((String) source);
        }
        else if (source instanceof DateTime) {
            if (type.equals("dateTime")) {
                return new DateTimeType().setValue(Date.from(((DateTime) source).getDateTime().toInstant()));
            }
            if (type.equals("date")) {
                return new DateType().setValue(Date.from(((DateTime) source).getDateTime().toInstant()));
            }
        }
        else if (source instanceof org.opencds.cqf.cql.runtime.Date)
        {
            if (type.equals("dateTime")) {
                return new DateTimeType().setValue(java.sql.Date.valueOf(((org.opencds.cqf.cql.runtime.Date) source).getDate()));
            }
            if (type.equals("date")) {
                return new DateType().setValue(java.sql.Date.valueOf(((org.opencds.cqf.cql.runtime.Date) source).getDate()));
            }
        }

        if (source instanceof Base) {
            return (Base) source;
        }

        throw new RuntimeException("Unable to resolve type: " + source.getClass().getSimpleName());
    }
}
