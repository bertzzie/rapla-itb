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

import java.net.MalformedURLException;
import java.net.URL;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.logger.ConsoleLogger;
import org.apache.avalon.framework.logger.Logger;
import org.rapla.components.util.IOUtil;
import org.rapla.components.util.JNLPUtil;
import org.rapla.framework.RaplaException;
import org.rapla.framework.StartupEnvironment;
import org.rapla.framework.internal.ConfigTools;

final public class RaplaStartupEnvironment implements StartupEnvironment
{
    private int startupMode = EMBEDDED;
    //private LoadingProgress progressbar;
    private Logger bootstrapLogger = new ConsoleLogger( ConsoleLogger.LEVEL_WARN );
    private URL configURL;
    private URL loggerConfigURL;
    private URL contextURL;
    private URL downloadURL;

    public Configuration getStartupConfiguration() throws RaplaException
    {
        return ConfigTools.createConfig( getConfigURL().toExternalForm() );
    }

    public URL getConfigURL() throws RaplaException
    {
        if ( configURL != null )
        {
            return configURL;
        }
        else
        {
            return ConfigTools.configFileToURL( null, "rapla.xconf" );
        }
    }

    public URL getLoggerConfigURL() throws RaplaException
    {
        if ( loggerConfigURL != null )
        {
            return loggerConfigURL;
        }
        else
        {
            try
            {
                URL loggerConfig = new URL( getConfigURL(), "raplaclient.xlog" );
                return loggerConfig;
            }
            catch ( MalformedURLException ex )
            {
                return null;
            }
        }
    }

    public Configuration getLoggerConfig() throws RaplaException
    {
        return ConfigTools.createConfig( getLoggerConfigURL().toExternalForm() );
    }

    public Logger getBootstrapLogger()
    {
        return bootstrapLogger;
    }

    public void setStartupMode( int startupMode )
    {
        this.startupMode = startupMode;
    }

    /* (non-Javadoc)
     * @see org.rapla.framework.IStartupEnvironment#getStartupMode()
     */
    public int getStartupMode()
    {
        return startupMode;
    }

    public void setBootstrapLogger( Logger logger )
    {
        bootstrapLogger = logger;
    }

    public void setLogConfigURL( URL logConfigURL )
    {
        this.loggerConfigURL = logConfigURL;
    }

    public void setConfigURL( URL configURL )
    {
        this.configURL = configURL;
    }

    public URL getContextRootURL() throws RaplaException
    {
        if ( contextURL != null )
            return contextURL;
        return IOUtil.getBase( getConfigURL() );
    }

    public URL getDownloadURL() throws RaplaException
    {
        if ( downloadURL != null )
        {
            return downloadURL;
        }
        if ( startupMode == APPLET )
        {
            return IOUtil.getBase( getConfigURL() );
        }
        if ( startupMode == WEBSTART )
        {
            try
            {
                return JNLPUtil.getCodeBase();
            }
            catch ( Exception e )
            {
                throw new RaplaException( e );
            }
        }
        else
        {
            try
            {
                return new URL( "http://localhost:8051" );
            }
            catch ( MalformedURLException e )
            {
                throw new RaplaException( "Invalid URL" );
            }
        }
    }

    public void setDownloadURL( URL downloadURL )
    {
        this.downloadURL = downloadURL;
    }

}
