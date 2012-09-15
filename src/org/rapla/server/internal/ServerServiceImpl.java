/*--------------------------------------------------------------------------*
 | Copyright (C) 2006 Praktikum Gruppe2?, Christopher Kohlhaas              |
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

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.DefaultConfiguration;
import org.rapla.MainServlet;
import org.rapla.components.util.DateTools;
import org.rapla.components.xmlbundle.I18nBundle;
import org.rapla.entities.RaplaObject;
import org.rapla.entities.User;
import org.rapla.entities.configuration.Preferences;
import org.rapla.entities.configuration.RaplaConfiguration;
import org.rapla.entities.domain.Permission;
import org.rapla.entities.internal.UserImpl;
import org.rapla.entities.storage.RefEntity;
import org.rapla.facade.ClientFacade;
import org.rapla.facade.UpdateModule;
import org.rapla.facade.internal.FacadeImpl;
import org.rapla.framework.PluginDescriptor;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaContextException;
import org.rapla.framework.RaplaDefaultContext;
import org.rapla.framework.RaplaException;
import org.rapla.framework.internal.ContainerImpl;
import org.rapla.plugin.RaplaExtensionPoints;
import org.rapla.server.RemoteServer;
import org.rapla.server.RemoteSession;
import org.rapla.server.RemoteStorage;
import org.rapla.server.ServerService;
import org.rapla.servletpages.DefaultHTMLMenuEntry;
import org.rapla.servletpages.DefaultHTMLMenuExtensionPoint;
import org.rapla.servletpages.RaplaAppletPageGenerator;
import org.rapla.servletpages.RaplaIndexPageGenerator;
import org.rapla.servletpages.RaplaJNLPPageGenerator;
import org.rapla.servletpages.RaplaResourcePageGenerator;
import org.rapla.servletpages.RaplaStatusPageGenerator;
import org.rapla.storage.AuthenticationStore;
import org.rapla.storage.CachableStorageOperator;
import org.rapla.storage.IOContext;
import org.rapla.storage.RaplaSecurityException;
import org.rapla.storage.StorageOperator;
import org.rapla.storage.StorageUpdateListener;
import org.rapla.storage.UpdateEvent;
import org.rapla.storage.UpdateResult;
import org.rapla.storage.dbrm.EntityList;
import org.rapla.storage.dbrm.RaplaStorePage;
import org.rapla.storage.dbrm.RemoteOperator;
import org.rapla.storage.xml.RaplaMainWriter;

/** Default implementation of StorageService.
 * <p>Sample configuration 1:
 <pre>
 &lt;storage id="storage" >
 &lt;store>file&lt;/store>
 &lt;/storage>
 </pre>
 * The store value contains the id of a storage-component.
 * Storage-Components are all components that implement the
 * <code>CachableStorageOperator<code> interface.
 * </p>
 * <p>Sample configuration 2:
 <pre>
 &lt;storage id="storage">
 &lt;store>remote&lt;/store>
 &lt;login>
 &lt;username>homer&lt;/username>
 &lt;password>duffs&lt;/password>
 &lt;/login>
 &lt;/storage>
 </pre>
 * This is configuration where the servers are cascaded. The remote-server
 * normally needs an authentication. The provided account should have
 * admin privileges.
 * </p>

 @see ServerService
 */

public class ServerServiceImpl extends ContainerImpl implements StorageUpdateListener, ServerService, RemoteServer
{
    protected CachableStorageOperator operator;
    protected I18nBundle i18n;
    protected AuthenticationStore authenticationStore;
    private Configuration operatorConfig;
    List<PluginDescriptor> pluginList;

    ClientFacade facade;

    private Map<RefEntity<?>,Long> updateMap = new HashMap<RefEntity<?>,Long>();
    private Map<RefEntity<?>,Long> removeMap = new HashMap<RefEntity<?>,Long>();

    long repositoryVersion = 0;
    long cleanupPointVersion = 0;
        
