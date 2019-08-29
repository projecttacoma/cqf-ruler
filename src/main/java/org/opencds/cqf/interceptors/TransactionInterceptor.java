package org.opencds.cqf.interceptors;

import ca.uhn.fhir.model.api.TagList;
import ca.uhn.fhir.rest.api.RequestTypeEnum;
import ca.uhn.fhir.rest.api.RestOperationTypeEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.api.server.ResponseDetails;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import ca.uhn.fhir.rest.server.interceptor.IServerInterceptor;
import ca.uhn.fhir.rest.server.interceptor.auth.*;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.ValueSet;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.opencds.cqf.config.FhirAuthConfig;
import org.opencds.cqf.providers.FHIRValueSetResourceProvider;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class TransactionInterceptor  extends AuthorizationInterceptor implements IServerInterceptor {

    private boolean isTransaction;
    private FHIRValueSetResourceProvider valueSetResourceProvider;
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TransactionInterceptor.class);
     private String introspectUrl = "http://localhost:8180/auth/realms/"
            + FhirAuthConfig.get("realm") + "/protocol/openid-connect/token/introspect";

    public TransactionInterceptor(FHIRValueSetResourceProvider valueSetResourceProvider) {
        isTransaction = false;
        this.valueSetResourceProvider = valueSetResourceProvider;
    }

    @Override
    public boolean handleException(RequestDetails requestDetails, BaseServerResponseException e, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws ServletException, IOException {
        return true;
    }

    @Override
    public boolean incomingRequestPostProcessed(RequestDetails requestDetails, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws AuthenticationException {
        applyRulesAndFailIfDeny(requestDetails.getRestOperationType(), requestDetails, null,null, null);
        isTransaction = requestDetails.getRestOperationType() == RestOperationTypeEnum.TRANSACTION;
        return true;
    }

    @Override
    public void incomingRequestPreHandled(RestOperationTypeEnum restOperationTypeEnum, ActionRequestDetails actionRequestDetails) {
    }

    @Override
    public boolean incomingRequestPreProcessed(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        return true;
    }

    @Override
    public boolean outgoingResponse(RequestDetails requestDetails) {
        applyRulesAndFailIfDeny(requestDetails.getRestOperationType(), requestDetails, null,null, null);
        return true;
    }

    @Override
    public boolean outgoingResponse(RequestDetails requestDetails, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws AuthenticationException {
        return true;
    }

    @Override
    public boolean outgoingResponse(RequestDetails requestDetails, IBaseResource iBaseResource) {
        return true;
    }

    @Override
    public boolean outgoingResponse(RequestDetails requestDetails, IBaseResource iBaseResource, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws AuthenticationException {
        return true;
    }

    @Override
    public boolean outgoingResponse(RequestDetails requestDetails, ResponseDetails responseDetails, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws AuthenticationException {
        try {
            return true;
        } finally {
            if (responseDetails.getResponseResource() instanceof Bundle) {
                Bundle responseBundle = (Bundle) responseDetails.getResponseResource();
                if (responseBundle.getType() == Bundle.BundleType.TRANSACTIONRESPONSE && responseBundle.hasEntry()) {
                    for (Bundle.BundleEntryComponent entry : responseBundle.getEntry()) {
                        if (entry.hasResponse() && entry.getResponse().hasLocation()) {
                            if (entry.getResponse().getLocation().startsWith("ValueSet")) {
                                String id = entry.getResponse().getLocation().replace("ValueSet/", "").split("/")[0];
                                valueSetResourceProvider.populateCodeSystem(valueSetResourceProvider.getDao().read(new IdType(id)));
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public boolean outgoingResponse(RequestDetails requestDetails, TagList tagList) {
        return true;
    }

    @Override
    public boolean outgoingResponse(RequestDetails requestDetails, TagList tagList, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws AuthenticationException {
        return true;
    }

    @Override
    public BaseServerResponseException preProcessOutgoingException(RequestDetails requestDetails, Throwable throwable, HttpServletRequest httpServletRequest) throws ServletException {
        return null;
    }

    @Override
    public void processingCompletedNormally(ServletRequestDetails servletRequestDetails) {

    }

    @Override
    public List<IAuthRule> buildRuleList(RequestDetails theRequestDetails) {

        //get OAuth value from prop files
        String useOauth = FhirAuthConfig.get("use_oauth");
        if (!Boolean.parseBoolean(String.valueOf(useOauth))) {
            return new RuleBuilder()
                    .allowAll()
                    .build();
        }
        String authHeader = theRequestDetails.getHeader("Authorization");
        // Get the token and drop "Bearer" from the string
        if (authHeader == null) {
            return new RuleBuilder()
                    .allow().metadata().andThen()
                    .denyAll("No authorization header present")
                    .build();
        }
        String token = authHeader.split(" ")[1];
        String secret = FhirAuthConfig.get("client_secret");
        String clientId = FhirAuthConfig.get("client_id");

        HttpPost httpPost = new HttpPost(introspectUrl);
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("client_id", clientId));
        params.add(new BasicNameValuePair("client_secret", secret));
        params.add(new BasicNameValuePair("token", token));

        try {
            httpPost.setEntity(new UrlEncodedFormEntity(params));
        } catch (UnsupportedEncodingException e) {
            log.trace(e.getMessage());
        }
        JsonObject jsonResponse;
        try {
            CloseableHttpClient client = HttpClients.createDefault();
            CloseableHttpResponse response = client.execute(httpPost);
            String jsonString = EntityUtils.toString(response.getEntity());
            jsonResponse = new JsonParser().parse(jsonString).getAsJsonObject();
            client.close();

        } catch (IOException e) {
            e.printStackTrace();
            log.trace(e.getMessage());
            jsonResponse = null;
        }
        if (jsonResponse.has("error")) {
            return new RuleBuilder()
                    .allow().metadata().andThen()
                    .denyAll("Rejected OAuth token - encountered error")
                    .build();
        } else if (jsonResponse.get("active").getAsBoolean()) {
            return new RuleBuilder()
                    .allowAll()
                    .build();
        } else {
            return new RuleBuilder()
                    .allow().metadata().andThen()
                    .denyAll("Rejected OAuth token - failed introspection")
                    .build();
        }

    }

private void applyRulesAndFailIfDeny(RestOperationTypeEnum theOperation, RequestDetails theRequestDetails, IBaseResource theInputResource, IIdType theInputResourceId,
													 IBaseResource theOutputResource) {
		Verdict decision = applyRulesAndReturnDecision(theOperation, theRequestDetails, theInputResource, theInputResourceId, theOutputResource);

		if (decision.getDecision() == PolicyEnum.ALLOW) {
			return;
		}

		handleDeny(decision);
	}

    @Override
    public Verdict applyRulesAndReturnDecision(RestOperationTypeEnum theOperation, RequestDetails theRequestDetails, IBaseResource theInputResource, IIdType theInputResourceId,
                                               IBaseResource theOutputResource) {
        List<IAuthRule> rules = buildRuleList(theRequestDetails);
        Set<AuthorizationFlagsEnum> flags = getFlags();
        log.trace("Applying {} rules to render an auth decision for operation {}", rules.size(), theOperation);

        Verdict verdict = null;
        for (IAuthRule nextRule : rules) {
            verdict = nextRule.applyRule(theOperation, theRequestDetails, theInputResource, theInputResourceId, theOutputResource, this, flags);
            if (verdict != null) {
                log.trace("Rule {} returned decision {}", nextRule, verdict.getDecision());
                break;
            }
        }

        if (verdict == null) {
          return super.applyRulesAndReturnDecision(theOperation,theRequestDetails,theInputResource,theInputResourceId, theOutputResource);
        }
        return verdict;
    }

}
