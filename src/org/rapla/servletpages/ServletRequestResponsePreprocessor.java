package org.rapla.servletpages;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.rapla.framework.RaplaContext;

/**
 * User: kuestermann
 * Date: 15.08.12
 * Time: 19:24
 */
public interface ServletRequestResponsePreprocessor {

    /**
     * will return request handle to service
     *
     * @param context
     * @param servletContext
     * @param request
     * @return  null values will be ignored, otherwise return object will be used for further processing
     */
    HttpServletRequest handleRequest(RaplaContext context, ServletContext servletContext, HttpServletRequest request) throws ServletException;

    /**
     * will return response handle to service, committed response will prevent from processing further
     *
     * @param context
     * @param servletContext
     * @param response
     * @return   null values will be ignored by calling servlet, otherwise new return object will be used.
     */
    HttpServletResponse handleResponse(RaplaContext context, ServletContext servletContext, HttpServletResponse response) throws ServletException;

}
