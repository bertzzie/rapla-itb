/**
 *
 */
package org.rapla.servletpages;

import java.io.IOException;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.rapla.components.util.IOUtil;

public class RaplaAppletPageGenerator implements RaplaPageGenerator{


    private String getLibsApplet(ServletContext context) throws java.io.IOException {
        StringBuffer buf = new StringBuffer();
        String base = context.getRealPath("/");
        java.io.File baseFile = new java.io.File(base);
        java.io.File[] files = IOUtil.getJarFiles(base,"webclient");
        for (int i=0;i<files.length;i++) {
          buf.append('.');
          buf.append(IOUtil.getRelativeURL(baseFile,files[i]));
          if ( i < files.length-1)
            buf.append(", ");
       }
       return buf.toString();
    }


    public void generatePage( ServletContext context, HttpServletRequest request, HttpServletResponse response ) throws IOException {
        response.setContentType("text/html; charset=ISO-8859-1");
        java.io.PrintWriter out = response.getWriter();
        out.println("<HTML>");
        out.println("<HEAD>");
        out.println("  <title>Rapla Applet</title>");
        out.println("  <link REL=\"stylesheet\" href=\"default.css\" type=\"text/css\">");
        out.println("</HEAD>");
        out.println("<BODY>");
        out.println("   <APPLET code=\"org.rapla.MainApplet\" codebase=\".\" align=\"baseline\"");
        out.println("        width=\"300\" height=\"300\" archive=\""+getLibsApplet(context)+"\"");
        out.println("   >");
        out.println("     <PARAM NAME=\"archive\" VALUE=\""+getLibsApplet(context) +"\">");
        out.println("     <PARAM NAME=\"java_code\" VALUE=\"org.rapla.MainApplet\">");
        out.println("     <PARAM NAME=\"java_codebase\" VALUE=\"./\">");
        out.println("     <PARAM NAME=\"java_type\" VALUE=\"application/x-java-applet;jpi-version=1.4.1\">");
        out.println("     <PARAM NAME=\"scriptable\" VALUE=\"true\">");
        out.println("      No Java support for APPLET tags please install java plugin for your browser!!");
        out.println("   </APPLET>");
        out.println("</BODY>");
        out.println("</HTML>");
    }

}