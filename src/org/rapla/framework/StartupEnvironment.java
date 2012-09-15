package org.rapla.framework;

import java.net.URL;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.logger.Logger;

public interface StartupEnvironment
{
	int EMBEDDED = 0;
    int CONSOLE = 1;
    int WEBSTART = 2;
    int APPLET = 3;
    int SERVLET = 4;
    int CLIENT = 5;

    Configuration getStartupConfiguration() throws RaplaException;

    URL getDownloadURL() throws RaplaException;
    URL getConfigURL() throws RaplaException; 
    URL getLoggerConfigURL() throws RaplaException; 
    
    /** either EMBEDDED, CONSOLE, WEBSTART, APPLET,SERVLET or CLIENT */
    int getStartupMode();

    URL getContextRootURL() throws RaplaException;

    Logger getBootstrapLogger();
}