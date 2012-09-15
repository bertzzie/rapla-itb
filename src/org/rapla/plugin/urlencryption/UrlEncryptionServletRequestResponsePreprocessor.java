package org.rapla.plugin.urlencryption;

import org.rapla.framework.RaplaContext;
import org.rapla.servletpages.ServletRequestResponsePreprocessor;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * User: kuestermann
 * Date: 15.08.12
 * Time: 19:39
 */
public class UrlEncryptionServletRequestResponsePreprocessor implements ServletRequestResponsePreprocessor {
    private UrlEncryptionService urlEncryptionService;
    private HttpServletRequest newRequest;

    public HttpServletRequest handleRequest(RaplaContext context, ServletContext servletContext, HttpServletRequest request) {

        newRequest = request;

        try {
            urlEncryptionService = (UrlEncryptionService) context.lookup(UrlEncryption.ROLE);
            if (urlEncryptionService.isEnabled())
                return request;
            if (request.getParameter(UrlEncryption.ENCRYPTED_PARAMETER_NAME) != null)
                newRequest = urlEncryptionService.handleEncryptedSource(request);
        } catch (Exception ignored) {
            newRequest = request;
        }
        return newRequest;
    }

    public HttpServletResponse handleResponse(RaplaContext context, ServletContext servletContext, HttpServletResponse response) throws ServletException {
        if (urlEncryptionService != null && newRequest != null) {
            try {
                if (urlEncryptionService.isEnabled())
                    return response;
                if (urlEncryptionService.isCalledIllegally(newRequest)) {
                    response.sendError(403);
                }
            } catch (Exception e) {
                throw new ServletException(e);
            }
        }
        return response;
    }


}
