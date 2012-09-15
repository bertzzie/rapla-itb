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

import java.io.IOException;

import org.rapla.framework.RaplaException;

public interface RemoteServer  {
    String ROLE = RemoteServer.class.getName();    
    void login(String username,String password) throws RaplaException;
    void checkServerVersion(String clientVersion) throws RaplaException;
    long getRepositoryVersion();
	void authenticate(String username, String password) throws RaplaException;
	String createUpdateXML(long longValue) throws RaplaException, IOException;
	void logout() throws RaplaException;
}