    public ServerServiceImpl( RaplaContext parentContext, Configuration config ) throws RaplaException
    {
        super( parentContext, config );
        pluginList = (List<PluginDescriptor>) parentContext.lookup( PluginDescriptor.PLUGIN_LIST );
        i18n = (I18nBundle) parentContext.lookup( I18nBundle.ROLE + "/org.rapla.RaplaResources" );
        Configuration login = config.getChild( "login" );
        String username = login.getChild( "username" ).getValue( null );
        String password = login.getChild( "password" ).getValue( null );
        operatorConfig = config.getChild( "store" );

        try
        {
            operator = (CachableStorageOperator) getContext().lookup(
                                                                      CachableStorageOperator.ROLE
                                                                              + "/"
                                                                              + operatorConfig.getValue( "*" ) );
            addContainerProvidedComponentInstance( CachableStorageOperator.ROLE, operator );
            addContainerProvidedComponentInstance( StorageOperator.ROLE, operator );
            facade = new FacadeImpl( getContext(), new DefaultConfiguration( "facade" ), getLogger() );
            addContainerProvidedComponentInstance( ClientFacade.ROLE, facade );
            addContainerProvidedComponent( SecurityManager.class.getName() );
            addContainerProvidedComponent( RemoteStorage.class.getName(), RemoteStorageImpl.class.getName(),null);
            
            // adds 5 basic pages to the webapplication
            addContainerProvidedComponent( RaplaExtensionPoints.SERVLET_PAGE_EXTENSION, RaplaStatusPageGenerator.class.getName(), "server", null);
            addContainerProvidedComponent( RaplaExtensionPoints.SERVLET_PAGE_EXTENSION, RaplaIndexPageGenerator.class.getName(), "index", null);
            addContainerProvidedComponent( RaplaExtensionPoints.SERVLET_PAGE_EXTENSION, RaplaJNLPPageGenerator.class.getName(), "raplaclient", null);
            addContainerProvidedComponent( RaplaExtensionPoints.SERVLET_PAGE_EXTENSION, RaplaAppletPageGenerator.class.getName(), "raplaapplet", null);
            addContainerProvidedComponent( RaplaExtensionPoints.SERVLET_PAGE_EXTENSION, RaplaResourcePageGenerator.class.getName(), "resource", null);
            addContainerProvidedComponent( RaplaExtensionPoints.SERVLET_PAGE_EXTENSION, RaplaStorePage.class.getName(), "store", null);
    
            addContainerProvidedComponentInstance( RemoteServer.ROLE, this);
            
            // Index page menu 
            DefaultHTMLMenuExtensionPoint indexMenu = new DefaultHTMLMenuExtensionPoint();
            addContainerProvidedComponentInstance( RaplaExtensionPoints.HTML_MAIN_MENU_EXTENSION_POINT, indexMenu);
            I18nBundle i18n = (I18nBundle)getContext().lookup(I18nBundle.ROLE +  "/org.rapla.RaplaResources");

            indexMenu.insert( new DefaultHTMLMenuEntry(i18n.getString( "start_rapla_with_webstart" ),"rapla?page=raplaclient") );
            indexMenu.insert( new DefaultHTMLMenuEntry(i18n.getString( "start_rapla_with_applet" ),"rapla?page=raplaapplet") );
            indexMenu.insert( new DefaultHTMLMenuEntry(i18n.getString( "server_status" ),"rapla?page=server") );
        }
        catch ( RaplaContextException ex )
        {
            throw new RaplaContextException( CachableStorageOperator.ROLE, "Store at "
                    + operatorConfig.getLocation()
                    + " is not found (or could not be initialized) ", ex );
        }

        operator.addStorageUpdateListener( this );
        if ( username != null && password != null )
            operator.connect( username, password.toCharArray() );
        else
            operator.connect();

        initializePlugins( pluginList, operator.getPreferences( null ) );

        if ( getContext().has( AuthenticationStore.ROLE ) )
        {
            try 
       	    {
               	authenticationStore = (AuthenticationStore) getContext().lookup( AuthenticationStore.ROLE );
                getLogger().info( "Using AuthenticationStore " + authenticationStore.getName() );
            } 
            catch ( RaplaException ex)
            {
                getLogger().error( "Can't initialize configured authentication store. Using default authentication." , ex);
            }
        }
        initEventCleanup();
    }

    /**
     * @see org.rapla.server.ServerService#getFacade()
     */
    public ClientFacade getFacade()
    {
        return facade;
    }

