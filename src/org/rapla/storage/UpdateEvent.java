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
package org.rapla.storage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.rapla.entities.storage.RefEntity;

public class UpdateEvent implements java.io.Serializable,Cloneable
{
    private static final long serialVersionUID = 1L;
    
    HashMap<Object,RefEntity<?>> removeSet = new HashMap<Object,RefEntity<?>>();
    HashMap<Object,RefEntity<?>> storeSet = new HashMap<Object,RefEntity<?>>();
    
    ArrayList<RefEntity<?>> removeObjects = new ArrayList<RefEntity<?>>();
    ArrayList<RefEntity<?>> storeObjects = new ArrayList<RefEntity<?>>();
    Object userId;
    long repositoryVersion;
    
    public UpdateEvent() {
    }
    
    public void setUserId( Object userId) {
        this.userId = userId;
    }
    public Object getUserId() {
        return userId;
    }
    
    private void addRemove(RefEntity<?> entity) {
        removeObjects.add(entity);
        removeSet.put( entity.getId(),entity);
    }
    private void addStore(RefEntity<?> entity) {
        storeObjects.add(entity);
        storeSet.put( entity.getId(), entity);
    }

    public List<RefEntity<?>> getRemoveObjects() {
        return removeObjects;
    }

    public List<RefEntity<?>> getStoreObjects() {
        return storeObjects;
    }

    /** use this method if you want to avoid adding the same Entity twice.*/
    public void putStore(RefEntity<?> entity) {
       
        if (storeSet.get(entity.getId()) == null)
            addStore(entity);
    }

    /** use this method if you want to avoid adding the same Entity twice.*/
    public void putRemove(RefEntity<?> entity) {
        if (removeSet.get(entity.getId()) == null)
            addRemove(entity);
    }

    /** find an entity in the update-event that matches the passed original. Returns null
     * if no such entity is found. */
    public RefEntity<?> findEntity(RefEntity<?> original) {
        RefEntity<?> entity =  storeSet.get( original.getId());
        if ( entity != null)
            return entity;
        entity =  removeSet.get( original.getId());
        if ( entity != null)
            return entity;
        return null;
    }

    @SuppressWarnings("unchecked")
	public UpdateEvent clone() {
        UpdateEvent clone = new UpdateEvent( );
        clone.userId = userId;
        clone.removeObjects = (ArrayList<RefEntity<?>>) removeObjects.clone();
        clone.storeObjects = (ArrayList<RefEntity<?>>) storeObjects.clone();
        clone.removeSet = (HashMap<Object,RefEntity<?>>) removeSet.clone();
        clone.storeSet = (HashMap<Object,RefEntity<?>>) storeSet.clone();
        return clone;
    }

    public long getRepositoryVersion()
    {
        return repositoryVersion;
    }

    public void setRepositoryVersion( long repositoryVersion )
    {
        this.repositoryVersion = repositoryVersion;
    }
}
