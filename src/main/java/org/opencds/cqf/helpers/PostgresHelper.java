package org.opencds.cqf.helpers;

import java.sql.Connection;
import java.sql.SQLException;

import org.apache.commons.dbcp2.BasicDataSource;
import org.opencds.cqf.config.HapiProperties;

// TODO: Once the QDM and HAPI FHIR Persistence layers are the same we can deprecate this
public class PostgresHelper {

    protected static final String POSTGRES_HOST = "themis.postgres.host";
    protected static final String POSTGRES_DB = "themis.postgres.database";
    protected static final String POSTGRES_USER = "themis.postgres.user";
    protected static final String POSTGRES_PASSWORD = "themis.postgres.password";

    private static String getPassword() {
        return HapiProperties.getProperty(POSTGRES_PASSWORD, "");
    }

    private static String getUser() {
        return HapiProperties.getProperty(POSTGRES_USER, "postgres");
    }

    private static String getHost() {
        return HapiProperties.getProperty(POSTGRES_HOST, "localhost:5432");
    }

    private static String getDatabase() {
        return HapiProperties.getProperty(POSTGRES_DB, "fhir");
    }

    private static BasicDataSource getDataSource(String database) {
        BasicDataSource retVal = new BasicDataSource();
        retVal.setDriver(new org.postgresql.Driver());
        retVal.setUsername(getUser());
        retVal.setPassword(getPassword());
        retVal.setUrl("jdbc:postgresql://" + getHost() + "/" + database);
        return retVal;
    }

    private static BasicDataSource getPostgresMasterDataSource() {
        return getDataSource("postgres");
    }

    public static void ensureUserDatabase() throws SQLException {
        BasicDataSource ds = getPostgresMasterDataSource();
        try {
            Connection conn = ds.getConnection();
            conn.createStatement().execute("CREATE DATABASE " + getDatabase());
        } catch (SQLException e) {
            if (!(e.getSQLState() != null && e.getSQLState().equals("42P04"))) {
                throw e;
            }
        } finally {
            ds.close();
        }

    }

    public static BasicDataSource getUserDataSource() {
        return getDataSource(getDatabase());
    }

}