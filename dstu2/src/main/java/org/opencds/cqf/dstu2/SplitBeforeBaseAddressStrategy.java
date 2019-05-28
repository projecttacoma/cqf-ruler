package org.opencds.cqf.dstu2;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.Validate;

import ca.uhn.fhir.rest.server.IServerAddressStrategy;

/**
 * Server address strategy which looks for "/base*" in the URL
 */
public class SplitBeforeBaseAddressStrategy implements IServerAddressStrategy {

	@Override
	public String determineServerBase(ServletContext theServletContext, HttpServletRequest theRequest) {
		if (theRequest == null) {
			return null;
		}


		StringBuffer requestUrl = theRequest.getRequestURL();

        int startOfPath = requestUrl.indexOf("/base");
        if (startOfPath == -1) {
            return null;
        }
        else {
            return requestUrl.substring(0, startOfPath);
        }
	}

}
