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
import org.springframework.transaction.annotation.EnableTransactionManagement;

import ca.uhn.fhir.jpa.search.LuceneSearchMappingFactory;

@Configuration
@Import(FhirServerConfigCommon.class)
@EnableTransactionManagement()
public class ThemisServerConfigDstu3 extends FhirServerConfigDstu3 {

    protected static final String POSTGRES_HOST = "themis.postgres.host";
    protected static final String POSTGRES_DB = "themis.postgres.database";
    protected static final String POSTGRES_USER = "themis.postgres.user";
    protected static final String POSTGRES_PASSWORD = "themis.postgres.password";

    @PostConstruct
    public void initialize() throws SQLException {
        BasicDataSource ds = new BasicDataSource();
        ds.setDriver(new org.postgresql.Driver());
        ds.setUsername(this.getUser());
        ds.setPassword(this.getPassword());
        // ds.setPasswork("");
        ds.setUrl("jdbc:postgresql://" + this.getHost() + "/postgres");

        try {
            Connection conn = ds.getConnection();
            conn.createStatement().execute("CREATE DATABASE " + this.getDatabase());
        } catch (SQLException e) {
            if (!e.getSQLState().equals("42P04")) {
                throw e;
            }
        } finally {
            ds.close();
        }
    }

    private String getPassword() {
        return this.getPropertyOrDefault(POSTGRES_PASSWORD, "");
    }

    private String getUser() {
        return this.getPropertyOrDefault(POSTGRES_USER, "postgres");
    }

    private String getHost() {
        return this.getPropertyOrDefault(POSTGRES_HOST, "localhost:5432");
    }

    private String getDatabase() {
        return this.getPropertyOrDefault(POSTGRES_DB, "fhir");
    }

    private String getPropertyOrDefault(String property, String defaultValue) {
        String value = System.getProperty(property);
        return StringUtils.isEmpty(value) ? defaultValue : value;
    }

    // PostgreSQL config
    @Override
    @Bean(name = "myPersistenceDataSourceDstu3", destroyMethod = "close")
    public DataSource dataSource() {
        BasicDataSource retVal = new BasicDataSource();
        retVal.setDriver(new org.postgresql.Driver());
        retVal.setUsername(this.getUser());
        retVal.setPassword(this.getPassword());
        retVal.setUrl("jdbc:postgresql://" + this.getHost() + "/" + this.getDatabase());
        return retVal;
    }

    // PostgreSQL config
    @Override
    protected Properties jpaProperties() {
        Properties extraProperties = new Properties();
        extraProperties.put("hibernate.dialect", org.hibernate.dialect.PostgreSQL94Dialect.class.getName());
        extraProperties.put("hibernate.format_sql", "true");
        extraProperties.put("hibernate.show_sql", "false");
        extraProperties.put("hibernate.hbm2ddl.auto", "update");
        extraProperties.put("hibernate.jdbc.batch_size", "20");
        extraProperties.put("hibernate.cache.use_query_cache", "false");
        extraProperties.put("hibernate.cache.use_second_level_cache", "false");
        extraProperties.put("hibernate.cache.use_structured_entries", "false");
        extraProperties.put("hibernate.cache.use_minimal_puts", "false");
        extraProperties.put("hibernate.search.model_mapping", LuceneSearchMappingFactory.class.getName());
        extraProperties.put("hibernate.search.default.directory_provider", "filesystem");
        extraProperties.put("hibernate.search.default.indexBase", "target/lucenefiles_stu3");
        extraProperties.put("hibernate.search.lucene_version", "LUCENE_CURRENT");
        // extraProperties.put("hibernate.search.default.worker.execution", "async");
        return extraProperties;
    }
}