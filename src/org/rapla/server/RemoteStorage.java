/*--------------------------------------------------------------------------*
 | Copyright (C) 2006 ?, Christopher Kohlhaas                               |
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
package org.rapla.server;

import java.util.Date;

import org.rapla.entities.RaplaType;
import org.rapla.framework.RaplaException;
import org.rapla.storage.dbrm.EntityList;

public interface RemoteStorage  {
  //  RemoteMethod GET_RESOURCES = new RemoteMethod("getResources");
//    RemoteMethod GET_RESERVATIONS= new RemoteMethod("getReservations", new String[] {"start","end"});
    //RemoteMethod GET_ENTITY_RECURSIVE= new RemoteMethod("getEntityRecursive", new String[] {"id"});
    //RemoteMethod DISPATCH= new RemoteMethod("dispatch", new String[] {"evt"});
    //RemoteMethod CREATE_IDENTIFIER= new RemoteMethod("createIdentifier", new String[] {"raplaType"});
    
    // These Methods belong to the server
    //RemoteMethod RESTART_SERVER = new RemoteMethod("restartServer",new String[] { });
    //RemoteMethod REFRESH = new RemoteMethod("refresh",new String[] {"clientRepositoryVersion" });
    //RemoteMethod LOGIN = new RemoteMethod("login",new String[] { "username", "password"});
    //RemoteMethod CHECK_SERVER_VERSION = new RemoteMethod("checkServerVersion",new String[] {"clientVersion" });

    
    void authenticate(String username,String password) throws RaplaException;
    boolean canChangePassword() throws RaplaException;
    void changePassword(String username,String oldPassword,String newPassword) throws RaplaException;
    EntityList getResources() throws RaplaException;
    /** returns the time on the server */
    long getServerTime() throws RaplaException;
    /** delegates the corresponding method in the StorageOperator. */
    EntityList getReservations(Date start,Date end) throws RaplaException;

    EntityList getEntityRecursive(Object id) throws RaplaException;

    String refresh(String clientRepoVersion) throws RaplaException;
    
    void restartServer() throws RaplaException;
    void dispatch(String xml) throws RaplaException;
    String createIdentifier(RaplaType raplaType) throws RaplaException;
}
