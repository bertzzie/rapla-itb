package org.rapla.servletpages;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class DefaultHTMLMenuEntry implements RaplaPageGenerator
{
    String name;
    String linkName;

    public DefaultHTMLMenuEntry(String name, String linkName)
    {
        this.name = name;
        this.linkName = linkName;
    }
    
    public void generatePage( ServletContext context, HttpServletRequest request, HttpServletResponse response ) throws IOException, ServletException
    {
		PrintWriter out = response.getWriter();
		// writing the html code line for a button
		// including the link to the appropriate servletpage
		out.println("<span class=\"button\"><a href=\"" + linkName + "\">" + name + "</a></span>");
    }

}
