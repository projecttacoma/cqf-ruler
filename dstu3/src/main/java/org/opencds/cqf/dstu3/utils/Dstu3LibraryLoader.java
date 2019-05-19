package org.opencds.cqf.dstu3.utils;

import org.cqframework.cql.elm.execution.Library;
import org.cqframework.cql.elm.execution.VersionedIdentifier;
import org.opencds.cqf.cql.execution.LibraryLoader;
import org.opencds.cqf.dstu3.daos.LibraryDao;

public class Dstu3LibraryLoader implements LibraryLoader
{
    private LibraryDao dao;

    public Dstu3LibraryLoader(LibraryDao dao)
    {
        this.dao = dao;
    }

    @Override
    public Library load(VersionedIdentifier versionedIdentifier)
    {
        return dao.getLibrary(versionedIdentifier.getId());
    }
}
