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
package org.rapla.storage.dbsql;

import java.sql.Connection;
import java.sql.SQLException;

import org.rapla.entities.storage.RefEntity;
import org.rapla.framework.RaplaException;

interface Storage {
    void loadAll() throws SQLException,RaplaException;
    void deleteAll() throws SQLException;
    void setConnection(Connection con);
    void save( RefEntity<?> entity) throws SQLException,RaplaException ;
    void insert( RefEntity<?> entity) throws SQLException,RaplaException ;
    void update( RefEntity<?> entity) throws SQLException,RaplaException ;
    /**
     * @param entity
     */
    void delete(RefEntity<?> entity) throws SQLException,RaplaException ;
}