    protected void initializePlugins( List<PluginDescriptor> pluginList, Preferences preferences ) throws RaplaException
    {

        RaplaConfiguration raplaConfig = (RaplaConfiguration) preferences.getEntry( "org.rapla.plugin" );
        // Add plugin configs
        for ( Iterator<PluginDescriptor> it = pluginList.iterator(); it.hasNext(); )
        {
            PluginDescriptor pluginDescriptor = it.next();
            String pluginClassname = pluginDescriptor.getClass().getName();
            Configuration pluginConfig = null;
            if ( raplaConfig != null )
            {
                pluginConfig = raplaConfig.find( "class", pluginClassname );
            }
            if ( pluginConfig == null )
            {
                pluginConfig = new DefaultConfiguration( "plugin" );
            }
            pluginDescriptor.provideServices( this, pluginConfig );
        }

        Collection<?> clientPlugins = getAllServicesFor( RaplaExtensionPoints.SERVER_EXTENSION );
        // start plugins
        for ( Iterator<?> it = clientPlugins.iterator(); it.hasNext(); )
        {
            Object hint = it.next();
            try
            {
                getContext().lookup( RaplaExtensionPoints.SERVER_EXTENSION + "/" + hint.toString() );
                getLogger().info( "Initialize " + hint );
            }
            catch ( RaplaContextException ex )
            {
                getLogger().error( "Can't initialize " + hint, ex );
            }
        }
    }

    public void start() throws Exception
    {
        getLogger().info( "Storage service started" );
    }

    public void stop() throws Exception
    {
        operator.removeStorageUpdateListener( this );
        try
        {
            operator.disconnect();
        }
        finally
        {
        }
        getLogger().info( "Storage service stopped" );
    }

    public void dispose()
    {
        super.dispose();
    }

    
    public Method findMethod( String role,String methodName,Map<String,String> args) throws ClassNotFoundException
    {
    	Class<?> inter = Class.forName( role);
        Method[] methods = inter.getMethods();
    	for ( Method method: methods)
    	{
    		Class<?>[] parameterTypes = method.getParameterTypes();
			if ( method.getName().equals( methodName) && parameterTypes.length == args.size())
    		{
				return method;
    		}
    	}
    	return null;
    }

    public byte[] dispatch( RemoteSession session, String methodName, Map<String,String> args ) throws Exception
    {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            int indexRole = methodName.indexOf( "/" );
            String interfaceName = RemoteStorage.class.getName();
            if ( indexRole > 0 )
            {
                interfaceName = methodName.substring( 0, indexRole );
                methodName = methodName.substring( indexRole + 1 );

            }
            try
            {
            	final Object serviceUncasted;
            	RaplaContext context = getContext();
				serviceUncasted = context.lookup( interfaceName);
            	//if ( serviceUncasted instanceof RemoteService)
                {
                    
                	Method method = findMethod( interfaceName, methodName, args);
                	if ( method == null)
                	{
                		throw new RaplaException("Can't find method with name " + methodName);
                	}
            		Class<?>[] parameterTypes = method.getParameterTypes();
					if ( method.getName().equals( methodName) && parameterTypes.length == args.size())
            		{

            			Object[] convertedArgs = new Object[ args.size()];
            			int i=0;
            			for (Object arg: args.values())
            			{
            				Class<?> type = parameterTypes[i];
            				convertedArgs[i++] = RemoteOperator.convertFromString(type,(String)arg);
            			}
            			Class<?> returnType = method.getReturnType();
            			Object result = null;
            			try
            			{
            				result = method.invoke( serviceUncasted, convertedArgs);
            			}
            			catch (InvocationTargetException ex)
            			{
            				Throwable cause = ex.getCause();
							if (cause instanceof RaplaException)
            				{
            					throw (RaplaException)cause;
            				}
            				else
            				{
            					throw new RaplaException( cause.getMessage(), cause );
            				} 
            					
						}
            			
            			if ( result != null)
            			{
            				BufferedWriter outWriter = new BufferedWriter( new OutputStreamWriter( out,"utf-8"));
                        	if ( returnType.equals( EntityList.class))
                        	{
                        		RaplaDefaultContext ioContext = new IOContext().createOutputContext( context, operator.getCache(), true, true);
                            	RaplaMainWriter writer = new RaplaMainWriter(ioContext);
                            	writer.setWriter( outWriter);
                        		EntityList resources = (EntityList) result;
								List<RaplaObject> removeList = Collections.emptyList();
								writer.printList( resources, removeList, repositoryVersion );
                        	}
                        	else
                        	{
                        		String resultString = RemoteOperator.convertToString(result);
                        		outWriter.write( resultString );
                        	}
                        	outWriter.flush();
                        	out.flush();
            			}
            		}
            	}
            }
            catch ( RaplaException ex )
            {
                getLogger().error( ex.getMessage(), ex );
                throw ex;
            }
            out.close();
            return out.toByteArray();
        
    }


