/*--------------------------------------------------------------------------*
 | Copyright (C) 2006 Christopher Kohlhaas              |
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
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.avalon.framework.logger.Logger;
import org.rapla.MainServlet;
import org.rapla.components.xmlbundle.I18nBundle;
import org.rapla.entities.DependencyException;
import org.rapla.entities.RaplaType;
import org.rapla.entities.User;
import org.rapla.entities.domain.Reservation;
import org.rapla.entities.storage.EntityResolver;
import org.rapla.entities.storage.Mementable;
import org.rapla.entities.storage.RefEntity;
import org.rapla.entities.storage.internal.SimpleIdentifier;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaDefaultContext;
import org.rapla.framework.RaplaException;
import org.rapla.server.RemoteServer;
import org.rapla.server.RemoteSession;
import org.rapla.server.RemoteStorage;
import org.rapla.server.ShutdownService;
import org.rapla.storage.AuthenticationStore;
import org.rapla.storage.CachableStorageOperator;
import org.rapla.storage.IOContext;
import org.rapla.storage.IdTable;
import org.rapla.storage.LocalCache;
import org.rapla.storage.RaplaSecurityException;
import org.rapla.storage.UpdateEvent;
import org.rapla.storage.dbrm.EntityList;
import org.rapla.storage.impl.EntityStore;
import org.rapla.storage.xml.RaplaInput;
import org.rapla.storage.xml.RaplaMainReader;
import org.rapla.storage.xml.RaplaMainWriter;

/** Provides an adapter for each client-session to their shared storage operator
 * Handles security and synchronizing aspects.
 * @author ckohlhaas
 *
 */
public class RemoteStorageImpl  implements RemoteStorage {
    CachableStorageOperator operator;
    
    protected SecurityManager security;
    boolean authenticationStore;
    
    RemoteServer server;
    RaplaContext context;
    
    public RemoteStorageImpl(RaplaContext context) throws RaplaException {
        this.context = context;
        this.server = (RemoteServer) context.lookup(RemoteServer.ROLE);
        operator = (CachableStorageOperator)context.lookup(CachableStorageOperator.ROLE);
        security = (SecurityManager)context.lookup( SecurityManager.class.getName());
        authenticationStore = context.has(AuthenticationStore.ROLE);
    }
    

	public RemoteSession getSession() {
		return MainServlet.getSession();
	}
    
    public Logger getLogger()
    {
    	return getSession().getLogger();
    }
   

    private void checkAuthentified() throws RaplaSecurityException {
        RemoteSession session = getSession();
		if (!session.isAuthentified())
            throw new RaplaSecurityException("User was not authentified ");
    }

    private User getSessionUser() throws RaplaException {
        return getSession().getUser();
    }

    public EntityList getResources() throws RaplaException
    {
    	
    	checkAuthentified();
        User user = null;
        this.getLogger().debug ("A RemoteServer wants to get all resource-objects.");
        synchronized (operator.getLock()) 
        {
            EntityList resources = makeTransactionSafe(operator.getVisibleEntities(user));
            return resources;
        }
    }
    
   

    public String refresh(String time) throws RaplaException
    {
        String xml;
		try {
			xml = server.createUpdateXML(Long.valueOf( time).longValue());
		} catch (Exception e) 
		{
			throw new RaplaException(e.getMessage(),e);
		}
        return xml;
    }

//    private long getRepositoryVersion()
//    {
//        return server.getRepositoryVersion();
//    }
    
    public I18nBundle getI18n() throws RaplaException {
    	return (I18nBundle)context.lookup(I18nBundle.ROLE + "/org.rapla.RaplaResources");
    }



    public EntityList getEntityRecursive(Object id) throws RaplaException {
        checkAuthentified();
        //synchronized (operator.getLock()) 
        {
            RefEntity<?> entity = operator.resolveId(id);
            ArrayList<RefEntity<?>> completeList = new ArrayList<RefEntity<?>>();
            completeList.add( entity );
            Iterator<RefEntity<?>> it =  entity.getSubEntities();
            while (it.hasNext()) {
                completeList.add( it.next() );
            }
            EntityList list = makeTransactionSafe( completeList );
            getLogger().debug("Get entity " + entity);
            return list;
        }
    }

