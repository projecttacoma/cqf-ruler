package org.opencds.cqf.dstu3;

import java.util.List;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jaxrs.server.AbstractJaxRsProvider;
import ca.uhn.fhir.jpa.dao.DaoConfig;
import ca.uhn.fhir.jpa.dao.IFhirSystemDao;
import ca.uhn.fhir.jpa.provider.BaseJpaProvider;
import ca.uhn.fhir.jpa.provider.BaseJpaSystemProvider;
import ca.uhn.fhir.jpa.provider.dstu3.JpaConformanceProviderDstu3;
import ca.uhn.fhir.jpa.rp.dstu3.*;
import ca.uhn.fhir.jpa.term.IHapiTerminologySvcDstu3;
import ca.uhn.fhir.rest.server.HardcodedServerAddressStrategy;
import ca.uhn.fhir.rest.server.IPagingProvider;
import ca.uhn.fhir.rest.server.RestfulServer;
import ca.uhn.fhir.rest.server.interceptor.IServerInterceptor;
import ca.uhn.fhir.spring.boot.autoconfigure.FhirProperties;
import ca.uhn.fhir.spring.boot.autoconfigure.FhirRestfulServerCustomizer;
import org.hl7.fhir.dstu3.model.Library;
import org.hl7.fhir.dstu3.model.ValueSet;
import org.opencds.cqf.dstu3.daos.LibraryDao;
import org.opencds.cqf.dstu3.daos.ValueSetDao;
import org.opencds.cqf.dstu3.providers.*;
import org.opencds.cqf.dstu3.utils.Dstu3LibraryLoader;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.context.annotation.Lazy;

import ca.uhn.fhir.jpa.config.BaseJavaConfigDstu3;
import ca.uhn.fhir.rest.server.IResourceProvider;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.persistence.EntityManagerFactory;
import javax.servlet.ServletException;
import javax.sql.DataSource;

