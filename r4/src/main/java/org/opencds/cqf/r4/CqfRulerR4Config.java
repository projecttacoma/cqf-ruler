package org.opencds.cqf.r4;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jaxrs.server.AbstractJaxRsProvider;
import ca.uhn.fhir.jpa.config.BaseJavaConfigR4;
import ca.uhn.fhir.jpa.dao.DaoConfig;
import ca.uhn.fhir.jpa.provider.BaseJpaProvider;
import ca.uhn.fhir.jpa.provider.BaseJpaSystemProvider;
import ca.uhn.fhir.rest.server.HardcodedServerAddressStrategy;
import ca.uhn.fhir.rest.server.IPagingProvider;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.RestfulServer;
import ca.uhn.fhir.rest.server.interceptor.IServerInterceptor;
import ca.uhn.fhir.spring.boot.autoconfigure.FhirProperties;
import ca.uhn.fhir.spring.boot.autoconfigure.FhirRestfulServerCustomizer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.persistence.EntityManagerFactory;
import javax.servlet.ServletException;
import javax.sql.DataSource;
import java.util.List;

@Configuration
@AutoConfigureAfter({DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
@EnableConfigurationProperties(FhirProperties.class)
public class CqfRulerR4Config
{
    @Configuration
    @ConditionalOnClass(AbstractJaxRsProvider.class)
    @EnableConfigurationProperties(FhirProperties.class)
    @ConfigurationProperties("hapi.fhir.rest")
    @SuppressWarnings("serial")
    static class FhirRestfulServerConfiguration extends RestfulServer {

        private final FhirProperties properties;

        private final FhirContext fhirContext;

        private final List<IResourceProvider> resourceProviders;

        private final IPagingProvider pagingProvider;

        private final List<FhirRestfulServerCustomizer> customizers;

        public FhirRestfulServerConfiguration(
                FhirProperties properties,
                FhirContext fhirContext,
                ObjectProvider<List<IResourceProvider>> resourceProviders,
                ObjectProvider<BaseJpaSystemProvider> systemProviders,
                ObjectProvider<IPagingProvider> pagingProvider,
                ObjectProvider<List<IServerInterceptor>> interceptors,
                ObjectProvider<List<FhirRestfulServerCustomizer>> customizers)
        {
            this.properties = properties;
            this.fhirContext = fhirContext;
            this.resourceProviders = resourceProviders.getIfAvailable();
            this.pagingProvider = pagingProvider.getIfAvailable();
            this.customizers = customizers.getIfAvailable();

            RestfulServerCustomizer customizer = new RestfulServerCustomizer(systemProviders);
            customizer.customize(this);
        }

        private void customize() {
            if (this.customizers != null) {
                AnnotationAwareOrderComparator.sort(this.customizers);
                for (FhirRestfulServerCustomizer customizer : this.customizers) {
                    customizer.customize(this);
                }
            }
        }

        @Bean
        public ServletRegistrationBean serverRegistrationBean() {
            ServletRegistrationBean registration = new ServletRegistrationBean(this, this.properties.getServer().getPath());
            registration.setLoadOnStartup(1);
            return registration;
        }

        @Override
        protected void initialize() throws ServletException {
            super.initialize();

            setFhirContext(this.fhirContext);
            setResourceProviders(this.resourceProviders);
            setPagingProvider(this.pagingProvider);

            setServerAddressStrategy(new HardcodedServerAddressStrategy(this.properties.getServer().getPath()));

            customize();
        }
    }

    @Configuration
    @EnableTransactionManagement
    static class CqfRulerDaoConfig extends BaseJavaConfigR4
    {
        @Bean
        public JpaTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
            JpaTransactionManager retVal = new JpaTransactionManager();
            retVal.setEntityManagerFactory(entityManagerFactory);
            return retVal;
        }
    }

    @Configuration
    @ConditionalOnClass(BaseJpaProvider.class)
    @ConditionalOnBean({DaoConfig.class, RestfulServer.class, DataSource.class})
    @SuppressWarnings("rawtypes")
    static class RestfulServerCustomizer implements FhirRestfulServerCustomizer {

        private final BaseJpaSystemProvider systemProviders;

        public RestfulServerCustomizer(ObjectProvider<BaseJpaSystemProvider> systemProviders) {
            this.systemProviders = systemProviders.getIfAvailable();
        }

        @Override
        public void customize(RestfulServer server) {
            server.registerProviders(systemProviders);
        }
    }
}