	public EntityList getReservations(Date start,Date end) throws RaplaException
    {
        checkAuthentified();
        User user = null;
        this.getLogger().debug ("A RemoteServer wants to reservations from ." + start + " to " + end);
        synchronized (operator.getLock()) 
        {
        	// Reservations and appointments
            ArrayList<RefEntity<?>> completeList = new ArrayList<RefEntity<?>>();
            List<Reservation> reservations = operator.getReservations(user, start, end );
            for (Reservation res:reservations)
            {
            	completeList.add( (RefEntity<?>) res);
            }
            Iterator<Reservation> it = reservations.iterator();
            while (it.hasNext()) {
                Iterator<RefEntity<?>> it2 = ((RefEntity<?>)it.next()).getSubEntities();
                while (it2.hasNext()) {
                    completeList.add( it2.next() );
                }
            }
            EntityList list = makeTransactionSafe( completeList );
            getLogger().debug("Get reservations " + start + " " + end + ": "
                               + reservations.size() + "," + list.size());
            return list;
        }
    }

   

    public void restartServer() throws RaplaException {
        checkAuthentified();
        if (!getSessionUser().isAdmin())
            throw new RaplaSecurityException("Only admins can restart the server");

        ((ShutdownService) context.lookup(ShutdownService.ROLE)).shutdown(null, true);
    }


    public long getServerTime() throws RaplaException {
        return System.currentTimeMillis();
    }

    public void dispatch(String xml) throws RaplaException
    {
        LocalCache cache = operator.getCache();
        UpdateEvent event = createUpdateEvent( context,xml, cache );
        dispatch_( event);
    }
    
    private void dispatch_(UpdateEvent evt)
        throws RaplaException {
        checkAuthentified();
        try {
            User user;
            if ( evt.getUserId() != null)
            {
                user = (User) operator.resolveId(evt.getUserId());
            }
            else
            {
                user = getSession().getUser();
            }
            synchronized (operator.getLock()) 
            {
                List<RefEntity<?>> storeObjects = evt.getStoreObjects();
				EntityResolver resolver = operator.createEntityResolver(storeObjects
                                                               ,operator.getCache());
                Iterator<RefEntity<?>> it = storeObjects.iterator();
                while (it.hasNext()) {
                	RefEntity<?> entity =  it.next();
                    if (getLogger().isDebugEnabled())
                        getLogger().debug("Contextualizing " + entity);
                    entity.resolveEntities( resolver);
                }

                List<RefEntity<?>> removeObjects = evt.getRemoveObjects();
				it = removeObjects.iterator();
                while (it.hasNext()) {
                	RefEntity<?> entity =  it.next();
                    entity.resolveEntities( resolver);
                }

                it = storeObjects.iterator();
                while (it.hasNext()) {
                	RefEntity<?> entity = it.next();
                    security.checkWritePermissions(user,entity);
                }

                it = removeObjects.iterator();
                while (it.hasNext()) {
                	RefEntity<?> entity = it.next();
                    security.checkWritePermissions(user,entity);
                }

                if (this.getLogger().isDebugEnabled())
                    this.getLogger().debug("Dispatching changes to " + operator.getClass());

                operator.dispatch(evt);
                if (this.getLogger().isDebugEnabled())
                    this.getLogger().debug("Changes dispatched returning result.");
            }
        } catch (DependencyException ex) {
            throw ex;
        } catch (RaplaException ex) {
            this.getLogger().error(ex.getMessage(),ex);
            throw ex;
        } catch (Exception ex) {
            this.getLogger().error(ex.getMessage(),ex);
            throw new RaplaException(ex);
        } catch (Error ex) {
            this.getLogger().error(ex.getMessage(),ex);
            throw ex;
        }
    }

