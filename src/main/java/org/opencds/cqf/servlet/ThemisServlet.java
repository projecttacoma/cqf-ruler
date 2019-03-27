package org.opencds.cqf.servlet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.servlet.ServletException;

import org.opencds.cqf.config.ThemisServerConfigDstu3;
import org.opencds.cqf.cql.terminology.TerminologyProvider;
import org.opencds.cqf.providers.CqlExecutionProvider;
import org.opencds.cqf.providers.JpaDataProvider;
import org.opencds.cqf.providers.JpaTerminologyProvider;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.cors.CorsConfiguration;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.config.WebsocketDispatcherConfig;
import ca.uhn.fhir.jpa.dao.DaoConfig;
import ca.uhn.fhir.jpa.dao.IFhirSystemDao;
import ca.uhn.fhir.jpa.provider.dstu3.JpaConformanceProviderDstu3;
import ca.uhn.fhir.jpa.provider.dstu3.JpaSystemProviderDstu3;
import ca.uhn.fhir.jpa.provider.dstu3.TerminologyUploaderProviderDstu3;
import ca.uhn.fhir.jpa.rp.dstu3.ValueSetResourceProvider;
import ca.uhn.fhir.jpa.search.DatabaseBackedPagingProvider;
import ca.uhn.fhir.jpa.term.IHapiTerminologySvcDstu3;
import ca.uhn.fhir.narrative.DefaultThymeleafNarrativeGenerator;
import ca.uhn.fhir.rest.api.EncodingEnum;
import ca.uhn.fhir.rest.server.ETagSupportEnum;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.interceptor.BanUnsupportedHttpMethodsInterceptor;
import ca.uhn.fhir.rest.server.interceptor.CorsInterceptor;
import ca.uhn.fhir.rest.server.interceptor.IServerInterceptor;
import ca.uhn.fhir.rest.server.interceptor.ResponseHighlighterInterceptor;

public class ThemisServlet extends BaseServlet {

    @SuppressWarnings("unchecked")
    @Override
    protected void initialize() throws ServletException {

        WebApplicationContext parentAppCtx = ContextLoaderListener.getCurrentWebApplicationContext();

        String implDesc = getInitParameter("ImplementationDescription");

        List<IResourceProvider> beans;
        @SuppressWarnings("rawtypes")
        IFhirSystemDao systemDao;
        ETagSupportEnum etagSupport;
        String baseUrlProperty;
        List<Object> plainProviders = new ArrayList<>();

        myAppCtx = new AnnotationConfigWebApplicationContext();
        myAppCtx.setServletConfig(getServletConfig());
        myAppCtx.setParent(parentAppCtx);
        myAppCtx.register(ThemisServerConfigDstu3.class, WebsocketDispatcherConfig.class);
        baseUrlProperty = FHIR_BASEURL_DSTU3;
        myAppCtx.refresh();
        setFhirContext(FhirContext.forDstu3());
        beans = myAppCtx.getBean("myResourceProvidersDstu3", List.class);
        plainProviders.add(myAppCtx.getBean("mySystemProviderDstu3", JpaSystemProviderDstu3.class));
        systemDao = myAppCtx.getBean("mySystemDaoDstu3", IFhirSystemDao.class);
        etagSupport = ETagSupportEnum.ENABLED;
        JpaConformanceProviderDstu3 confProvider = new JpaConformanceProviderDstu3(this, systemDao,
                myAppCtx.getBean(DaoConfig.class));
        confProvider.setImplementationDescription(implDesc);
        setServerConformanceProvider(confProvider);
        plainProviders.add(myAppCtx.getBean(TerminologyUploaderProviderDstu3.class));
        provider = new JpaDataProvider(beans);
        TerminologyProvider terminologyProvider = new JpaTerminologyProvider(
                myAppCtx.getBean("terminologyService", IHapiTerminologySvcDstu3.class), getFhirContext(),
                (ValueSetResourceProvider) provider.resolveResourceProvider("ValueSet"));
        provider.setTerminologyProvider(terminologyProvider);
        
        resolveResourceProviders(provider, systemDao);

        CqlExecutionProvider cql = new CqlExecutionProvider(provider);
        plainProviders.add(cql);

        setETagSupport(etagSupport);

        FhirContext ctx = getFhirContext();
        ctx.setNarrativeGenerator(new DefaultThymeleafNarrativeGenerator());

        for (IResourceProvider nextResourceProvider : beans) {
            ourLog.info(" * Have resource provider for: {}", nextResourceProvider.getResourceType().getSimpleName());
        }
        setResourceProviders(beans);

        setPlainProviders(plainProviders);

        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedHeader("x-fhir-starter");
        config.addAllowedHeader("Origin");
        config.addAllowedHeader("Accept");
        config.addAllowedHeader("X-Requested-With");
        config.addAllowedHeader("Content-Type");
        config.addAllowedHeader("Authorization");
        config.addAllowedHeader("Cache-Control");

        config.addAllowedOrigin("*");

        config.addExposedHeader("Location");
        config.addExposedHeader("Content-Location");
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        // Create the interceptor and register it
        CorsInterceptor corsInterceptor = new CorsInterceptor(config);
        // corsInterceptor.getConfig().addAllowedOrigin("http://sandbox.cds-hooks.org");
        // corsInterceptor.getConfig().addAllowedOrigin("*");
        registerInterceptor(corsInterceptor);

        ResponseHighlighterInterceptor responseHighlighterInterceptor = new ResponseHighlighterInterceptor();
        responseHighlighterInterceptor.setShowRequestHeaders(false);
        responseHighlighterInterceptor.setShowResponseHeaders(true);
        registerInterceptor(responseHighlighterInterceptor);

        registerInterceptor(new BanUnsupportedHttpMethodsInterceptor());

        setDefaultPrettyPrint(true);
        setDefaultResponseEncoding(EncodingEnum.JSON);

        String baseUrl = System.getProperty(baseUrlProperty);
        setServerAddressStrategy(new MyHardcodedServerAddressStrategy(baseUrl));

        setPagingProvider(myAppCtx.getBean(DatabaseBackedPagingProvider.class));

        Collection<IServerInterceptor> interceptorBeans = myAppCtx.getBeansOfType(IServerInterceptor.class).values();
        for (IServerInterceptor interceptor : interceptorBeans) {
            this.registerInterceptor(interceptor);
        }
    }
}
