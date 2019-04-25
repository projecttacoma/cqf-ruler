package org.opencds.cqf.config;

import java.sql.SQLException;
import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

import org.opencds.cqf.helpers.PostgresHelper;
import org.springframework.context.annotation.Bean;

import ca.uhn.fhir.jpa.search.LuceneSearchMappingFactory;


public class ThemisServerConfigQdm extends QdmServerConfig {

    @PostConstruct
    public void initialize() throws SQLException {
        PostgresHelper.ensureUserDatabase();
    }

    // PostgreSQL config
    @Override
    @Bean(name = "myPersistenceDataSourceDstu3", destroyMethod = "close")
    public DataSource dataSource() {
        return PostgresHelper.getUserDataSource();
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