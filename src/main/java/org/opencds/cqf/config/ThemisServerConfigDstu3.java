package org.opencds.cqf.config;

import java.sql.SQLException;
import java.util.Properties;

import javax.annotation.PostConstruct;

import org.opencds.cqf.helpers.PostgresHelper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import ca.uhn.fhir.context.ConfigurationException;

@Configuration
@Import(FhirServerConfigCommon.class)
@EnableTransactionManagement()
public class ThemisServerConfigDstu3 extends FhirServerConfigDstu3 {
    public ThemisServerConfigDstu3() {
        super(PostgresHelper.getUserDataSource());
    }

    @PostConstruct
    public void initialize() throws SQLException {
        PostgresHelper.ensureUserDatabase();
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