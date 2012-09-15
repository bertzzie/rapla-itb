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
package org.rapla;

import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.avalon.framework.container.ContainerUtil;
import org.apache.avalon.framework.logger.Logger;
import org.rapla.components.util.IOUtil;
import org.rapla.framework.Container;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaContextException;
import org.rapla.framework.RaplaException;
import org.rapla.framework.StartupEnvironment;
import org.rapla.plugin.RaplaExtensionPoints;
import org.rapla.server.RemoteServer;
import org.rapla.server.RemoteSession;
import org.rapla.server.ServerService;
import org.rapla.server.ShutdownListener;
import org.rapla.server.ShutdownService;
import org.rapla.server.internal.RemoteSessionImpl;
import org.rapla.server.internal.ServerServiceImpl;
import org.rapla.servletpages.RaplaPageGenerator;
import org.rapla.servletpages.ServletRequestResponsePreprocessor;


final public class MainServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    /** The default config filename is raplaserver.xconf*/
    Container raplaMainContainer;
    public final static String DEFAULT_CONFIG_NAME = "raplaserver.xconf";
    Collection<String> pageList;
    ServerService serverService;

    long serverStartTime;
	Logger logger = null;

    private File getConfigFile(String entryName, String defaultName) throws ServletException,IOException {
        String configName = getServletConfig().getInitParameter(entryName);
        if (configName == null)
            configName = defaultName;
        if (configName == null)
            throw new ServletException("Must specify " + entryName + " entry in web.xml !");

        File configFile = new File(getServletConfig().getServletContext().getRealPath("/WEB-INF/" + configName));
        if (!configFile.exists()) {
            String message = "ERROR: Config file not found " + configName;
            throw new ServletException(message);
        }
        return configFile.getCanonicalFile();
    }

    /**
     * Initializes Servlet and creates a <code>RaplaMainContainer</code> instance
     *
     * @exception ServletException if an error occurs
     */
    public void init()
        throws ServletException
    {
        String realPath = getServletContext().getRealPath("webclient");
 //       if (realPath != null)
        {
            File webclientFolder= new File(realPath );
            webclientFolder.mkdir();
            copy( "WEB-INF/lib/rapla.jar", "webclient/rapla.jar" );
        }
        startServer();
    }

    private void copy( String sourceLib, String destLib ) throws ServletException
    {
        if (!new File(getServletContext().getRealPath(sourceLib)).exists())
        {
            return;
        }
        try
        {
            log("Copy " + sourceLib + " to  " + destLib);
            IOUtil.copy( getServletContext().getRealPath(sourceLib), getServletContext().getRealPath(destLib), true);
        }
        catch (IOException e)
        {
            throw new ServletException("Can't copy " + sourceLib + " Cause " + e.getMessage());
        }
    }

    ShutdownListener shutdownListener = new ShutdownListener() {
        public void shutdownInitiated() {
        }

        public void shutdownComplete( boolean restart) {
            if ( restart ) {
                Thread restartThread = new Thread() {
                     public void run() {
                         try {
                             log( "Stopping  Server");
                             stopServer();
                             //getServletContext()
                             log( "Restarting Server");
                             startServer();
                         } catch (Exception e) {
                            log( "Error while restarting Server", e );
                         }
                     }
                };
                restartThread.setDaemon( false );
                restartThread.start();
            }
        }
    };

	void startServer()throws ServletException {
        log("Starting Rapla Servlet");
        serverStartTime = System.currentTimeMillis();

        try
        {
        	File configFile = getConfigFile("config-file",DEFAULT_CONFIG_NAME);
            URL configURL = configFile.toURI().toURL();
            URL logConfigURL = getConfigFile("log-config-file","raplaserver.xlog").toURI().toURL();

            RaplaStartupEnvironment env = new RaplaStartupEnvironment();
            env.setStartupMode( StartupEnvironment.SERVLET);
            env.setConfigURL( configURL );
            env.setLogConfigURL( logConfigURL );

            raplaMainContainer = new RaplaMainContainer( env );
            logger = (Logger)raplaMainContainer.getContext().lookup(Logger.class.getName());
            try {
                //lookup shutdownService
                ShutdownService shutdownService = (ShutdownService) raplaMainContainer.getContext().lookup(ShutdownService.ROLE);
                shutdownService.addShutdownListener( shutdownListener );
            } catch (RaplaContextException ex) {
            	log("No shutdown service found. You must stop the server with ctrl-c!");
            }
            // Start the storage service
            RaplaContext sm = raplaMainContainer.getContext();



            String lookupName = ServerService.ROLE ;
            serverService = (ServerService)sm.lookup( lookupName );

            Collection<?> allServicesFor = serverService.getAllServicesFor( RaplaExtensionPoints.SERVLET_PAGE_EXTENSION);
			pageList = ((Collection<String>)allServicesFor);

            getServletConfig().getServletContext().setAttribute("context", sm);
        }
        catch( Exception e )
        {
        	if ( logger != null) {
        		logger.fatalError("Could not start server", e);
        	}
        	ContainerUtil.dispose( raplaMainContainer);
        	log( "Problem starting Rapla ", e );
            throw new ServletException( "Error during initialization", e );
        }
        log("Rapla Servlet started");

    }

    /**
     * Pass all servlet requests through to container to be handled.
     */
    public void service( HttpServletRequest request, HttpServletResponse response )
        throws IOException, ServletException
    {
        RaplaContext context = serverService.getContext();
		try {
			if (context.has(RaplaExtensionPoints.SERVLET_REQUEST_RESPONSE_PREPROCESSING_POINT))
			{
	            ServletRequestResponsePreprocessor preprocessor = (ServletRequestResponsePreprocessor) context.lookup(RaplaExtensionPoints.SERVLET_REQUEST_RESPONSE_PREPROCESSING_POINT );
	            final HttpServletRequest newRequest = preprocessor.handleRequest(context, getServletContext(), request);
	            if (newRequest != null)
	                request = newRequest;
	            final HttpServletResponse newResponse = preprocessor.handleResponse(context, getServletContext(), response);
	            if (newResponse != null) {
	                response = newResponse;
	                if (response.isCommitted())
	                    return;
	            }
			}
        } catch (RaplaContextException e) {
            //logger.error("Error using preprocessor servlet", e);
        }

        String page =  request.getParameter("page");
        String contextPath =request.getRequestURI();

        int rpcIndex=contextPath.indexOf("/rapla/rpc/") ;
        if ( rpcIndex>= 0)  {
            handleRPCCall( request, response, contextPath );
            return;
        }
        if ( page == null || page.trim().length() == 0) {
            page = "index";
        }

        if (pageList.contains( page) ) {
            RaplaPageGenerator  servletPage;
            try {
                servletPage = (RaplaPageGenerator) context.lookup( RaplaExtensionPoints.SERVLET_PAGE_EXTENSION + "/" +  page );
            } catch (RaplaContextException e) {
                java.io.PrintWriter out = response.getWriter();
                out.println(IOUtil.getStackTraceAsString( e));
                throw new ServletException( e);
            }
            servletPage.generatePage( getServletContext(), request, response);
        } else {
        	String message = "404: Page " + page + " not found in Rapla context";
			response.getWriter().print(message);
        	response.setStatus( 404 );
        	logger.warn( message);
        }
    }

    private void handleRPCCall( HttpServletRequest request, HttpServletResponse response, String contextPath ) throws IOException
    {
        int rpcIndex=contextPath.indexOf("/rapla/rpc/") ;
        String methodName = contextPath.substring(rpcIndex + "/rapla/rpc/".length());
        final HttpSession session = request.getSession( true);
        //String sessionId = session.getId();

        if ( methodName.equals("getException"))
        {
            Exception ex = (Exception)session.getAttribute("lastException");
            if ( ex == null)
            {
                response.sendError( 500 , "No exception found");
            } else {
                ObjectOutputStream out = new ObjectOutputStream( response.getOutputStream());
                out.writeObject( ex);
                out.flush();
            }
            return;
        }

        try
        {
            @SuppressWarnings("unchecked")
			Map<String,String[]> originalMap = request.getParameterMap();
			Map<String,String> parameterMap = makeSingles(originalMap);
            ServerServiceImpl server = (ServerServiceImpl)raplaMainContainer.getContext().lookup( ServerService.ROLE);
            if ( methodName.equals(RemoteServer.ROLE + "/login"))
            {
            	List<String> arg = new ArrayList<String>(parameterMap.values());
            	String username = (String) arg.get(0);
            	String password = (String) arg.get( 1);
            			//parameterMap.get("password");
            	server.login( username, password);
                session.setAttribute("username", username);
                response.getWriter().println("Login successfull" );
				logger.info("Login " + username);
            }
            else if ( methodName.equals(RemoteServer.ROLE + "/logout"))
            {
                response.getWriter().println("User logout" );
                RemoteSession remoteSession = (RemoteSession)session.getAttribute(RemoteSession.class.getName());
                if ( remoteSession != null)
                {
                	currentSession.set( remoteSession);
                	server.logout();
                }
            }
            else
            {
            	RemoteSession  remoteSession = (RemoteSession)session.getAttribute(RemoteSession.class.getName());
                if ( remoteSession != null)
                {
                    // If session was created by another server, than invalidate
                    if (((RemoteSessionImpl)remoteSession).getServerStartTime() != serverStartTime)
                    {
                        remoteSession = null;
                    }
                }
                if ( remoteSession == null)
                {
                    remoteSession = new RemoteSessionImpl(server.getContext(), session.getId(), serverStartTime)
                    {

						@Override
						public void logout() throws RaplaException {
							//String sessionId = session.getId();
							session.removeAttribute(RemoteSession.class.getName());
							session.removeAttribute("username");
						}

                    };
                    session.setAttribute( RemoteSession.class.getName(), remoteSession);
                }
                currentSession.set( remoteSession);
                final String username = (String)session.getAttribute("username");
               ((RemoteSessionImpl)remoteSession).setUsername( username);
                byte[] out = server.dispatch(remoteSession, methodName, parameterMap);
                response.setContentType( "text/html; charset=utf-8");
                //response.setCharacterEncoding( "utf-8" );
                response.getOutputStream().write( out );
            }
                //  response.getWriter().println("There are currently " + reservations + " reservations");
        }
        catch (Exception e)
        {
            String message = e.getMessage();
            if ( message == null )
            {
                message = e.getClass().getName();
            }
            session.setAttribute( "lastException", e);
            response.addHeader("X-Error-Stacktrace", message);
            response.getWriter().println("Error: " + IOUtil.getStackTraceAsString( e));

            //response.sendError( 500, e.getMessage());

            response.setStatus( 500);
            //throw new ServletException( e);
        }
    }

    private Map<String,String> makeSingles( Map<String, String[]> parameterMap )
    {
        TreeMap<String,String> singlesMap = new TreeMap<String,String>();
        for (Iterator<String> it = parameterMap.keySet().iterator();it.hasNext();)
        {
            String key = it.next();
            String[] values =  parameterMap.get( key);
            if ( values != null && values.length > 0 )
            {
                singlesMap.put( key,values[0]);
            }
            else
            {
                singlesMap.put( key,null);
            }
        }

        return singlesMap;

    }

    private void stopServer() {
        try {
            ShutdownService shutdownService = (ShutdownService) raplaMainContainer.getContext().lookup(ShutdownService.ROLE);
            shutdownService.removeShutdownListener( shutdownListener );
            ContainerUtil.dispose( raplaMainContainer );
        } catch (Exception ex) {
            log("Error while stopping server");
        }
    }

    /**
     * Disposes of container manager and container instance.
     */
    public void destroy()
    {
        log("Destroying rapla");
        stopServer();
    }

    public RaplaContext getContext()
    {
        return raplaMainContainer.getContext();
    }

    static ThreadLocal<RemoteSession> currentSession = new ThreadLocal<RemoteSession>();
    static public RemoteSession getSession()
    {
    	return currentSession.get();
    }



}

