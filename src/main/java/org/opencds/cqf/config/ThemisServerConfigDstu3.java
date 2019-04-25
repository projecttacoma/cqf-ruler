package org.opencds.cqf.config;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import ca.uhn.fhir.context.ConfigurationException;
import ca.uhn.fhir.jpa.search.LuceneSearchMappingFactory;

@Configuration
@Import(FhirServerConfigCommon.class)
@EnableTransactionManagement()
public class ThemisServerConfigDstu3 extends FhirServerConfigDstu3 {

    protected static final String POSTGRES_HOST = "themis.postgres.host";
    protected static final String POSTGRES_DB = "themis.postgres.database";
    protected static final String POSTGRES_USER = "themis.postgres.user";
    protected static final String POSTGRES_PASSWORD = "themis.postgres.password";

    public ThemisServerConfigDstu3() {
        super(getDataSource());
    }


    @PostConstruct
    public void initialize() throws SQLException {
        BasicDataSource ds = getDataSource();

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

    private static String getPassword() {
        return getPropertyOrDefault(POSTGRES_PASSWORD, "");
    }

    private static String getUser() {
        return getPropertyOrDefault(POSTGRES_USER, "postgres");
    }

    private static String getHost() {
        return getPropertyOrDefault(POSTGRES_HOST, "localhost:5432");
    }

    private static String getDatabase() {
        return getPropertyOrDefault(POSTGRES_DB, "fhir");
    }

    private static String getPropertyOrDefault(String property, String defaultValue) {
        String value = System.getProperty(property);
        return StringUtils.isEmpty(value) ? defaultValue : value;
    }

    private static BasicDataSource getDataSource() {
        BasicDataSource retVal = new BasicDataSource();
        retVal.setDriver(new org.postgresql.Driver());
        retVal.setUsername(getUser());
        retVal.setPassword(getPassword());
        retVal.setUrl("jdbc:postgresql://" + getHost() + "/" + getDatabase());
        return retVal;
    }

    @Override
    @Bean()
    public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
        LocalContainerEntityManagerFactoryBean retVal = super.entityManagerFactory();
        retVal.setPersistenceUnitName(HapiProperties.getPersistenceUnitName());

        try {
            retVal.setDataSource(myDataSource);
        } catch (Exception e) {
            throw new ConfigurationException("Could not set the data source due to a configuration issue", e);
        }

        Properties extraProperties = HapiProperties.getProperties();
        extraProperties.put("hibernate.dialect", org.hibernate.dialect.PostgreSQL94Dialect.class.getName());
        retVal.setJpaProperties(extraProperties);
        return retVal;
    }
}