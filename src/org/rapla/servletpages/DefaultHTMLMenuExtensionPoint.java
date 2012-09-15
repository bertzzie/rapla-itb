package org.rapla.servletpages;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class DefaultHTMLMenuExtensionPoint implements HTMLMenuExtensionPoint, RaplaPageGenerator
{
    List<RaplaPageGenerator> entries = new ArrayList<RaplaPageGenerator>();
    public void insert( RaplaPageGenerator gen )
    {
        entries.add( gen);
    }
    
    public void generatePage( ServletContext context, HttpServletRequest request, HttpServletResponse response ) throws IOException, ServletException
    {
        if ( entries.size() == 0)
        {
            return;
        }
        PrintWriter out = response.getWriter();
//        out.println("<ul>");
        
     // there is an ArraList of entries that wants to be part of the HTML
        // menu we go through this ArraList,
        
        for (Iterator<RaplaPageGenerator> it = entries.iterator();it.hasNext();)
        {
            RaplaPageGenerator entry = (RaplaPageGenerator)it.next();
            out.println("<div class=\"menuEntry\">");
            entry.generatePage(  context, request, response );
            out.println("</div>");
        }
//        out.println("</ul>");
    }

}
