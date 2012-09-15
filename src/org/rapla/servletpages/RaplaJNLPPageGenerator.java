/**
 * 
 */
package org.rapla.servletpages;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.rapla.components.util.IOUtil;
import org.rapla.facade.RaplaComponent;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.gui.internal.RaplaStartOption;

public class RaplaJNLPPageGenerator extends RaplaComponent implements RaplaPageGenerator{
    
    public RaplaJNLPPageGenerator( RaplaContext context ) throws RaplaException
    {
        super( context);
    }

    private String getCodebase( HttpServletRequest request)  {
        StringBuffer codebaseBuffer = new StringBuffer();
        codebaseBuffer.append(!request.isSecure() ? "http://" : "https://");
        codebaseBuffer.append(request.getServerName());
        if (request.getServerPort() != (!request.isSecure() ? 80 : 443))
        {
           codebaseBuffer.append(':');
           codebaseBuffer.append(request.getServerPort());
        }
        codebaseBuffer.append(request.getContextPath());
        codebaseBuffer.append('/');
        return codebaseBuffer.toString();
    }

    private String getLibsJNLP(ServletContext context) throws java.io.IOException {
        StringBuffer buf = new StringBuffer();
        String base = context.getRealPath(".");
        java.io.File baseFile = new java.io.File(base);
        java.io.File[] files = IOUtil.getJarFiles(base,"webclient");
        for (int i=0;i<files.length;i++) {
          buf.append("\n<jar href=\".");
          buf.append(IOUtil.getRelativeURL(baseFile,files[i]));
          buf.append("\"");
          if (files[i].getName().indexOf("rapla") == 0) {
             buf.append(" main=\"true\"");
          }
          buf.append("/>");
       }
       return buf.toString();
    }
    
    protected List<String> getProgramArguments() {
        List<String> list = new ArrayList<String>();
/*        list.add("-c");
        list.add("rapla?page=jnlp_http_auth.xconf");*/
        list.add("webstart");
        return list;
    }
    
    public void generatePage( ServletContext context, HttpServletRequest request, HttpServletResponse response ) throws IOException {
		java.io.PrintWriter out = response.getWriter();
        final String defaultTitle = getI18n().getString("rapla.title");
        String menuName;
        try
        {
            menuName= getQuery().getPreferences( null ).getEntryAsString(RaplaStartOption.TITLE, defaultTitle);
        }
        catch (RaplaException e) {
            menuName = defaultTitle;
        }
		response.setContentType("application/x-java-jnlp-file;charset=utf-8");
		out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		out.println("<jnlp spec=\"1.0+\" codebase=\"" + getCodebase(request) + "\">");
		out.println("<information>");
		out.println(" <title>"+menuName+"</title>");
		out.println(" <vendor>rapla.sourceforge.net (development started at Uni Bonn)</vendor>");
		out.println(" <homepage href=\"http://rapla.sourceforge.net\"/>");
		out.println(" <description>Resource Scheduling Application</description>");
		// we changed the logo from .gif to .png to make it more sexy
        //differentiate between icon and splash because of different sizes!
		out.println(" <icon href=\"./webclient/logo.gif\"/> ");
		// and here aswell


		out.println(" <icon kind=\"splash\" href=\"./webclient/logo.png\"/> ");
		 out.println("  <offline-allowed/>");
         out.println(" <shortcut online=\"true\">");
         out.println("       <desktop/>");
         out.println("       <menu submenu=\"" + menuName +  "\"/>");
         out.println(" </shortcut>");
		out.println("</information>");
		boolean allpermissionsAllowed = isSigned();
        final String parameter = request.getParameter("sandbox");
        if (allpermissionsAllowed && (parameter== null || parameter.trim().toLowerCase().equals("false")))
		{
    		out.println("<security>");
    		out.println("  <all-permissions/>");
    		out.println("</security>");
		}
    	out.println("<resources>");
		out.println("  <j2se version=\"1.4+\"/>");
		out.println(getLibsJNLP(context));
		out.println("</resources>");
		out.println("<application-desc main-class=\"org.rapla.Main\">");
		for (Iterator<String> it = getProgramArguments().iterator(); it.hasNext();)
		{
			out.println("  <argument>" + it.next() + "</argument> ");
		}
		out.println("</application-desc>");
		

		out.println("</jnlp>");
     }
    
    
}