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
package org.rapla.server.internal;

import java.util.Vector;

import org.apache.avalon.framework.activity.Disposable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.logger.Logger;
import org.rapla.framework.Container;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.server.ShutdownListener;
import org.rapla.server.ShutdownService;

/** Default implementation of the shutdown service. On a shutdown call
 to shutdown all registered ShutdownListeners will first be notified with
 shutdownInitated() and then wit shutdownComplete(). A shutdown-request will only be accepted from
 process running on the localhost.  <p>Sample configuration:
 <pre>
 &lt;shutdown-service id="shutdown">
 &lt;password>xxx&lt;/password>
 &lt;/shutdownservice>
 </pre>
 @see ShutdownService
 */
public class ShutdownServiceImpl implements ShutdownService, Disposable
{
    Vector<ShutdownListener> listenerList = new Vector<ShutdownListener>();
    protected Logger logger;
    RaplaContext context;

    public ShutdownServiceImpl( RaplaContext context, Configuration config ) throws RaplaException
    {
        this.context = context;
        this.logger = (Logger) context.lookup( Logger.class.getName() );
        @SuppressWarnings("unused")
		Container container = (Container) context.lookup( Container.class.getName() );
        getLogger().info( "Shutdown service started" );
    }

    public void addShutdownListener( ShutdownListener listener )
    {
        listenerList.add( listener );
    }

    public void removeShutdownListener( ShutdownListener listener )
    {
        listenerList.remove( listener );
    }

    public ShutdownListener[] getShutdownListeners()
    {
        return  listenerList.toArray( new ShutdownListener[] {} );
    }

    protected void fireShutdownInitiated()
    {
        ShutdownListener[] listeners = getShutdownListeners();
        for ( int i = 0; i < listeners.length; i++ )
        {
            try
            {
                listeners[i].shutdownInitiated();
            }
            catch ( Throwable ex )
            {
                getLogger().error( "Error calling shutdownInitiated " + listeners[i], ex );
            }
        }
    }

    protected void fireShutdownComplete( boolean restart )
    {
        ShutdownListener[] listeners = getShutdownListeners();
        for ( int i = 0; i < listeners.length; i++ )
            try
            {
                listeners[i].shutdownComplete( restart );
            }
            catch ( Throwable ex )
            {
                getLogger().error( "Error calling shutdownComplete " + listeners[i], ex );
            }
    }

    public void dispose()
    {

    }

    protected Logger getLogger()
    {
        return logger;
    }

    private void checkShutdownPermissions( String shutdownPassword ) throws RaplaException
    {
    // in process call, that means everything is ok
    }

    public synchronized void shutdown( String shutdownPassword, boolean restart ) throws RaplaException
    {
        checkShutdownPermissions( shutdownPassword );
        fireShutdownInitiated();
        fireShutdownComplete( restart );
    }

}
