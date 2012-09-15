package org.rapla.plugin.tableview.internal;

import java.io.IOException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.rapla.facade.RaplaComponent;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.framework.RaplaLocale;
import org.rapla.gui.CalendarModel;

abstract public class TableViewPage extends RaplaComponent {

	protected CalendarModel model;

	public TableViewPage(RaplaContext context) throws RaplaException {
		super(context);
	}

	public String getTitle() {
	    return model.getNonEmptyTitle();
	}

	public void generatePage(ServletContext context, HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException 
	{
	    response.setContentType("text/html; charset=" + getRaplaLocale().getCharsetNonUtf() );
	    java.io.PrintWriter out = response.getWriter();
	
	    RaplaLocale raplaLocale= getRaplaLocale();
	
	    out.println("<html>");
	    out.println("<head>");
	    out.println("  <title>" + getTitle() + "</title>");
	    out.println("  <link REL=\"stylesheet\" href=\"rapla?page=resource&name=calendar.css\" type=\"text/css\">");
	    out.println("  <link REL=\"stylesheet\" href=\"default.css\" type=\"text/css\">");
	    // tell the html page where its favourite icon is stored
	    out.println("    <link REL=\"shortcut icon\" type=\"image/x-icon\" href=\"/images/favicon.ico\">");
	    out.println("  <meta HTTP-EQUIV=\"Content-Type\" content=\"text/html; charset=" + raplaLocale.getCharsetNonUtf() + "\">");
	    out.println("</head>");
	    out.println("<body>");
	    out.println("<h2 class=\"title\">");
	    out.println(getTitle());
	    out.println("</h2>");
	    out.println("<div id=\"calendar\">");
	    try {
	        final String calendarHTML = getCalendarHTML();
	        out.println(calendarHTML);
	    } catch (RaplaException e) {
	        throw new ServletException( e);
	    }
	    out.println("</div>");
	    
	    // end weekview
	    out.println("</body>");
	    out.println("</html>");
	    
	}

	abstract String getCalendarHTML() throws RaplaException;

}