	public StorageOperator getOperator()
    {
        return operator;
    }

    static UpdateEvent createTransactionSafeUpdateEvent( UpdateResult updateResult )
    {
        User user = updateResult.getUser();
        UpdateEvent saveEvent = new UpdateEvent();
        if ( user != null )
        {
            saveEvent.setUserId( ( (RefEntity<?>) updateResult.getUser() ).getId() );
        }
        {
	        Iterator<UpdateResult.Add> it = updateResult.getOperations( UpdateResult.Add.class );
	        while ( it.hasNext() )
	        {
	            saveEvent.putStore( (RefEntity<?>) (  it.next() ).getNew() );
	        }
        }
        {
	        Iterator<UpdateResult.Change> it = updateResult.getOperations( UpdateResult.Change.class );
	        while ( it.hasNext() )
	        {
	            saveEvent.putStore( (RefEntity<?>) ( it.next() ).getNew() );
	        }
        }
        {
        	Iterator<UpdateResult.Remove> it = updateResult.getOperations( UpdateResult.Remove.class );
	        while ( it.hasNext() )
	        {
	            saveEvent.putRemove( (RefEntity<?>) (it.next() ).getCurrent() );
	        }
        }
        return saveEvent;
    }

    // Implementation of StorageUpdateListener
    synchronized public void objectsUpdated( UpdateResult evt )
    {
        // notify the client for changes
        repositoryVersion++;
        UpdateEvent safeResultEvent = createTransactionSafeUpdateEvent( evt );
        if ( getLogger().isDebugEnabled() )
            getLogger().debug( "Storage was modified. Calling notify." );
        for ( Iterator<RefEntity<?>> it = safeResultEvent.getStoreObjects().iterator(); it.hasNext(); )
        {
        	RefEntity<?> obj = it.next();
            updateMap.remove( obj );
            updateMap.put( obj, new Long( repositoryVersion ) );
        }
        for ( Iterator<RefEntity<?>> it = safeResultEvent.getRemoveObjects().iterator(); it.hasNext(); )
        {
        	RefEntity<?> obj =  it.next();
            updateMap.remove( obj );
            removeMap.remove( obj );
            removeMap.put( obj, new Long( repositoryVersion ) );
        }
    }

    /** regulary removes all old update messages that are older than the updateInterval ( factor 10) and at least 1 hour old */
    private final void initEventCleanup()
    {
        TimerTask cleanupTask = new TimerTask()
        {
            public void run()
            {
                initEventCleanup();
            }
        };
        synchronized ( operator.getLock() )
        {
            Timer timer = new Timer( true ); // Start timer as daemon-thread
            int delay = 10000;
            
            {
                RefEntity<?>[] keys = updateMap.keySet().toArray(new RefEntity[] {});
                for ( int i=0;i<keys.length;i++)
                {
                    RefEntity<?> key = keys[i];
                    Long lastVersion =  updateMap.get( key );
                    if ( lastVersion.longValue() <= cleanupPointVersion )
                    {
                        updateMap.remove( key );
                    }
                }
            }
            {
                RefEntity<?>[] keys = removeMap.keySet().toArray(new RefEntity[] {});
                for ( int i=0;i<keys.length;i++)
                {
                    RefEntity<?> key = keys[i];
                    Long lastVersion = (Long) removeMap.get( key );
                    if ( lastVersion.longValue() <= cleanupPointVersion )
                    {
                        removeMap.remove( key );
                    }
                }
            }
            cleanupPointVersion = repositoryVersion;

            if ( operator.isConnected() )
            {
                try
                {
                    delay = operator.getPreferences( null ).getEntryAsInteger( UpdateModule.REFRESH_INTERVAL_ENTRY,
                                                                               delay );
                }
                catch ( RaplaException e )
                {
                    getLogger().error( "Error during cleanup.", e );
                }
            }
            long scheduleDelay = Math.max( DateTools.MILLISECONDS_PER_HOUR, delay * 10 );
            //scheduleDelay = 30000;
            timer.schedule( cleanupTask,  scheduleDelay);
        }
    }