@Configuration
@AutoConfigureAfter({DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
@EnableConfigurationProperties(FhirProperties.class)
public class CqfRulerDstu3Config
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

        private final IHapiTerminologySvcDstu3 terminologySvc;

        private final IFhirSystemDao systemDao;

        private final DaoConfig daoConfig;

        @Autowired
        public FhirRestfulServerConfiguration(
                FhirProperties properties,
                FhirContext fhirContext,
                ObjectProvider<List<IResourceProvider>> resourceProviders,
                ObjectProvider<BaseJpaSystemProvider> systemProviders,
                ObjectProvider<IPagingProvider> pagingProvider,
                ObjectProvider<List<IServerInterceptor>> interceptors,
                ObjectProvider<List<FhirRestfulServerCustomizer>> customizers,
                IHapiTerminologySvcDstu3 terminologySvc,
                IFhirSystemDao systemDao,
                DaoConfig daoConfig)
        {
            this.properties = properties;
            this.fhirContext = fhirContext;
            this.resourceProviders = resourceProviders.getIfAvailable();
            this.pagingProvider = pagingProvider.getIfAvailable();
            this.customizers = customizers.getIfAvailable();
            this.terminologySvc = terminologySvc;
            this.systemDao = systemDao;
            this.daoConfig = daoConfig;

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

            JpaDataProvider dataProvider = new JpaDataProvider(resourceProviders, fhirContext);

            NarrativeProvider narrativeProvider = new NarrativeProvider();

            LibraryProvider libraryProvider = new LibraryProvider(narrativeProvider);
            LibraryResourceProvider libraryResourceProvider = (LibraryResourceProvider) dataProvider.resolveResourceProvider("Library");
            libraryProvider.setContext(libraryResourceProvider.getContext());
            libraryProvider.setDao(libraryResourceProvider.getDao());

            dataProvider.getResourceProviders().remove(libraryResourceProvider);
            dataProvider.getResourceProviders().add(libraryProvider);

            Dstu3LibraryLoader libraryLoader = new Dstu3LibraryLoader((LibraryDao) libraryProvider.getDao());

            ValueSetResourceProvider valueSetProvider = (ValueSetResourceProvider) dataProvider.resolveResourceProvider("ValueSet");
            dataProvider.getResourceProviders().remove(valueSetProvider);
            valueSetProvider.setContext(valueSetProvider.getContext());
            ValueSetDao valueSetDao = (ValueSetDao) valueSetProvider.getDao();
            valueSetDao.setCodeSystemResourceProvider((CodeSystemResourceProvider) dataProvider.resolveResourceProvider("CodeSystem"));
            valueSetProvider.setDao(valueSetDao);

            dataProvider.getResourceProviders().add(valueSetProvider);

            BundleProvider bundleProvider = new BundleProvider(dataProvider);
            BundleResourceProvider bundleResourceProvider = (BundleResourceProvider) dataProvider.resolveResourceProvider("Bundle");
            bundleProvider.setContext(bundleResourceProvider.getContext());
            bundleProvider.setDao(bundleResourceProvider.getDao());

            dataProvider.getResourceProviders().remove(bundleResourceProvider);
            dataProvider.getResourceProviders().add(bundleProvider);

            JpaTerminologyProvider terminologyProvider = new JpaTerminologyProvider(terminologySvc, fhirContext, valueSetProvider);
            dataProvider.setTerminologyProvider(terminologyProvider);

            MeasureProvider measureProvider = new MeasureProvider(
                    dataProvider,
                    systemDao,
                    libraryLoader,
                    new NarrativeProvider()
            );
            MeasureResourceProvider measureResourceProvider = (MeasureResourceProvider) dataProvider.resolveResourceProvider("Measure");
            measureProvider.setContext(measureResourceProvider.getContext());
            measureProvider.setDao(measureResourceProvider.getDao());

            dataProvider.getResourceProviders().remove(measureResourceProvider);
            dataProvider.getResourceProviders().add(measureProvider);

            ActivityDefinitionProvider activityDefinitionProvider = new ActivityDefinitionProvider(dataProvider);
            ActivityDefinitionResourceProvider activityDefinitionResourceProvider = (ActivityDefinitionResourceProvider) dataProvider.resolveResourceProvider("ActivityDefinition");
            activityDefinitionProvider.setContext(activityDefinitionResourceProvider.getContext());
            activityDefinitionProvider.setDao(activityDefinitionResourceProvider.getDao());

            dataProvider.getResourceProviders().remove(activityDefinitionResourceProvider);
            dataProvider.getResourceProviders().add(activityDefinitionProvider);

            PlanDefinitionProvider planDefinitionProvider = new PlanDefinitionProvider(dataProvider);
            PlanDefinitionResourceProvider planDefinitionResourceProvider = (PlanDefinitionResourceProvider) dataProvider.resolveResourceProvider("PlanDefinition");
            planDefinitionProvider.setContext(planDefinitionResourceProvider.getContext());
            planDefinitionProvider.setDao(planDefinitionResourceProvider.getDao());

            dataProvider.getResourceProviders().remove(planDefinitionResourceProvider);
            dataProvider.getResourceProviders().add(planDefinitionProvider);

            EndpointProvider endpointProvider = new EndpointProvider(dataProvider, systemDao);
            EndpointResourceProvider endpointResourceProvider = (EndpointResourceProvider) dataProvider.resolveResourceProvider("Endpoint");
            endpointProvider.setContext(endpointResourceProvider.getContext());
            endpointProvider.setDao(endpointResourceProvider.getDao());

            dataProvider.getResourceProviders().remove(endpointResourceProvider);
            dataProvider.getResourceProviders().add(endpointProvider);

            setResourceProviders(dataProvider.getResourceProviders());
            setPagingProvider(this.pagingProvider);

            setServerAddressStrategy(new HardcodedServerAddressStrategy(this.properties.getServer().getPath()));

            JpaConformanceProviderDstu3 confProvider = new JpaConformanceProviderDstu3(this, systemDao, daoConfig);
            confProvider.initializeOperations();
            setServerConformanceProvider(confProvider);

            customize();
        }
    }

    @Configuration
    @EnableTransactionManagement
    static class CqfRulerDaoConfig extends BaseJavaConfigDstu3
    {
        @Bean
        @Lazy
        public LibraryDao daoLibraryDstu3()
        {
            LibraryDao retVal = new LibraryDao();
            retVal.setResourceType(Library.class);
            retVal.setContext(fhirContextDstu3());
            return retVal;
        }

        @Bean
        @Lazy
        public ValueSetDao daoValueSetDstu3()
        {
            ValueSetDao retVal = new ValueSetDao();
            retVal.setResourceType(ValueSet.class);
            retVal.setContext(fhirContextDstu3());
            return retVal;
        }

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
