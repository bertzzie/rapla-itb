package org.rapla.server.internal;

import org.apache.avalon.framework.logger.Logger;
import org.rapla.entities.User;
import org.rapla.facade.RaplaComponent;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.server.RemoteSession;
import org.rapla.storage.StorageOperator;

/** Implementation of RemoteStorage as a RemoteService
 * @see org.rapla.server.RemoteStorage
 * @see org.rapla.components.rpc.RemoteServiceDispatcher
 *
 */
public abstract class RemoteSessionImpl extends RaplaComponent implements RemoteSession {
    /**
     *
     */
    String username;
    Logger logger;
    StorageOperator operator;
    long serverStartTime;
    
    public RemoteSessionImpl(RaplaContext context, String clientName, long serverStartTime) throws RaplaException {
        super( context );
        this.serverStartTime = serverStartTime;
        operator = (StorageOperator)context.lookup(StorageOperator.ROLE);
        logger = super.getLogger().getChildLogger(clientName);
      
    }

    public Logger getLogger() {
    	return logger;
    }

    public RaplaContext getContext() {
        return super.getContext();
    }
   
    String getUsername() {
    	return username;
    }

    public User getUser() throws RaplaException {
    	String username = getUsername();
    	if (username == null)
    		throw new IllegalStateException("No username found in session.");
    	User user = this.operator.getUser(username);
    	if (user == null)
    		throw new RaplaException("No user found for username: " + username);
    	return user;
    }

    public boolean isAuthentified() {
        return getUsername() != null && operator.isConnected();
    }

    public void setUsername( String username )
    {
        this.username = username;
    }

    public long getServerStartTime()
    {
        return serverStartTime;
    }

    public abstract void logout() throws RaplaException;



}