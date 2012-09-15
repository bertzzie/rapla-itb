/**
 *
 */
package org.rapla.servletpages;

import java.io.IOException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.rapla.facade.RaplaComponent;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.gui.internal.RaplaStartOption;
import org.rapla.plugin.RaplaExtensionPoints;

public class RaplaIndexPageGenerator extends RaplaComponent implements RaplaPageGenerator
{
    RaplaPageGenerator menu;
    
    public RaplaIndexPageGenerator( RaplaContext context ) throws RaplaException
    {
        super( context);
        menu = (RaplaPageGenerator) context.lookup( RaplaExtensionPoints.HTML_MAIN_MENU_EXTENSION_POINT );
    }

    public void generatePage( ServletContext context, HttpServletRequest request, HttpServletResponse response )
            throws IOException, ServletException
    {
		response.setContentType("text/html; charset=ISO-8859-1");
		java.io.PrintWriter out = response.getWriter();
		out.println("<html>");
		out.println("  <head>");
		// add the link to the stylesheet for this page within the <head> tag
		out.println("    <link REL=\"stylesheet\" href=\"default.css\" type=\"text/css\">");
		// tell the html page where its favourite icon is stored
		out.println("    <link REL=\"shortcut icon\" type=\"image/x-icon\" href=\"/images/favicon.ico\">");
		out.println("    <title>");
		 String title;
		 final String defaultTitle = getI18n().getString("rapla.title");
		 try {
            title= getQuery().getPreferences( null ).getEntryAsString(RaplaStartOption.TITLE, defaultTitle);
        } catch (RaplaException e) {
            title = defaultTitle; 
        }
	       
		out.println(title);
		out.println("    </title>");
		out.println("  </head>");
		out.println("  <body>");
		out.println("    <h3>");
		out.println(title);
		out.println("    </h3>");
		menu.generatePage(context, request, response);
		out.println(getI18n().getString("webinfo.text"));
		out.println("  </body>");
		out.println("</html>");
    }

}