    public String createIdentifier(RaplaType raplaType) throws RaplaException {
        checkAuthentified();
        synchronized (operator.getLock()) 
        {
            //User user =
            getSessionUser(); //check if authenified
            SimpleIdentifier simpleIdentifier = (SimpleIdentifier)operator.createIdentifier(raplaType);
			String string = simpleIdentifier.toString();
			return string;
        }
    }

    public void authenticate(String username,
                                    String password) throws RaplaException
    {
        synchronized (operator.getLock()) 
        {
            getSessionUser(); //check if authenified
            server.authenticate( username, password );
        }
    }

    public boolean canChangePassword() throws RaplaException {
        checkAuthentified();
        synchronized (operator.getLock()) 
        {
            return !authenticationStore  && operator.canChangePassword();
       }
    }

    public void changePassword(String username
                               ,String oldPassword
                               ,String newPassword
                               ) throws RaplaException
    {
        checkAuthentified();
        if ( authenticationStore ) {
            throw new RaplaException("Rapla can't change your password. "
                                     + "Authentication is done via plugin." );
        }
        synchronized (operator.getLock()) {
            User sessionUser = getSessionUser();
            if (!sessionUser.isAdmin()) {
                operator.authenticate(username,new String(oldPassword));
            }
            @SuppressWarnings("unchecked")
			RefEntity<User> user = (RefEntity<User>)operator.getUser(username);
            operator.changePassword(user,oldPassword.toCharArray(),newPassword.toCharArray());
        }
    }


    static public UpdateEvent createUpdateEvent( RaplaContext context,String xml, LocalCache cache ) throws RaplaException
    {
        EntityStore store = new EntityStore( cache, cache.getSuperCategory());
        RaplaContext inputContext = new IOContext().createInputContext(context,store,new IdTable());
        Logger logger = (Logger)context.lookup( Logger.class.getName());
        RaplaInput xmlAdapter = new RaplaInput( logger.getChildLogger("reading"));
        RaplaMainReader contentHandler = new RaplaMainReader( inputContext);
        try
        {
            xmlAdapter.read(new StringReader( xml), contentHandler, false);
        }
        catch (IOException e)
        {
            throw new RaplaException(e);
        }
        UpdateEvent event = new UpdateEvent();
        event.setRepositoryVersion(store.getRepositoryVersion());
        for (Iterator<RefEntity<?>> it = store.getList().iterator();it.hasNext();)
        {
            RefEntity<?> object = it.next();
            event.putStore( object);
        }
        for (Iterator<?> it = store.getRemoveIds().iterator();it.hasNext();)
        {
            Object id = (Object)it.next();
            RefEntity<?> entity = (RefEntity<?>)cache.get( id );
            if ( entity != null)
            {
                event.putRemove( entity);
            }
        }
        return event;
    }

    static public String createUpdateEvent( RaplaContext context, LocalCache cache,UpdateEvent evt) throws RaplaException, IOException
    {
        RaplaDefaultContext ioContext = new IOContext().createOutputContext( context, cache, true, true);
        StringWriter stringWriter = new StringWriter( );
        RaplaMainWriter writer = new RaplaMainWriter(ioContext);
        BufferedWriter buf = new BufferedWriter(stringWriter); 
        writer.setWriter( buf);
        writer.printList( evt.getStoreObjects(),evt.getRemoveObjects(), evt.getRepositoryVersion());
        buf.flush();
        String xml = stringWriter.toString();
        return xml;
    }

    @SuppressWarnings("unchecked")
	private static EntityList makeTransactionSafe(List<? extends RefEntity<?>> objectList) {
        EntityList saveList = new EntityList(objectList);
        Iterator<? extends RefEntity<?>> it = objectList.iterator();
        while (it.hasNext()) {
            saveList.add((((Mementable<RefEntity<?>>)it.next()).clone()));
        }
        return saveList;
    }

}

