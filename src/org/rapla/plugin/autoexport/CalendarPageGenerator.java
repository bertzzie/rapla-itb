/*--------------------------------------------------------------------------*
 | Copyright (C) 2006 Christopher Kohlhaas                                  |
 |                                                                          |
 | This program is free software; you can redistribute it and/or modify     |
 | it under the terms of the GNU General Public License as published by the |
 | Free Software Foundation. A copy of the license has been included with   |
 | these distribution in the COPYING file, if not go to www.fsf.org         |
 |                                                                          |
 | As a special exception, you are granted the permissions to link this     |
 | program with every library, which license fulfills the Open Source       |
 | Definition as published by the Open Source Initiative (OSI).             |
 *--------------------------------------------------------------------------*/
package org.rapla.plugin.autoexport;

import java.io.IOException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.rapla.components.util.IOUtil;
import org.rapla.entities.User;
import org.rapla.facade.RaplaComponent;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.gui.ViewFactory;
import org.rapla.gui.internal.common.CalendarModelImpl;
import org.rapla.gui.internal.common.CalendarNotFoundExeption;
import org.rapla.plugin.RaplaExtensionPoints;
import org.rapla.servletpages.RaplaPageGenerator;

/******* USAGE: ************
 * ReadOnly calendarview view.
 * You will need the autoexport plugin to create a calendarview-view.
 *
 * Call:
 * rapla?page=calendar&user=<username>&file=<export_name>
 *
 * Optional Parameters:
 *
 * &hide_nav: will hide the navigation bar.
 * &day=<day>:  int-value of the day of month that should be displayed
 * &month=<month>:  int-value of the month
 * &year=<year>:  int-value of the year
 * &today:  will set the view to the current day. Ignores day, month and year
 * @throws IOException
 * @throws ServletException
 * @throws Exception
 * @throws RaplaException
 */
public class CalendarPageGenerator extends RaplaComponent implements RaplaPageGenerator
{
    public CalendarPageGenerator( RaplaContext context ) throws RaplaException
    {
        super( context );
    }

    public void generatePage( ServletContext servletContext, HttpServletRequest request, HttpServletResponse response )
            throws IOException, ServletException
    {
        try
        {
            String username = request.getParameter( "user" );
            String filename = request.getParameter( "file" );
            CalendarModelImpl model = null;
            User user = getQuery().getUser( username );
            model = new CalendarModelImpl( getContext(),user );
            try
            {
            	model.load(filename);
            } 
            catch (CalendarNotFoundExeption ex)
            {
              	String message = "404 Calendar with name '" + filename + "' not available!" ;
    			response.getWriter().print(message);
            	response.setStatus( 404 );
            	getLogger().warn( message);
            	return;            	
            }
            final Object isSet = model.getOption(AutoExportPlugin.HTML_EXPORT);
            if( isSet == null || isSet.equals("false"))
            {
              	String message = "404 Exportfile with name '" + filename + "' not published!" ;
    			response.getWriter().print(message);
            	response.setStatus( 404 );
            	getLogger().warn( message);
            	return;
            }
            
            final String viewId = model.getViewId();
            ViewFactory factory = (ViewFactory) getService( RaplaExtensionPoints.CALENDAR_VIEW_EXTENSION
                    + "/"
                    + viewId );

            if ( factory != null )
            {
                RaplaPageGenerator currentView = factory.createHTMLView( getContext(), model );
                if ( currentView != null )
                {
                    currentView.generatePage( servletContext, request, response );
                }
                else
                {
                    writeFehler( response, "No view available for exportfile '"
                            + filename
                            + "'. Rapla has currently no html support for the view with the id '"
                            + viewId
                            + "'." );
                }
            }
            else
            {
                writeFehler( response, "No view available for exportfile '"
                        + filename
                        + "'. Please install and select the plugin for "
                        + viewId );
            }
        }
        catch ( Exception ex )
        {
            java.io.PrintWriter out = response.getWriter();
            out.println( IOUtil.getStackTraceAsString( ex ) );
            throw new ServletException( ex );
        }
    }

    private void writeFehler( HttpServletResponse response, String message ) throws IOException
    {
        response.setContentType( "text/html; charset=" + getRaplaLocale().getCharsetNonUtf() );
        java.io.PrintWriter out = response.getWriter();
        out.println( message );
    }

}
