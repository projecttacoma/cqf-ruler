package org.opencds.cqf.dstu3.daos;

import ca.uhn.fhir.jpa.dao.DaoMethodOutcome;
import ca.uhn.fhir.jpa.dao.dstu3.FhirResourceDaoDstu3;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.ModelManager;
import org.hl7.fhir.dstu3.model.Attachment;
import org.hl7.fhir.dstu3.model.Library;
import org.hl7.fhir.instance.model.api.IIdType;
import org.opencds.cqf.dstu3.utils.CqlTranslationUtils;

import java.io.ByteArrayInputStream;
import java.util.HashMap;

public class LibraryDao extends FhirResourceDaoDstu3<Library>
{
    private HashMap<String, org.cqframework.cql.elm.execution.Library> libraryCache = new HashMap<>();
    private HashMap<String, IllegalArgumentException> errors = new HashMap<>();
    private LibraryManager libraryManager;
    private ModelManager modelManager;

    public LibraryDao()
    {
        super();
    }

    @Override
    public DaoMethodOutcome delete(IIdType theId)
    {
        try
        {
            return delete(theId, null);
        }
        finally
        {
            if (libraryCache.containsKey(theId.getIdPart()))
            {
                libraryCache.remove(theId.getIdPart());
            }

            if (errors.containsKey(theId.getIdPart()))
            {
                errors.remove(theId.getIdPart());
            }
        }
    }

//    @Override
//    public DaoMethodOutcome update(Library theResource)
//    {
//        boolean error = false;
//        try
//        {
//            return update(theResource, null, null);
//        }
//        catch (Exception e)
//        {
//            error = true;
//            throw e;
//        }
//        finally
//        {
//            if (!error)
//            {
//                updateCache(theResource);
//            }
//        }
//    }
//
//    @Override
//    public DaoMethodOutcome update(Library theResource, RequestDetails theRequestDetails)
//    {
//        boolean error = false;
//        try
//        {
//            return update(theResource, null, theRequestDetails);
//        }
//        catch (Exception e)
//        {
//            error = true;
//            throw e;
//        }
//        finally
//        {
//            if (!error)
//            {
//                updateCache(theResource);
//            }
//        }
//    }
//
//    @Override
//    public DaoMethodOutcome update(Library theResource, String theMatchUrl)
//    {
//        boolean error = false;
//        try
//        {
//            return update(theResource, theMatchUrl, null);
//        }
//        catch (Exception e)
//        {
//            error = true;
//            throw e;
//        }
//        finally
//        {
//            if (!error)
//            {
//                updateCache(theResource);
//            }
//        }
//    }
//
//    @Override
//    public DaoMethodOutcome update(Library theResource, String theMatchUrl, RequestDetails theRequestDetails) {
//        boolean error = false;
//        try
//        {
//            return update(theResource, theMatchUrl, true, theRequestDetails);
//        }
//        catch (Exception e)
//        {
//            error = true;
//            throw e;
//        }
//        finally
//        {
//            if (!error)
//            {
//                updateCache(theResource);
//            }
//        }
//    }

    @Override
    public DaoMethodOutcome update(Library theResource, String theMatchUrl, boolean thePerformIndexing, RequestDetails theRequestDetails)
    {
        boolean error = false;
        try
        {
            return update(theResource, theMatchUrl, thePerformIndexing, false, theRequestDetails);
        }
        catch (Exception e)
        {
            error = true;
            throw e;
        }
        finally
        {
            if (!error)
            {
                updateCache(theResource);
            }
        }
    }

    public org.cqframework.cql.elm.execution.Library getLibrary(String id)
    {
        if (libraryCache != null)
        {
            if (errors.containsKey(id))
            {
                throw errors.get(id);
            }
            else if (libraryCache.containsKey(id))
            {
                return libraryCache.get(id);
            }
        }

        throw new RuntimeException("Cannot find Library with id or name: " + id);
    }

    // TODO: cache contained libraries as well
    private void updateCache(Library library)
    {
        if (modelManager == null || libraryManager ==  null)
        {
            modelManager = new ModelManager();
            libraryManager = new LibraryManager(modelManager);
        }

        byte[] cqlContent = null;
        byte[] elmContent = null;
        org.cqframework.cql.elm.execution.Library elmLibrary;
        if (library.hasContent())
        {
            for (Attachment content : library.getContent())
            {
                if (content.hasData() && content.hasContentType())
                {
                    if (content.getContentType().equals("text/cql"))
                    {
                        cqlContent = content.getData();
                    }
                    else if (content.getContentType().equals("application/elm+xml"))
                    {
                        elmContent = content.getData();
                    }
                }
            }
        }

        try
        {
            if (elmContent != null)
            {
                elmLibrary = CqlTranslationUtils.readLibrary(new ByteArrayInputStream(elmContent));
                resolveCache(library, elmLibrary);
            }

            else if (cqlContent != null)
            {
                elmLibrary =
                        CqlTranslationUtils.translateLibrary(
                                new ByteArrayInputStream(cqlContent),
                                libraryManager,
                                modelManager
                        );

                resolveCache(library, elmLibrary);
            }
        }
        catch (IllegalArgumentException iae)
        {
            errors.put(library.getId(), iae);
            if (library.hasName())
            {
                errors.put(library.getName(), iae);
            }
        }
    }

    private void resolveCache(Library library, org.cqframework.cql.elm.execution.Library elmLibrary)
    {
        libraryCache.put(library.getId(), elmLibrary);
        if (errors.containsKey(library.getId()))
        {
            errors.remove(library.getId());
        }

        if (library.hasName())
        {
            libraryCache.put(library.getName(), elmLibrary);
            if (errors.containsKey(library.getName()))
            {
                errors.remove(library.getName());
            }
        }
    }
}