    synchronized public String createUpdateXML( long clientRepositoryVersion ) throws RaplaException, IOException
    {
        long currentVersion = this.repositoryVersion;
        if ( clientRepositoryVersion < currentVersion )
        {
            UpdateEvent safeResultEvent = new UpdateEvent();
            safeResultEvent.setRepositoryVersion( currentVersion );
            for ( Iterator<RefEntity<?>> it = updateMap.keySet().iterator(); it.hasNext(); )
            {
                RefEntity<?> key = it.next();
                Long lastVersion = updateMap.get( key );
                if ( lastVersion.longValue() > clientRepositoryVersion )
                {
                    safeResultEvent.putStore( key );
                }
            }
            for ( Iterator<RefEntity<?>> it = removeMap.keySet().iterator(); it.hasNext(); )
            {
                RefEntity<?> key =  it.next();
                Long lastVersion = removeMap.get( key );
                if ( lastVersion.longValue() > clientRepositoryVersion )
                {
                    safeResultEvent.putRemove( key );
                }
            }
            String xml = RemoteStorageImpl.createUpdateEvent( getContext(), operator.getCache(), safeResultEvent );
            return xml;
        }
        // Empty String if nothing is expected
        return "<uptodate/>";
    }

    public void updateError( RaplaException ex )
    {
        if ( getLogger() != null )
            getLogger().error( ex.getMessage(), ex );
        try
        {
            stop();
            //      messagingServer.disconnect();
        }
        catch ( Exception e )
        {
            if ( getLogger() != null )
                getLogger().error( e.getMessage() );
        }
    }

   
    
    public void storageDisconnected()
    {
        try
        {
            stop();
        }
        catch ( Exception e )
        {
            if ( getLogger() != null )
                getLogger().error( e.getMessage() );
        }
    }

    public void checkServerVersion( String clientVersion ) throws RaplaException
    {
    	if ( clientVersion.equals("@doc.version@"))
    	{
    		return;
    	}
    	// No check on server until correct versioning schema released.
    	
//        String serverVersion = i18n.getString( "rapla.version" );
//        if ( !serverVersion.equals( clientVersion ) )
//        {
//            throw new RaplaException( "Incompatible client/server versions. Please change your client to version "
//                    + serverVersion
//                    + ". If you are using java-webstart a simple reload and restart could do that!" );
//        }
    }

    public void login( String username, String password ) throws RaplaException
    {
        this.getLogger().debug( "User '" + username + "' is requesting login " );
        if ( authenticationStore != null && authenticationStore.authenticate( username, password ))
        {
            @SuppressWarnings("unchecked")
			RefEntity<User> user = (RefEntity<User>)this.operator.getUser( username );
            if ( user == null )
            {
                user = new UserImpl();
                user.setId( this.operator.createIdentifier( User.TYPE ) );
            }
            else
            {
                user = this.operator.editObject( user, null );
            }
            
            boolean initUser ;
            try
            {
                initUser = authenticationStore.initUser( user.cast(), username, password,
                                               this.operator.getSuperCategory()
                                                            .getCategory( Permission.GROUP_CATEGORY_KEY ) );
            } catch (RaplaSecurityException ex){
                throw new RaplaSecurityException(i18n.getString("error.login"));
            }
            if ( initUser )
            {
            	List<RefEntity<?>> storeList = new ArrayList<RefEntity<?>>(1);
            	storeList.add( user);
            	List<RefEntity<?>> removeList = Collections.emptyList();
                
            	this.operator.storeAndRemove( storeList, removeList, null );
            }
        }
        else
        {
            this.operator.authenticate( username, password );
        }
    
    }

    /** @Override
    */
	public void logout() throws RaplaException {
		
		RemoteSession session = MainServlet.getSession();
		if ( session != null)
		{
			if ( session.isAuthentified())
			{
				User user = session.getUser();
				if ( user != null)
				{
					this.getLogger().info( "Request Logout " + user.getUsername());
				}
				session.logout();
			}
		}
	}

    
    public void authenticate( String username, String password ) throws RaplaException
    {
        synchronized ( this.operator.getLock() )
        {
            if ( authenticationStore != null && authenticationStore.authenticate( username, password ) )
            {
                // do nothing
            } // if the authenticationStore cant authentify the user is checked against the local database
            else
            {
                this.operator.authenticate( username, password );
            }
        }
    }

	public long getRepositoryVersion() 
	{
		return repositoryVersion;
	}


}
