package org.rapla;

import java.io.File;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;

import junit.framework.TestCase;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.webapp.WebAppContext;
import org.rapla.components.util.IOUtil;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.framework.RaplaLocale;

public abstract class ServletTestBase extends TestCase
{
	MainServlet mainServlet;
    Server jettyServer;
    final public static String WEBAPP_FOLDER_NAME = RaplaTestCase.TEST_FOLDER_NAME  + "/webapp";
    final public static String WEBAPP_INF_FOLDER_NAME = WEBAPP_FOLDER_NAME + "/WEB-INF";
  
    public ServletTestBase( String name )
    {
        super( name );
        new File("temp").mkdir();
        File testFolder =new File(RaplaTestCase.TEST_FOLDER_NAME);
        testFolder.mkdir();
    }

    protected void setUp() throws Exception
    {
        super.setUp();
        new File(WEBAPP_FOLDER_NAME).mkdir();
        new File(WEBAPP_INF_FOLDER_NAME).mkdir();
        
        IOUtil.copy( "test-src/test.xconf", WEBAPP_INF_FOLDER_NAME + "/raplaserver.xconf" );
        IOUtil.copy( "test-src/test.xlog", WEBAPP_INF_FOLDER_NAME + "/raplaserver.xlog" );
        IOUtil.copy( "test-src/testdefault.xml", WEBAPP_INF_FOLDER_NAME + "/test.xml" );
        IOUtil.copy( "webapp/WEB-INF/web.xml", WEBAPP_INF_FOLDER_NAME + "/web.xml" );
        
        
        jettyServer =new Server(8051);
        WebAppContext context = new WebAppContext( jettyServer,"rapla","/" );
        context.setResourceBase( WEBAPP_FOLDER_NAME );
        context.setMaxFormContentSize(64000000);
        
      //  context.addServlet( new ServletHolder(mainServlet), "/*" );
        jettyServer.start();
        Handler[] childHandlers = context.getChildHandlersByClass(ServletHandler.class);
        ServletHolder servlet = ((ServletHandler)childHandlers[0]).getServlet("RaplaServer");
        mainServlet = (MainServlet) servlet.getServlet();
        URL server = new URL("http://127.0.0.1:8051/rapla/ping");
        HttpURLConnection connection = (HttpURLConnection)server.openConnection();
        int timeout = 10000;
        int interval = 200;
        for ( int i=0;i<timeout / interval;i++)
        {
            try
            {
                connection.connect();
            } 
            catch (ConnectException ex) {
                Thread.sleep(interval);
            }
        }
    }
    
    protected RaplaContext getContext()
    {
        return mainServlet.getContext();
    }

    /** lookup the service in the serviceManager under the specified key:
	    serviceManager.lookup(role).
	    @throws IllegalStateException if GUIComponent wasn't serviced. No service method called
	    @throws UnsupportedOperationException if service not available.
	 */
	@SuppressWarnings("unchecked")
	protected <T> T getService(Class<T> role) throws RaplaException {
	    return (T) getService( role.getName());
	}

	protected Object getService(String role) throws RaplaException {
        return  getContext().lookup(role.toString());
	}
	
    protected RaplaLocale getRaplaLocale() throws Exception {
        return (RaplaLocale) getContext().lookup(RaplaLocale.ROLE);
    }

    
    protected void tearDown() throws Exception
    {
        jettyServer.stop();
        super.tearDown();
    }
    
    protected String getStorageName() {
        return "storage-file";
    }
 }
